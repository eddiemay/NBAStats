package com.digitald4.nbastats.storage;

import static com.digitald4.common.util.Calculate.standardDeviation;
import static java.lang.String.format;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.tools.DataImporter;
import com.digitald4.common.util.Calculate;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay.FantasySiteInfo;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay.Stats;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import com.digitald4.nbastats.proto.NBAStatsProtos.Position;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;

public class APIDAO {
	private static final String API_DATE_FORMAT = "MM/dd/yyyy";

	private static final String ROTO_GRINDER =
			"https://rotogrinders.com/projected-stats/nba-player.csv?site=%s";
	private static final String COMMON_ALL_PLAYERS =
			"http://stats.nba.com/stats/commonallplayers/?Season=%s&LeagueID=00&IsOnlyCurrentSeason=1";
	private static final String PLAYER_GAMELOG =
			"http://stats.nba.com/stats/playergamelog/?PlayerID=%d&Season=%s&DateTo=%s&SeasonType=Regular Season";
	private static final int SAMPLE_SIZE = 30;
	// Sets the data at -10% from the center which is the 40th percentile, 80% of mean. Giving a 60% chance of hitting the number.
	private static final double Z_SCORE = -.25;

	private final DataImporter importer;
	public APIDAO(DataImporter importer) {
		this.importer = importer;
	}

	public PlayerDay fillStats(PlayerDay playerDay) {
		try {
			return fillStats(playerDay.toBuilder()).build();
		} catch (IOException ioe) {
			throw new DD4StorageException("Error Reading Player", ioe);
		}
	}

	private double round(double n) {
		return Calculate.round(n, 3);
	}

	private PlayerDay.Builder fillStats(PlayerDay.Builder player) throws IOException {
		DateTime date = DateTime.parse(player.getAsOfDate(), DateTimeFormat.forPattern(API_DATE_FORMAT));
		String dateTo = date.minusDays(1).toString(API_DATE_FORMAT);
		List<Stats> games = addGames(player, SAMPLE_SIZE, getSeason(date), dateTo);
		if (games.size() < SAMPLE_SIZE) {
			games.addAll(addGames(player, SAMPLE_SIZE - games.size(), getPrevSeason(date), dateTo));
		}
		if (games.size() > 0) {
			int sampleSize = games.size();
			double[][] matrix = new double[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER + 2][sampleSize];
			double[] totals = new double[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER + 2];
			int g = 0;
			for (Stats game : games) {
				totals[Stats.POINTS_FIELD_NUMBER] += matrix[Stats.POINTS_FIELD_NUMBER][g] = game.getPoints();
				totals[Stats.MADE3S_FIELD_NUMBER] += matrix[Stats.MADE3S_FIELD_NUMBER][g] = game.getMade3S();
				totals[Stats.REBOUNDS_FIELD_NUMBER] += matrix[Stats.REBOUNDS_FIELD_NUMBER][g] = game.getRebounds();
				totals[Stats.ASSISTS_FIELD_NUMBER] += matrix[Stats.ASSISTS_FIELD_NUMBER][g] = game.getAssists();
				totals[Stats.STEALS_FIELD_NUMBER] += matrix[Stats.STEALS_FIELD_NUMBER][g] = game.getSteals();
				totals[Stats.BLOCKS_FIELD_NUMBER] += matrix[Stats.BLOCKS_FIELD_NUMBER][g] = game.getBlocks();
				totals[Stats.TURNOVERS_FIELD_NUMBER] += matrix[Stats.TURNOVERS_FIELD_NUMBER][g] = game.getTurnovers();
				totals[Stats.DOUBLE_DOUBLES_FIELD_NUMBER] += matrix[Stats.DOUBLE_DOUBLES_FIELD_NUMBER][g] = game.getDoubleDoubles();
				totals[Stats.TRIPLE_DOUBLES_FIELD_NUMBER] += matrix[Stats.TRIPLE_DOUBLES_FIELD_NUMBER][g] = game.getTripleDoubles();
				totals[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER] += matrix[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER][g] = game.getFantasySitePointsOrDefault("draftkings", 0);
				totals[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER + 1] += matrix[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER + 1][g] = game.getFantasySitePointsOrDefault("fanduel", 0);
				g++;
			}
			Stats sampleGameAvgs = Stats.newBuilder()
					.setPoints(totals[Stats.POINTS_FIELD_NUMBER] / sampleSize)
					.setMade3S(totals[Stats.MADE3S_FIELD_NUMBER] / sampleSize)
					.setRebounds(totals[Stats.REBOUNDS_FIELD_NUMBER] / sampleSize)
					.setAssists(totals[Stats.ASSISTS_FIELD_NUMBER] / sampleSize)
					.setSteals(totals[Stats.STEALS_FIELD_NUMBER] / sampleSize)
					.setBlocks(totals[Stats.BLOCKS_FIELD_NUMBER] / sampleSize)
					.setTurnovers(totals[Stats.TURNOVERS_FIELD_NUMBER] / sampleSize)
					.setDoubleDoubles(totals[Stats.DOUBLE_DOUBLES_FIELD_NUMBER] / sampleSize)
					.setTripleDoubles(totals[Stats.TRIPLE_DOUBLES_FIELD_NUMBER] / sampleSize)
					.putFantasySitePoints("draftkings", totals[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER] / sampleSize)
					.putFantasySitePoints("fanduel", totals[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER + 1] / sampleSize)
					.build();
			player.putStats("Sample Games Avg", sampleGameAvgs);

			if (sampleSize == SAMPLE_SIZE) {
				player.putStats("Projection", Stats.newBuilder()
						.setPoints(round(standardDeviation(matrix[Stats.POINTS_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getPoints()))
						.setMade3S(round(standardDeviation(matrix[Stats.MADE3S_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getMade3S()))
						.setRebounds(round(standardDeviation(matrix[Stats.REBOUNDS_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getRebounds()))
						.setAssists(round(standardDeviation(matrix[Stats.ASSISTS_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getAssists()))
						.setSteals(round(standardDeviation(matrix[Stats.STEALS_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getSteals()))
						.setBlocks(round(standardDeviation(matrix[Stats.BLOCKS_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getBlocks()))
						.setTurnovers(round(standardDeviation(matrix[Stats.TURNOVERS_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getTurnovers()))
						.setDoubleDoubles(round(standardDeviation(matrix[Stats.DOUBLE_DOUBLES_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getDoubleDoubles()))
						.setTripleDoubles(round(standardDeviation(matrix[Stats.TRIPLE_DOUBLES_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getTripleDoubles()))
						.putFantasySitePoints("draftkings", round(standardDeviation(matrix[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER]) * Z_SCORE + sampleGameAvgs.getFantasySitePointsOrDefault("draftkings", 0)))
						.putFantasySitePoints("fanduel", round(standardDeviation(matrix[Stats.FANTASY_SITE_POINTS_FIELD_NUMBER + 1]) * Z_SCORE + sampleGameAvgs.getFantasySitePointsOrDefault("fanduel", 0)))
						.build());
			} else {
				System.err.println(String.format("Not enough data for: %d - %s (%d)", player.getPlayerId(), player.getName(), games.size()));
			}
		}
		return player;
	}

	private List<Stats> addGames(PlayerDay.Builder player, int count, String season, String dateTo) throws IOException {
		List<Stats> games = new ArrayList<>();
		JSONObject json = new JSONObject(importer.sendGet(format(PLAYER_GAMELOG, player.getPlayerId(), season, dateTo)));
		JSONArray resultSets = json.getJSONArray("resultSets");
		for (int x = 0; x < resultSets.length(); x++) {
			JSONObject resultSet = resultSets.getJSONObject(x);
			if (resultSet.get("name").equals("PlayerGameLog")) {
				JSONArray rowSets = resultSet.getJSONArray("rowSet");
				for (int i = 0; i < rowSets.length() && games.size() < count; i++) {
					JSONArray rowSet = rowSets.getJSONArray(i);
					Stats game = fillFantasy(fillMultiples(Stats.newBuilder()
							.setPoints(rowSet.getDouble(24))
							.setMade3S(rowSet.getDouble(10))
							.setRebounds(rowSet.getDouble(18))
							.setAssists(rowSet.getDouble(19))
							.setSteals(rowSet.getDouble(20))
							.setBlocks(rowSet.getDouble(21))
							.setTurnovers(rowSet.getDouble(22))));
					games.add(game);
					player.putStats(rowSet.getString(3) + " " + rowSet.getString(4), game);
				}
			}
		}
		try {
			Thread.sleep(500); // Wait 1/2 a second to try and stop API from dectecting automated code.
		} catch (Exception e) {
			e.printStackTrace();
		}
		return games;
	}

	private static String getSeason(DateTime date) {
		int startYear = date.getYear() - (date.getMonthOfYear() < 7 ?  1 : 0);
		return startYear + "-" + ((startYear + 1) % 100);
	}

	private static String getPrevSeason(DateTime date) {
		int startYear = date.getYear() - (date.getMonthOfYear() < 7 ?  2 : 1);
		return startYear + "-" + ((startYear + 1) % 100);
	}

	private static int getDoublesCount(double... options) {
		return (int) Arrays.stream(options)
				.filter(option -> option >= 10)
				.count();
	}

	private static Stats.Builder fillMultiples(Stats.Builder stats) {
		int doubles = getDoublesCount(
				stats.getPoints(), stats.getRebounds(), stats.getAssists(), stats.getBlocks(), stats.getSteals());
		return stats.setDoubleDoubles(doubles == 2 || doubles > 3 ? 1 : 0)
				.setTripleDoubles(doubles >= 3 ? 1 : 0);
	}

	private static Stats fillFantasy(Stats.Builder stats) {
		return stats
				.putFantasySitePoints("draftkings", stats.getPoints()
						+ stats.getMade3S() * .5
						+ stats.getRebounds() * 1.25
						+ stats.getAssists() * 1.5
						+ stats.getSteals() * 2
						+ stats.getBlocks() * 2
						+ stats.getTurnovers() * -.5
						+ stats.getDoubleDoubles() * 1.5
						+ stats.getTripleDoubles() * 3)
				.putFantasySitePoints("fanduel", stats.getPoints()
						+ stats.getRebounds() * 1.2
						+ stats.getAssists() * 1.5
						+ stats.getBlocks() * 3
						+ stats.getSteals() * 3
						- stats.getTurnovers())
				.build();
	}

	public List<Player> listAllPlayers() {
		return listAllPlayers(getSeason(DateTime.now()));
	}

	public List<Player> listAllPlayers(String season) {
		try {
			List<Player> players = new ArrayList<>();
			JSONObject json = new JSONObject(importer.sendGet(format(COMMON_ALL_PLAYERS, season)));
			JSONArray resultSets = json.getJSONArray("resultSets");
			for (int x = 0; x < resultSets.length(); x++) {
				JSONObject resultSet = resultSets.getJSONObject(x);
				if (resultSet.get("name").equals("CommonAllPlayers")) {
					JSONArray rowSets = resultSet.getJSONArray("rowSet");
					for (int i = 0; i < rowSets.length(); i++) {
						JSONArray rowSet = rowSets.getJSONArray(i);
						players.add(Player.newBuilder()
								.setPlayerId(rowSet.getInt(0))
								.setName(rowSet.getString(2))
								.build());
					}
				}
			}
			return players;
		} catch (IOException ioe) {
			throw new DD4StorageException("Error reading players", ioe, 500);
		}
	}

	public List<PlayerDay> getGameDay(DateTime date) {
		Map<String, PlayerDay.Builder> playerDayMap = new HashMap<>();
		getFantasyData("draftkings", playerDayMap, date);
		getFantasyData("fanduel", playerDayMap, date);
		return playerDayMap.values().stream()
				.map(PlayerDay.Builder::build)
				.collect(Collectors.toList());
	}

	private void getFantasyData(String site, Map<String, PlayerDay.Builder> playerDayMap, DateTime date) {
		DateTime now = DateTime.now();
		if (date.getDayOfYear() != now.getDayOfYear() || date.getYear() != now.getYear()) {
			throw new IllegalArgumentException("Can only read fantasy data of today");
		}
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(String.format(ROTO_GRINDER, site)).openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			int responseCode = con.getResponseCode();
			System.out.println("Response Code: " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				System.out.println(line);
				String[] values = line.split(",");
				String name = values[0].substring(1, values[0].length() - 1);
				playerDayMap
						.computeIfAbsent(name, name_ -> PlayerDay.newBuilder()
								.setAsOfDate(date.toString(API_DATE_FORMAT))
								.setName(name)
								.setTeam(values[2])
								.setOpponent(values[4]))
						.putFantasySiteInfo(site, FantasySiteInfo.newBuilder()
								.setCost(Integer.parseInt(values[1]))
								.addAllPosition(Arrays.stream(values[3].split("/"))
										.map(Position::valueOf)
										.collect(Collectors.toList()))
								.build());
			}
			in.close();
		} catch (IOException ioe) {
			throw new DD4StorageException("Error reading player options", ioe);
		}
	}
}
