package com.digitald4.nbastats.storage;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOSQLImpl;
import com.digitald4.common.tools.DataImporter;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Provider;
import com.digitald4.common.util.TopSet;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp.LineUpPlayer;
import com.digitald4.nbastats.proto.NBAStatsProtos.Position;
import com.digitald4.nbastats.storage.LineUpStore.FantasyLeague;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

	private static final int LINEUP_LIMIT = 100;

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
		private static final String PLAYER_OUT = "%s (%d),";

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			long sTime = System.currentTimeMillis();
			String[] ids = value.toString().split(",");
			LineUpPlayer pg = lineUpPlayerMap.get(Integer.parseInt(ids[0]));
			LineUpPlayer sg = lineUpPlayerMap.get(Integer.parseInt(ids[1]));
			sfs.parallelStream().forEach(sf -> {
				if (sf == sg || sf == pg) return;
				TopLineUps topLineUps = new TopLineUps();
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
										double projected = pg.getProjected() + sg.getProjected() + sf.getProjected() + pf.getProjected()
												+ c.getProjected() + g.getProjected() + f.getProjected() + util.getProjected();
										topLineUps.add(LineUp.newBuilder()
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
			super(LINEUP_LIMIT, Comparator.comparing(LineUp::getProjected).reversed().thenComparing(LineUp::getHashId));
		}
	}

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		DAO dao = new DAOSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
				"jdbc:mysql://localhost/NBAStats?autoReconnect=true",
				"dd4_user", "getSchooled85"));
		Provider<DAO> daoProvider = () -> dao;
		APIDAO apiDAO = new APIDAO(new DataImporter(dao, null));
		PlayerDayStore playerDayStore = new PlayerDayStore(daoProvider, apiDAO,
				new PlayerStore(daoProvider, apiDAO));
		//LineUpStore lineUpStore = new LineUpStore(daoProvider, playerDayStore);
		String leauge = FantasyLeague.DRAFT_KINGS.name;
		lineUpPlayerMap = playerDayStore.processStats(DateTime.parse("2017-12-14"))
				.stream()
				.filter(playerDay -> playerDay.getStatsMap().get("Projection") != null)
				.filter(playerDay -> playerDay.getStatsMap().get("Projection").getFantasySitePointsMap().get(leauge) != null)
				.map(playerDay -> LineUpPlayer.newBuilder()
						.setPlayerId(playerDay.getPlayerId())
						.setName(playerDay.getName())
						.addAllPosition(playerDay.getFantasySiteInfoOrThrow(leauge).getPositionList())
						.setCost(playerDay.getFantasySiteInfoOrThrow(leauge).getCost())
						.setProjected(playerDay.getStatsOrThrow("Projection").getFantasySitePointsOrThrow(leauge))
						.build())
				.collect(Collectors.toMap(LineUpPlayer::getPlayerId, Function.identity()));
		lineUpPlayerMap.values().stream().sorted(Comparator.comparing(LineUpPlayer::getProjected).reversed()).forEach(player -> {
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

		FileWriter fw = new FileWriter("input/pgs_sgs_sfs.txt");
		int count = 0;
		for (LineUpPlayer pg : pgs) {
			for (LineUpPlayer sg : sgs) {
				if (sg == pg) continue;
				if (count++ < 6) {
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
		FileInputFormat.addInputPath(job, new Path("input/"));
		FileOutputFormat.setOutputPath(job, new Path("target/output/draftkings/" + FormatText.formatDate(DateTime.now(), FormatText.MYSQL_DATETIME)));
		int result = job.waitForCompletion(true) ? 0 : 1;
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
		System.exit(result);
	}
}
