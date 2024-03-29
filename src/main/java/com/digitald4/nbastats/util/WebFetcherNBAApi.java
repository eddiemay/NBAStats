package com.digitald4.nbastats.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.lang.String.format;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.model.PlayerDay.FantasySiteInfo.Projection;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.digitald4.nbastats.model.PlayerGameLog.Result;
import com.digitald4.nbastats.util.Constaints.FantasyLeague;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.inject.Inject;

public class WebFetcherNBAApi implements WebFetcher {
	private static final String ROTO_GRINDER =
			"https://rotogrinders.com/projected-stats/nba-player.csv?site=%s&date=%s";
	// private static final String COMMON_ALL_PLAYERS =
			// "http://stats.nba.com/stats/commonallplayers?LeagueID=00&Season=%s&IsOnlyCurrentSeason=1";
	private static final String PLAYER_GAMELOGS =
			"http://stats.nba.com/stats/playergamelogs?LeagueID=00&Season=%s&SeasonType=Regular+Season&PlayerID=%d&DateFrom=%s&DateTo=%s";
	static final String COMMON_TEAM_YEARS = "https://stats.nba.com/stats/commonteamyears?LeagueID=00";
	static final String TEAM_ROSTER = "https://stats.nba.com/stats/commonteamroster?LeagueID=00&Season=%s&TeamID=%d";
	// private static final String GAME_FINDER = "https://stats.nba.com/stats/leaguegamefinder?Conference=&DateFrom=&DateTo=&Division=&DraftNumber=&DraftRound=&DraftYear=&GB=N&LeagueID=00&Location=&Outcome=&PlayerOrTeam=T&Season=&SeasonType=&StatCategory=PTS&TeamID=&VsConference=&VsDivision=&VsTeamID=&gtPTS=150";
	//                                         https://stats.nba.com/stats/leaguegamefinder?Conference=&DateFrom=&DateTo=&Division=&DraftNumber=&DraftRound=&DraftYear=&GB=N&LeagueID=00&Location=&Outcome=&PlayerOrTeam=P&Season=&SeasonType=&StatCategory=PTS&TeamID=&VsConference=&VsDivision=&VsTeamID=&gtPTS=50
	//                                         https://stats.nba.com/stats/leaguegamefinder?Conference=&DateFrom=&DateTo=&Division=&DraftNumber=&DraftRound=&DraftYear=&GB=N&LeagueID=00&Location=&Outcome=&PlayerID=2544&PlayerOrTeam=P&Season=2020-21&SeasonType=Regular+Season&StatCategory=PTS&TeamID=&VsConference=&VsDivision=&VsTeamID=
	//http://stats.nba.com/stats/playergamelogs?DateFrom=&DateTo=&GameSegment=&LastNGames=0&Location=&MeasureType=Base&Month=0&OpponentTeamID=0&Outcome=&PORound=0&PaceAdjust=N&PerMode=Totals&Period=0&PlayerID=203584&PlusMinus=N&Rank=N&Season=2017-18&SeasonSegment=&SeasonType=Regular+Season&ShotClockRange=&VsConference=&VsDivision=

	/*
	Accept: * /*
	Accept-Encoding: gzip, deflate, br
	Accept-Language: en-US,en;q=0.9
	Access-Control-Request-Headers: x-nba-stats-origin,x-nba-stats-token
	Access-Control-Request-Method: GET
	Connection: keep-alive
	Host: stats.nba.com
	Origin: https://www.nba.com
	Referer: https://www.nba.com/
	Sec-Fetch-Dest: empty
	Sec-Fetch-Mode: cors
	Sec-Fetch-Site: same-site
	User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36
	 */

	private static final DateTimeFormatter API_DATE = DateTimeFormat.forPattern("MM/dd/yyyy");

	private static final boolean fetchFromNBAApiEnabled = true;

	private final APIConnector apiConnector;

	@Inject
	public WebFetcherNBAApi(APIConnector apiConnector) {
		this.apiConnector = apiConnector;
	}

	public ImmutableList<PlayerGameLog> getGames(Player player, String season, DateTime dateFrom) {
		if (!fetchFromNBAApiEnabled) {
			return ImmutableList.of();
		}

		ImmutableList.Builder<PlayerGameLog> games = ImmutableList.builder();
		try {
			JSONObject json = new JSONObject(
					apiConnector.sendGet(
							format(PLAYER_GAMELOGS, season, player.getId(), dateFrom == null ? "" : dateFrom.toString(API_DATE),
									DateTime.now().minusDays(1).toString(API_DATE))));
			JSONArray resultSets = json.getJSONArray("resultSets");
			for (int x = 0; x < resultSets.length(); x++) {
				JSONObject resultSet = resultSets.getJSONObject(x);
				if (resultSet.get("name").equals("PlayerGameLogs")) {
					JSONArray rowSets = resultSet.getJSONArray("rowSet");
					for (int i = 0; i < rowSets.length(); i++) {
						JSONArray rowSet = rowSets.getJSONArray(i);
						PlayerGameLog game = WebFetcher.fillFantasy(
								new PlayerGameLog()
										.setPlayerId(player.getId())
										.setDate(DateTime.parse(rowSet.getString(7)).toString(Constaints.COMPUTER_DATE))
										.setSeason(season)
										.setMatchUp(rowSet.getString(8))
										.setResult(rowSet.getString(9).equals("W") ? Result.W : Result.L)
										.setMinutes(rowSet.getDouble(10))
										.setThreePointersMade(rowSet.getInt(14))
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

		return games.build();
	}

	public ImmutableList<Long> getActiveTeamIds() {
		NBADataResult result = new NBADataResult(apiConnector.sendGet(COMMON_TEAM_YEARS));

		return IntStream.range(0, result.getResultCount())
				.filter(i -> Integer.parseInt(result.getString("MAX_YEAR", i)) >= 2020)
				.mapToObj(i -> result.getLong("TEAM_ID", i))
				.collect(toImmutableList());
	}

	public ImmutableList<Player> getTeamRoster(String season, long teamId) {
		NBADataResult result = new NBADataResult(apiConnector.sendGet(format(TEAM_ROSTER, season, teamId)));

		return IntStream.range(0, result.getResultCount())
				.mapToObj(i -> new Player()
						.setId(result.getInt("PLAYER_ID", i))
						.setSeason(season)
						.setName(result.getString("PLAYER", i)))
				.collect(toImmutableList());
	}

	public ImmutableList<Player> listAllPlayers(String season) {
		if (!fetchFromNBAApiEnabled) {
			return ImmutableList.of();
		}

		return getActiveTeamIds().stream()
				.flatMap(teamId -> getTeamRoster(season, teamId).stream())
				.collect(toImmutableList());
	}

	public ImmutableList<PlayerDay> getGameDay(DateTime date) {
		Map<String, PlayerDay> playerDayMap = new HashMap<>();
		for (FantasyLeague league : FantasyLeague.values()) {
			getFantasyData(league.name, playerDayMap, date);
		}

		return playerDayMap.values().stream().collect(toImmutableList());
	}

	private void getFantasyData(String site, Map<String, PlayerDay> playerDayMap, DateTime date) {
		try {
			String request = String.format(ROTO_GRINDER, site, date.toString(Constaints.COMPUTER_DATE));
			HttpURLConnection con = (HttpURLConnection) new URL(request).openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			System.out.println("\nSending request: " + request);
			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				throw new DD4StorageException("Error reading from Roto Grinder, bad response code: " + responseCode);
			}
			JSONArray rotoPlayers = getJson(con);
			System.out.println("Roto Grinder player count: " + rotoPlayers.length());
			// System.out.println("First player: " + rotoPlayers.get(0));

			for (int p = 0; p < rotoPlayers.length(); p++) {
				JSONObject rotoPlayer = rotoPlayers.getJSONObject(p);
				String name = rotoPlayer.getString("player_name");
				System.out.println(name + " " + rotoPlayer.getString("position") + " " + Arrays.toString(rotoPlayer.getString("position").split("/")));
				playerDayMap
						.computeIfAbsent(name, name_ -> new PlayerDay()
								.setDate(date.toString(Constaints.COMPUTER_DATE))
								.setName(name)
								.setTeam(rotoPlayer.getString("team"))
								.setOpponent(rotoPlayer.getString("opp")))
						.getFantasySiteInfo(site)
								.setCost((int) rotoPlayer.getDouble("salary"))
								.setPositions(
										Arrays.stream(rotoPlayer.getString("position").split("/"))
												.map(String::trim)
												// .map(Position::valueOf)
												.collect(toImmutableList()))
								.addProjections(
										ImmutableList.of(
												Projection.forValues("RotoG Ceil", rotoPlayer.getDouble("ceil")),
												Projection.forValues("RotoG Floor", rotoPlayer.getDouble("floor")),
												Projection.forValues("RotoG Proj", rotoPlayer.getDouble("dvp"))));
			}
		} catch (IOException ioe) {
			throw new DD4StorageException("Error reading player options", ioe);
		}
	}

	private static JSONArray getJson(HttpURLConnection con) throws IOException {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("data = ")) {
					return new JSONArray(line.substring("data = ".length()));
				}
			}
		}

		return null;
	}

	static class NBADataResult {
		private final ImmutableMap<String, Integer> headers;
		private final JSONArray rowSet;

		public NBADataResult(String json) {
			this(new JSONObject(json));
		}

		public NBADataResult(JSONObject json) {
			JSONArray headerArray = json.optJSONArray("resultSets").getJSONObject(0).getJSONArray("headers");
			AtomicInteger col = new AtomicInteger();
			this.headers = IntStream.range(0, headerArray.length())
					.mapToObj(headerArray::getString)
					.collect(toImmutableMap(Function.identity(), s -> col.getAndIncrement()));
			this.rowSet = json.getJSONArray("resultSets").getJSONObject(0).getJSONArray("rowSet");
		}

		public ImmutableList<String> getHeaders() {
			return headers.keySet().asList();
		}

		public int getInt(String property, int i) {
			return rowSet.getJSONArray(i).getInt(headers.get(property));
		}

		public long getLong(String property, int i) {
			return rowSet.getJSONArray(i).getLong(headers.get(property));
		}

		public String getString(String property, int i) {
			return rowSet.getJSONArray(i).getString(headers.get(property));
		}

		public int getResultCount() {
			return rowSet.length();
		}
	}
}
