package com.digitald4.nbastats.distributed.hadoop;

import com.digitald4.nbastats.distributed.Model.LineUp;
import com.digitald4.nbastats.distributed.Model.Player;
import com.digitald4.nbastats.distributed.Model.PlayerGroup;
import com.digitald4.nbastats.distributed.Model.TopLineUps;
import java.io.IOException;
import java.net.URI;
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
import java.util.stream.Collectors;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobPriority;
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
		private final String[] projectionMethods;
		private final Map<Integer, Player> playerMap = new HashMap<>();
		private final List<PlayerGroup> firstGroup = new ArrayList<>(100);
		private final PriorityQueue<PlayerGroup> secondGroup = new PriorityQueue<>(100000, Comparator.comparing(PlayerGroup::getCost));

		public FDMapper() {
			try {
				Configuration conf = new Configuration ();
				FileSystem fs = FileSystem.get(URI.create(DATA_PATH), conf);
				FSDataInputStream reader = fs.open(new Path(URI.create(DATA_PATH + "players.csv")));
				String line = reader.readLine();
				projectionMethods = line.split(",");
				while ((line = reader.readLine()) != null) {
					Player player = new Player(line);
					playerMap.put(player.playerId, player);
				}
				reader.close();

				reader = fs.open(new Path(URI.create(DATA_PATH + "first_group.csv")));
				while ((line = reader.readLine()) != null) {
					firstGroup.add(new PlayerGroup(Arrays.stream(line.split(","))
							.map(id -> playerMap.get(Integer.parseInt(id)))
							.toArray(Player[]::new)));
				}
				reader.close();

				reader = fs.open(new Path(URI.create(DATA_PATH + "second_group.csv")));
				while ((line = reader.readLine()) != null) {
					secondGroup.add(new PlayerGroup(Arrays.stream(line.split(","))
							.map(id -> playerMap.get(Integer.parseInt(id)))
							.toArray(Player[]::new)));
				}
				reader.close();
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
			Player c = playerMap.get(Integer.parseInt(ids[2]));
			PlayerGroup outerPlayers = new PlayerGroup(pg1, pg2, c);
			Map<String, TopLineUps> topLineUpsMap = new HashMap<>();
			firstGroup.forEach(sgPair -> {
				for (PlayerGroup forwardSet : secondGroup) {
					int totalSalary = outerPlayers.cost + sgPair.cost + forwardSet.cost;
					if (totalSalary > SALARY_CAP) {
						break;
					}
					for (int pm = 0; pm < projectionMethods.length; pm++) {
						String method = projectionMethods[pm];
						TopLineUps topLineUps = topLineUpsMap.computeIfAbsent(method, m -> new TopLineUps(LINEUP_LIMIT));
						int projected = outerPlayers.projection[pm] + sgPair.projection[pm] + forwardSet.projection[pm];
						if (topLineUps.size() < LINEUP_LIMIT || projected > topLineUps.peek().projected) {
							topLineUps.add(new LineUp(method, projected, totalSalary, outerPlayers, sgPair, forwardSet));
						}
					}
				}
			});
			topLineUpsMap.forEach((method, topLineUps) -> {
				for (LineUp lineUp : topLineUps) {
					StringBuilder sb = new StringBuilder(",").append(method).append(",").append(lineUp.totalSalary)
							.append(Arrays.stream(lineUp.playerIds)
									.mapToObj(String::valueOf)
									.collect(Collectors.joining(",")));
					try {
						context.write(new IntWritable(lineUp.projected), new Text(sb.toString()));
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			});
			// System.out.println("Finished: " + value + " " + ((System.currentTimeMillis() - sTime) / 1000.0) + " secs");
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

	public static boolean run(Date date) throws Exception {
		long startTime = System.currentTimeMillis();
		Configuration conf = new Configuration();
		conf.set("mapred.job.priority", JobPriority.VERY_HIGH.toString());
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
