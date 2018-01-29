package com.digitald4.nbastats.hadoop;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class ProcessFanDuel {
	private static final SimpleDateFormat COMPUTER_DATE = new SimpleDateFormat("yyyy-MM-dd");
	private static final int SALARY_CAP = 60000;
	private static final int LINEUP_LIMIT = 100;
	public static final String DATA_PATH = "input/fanduel/data/";
	public static final String PROCESS_PATH = "input/fanduel/process/";
	public static final String OUTPUT_PATH = "target/output/fanduel/%s/";

	public static class FDMapper extends Mapper<Object, Text, IntWritable, Text> {
		private static final String PLAYER_OUT = ",%d";
		private final String[] projectionMethods;
		private final Map<Integer, Player> playerMap = new HashMap<>();
		private final List<PlayerGroup> sgPairs = new ArrayList<>(100);
		private final PriorityQueue<PlayerGroup> frontCourts;

		public FDMapper() {
			try {
				List<PlayerGroup> sfPairs = new ArrayList<>(100);
				List<PlayerGroup> pfPairs = new ArrayList<>(100);
				List<Player> cs = new ArrayList<>(100);
				BufferedReader reader = new BufferedReader(new FileReader(DATA_PATH + "players.csv"));
				String line = reader.readLine();
				projectionMethods = line.split(",");
				while ((line = reader.readLine()) != null) {
					Player player = new Player(line);
					playerMap.put(player.playerId, player);
				}
				reader.close();
				reader = new BufferedReader(new FileReader(DATA_PATH + "sg_pairs.csv"));
				while ((line = reader.readLine()) != null) {
					String[] ids = line.split(",");
					sgPairs.add(new PlayerGroup(playerMap.get(Integer.parseInt(ids[0])), playerMap.get(Integer.parseInt(ids[1]))));
				}
				reader.close();
				reader = new BufferedReader(new FileReader(DATA_PATH + "sf_pairs.csv"));
				while ((line = reader.readLine()) != null) {
					String[] ids = line.split(",");
					sfPairs.add(new PlayerGroup(playerMap.get(Integer.parseInt(ids[0])), playerMap.get(Integer.parseInt(ids[1]))));
				}
				reader.close();
				reader = new BufferedReader(new FileReader(DATA_PATH + "pf_pairs.csv"));
				while ((line = reader.readLine()) != null) {
					String[] ids = line.split(",");
					pfPairs.add(new PlayerGroup(playerMap.get(Integer.parseInt(ids[0])), playerMap.get(Integer.parseInt(ids[1]))));
				}
				reader.close();
				reader = new BufferedReader(new FileReader(DATA_PATH + "centers.csv"));
				while ((line = reader.readLine()) != null) {
					cs.add(playerMap.get(Integer.parseInt(line)));
				}
				reader.close();
				frontCourts = new PriorityQueue<>(sfPairs.size() * pfPairs.size() * cs.size(),
						Comparator.comparing(PlayerGroup::getCost));
				for (PlayerGroup sfPair : sfPairs) {
					for (PlayerGroup pfPair : pfPairs) {
						for (Player c : cs) {
							frontCourts.add(
									new PlayerGroup(sfPair.players[0], sfPair.players[1], pfPair.players[0], pfPair.players[1], c));
						}
					}
				}
			} catch (Exception ioe) {
				ioe.printStackTrace();
				throw new RuntimeException(ioe);
			}
		}

		public void map(Object key, Text value, Context context) {
			long sTime = System.currentTimeMillis();
			// System.out.println("Processing: " + value);
			String[] ids = value.toString().split(",");
			Player pg1 = playerMap.get(Integer.parseInt(ids[0]));
			Player pg2 = playerMap.get(Integer.parseInt(ids[1]));
			Map<String, TopLineUps> topLineUpsMap = new HashMap<>();
			sgPairs.parallelStream().forEach(sgPair -> {
				Map<String, TopLineUps> subTopLineUpsMap = new HashMap<>();
				PlayerGroup backCourt = new PlayerGroup(pg1, pg2, sgPair.players[0], sgPair.players[1]);
				for (PlayerGroup frontCourt : frontCourts) {
					int totalSalary = backCourt.cost + frontCourt.cost;
					if (totalSalary > SALARY_CAP) {
						break;
					}
					for (int pm = 0; pm < projectionMethods.length; pm++) {
						String method = projectionMethods[pm];
						TopLineUps topLineUps = subTopLineUpsMap.computeIfAbsent(method, m -> new TopLineUps());
						int projected = backCourt.projection[pm] + frontCourt.projection[pm];
						if (topLineUps.size() < LINEUP_LIMIT || projected > topLineUps.peek().projected) {
							topLineUps.add(new LineUp(projected, totalSalary, pg1, pg2, sgPair.players[0], sgPair.players[1],
									frontCourt.players[0], frontCourt.players[1], frontCourt.players[2], frontCourt.players[3], frontCourt.players[4]));
						}
					}
				}
				synchronized (this) {
					subTopLineUpsMap.forEach((method, subTopLineUps) -> {
						TopLineUps topLineUps = topLineUpsMap.computeIfAbsent(method, m -> new TopLineUps());
						while (topLineUps.size() >= LINEUP_LIMIT && subTopLineUps.size() > 0 && subTopLineUps.peek().projected < topLineUps.peek().projected) {
							subTopLineUps.poll();
						}
						topLineUps.addAll(subTopLineUps);
						while (topLineUps.size() > LINEUP_LIMIT) {
							topLineUps.poll();
						}
					});
				}
			});
			topLineUpsMap.forEach((method, topLineUps) -> {
				for (LineUp lineUp : topLineUps) {
					StringBuilder sb = new StringBuilder(",").append(method).append(",").append(lineUp.totalSalary);
					Arrays.stream(lineUp.players).forEach(p -> sb.append(String.format(PLAYER_OUT, p.playerId)));
					try {
						context.write(new IntWritable(lineUp.projected), new Text(sb.toString()));
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			});
			System.out.println("Finished: " + value + " " + ((System.currentTimeMillis() - sTime) / 1000.0) + " secs");
		}
	}

	public static class LineUpReducer extends Reducer<IntWritable, Text, IntWritable, Text> {
		public void reduce(IntWritable projected, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			// TODO(eddiemay) Reduce to 100 best lineups.
			for (Text value : values) {
				context.write(projected, value);
			}
		}
	}
	private static class ReverseSort extends WritableComparator {
		protected ReverseSort() {
			super(IntWritable.class, true);
		}

		@Override
		public int compare(WritableComparable o1, WritableComparable o2) {
			return o2.compareTo(o1); // sort descending
		}
	}

	private static class TopLineUps extends PriorityQueue<LineUp> {
		private TopLineUps() {
			super(100, Comparator.comparing(LineUp::getProjected));
		}

		@Override
		public boolean add(LineUp lineUp) {
			boolean ret = super.add(lineUp);
			if (size() > LINEUP_LIMIT) {
				poll();
			}
			return ret;
		}
	}

	private static class Player {
		private final int playerId;
		private final int cost;
		private final int[] projection;

		private Player(String in) {
			String[] parts = in.split(",");
			this.playerId = Integer.parseInt(parts[0]);
			if (parts.length < 8) {
				System.out.println("Not enough data: " + in + " only " + parts.length + " parts");
				throw new RuntimeException("Not enough data: " + in + " only " + parts.length + " parts");
			}
			this.cost = Integer.parseInt(parts[1]);
			this.projection = new int[parts.length - 2];
			for (int p = 2; p < parts.length; p++) {
				projection[p - 2] = (int) Math.round(Double.parseDouble(parts[p]));
			}
		}
	}

	private static class PlayerGroup {
		private final Player[] players;
		private final int cost;
		private final int[] projection;

		private PlayerGroup(Player... players) {
			this.players = players;
			int cost = 0;
			projection = new int[players[0].projection.length];
			for (Player player : players) {
				cost += player.cost;
				for (int i = 0; i < projection.length; i++) {
					projection[i] += player.projection[i];
				}
			}
			this.cost = cost;
		}

		public int getCost() {
			return cost;
		}
	}

	private static class LineUp {
		private final int projected;
		private final int totalSalary;
		private final Player[] players;

		private LineUp(int projected, int totalSalary, Player... players) {
			this.projected = projected;
			this.totalSalary = totalSalary;
			this.players = players;
		}

		private int getProjected() {
			return projected;
		}
	}

	public static boolean run(Date date) throws Exception {
		long startTime = System.currentTimeMillis();
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Process Fan Duel");
		job.setJarByClass(FDMapper.class);
		job.setMapperClass(FDMapper.class);
		job.setCombinerClass(LineUpReducer.class);
		job.setReducerClass(LineUpReducer.class);
		job.setSortComparatorClass(ReverseSort.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(PROCESS_PATH));
		FileOutputFormat.setOutputPath(job, new Path(String.format(OUTPUT_PATH, COMPUTER_DATE.format(date))));
		boolean result = job.waitForCompletion(true);
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
		return result;
	}

	public static void main(String[] args) throws Exception {
		Date date = Calendar.getInstance().getTime();
		System.exit(run(date) ? 0 : 1);
	}
}
