package com.digitald4.nbastats.storage;

import static java.lang.String.format;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.nbastats.proto.NBAStatsProtos.GameLog;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay.FantasySiteInfo;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import com.digitald4.nbastats.proto.NBAStatsProtos.Position;
import com.digitald4.nbastats.util.Constaints;
import com.digitald4.nbastats.util.Constaints.FantasyLeague;
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
import javax.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

public class APIDAO {
	private static final String ROTO_GRINDER =
			"https://rotogrinders.com/projected-stats/nba-player.csv?site=%s&date=%s";
	private static final String COMMON_ALL_PLAYERS =
			"http://stats.nba.com/stats/commonallplayers?LeagueID=00&Season=%s&IsOnlyCurrentSeason=1";
	private static final String PLAYER_GAMELOGS =
			"http://stats.nba.com/stats/playergamelogs?LeagueID=00&Season=%s&SeasonType=Regular Season&PlayerID=%d&DateFrom=%s&DateTo=%s";
	//http://stats.nba.com/stats/playergamelogs?DateFrom=&DateTo=&GameSegment=&LastNGames=0&Location=&MeasureType=Base&Month=0&OpponentTeamID=0&Outcome=&PORound=0&PaceAdjust=N&PerMode=Totals&Period=0&PlayerID=203584&PlusMinus=N&Rank=N&Season=2017-18&SeasonSegment=&SeasonType=Regular+Season&ShotClockRange=&VsConference=&VsDivision=

	private static final DateTimeFormatter API_DATE = DateTimeFormat.forPattern("MM/dd/yyyy");

	private static final boolean fetchFromNBAApiEnabled = true;

	private final APIConnector apiConnector;

	@Inject
	public APIDAO(APIConnector apiConnector) {
		this.apiConnector = apiConnector;
	}

	public List<GameLog> getGames(int playerId, String season, DateTime dateFrom) {
		List<GameLog> games = new ArrayList<>();
		if (fetchFromNBAApiEnabled) {
			try {
				JSONObject json = new JSONObject(apiConnector.sendGet(format(PLAYER_GAMELOGS, season, playerId,
						dateFrom == null ? "" : dateFrom.toString(API_DATE), DateTime.now().minusDays(1).toString(API_DATE))));
				JSONArray resultSets = json.getJSONArray("resultSets");
				for (int x = 0; x < resultSets.length(); x++) {
					JSONObject resultSet = resultSets.getJSONObject(x);
					if (resultSet.get("name").equals("PlayerGameLogs")) {
						JSONArray rowSets = resultSet.getJSONArray("rowSet");
						for (int i = 0; i < rowSets.length(); i++) {
							JSONArray rowSet = rowSets.getJSONArray(i);
							GameLog game = fillFantasy(GameLog.newBuilder()
									.setPlayerId(playerId)
									.setSeason(season)
									.setDate(DateTime.parse(rowSet.getString(7)).toString(Constaints.COMPUTER_DATE))
									.setMatchUp(rowSet.getString(8))
									.setResult(rowSet.getString(9))
									.setMinutes(rowSet.getDouble(10))
									.setMade3S(rowSet.getInt(14))
									.setRebounds(rowSet.getInt(22))
									.setAssists(rowSet.getInt(23))
									.setTurnovers(rowSet.getInt(24))
									.setSteals(rowSet.getInt(25))
									.setBlocks(rowSet.getInt(26))
									.setPoints(rowSet.getInt(30))
									.setPlusMinus(rowSet.getInt(31))
									.setNBAFantasyPoints(rowSet.getDouble(32))
									.setDoubleDouble(rowSet.getInt(33) == 1)
									.setTripleDouble(rowSet.getInt(34) == 1));
							games.add(game);
						}
					}
				}
				Thread.sleep(500); // Wait 1/2 a second to try and stop API from dectecting automated code.
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return games;
	}

	private static GameLog fillFantasy(GameLog.Builder stats) {
		return stats
				.putFantasySitePoints("draftkings", stats.getPoints()
						+ stats.getMade3S() * .5
						+ stats.getRebounds() * 1.25
						+ stats.getAssists() * 1.5
						+ stats.getSteals() * 2
						+ stats.getBlocks() * 2
						+ stats.getTurnovers() * -.5
						+ (stats.getDoubleDouble() ? 1.5 : 0)
						+ (stats.getTripleDouble() ? 3 : 0))
				.putFantasySitePoints("fanduel", stats.getPoints()
						+ stats.getRebounds() * 1.2
						+ stats.getAssists() * 1.5
						+ stats.getBlocks() * 3
						+ stats.getSteals() * 3
						- stats.getTurnovers())
				.build();
	}

	public List<Player> listAllPlayers(String season) {
		try {
			List<Player> players = new ArrayList<>();
			if (fetchFromNBAApiEnabled) {
				JSONObject json = new JSONObject(apiConnector.sendGet(format(COMMON_ALL_PLAYERS, season)));
				JSONArray resultSets = json.getJSONArray("resultSets");
				for (int x = 0; x < resultSets.length(); x++) {
					JSONObject resultSet = resultSets.getJSONObject(x);
					if (resultSet.get("name").equals("CommonAllPlayers")) {
						JSONArray rowSets = resultSet.getJSONArray("rowSet");
						for (int i = 0; i < rowSets.length(); i++) {
							JSONArray rowSet = rowSets.getJSONArray(i);
							players.add(Player.newBuilder()
									.setSeason(season)
									.setPlayerId(rowSet.getInt(0))
									.setName(rowSet.getString(2))
									.build());
						}
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
		for (FantasyLeague league : FantasyLeague.values()) {
			getFantasyData(league.name, playerDayMap, date);
		}
		return playerDayMap.values().stream()
				.map(PlayerDay.Builder::build)
				.collect(Collectors.toList());
	}

	private void getFantasyData(String site, Map<String, PlayerDay.Builder> playerDayMap, DateTime date) {
		try {
			HttpURLConnection con = (HttpURLConnection)
					new URL(String.format(ROTO_GRINDER, site, date.toString(Constaints.COMPUTER_DATE))).openConnection();
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
				String name = values[0].substring(1, values[0].length() - 1).trim();
				playerDayMap
						.computeIfAbsent(name, name_ -> PlayerDay.newBuilder()
								.setDate(date.toString(Constaints.COMPUTER_DATE))
								.setName(name)
								.setTeam(values[2])
								.setOpponent(values[4]))
						.putFantasySiteInfo(site, FantasySiteInfo.newBuilder()
								.setCost(Integer.parseInt(values[1]))
								.addAllPosition(Arrays.stream(values[3].split("/"))
										.map(Position::valueOf)
										.collect(Collectors.toList()))
								.putProjection("RotoG Ceil", Double.parseDouble(!values[5].isEmpty() ? values[5] : "0"))
								.putProjection("RotoG Floor", Double.parseDouble(!values[6].isEmpty() ? values[6] : "0" ))
								.putProjection("RotoG Proj", Double.parseDouble(!values[7].isEmpty() ? values[7] : "0"))
								.build());
			}
			in.close();
		} catch (IOException ioe) {
			throw new DD4StorageException("Error reading player options", ioe);
		}
	}
}
