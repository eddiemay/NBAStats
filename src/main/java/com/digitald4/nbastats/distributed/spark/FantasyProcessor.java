package com.digitald4.nbastats.distributed.spark;

import com.digitald4.nbastats.distributed.Model.LineUp;
import com.digitald4.nbastats.distributed.Model.Player;
import com.digitald4.nbastats.distributed.Model.PlayerGroup;
import com.digitald4.nbastats.distributed.Model.TopLineUps;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.SparkSession;

public class FantasyProcessor {
	private static final int SALARY_CAP = 60000;
	private static final int LINEUP_LIMIT = 100;
	private static final String DATA_PATH = "input/fanduel/data/";

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();

		// Should be some file on your system
		String logFile = "input/fanduel/process/outer.csv";
		SparkSession spark = SparkSession.builder()
				.appName("Process FanDuel")
				// .config("spark.default.parallelism", 100)
				.getOrCreate();
		JavaSparkContext javaCtx = new JavaSparkContext(spark.sparkContext());
		Map<Integer, Player> playerMap_ = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(DATA_PATH + "players.csv"));
		String line = br.readLine();
		Broadcast<String[]>  projectionMethodsBroadcast = javaCtx.broadcast(line.split(","));
		while ((line = br.readLine()) != null) {
			Player player = new Player(line);
			playerMap_.put(player.playerId, player);
		}
		br.close();

		List<PlayerGroup> aGroups = new ArrayList<>(10000);
		br = new BufferedReader(new FileReader(logFile));
		while ((line = br.readLine()) != null) {
			aGroups.add(new PlayerGroup(Arrays.stream(line.split(","))
					.map(id -> playerMap_.get(Integer.parseInt(id)))
					.toArray(Player[]::new)));
		}
		br.close();
		JavaRDD<PlayerGroup> aGroupsRDD = javaCtx.parallelize(aGroups);

		List<PlayerGroup> bGroups = new ArrayList<>(1000);
		br = new BufferedReader(new FileReader(DATA_PATH + "first_group.csv"));
		while ((line = br.readLine()) != null) {
			bGroups.add(new PlayerGroup(Arrays.stream(line.split(","))
					.map(id -> playerMap_.get(Integer.parseInt(id)))
					.toArray(Player[]::new)));
		}
		br.close();
		Broadcast<List<PlayerGroup>> bGroupsBroadcast = javaCtx.broadcast(bGroups);

		PriorityQueue<PlayerGroup> cGroups = new PriorityQueue<>(100000, Comparator.comparing(PlayerGroup::getCost));
		br = new BufferedReader(new FileReader(DATA_PATH + "second_group.csv"));
		while ((line = br.readLine()) != null) {
			cGroups.add(new PlayerGroup(Arrays.stream(line.split(","))
					.map(id -> playerMap_.get(Integer.parseInt(id)))
					.toArray(Player[]::new)));
		}
		br.close();
		PlayerGroup[] cGroupsArray = cGroups.toArray(new PlayerGroup[cGroups.size()]);
		Arrays.sort(cGroupsArray, Comparator.comparing(PlayerGroup::getCost));
		Broadcast<PlayerGroup[]> cGroupsBroadcast = javaCtx.broadcast(cGroupsArray);
		System.out.println("Setup Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");

		System.out.println("Number of lines: " + aGroupsRDD.count());
		Map<String, TopLineUps> lineups = aGroupsRDD
				.map(aGroup -> {
					String[] projectionMethods = projectionMethodsBroadcast.getValue();
					//long sTime = System.currentTimeMillis();
					// System.out.println("Processing: " + value);
					Map<String, TopLineUps> topLineUpsMap = new HashMap<>();
					PlayerGroup[] cGroups_ = cGroupsBroadcast.getValue();
					bGroupsBroadcast.getValue().forEach(bGroup -> {
						for (PlayerGroup cGroup : cGroups_) {
							int totalSalary = aGroup.cost + bGroup.cost + cGroup.cost;
							if (totalSalary > SALARY_CAP) {
								break;
							}
							for (int pm = 0; pm < projectionMethods.length; pm++) {
								String method = projectionMethods[pm];
								TopLineUps topLineUps = topLineUpsMap.computeIfAbsent(method, m -> new TopLineUps(LINEUP_LIMIT));
								int projected = aGroup.projection[pm] + bGroup.projection[pm] + cGroup.projection[pm];
								if (topLineUps.size() < LINEUP_LIMIT || projected > topLineUps.peek().projected) {
									topLineUps.add(new LineUp(method, projected, totalSalary, aGroup, bGroup, cGroup));
								}
							}
						}
					});
					//System.out.println("Finished: " + value + " " + ((System.currentTimeMillis() - sTime) / 1000.0) + " secs");
					return topLineUpsMap;
				})
				.reduce((topLineupsMapA, topLineupsMapB) -> {
					topLineupsMapA.forEach((method, topLineUps) -> topLineUps.addSorted(topLineupsMapB.get(method)));
					return topLineupsMapA;
				});
		FileWriter fileWriter = new FileWriter("output.csv");
		lineups.forEach((method, topLineUps) -> {
			System.out.println("\nTop lineUps for " + method);
			try {
				for (LineUp lineup : topLineUps) {
					fileWriter.write(lineup + "\n");
				}
			} catch (IOException ioe) {
				throw new RuntimeException("Error writing file");
			}
		});
		fileWriter.close();
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
	}
}
