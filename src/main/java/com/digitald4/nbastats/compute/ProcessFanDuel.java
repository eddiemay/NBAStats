package com.digitald4.nbastats.compute;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
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
import org.joda.time.format.DateTimeFormat;

public class ProcessFanDuel {
	private static final int SALARY_CAP = 60000;
	private static final int LINEUP_LIMIT = 100;
	public static final String DATA_PATH = "input/fanduel/data/";
	public static final String PROCESS_PATH = "input/fanduel/process/";
	public static final String OUTPUT_PATH = "target/output/fanduel/%s/";

	public static class FDMapper extends Mapper<Object, Text, DoubleWritable, Text> {
		private static final String PLAYER_OUT = ",%d";
		private final String[] projectionMethods;
		private final List<PlayerPair> sgPairs = new ArrayList<>(100);
		private final List<PlayerPair> sfPairs = new ArrayList<>(100);
		private final List<PlayerPair> pfPairs = new ArrayList<>(100);
		private final List<LineUpPlayer> cs = new ArrayList<>(100);

		public FDMapper() {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(DATA_PATH + "sg_pairs.txt"));
				String line = reader.readLine();
				projectionMethods = line.split(",");
				while ((line = reader.readLine()) != null) {
					sgPairs.add(new PlayerPair(line));
				}
				reader.close();
				reader = new BufferedReader(new FileReader(DATA_PATH + "sf_pairs.txt"));
				while ((line = reader.readLine()) != null) {
					sfPairs.add(new PlayerPair(line));
				}
				reader.close();
				reader = new BufferedReader(new FileReader(DATA_PATH + "pf_pairs.txt"));
				while ((line = reader.readLine()) != null) {
					pfPairs.add(new PlayerPair(line));
				}
				reader.close();
				reader = new BufferedReader(new FileReader(DATA_PATH + "centers.txt"));
				while ((line = reader.readLine()) != null) {
					cs.add(new LineUpPlayer(line));
				}
				reader.close();
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			long sTime = System.currentTimeMillis();
			PlayerPair pgPair = new PlayerPair(value.toString());
			LineUpPlayer pg1 = pgPair.one;
			LineUpPlayer pg2 = pgPair.two;
			Map<String, TopLineUps> topLineUpsMap = new HashMap<>();
			sgPairs.parallelStream().forEach(sgPair -> {
				Map<String, TopLineUps> subTopLineUpsMap = new HashMap<>();
				LineUpPlayer sg1 = sgPair.one;
				LineUpPlayer sg2 = sgPair.two;
				for (PlayerPair sfPair : sfPairs) {
					LineUpPlayer sf1 = sfPair.one;
					LineUpPlayer sf2 = sfPair.two;
					for (PlayerPair pfPair : pfPairs) {
						LineUpPlayer pf1 = pfPair.one;
						LineUpPlayer pf2 = pfPair.two;
						for (LineUpPlayer c : cs) {
							int totalSalary = pg1.cost + pg2.cost + sg1.cost + sg2.cost
									+ sf1.cost + sf2.cost + pf1.cost + pf2.cost + c.cost;
							if (totalSalary <= SALARY_CAP) {
								for (int pm = 0; pm < projectionMethods.length; pm++) {
									String method = projectionMethods[pm];
									TopLineUps topLineUps = subTopLineUpsMap.computeIfAbsent(method, m -> new TopLineUps());
									double projected = pg1.projection[pm] + pg2.projection[pm] + sg1.projection[pm] + sg2.projection[pm]
											+ sf1.projection[pm] + sf2.projection[pm] + pf1.projection[pm] + pf2.projection[pm] + c.projection[pm];
									if (topLineUps.size() < LINEUP_LIMIT || projected > topLineUps.last().projected) {
										topLineUps.add(new LineUp(projected, totalSalary, pg1, pg2, sg1, sg2, sf1, sf2, pf1, pf2, c));
									}
								}
							}
						}
					}
				}
				synchronized (this) {
					subTopLineUpsMap.forEach((method, subTopLineUps) -> {
						TopLineUps topLineUps = topLineUpsMap.computeIfAbsent(method, m -> new TopLineUps());
						Iterator<LineUp> iterator = subTopLineUps.iterator();
						if (iterator.hasNext()) {
							LineUp lineUp = iterator.next();
							while (lineUp != null && (topLineUps.size() < LINEUP_LIMIT || lineUp.projected > topLineUps.last().projected)) {
								topLineUps.add(lineUp);
								lineUp = iterator.hasNext() ? iterator.next() : null;
							}
						}
					});
				}
			});
			topLineUpsMap.forEach((method, topLineUps) -> {
				for (LineUp lineUp : topLineUps) {
					StringBuilder sb = new StringBuilder(",").append(method).append(",").append(lineUp.totalSalary);
					Arrays.stream(lineUp.players).forEach(p -> sb.append(String.format(PLAYER_OUT, p.playerId)));
					try {
						context.write(new DoubleWritable(lineUp.projected), new Text(sb.toString()));
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
			// TODO(eddiemay) Reduce to 100 best lineups.
			for (Text value : values) {
				context.write(projected, value);
			}
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

	private static class TopLineUps extends TreeSet<LineUp> {
		private TopLineUps() {
			super(Comparator.comparing(LineUp::getProjected).reversed());
		}

		@Override
		public boolean add(LineUp lineUp) {
			boolean ret = super.add(lineUp);
			if (size() > LINEUP_LIMIT) {
				remove(last());
			}
			return ret;
		}
	}

	private static class LineUpPlayer {
		private final int playerId;
		private final int cost;
		private final double[] projection;

		private LineUpPlayer(String in) {
			String[] parts = in.split(",");
			this.playerId = Integer.parseInt(parts[0]);
			this.cost = Integer.parseInt(parts[1]);
			this.projection = new double[parts.length - 2];
			for (int p = 2; p < parts.length; p++) {
				projection[p - 2] = Double.parseDouble(parts[p]);
			}
		}
	}

	private static class PlayerPair {
		private final LineUpPlayer one;
		private final LineUpPlayer two;

		private PlayerPair(String in) {
			String[] parts = in.split("\\|");
			one = new LineUpPlayer(parts[0]);
			two = new LineUpPlayer(parts[1]);
		}
	}

	private static class LineUp {
		private final double projected;
		private final int totalSalary;
		private final LineUpPlayer[] players;

		private LineUp(double projected, int totalSalary, LineUpPlayer... players) {
			this.projected = projected;
			this.totalSalary = totalSalary;
			this.players = players;
		}

		private double getProjected() {
			return projected;
		}
	}

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Process Fan Duel");
		job.setJarByClass(FDMapper.class);
		job.setMapperClass(FDMapper.class);
		job.setCombinerClass(LineUpReducer.class);
		job.setReducerClass(LineUpReducer.class);
		job.setSortComparatorClass(ReverseSort.class);
		job.setOutputKeyClass(DoubleWritable.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(PROCESS_PATH));
		DateTime date = DateTime.now();
		FileOutputFormat.setOutputPath(job, new Path(String.format(OUTPUT_PATH, date.toString(DateTimeFormat.forPattern("yyyy-MM-dd")))));
		boolean result = job.waitForCompletion(true);
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
		System.exit(result ? 0 : 1);
	}
}
