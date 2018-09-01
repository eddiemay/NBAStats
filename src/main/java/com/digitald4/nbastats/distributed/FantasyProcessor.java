package com.digitald4.nbastats.distributed;

import com.digitald4.nbastats.distributed.Model.LineUp;
import com.digitald4.nbastats.distributed.Model.Player;
import com.digitald4.nbastats.distributed.Model.PlayerGroup;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.SparkSession;

public class FantasyProcessor {
	private static final int SALARY_CAP = 60000;
	private static final int LINEUP_LIMIT = 100;
	public static final String BACK_COURT_PATH = "target/backCourts-%s.csv";
	public static final String FRONT_COURT_PATH = "target/frontCourts-%s.csv";
	public static final String PLAYERS_PATH = "target/players-%s.csv";
	public static final String OUTPUT_PATH = "target/lineups-%s-%s.csv";
	private static final SimpleDateFormat COMPUTER_DATE = new SimpleDateFormat("yyyy-MM-dd");

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		String dateStr = (args.length > 0) ? args[0] : COMPUTER_DATE.format(new Date(startTime));
		System.out.println("Processing for date: " + dateStr);

		List<String> frontCourts = new ArrayList<>(40000000);
		BufferedReader br = new BufferedReader(new FileReader(String.format(FRONT_COURT_PATH, dateStr)));
		String line;
		while ((line = br.readLine()) != null) {
			frontCourts.add(line);
		}
		br.close();
		System.out.println("Front Courts: " + frontCourts.size());

		SparkSession spark = SparkSession.builder()
				.appName("Process FanDuel")
				.config("spark.default.parallelism", 256)
				.getOrCreate();
		JavaSparkContext javaCtx = new JavaSparkContext(spark.sparkContext());
		JavaRDD<String> frontCourtsRDD = javaCtx.parallelize(frontCourts);

		Map<Integer, Player> playerMap_ = new HashMap<>();
		br = new BufferedReader(new FileReader(String.format(PLAYERS_PATH, dateStr)));
		String[]  projectionMethods = br.readLine().split(",");
		while ((line = br.readLine()) != null) {
			Player player = new Player(line);
			playerMap_.put(player.playerId, player);
		}
		br.close();
		Broadcast<Map<Integer, Player>> playerMapBroadcast = javaCtx.broadcast(playerMap_);

		List<PlayerGroup> backCourts = new ArrayList<>();
		br = new BufferedReader(new FileReader(String.format(BACK_COURT_PATH, dateStr)));
		while ((line = br.readLine()) != null) {
			backCourts.add(new PlayerGroup(Arrays.stream(line.split(","))
					.map(id -> playerMap_.get(Integer.parseInt(id)))
					.toArray(Player[]::new)));
		}
		br.close();

		for (int m = 0; m < projectionMethods.length; m++) {
			int cm = m;
			backCourts.parallelStream().forEach(bc -> bc.setProjectionMethod(cm));
			backCourts.sort(Comparator.comparing(PlayerGroup::getProjection).reversed());
			Broadcast<List<PlayerGroup>> backCourtsBroadcast = javaCtx.broadcast(backCourts);

			List<LineUp> lineups = frontCourtsRDD
					.map(line_ -> {
						Map<Integer, Player> playerMap = playerMapBroadcast.getValue();
						PlayerGroup frontCourt = new PlayerGroup(Arrays.stream(line_.split(","))
								.map(id -> playerMap.get(Integer.parseInt(id)))
								.toArray(Player[]::new));

						List<LineUp> topLineUps = new ArrayList<>(LINEUP_LIMIT);
						int pm = backCourtsBroadcast.getValue().get(0).getProjectionMethod();
						final int frontCourtCost = frontCourt.cost;
						final int frontCourtProjection = frontCourt.projection[pm];
						int totalSalary;
						for (PlayerGroup backCourt : backCourtsBroadcast.getValue()) {
							totalSalary = backCourt.cost + frontCourtCost;
							if (totalSalary <= SALARY_CAP) {
								topLineUps.add(new LineUp(
										backCourt.projection[pm] + frontCourtProjection, totalSalary, backCourt, frontCourt));
								if (topLineUps.size() == LINEUP_LIMIT) {
									break;
								}
							}
						}
						return topLineUps;
					})
					.reduce((topLineupsA, topLineupsB) -> {
						List<LineUp> topLineupsC = new ArrayList<>(LINEUP_LIMIT);
						int aIndex = 0;
						int bIndex = 0;
						int aSize = topLineupsA.size();
						int bSize = topLineupsB.size();
						while (topLineupsC.size() < LINEUP_LIMIT && (aIndex < aSize || bIndex < bSize)) {
							if (bIndex < bSize && (aIndex == aSize
									|| topLineupsB.get(bIndex).projected > topLineupsA.get(aIndex).projected)) {
								topLineupsC.add(topLineupsB.get(bIndex++));
							} else {
								topLineupsC.add(topLineupsA.get(aIndex++));
							}
						}
						return topLineupsC;
					});
			String method = projectionMethods[m];
			FileWriter fileWriter = new FileWriter(String.format(OUTPUT_PATH, dateStr, method));
			System.out.println("\nTop lineUps for " + method);
			try {
				for (LineUp lineup : lineups) {
					fileWriter.write(lineup.toString(method) + "\n");
				}
			} catch (IOException ioe) {
				throw new RuntimeException("Error writing file", ioe);
			}
			fileWriter.close();
		}
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
	}
}
