package com.digitald4.nbastats.compute;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOCloudDS;
import com.digitald4.common.storage.DAOSQLImpl;
import com.digitald4.common.tools.DataImporter;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Provider;
import com.digitald4.common.util.TopSet;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp.LineUpPlayer;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay.FantasySiteInfo;
import com.digitald4.nbastats.proto.NBAStatsProtos.Position;
import com.digitald4.nbastats.storage.APIDAO;
import com.digitald4.nbastats.storage.GameLogStore;
import com.digitald4.nbastats.util.DistinictSalaryList;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import com.digitald4.nbastats.util.Constaints;
import com.digitald4.nbastats.util.Constaints.FantasyLeague;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.joda.time.DateTime;

public class ProcessDraftKings {
	private static Map<Integer, LineUpPlayer> lineUpPlayerMap;
	private static final List<LineUpPlayer> pgs = new DistinictSalaryList(2);
	private static final List<LineUpPlayer> sgs = new DistinictSalaryList(2);
	private static final List<LineUpPlayer> sfs = new DistinictSalaryList(2);
	private static final List<LineUpPlayer> pfs = new DistinictSalaryList(2);
	private static final List<LineUpPlayer> cs = new DistinictSalaryList(2);
	private static final List<LineUpPlayer> gs = new DistinictSalaryList(3);
	private static final List<LineUpPlayer> fs = new DistinictSalaryList(3);
	private static final List<LineUpPlayer> utils = new DistinictSalaryList(4);

	public static class DKMapper extends Mapper<Object, Text, DoubleWritable, Text> {
		private static final String PLAYER_OUT = ",%s (%d)";

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			long sTime = System.currentTimeMillis();
			String[] ids = value.toString().split(",");
			LineUpPlayer pg = lineUpPlayerMap.get(Integer.parseInt(ids[0]));
			LineUpPlayer sg = lineUpPlayerMap.get(Integer.parseInt(ids[1]));
			Set<String> projectionMethods = pg.getProjectionMap().keySet();
			sfs.parallelStream().forEach(sf -> {
				if (sf == sg || sf == pg) return;
				Map<String, TopLineUps> topLineUpsMap = new HashMap<>();
				for (LineUpPlayer pf : pfs) {
					if (pf == sf) continue;
					for (LineUpPlayer c : cs) {
						if (c == pf) continue;
						for (LineUpPlayer g : gs) {
							if (g == pg || g == sg || g == sf) continue;
							for (LineUpPlayer f : fs) {
								if (f == sf || f == pf || f == c || f == g || f == pg || f == sg) continue;
								for (LineUpPlayer util : utils) {
									if (util == pg || util == sg || util == sf || util == pf || util == c || util == g || util == f)
										continue;
									int totalSalary = pg.getCost() + sg.getCost() + sf.getCost() + pf.getCost() + c.getCost()
											+ g.getCost() + f.getCost() + util.getCost();
									if (totalSalary <= FantasyLeague.DRAFT_KINGS.salaryCap) {
										for (String pm : projectionMethods) {
											double projected = pg.getProjectionMap().get(pm) + sg.getProjectionMap().get(pm)
													+ sf.getProjectionMap().get(pm) + pf.getProjectionMap().get(pm) + c.getProjectionMap().get(pm)
													+ g.getProjectionMap().get(pm) + f.getProjectionMap().get(pm) + util.getProjectionMap().get(pm);
											topLineUpsMap.get(pm).add(LineUp.newBuilder()
													.setProjectionMethod(pm)
													.setProjected(projected)
													.setTotalSalary(totalSalary)
													.addPlayer(pg).addPlayer(sg).addPlayer(sf).addPlayer(pf).addPlayer(c)
													.addPlayer(g).addPlayer(f).addPlayer(util)
													.build());
										}
									}
								}
							}
						}
					}
				}
				for (TopLineUps topLineUps : topLineUpsMap.values()) {
					for (LineUp lineUp : topLineUps) {
						StringBuilder sb = new StringBuilder();
						lineUp.getPlayerList().forEach(p -> sb.append(String.format(PLAYER_OUT, p.getName(), p.getPlayerId())));
						try {
							context.write(new DoubleWritable(lineUp.getProjected()), new Text(sb.toString()));
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}
				}
			});
			System.out.println("Finished: " + value + " " + ((System.currentTimeMillis() - sTime) / 1000) + " secs");
		}
	}

	public static class LineUpReducer extends Reducer<DoubleWritable, Text, DoubleWritable, Text> {
		public void reduce(DoubleWritable projected, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			// Only need 1 instance for any given lineup.
			// TODO(eddiemay) Make sure linues are indeed the same.
			context.write(projected, values.iterator().next());
		}
	}
	private static class ReverseSort extends WritableComparator {
		protected ReverseSort() {
			super(DoubleWritable.class, true);
		}

		@Override
		public int compare(WritableComparable o1, WritableComparable o2) {
			return o2.compareTo(o1); // sort descending
		}
	}

	private static class TopLineUps extends TopSet<LineUp> {
		private TopLineUps() {
			super(Constaints.LINEUP_LIMIT, Comparator.comparing(LineUp::getProjected).reversed());
		}
	}

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		System.out.println("CA Lottery Combinations: " + (Calculate.combinations(70, 5) * 25));
		String dataStore = "cloud";
		for (int a = 0; a < args.length; a++) {
			if (args[a].equals("--datastore")) {
				dataStore = args[++a];
			}
		}
		DAO dao = dataStore.equals("cloud") ? new DAOCloudDS()
				: new DAOSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
				"jdbc:mysql://localhost/NBAStats?autoReconnect=true",
				"dd4_user", "getSchooled85"));
		Provider<DAO> daoProvider = () -> dao;
		APIDAO apiDAO = new APIDAO(new DataImporter(null, null));
		PlayerStore playerStore = new PlayerStore(daoProvider, apiDAO);
		GameLogStore gameLogStore = new GameLogStore(daoProvider, apiDAO);
		PlayerDayStore playerDayStore = new PlayerDayStore(daoProvider, apiDAO);
		StatsProcessor statsProcessor = new StatsProcessor(playerStore, gameLogStore, playerDayStore);
		//LineUpStore lineUpStore = new LineUpStore(daoProvider, playerDayStore);
		String league = FantasyLeague.DRAFT_KINGS.name;
		lineUpPlayerMap = statsProcessor.processStats(DateTime.parse("2017-12-19"))
				.stream()
				.filter(playerDay -> playerDay.getFantasySiteInfoOrDefault(league, FantasySiteInfo.getDefaultInstance()).getProjectionCount() != 0)
				.map(playerDay -> LineUpPlayer.newBuilder()
						.setPlayerId(playerDay.getPlayerId())
						.setName(playerDay.getName())
						.addAllPosition(playerDay.getFantasySiteInfoOrThrow(league).getPositionList())
						.setCost(playerDay.getFantasySiteInfoOrThrow(league).getCost())
						.putAllProjection(playerDay.getFantasySiteInfoOrThrow(league).getProjectionMap())
						.build())
				.collect(Collectors.toMap(LineUpPlayer::getPlayerId, Function.identity()));
		lineUpPlayerMap.values().stream().sorted(Comparator.comparing(LineUpPlayer::getCost).reversed()).forEach(player -> {
			if (player.getPositionList().contains(Position.PG) || player.getPositionList().contains(Position.SG)) {
				gs.add(player);
				if (player.getPositionList().contains(Position.PG)) pgs.add(player);
				if (player.getPositionList().contains(Position.SG)) sgs.add(player);
			}
			if (player.getPositionList().contains(Position.SF) || player.getPositionList().contains(Position.PF)) {
				fs.add(player);
				if (player.getPositionList().contains(Position.SF)) sfs.add(player);
				if (player.getPositionList().contains(Position.PF)) pfs.add(player);
			}
			if (player.getPositionList().contains(Position.C)) cs.add(player);
			utils.add(player);
		});
		System.out.println("Utils: " + utils.size());
		System.out.println("  Gs: " + gs.size());
		System.out.println("    PGs: " + pgs.size());
		System.out.println("    SGs: " + sgs.size());
		System.out.println("  Fs: " + fs.size());
		System.out.println("    SFs: " + sfs.size());
		System.out.println("    PFs: " + pfs.size());
		System.out.println("  Cs: " + cs.size());
		System.out.println("Iterations: " + FormatText.formatCurrency(((long) pgs.size()) * sgs.size()
				* sfs.size() * pfs.size() * cs.size() * gs.size() * fs.size() * utils.size()));
		System.out.println("Combinations: " + FormatText.formatCurrency(Calculate.combinations(utils.size(), 8)));

		FileWriter fw = new FileWriter("input/draftkings/pgs_sgs.txt");
		int count = 0;
		for (LineUpPlayer pg : pgs) {
			for (LineUpPlayer sg : sgs) {
				if (sg == pg) continue;
				if (count++ < 300) {
					fw.write(pg.getPlayerId() + "," + sg.getPlayerId() + "\n");
				}
			}
		}
		System.out.println("Total count = " + count);
		fw.close();
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Process Draft Kings");
		job.setJarByClass(DKMapper.class);
		job.setMapperClass(DKMapper.class);
		job.setCombinerClass(LineUpReducer.class);
		job.setReducerClass(LineUpReducer.class);
		job.setSortComparatorClass(ReverseSort.class);
		job.setOutputKeyClass(DoubleWritable.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path("input/draftkings/"));
		FileOutputFormat.setOutputPath(job, new Path("target/output/draftkings/" + FormatText.formatDate(DateTime.now(), FormatText.MYSQL_DATETIME)));
		int result = job.waitForCompletion(true) ? 0 : 1;
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
		System.exit(result);
	}
}
