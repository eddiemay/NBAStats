package com.digitald4.nbastats.compute;

import static com.digitald4.common.util.Calculate.standardDeviation;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.proto.DD4Protos.Query.OrderBy;
import com.digitald4.common.util.Calculate;
import com.digitald4.nbastats.proto.NBAStatsProtos.GameLog;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay.FantasySiteInfo;
import com.digitald4.nbastats.storage.GameLogStore;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import com.digitald4.nbastats.util.Constaints;
import com.digitald4.nbastats.util.Constaints.FantasyLeague;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.joda.time.DateTime;

public class StatsProcessor {
	private static final boolean OVER_WRITE = false;
	private static final int SAMPLE_SIZE = 30;
	// Sets the data at 25% from the center which is the 25th percentile, or 75th percentile on the right side.
	private static final double Z_SCORE_25P = .675;

	private final PlayerStore playerStore;
	private final GameLogStore gameLogStore;
	private final PlayerDayStore playerDayStore;
	private final LineUpStore lineUpStore;

	public StatsProcessor(PlayerStore playerStore, GameLogStore gameLogStore,
												PlayerDayStore playerDayStore, LineUpStore lineUpStore) {
		this.playerStore = playerStore;
		this.gameLogStore = gameLogStore;
		this.playerDayStore = playerDayStore;
		this.lineUpStore = lineUpStore;
	}

	public List<PlayerDay> processStats(DateTime date) {
		List<Player> playerList = playerStore.list(Constaints.getSeason(date));
		Map<String, Player> playerMap = playerList.stream()
				.parallel()
				.collect(Collectors.toMap(Player::getName, Function.identity()));
		playerMap.putAll(playerList.stream()
				.parallel()
				.filter(player -> !player.getAka().isEmpty())
				.collect(Collectors.toMap(Player::getAka, Function.identity())));
		return playerDayStore.list(date).stream()
				.parallel()
				.map(playerDay -> {
					if (playerDay.getFantasySiteInfoOrThrow(FantasyLeague.FAN_DUEL.name).getProjectionCount() < 5 || OVER_WRITE) {
						Player player = playerMap.get(playerDay.getName());
						if (player != null) {
							PlayerDay statsFilled = fillStats(playerDay.toBuilder()
									.setPlayerId(player.getPlayerId())
									.build());
							System.out.println("About to update: " + player.getName());
							return playerDayStore.update(playerDay.getId(), current -> current.toBuilder()
									.setPlayerId(player.getPlayerId())
									.putAllFantasySiteInfo(statsFilled.getFantasySiteInfoMap())
									.build());
						}
					}
					return playerDay;
				})
				.collect(Collectors.toList());
	}

	private PlayerDay fillStats(PlayerDay player) {
		PlayerDay.Builder builder = player.toBuilder();
		String strDate = player.getDate();
		DateTime date = DateTime.parse(strDate, Constaints.COMPUTER_DATE);
		gameLogStore.refreshGames(player.getPlayerId(), date);
		List<GameLog> games = gameLogStore.list(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("player_id").setValue(String.valueOf(player.getPlayerId())))
				.addFilter(Filter.newBuilder().setColumn("season").setValue(Constaints.getSeason(date)))
				.addFilter(Filter.newBuilder().setColumn("date").setOperator("<").setValue(strDate))
				.addOrderBy(OrderBy.newBuilder().setColumn("date").setDesc(true))
				.setLimit(SAMPLE_SIZE)
				.build());
		if (games.size() < SAMPLE_SIZE) {
			games.addAll(gameLogStore.list(Query.newBuilder()
					.addFilter(Filter.newBuilder().setColumn("player_id").setValue(String.valueOf(player.getPlayerId())))
					.addFilter(Filter.newBuilder().setColumn("season").setValue(Constaints.getPrevSeason(date)))
					.addOrderBy(OrderBy.newBuilder().setColumn("date").setDesc(true))
					.setLimit(SAMPLE_SIZE - games.size())
					.build()));
		}
		if (games.size() > 0) {
			int sampleSize = games.size();
			double[][] matrix = new double[FantasyLeague.values().length][sampleSize];
			double[] totals = new double[FantasyLeague.values().length];
			int g = 0;
			for (GameLog game : games) {
				for (FantasyLeague fantasyLeague : FantasyLeague.values()) {
					totals[fantasyLeague.ordinal()] += matrix[fantasyLeague.ordinal()][g] = game.getFantasySitePointsOrDefault(fantasyLeague.name, 0);
				}
				g++;
			}

			for (FantasyLeague fantasyLeague : FantasyLeague.values()) {
				double average = totals[fantasyLeague.ordinal()] / sampleSize;
				builder.putFantasySiteInfo(fantasyLeague.name,
						player.getFantasySiteInfoOrDefault(fantasyLeague.name, FantasySiteInfo.getDefaultInstance()).toBuilder()
								// .putProjection("30 Game 40th Percentile", round(standardDeviation(matrix[fantasyLeague.ordinal()]) * -Z_SCORE_10P + average))
								.putProjection("30 Game Average", average)
								.removeProjection("30 Game 40th Percentile")
								.removeProjection("30 Game 60th Percentile")
								// .putProjection("30 Game 60th Percentile", round(standardDeviation(matrix[fantasyLeague.ordinal()]) * Z_SCORE_10P + average))
								.putProjection("30 Game 75th Pct", round(standardDeviation(matrix[fantasyLeague.ordinal()]) * Z_SCORE_25P + average))
								.build());
			}

			if (sampleSize < SAMPLE_SIZE) {
				builder.setLowDataWarn(true);
				System.err.println(String.format("Not enough data for: %d - %s (%d)", player.getPlayerId(), player.getName(), games.size()));
			}
		}
		return builder.build();
	}

	public List<PlayerDay> updateActuals(DateTime date) {
		/*gameLogStore.delete(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE)))
				.build());*/
		String strDate = date.toString(Constaints.COMPUTER_DATE);
		// playerStore.refreshPlayerList(Constaints.getSeason(date));
		Map<Integer, PlayerDay> playerDaysMap = playerDayStore.list(date)
				.stream()
				.parallel()
				.filter(playerDay -> playerDay.getPlayerId() != 0)
				.map(playerDay -> {
					boolean changeDetected  = false;
					GameLog gameLog = gameLogStore.get(playerDay.getPlayerId(), date);
					if (gameLog != null) {
						PlayerDay.Builder builder = playerDay.toBuilder();
						for (String site : playerDay.getFantasySiteInfoMap().keySet()) {
							builder.putFantasySiteInfo(site, builder.getFantasySiteInfoOrThrow(site).toBuilder()
									.setActual(gameLog.getFantasySitePointsOrDefault(site, 0))
									.build());
							if (playerDay.getFantasySiteInfoOrThrow(site).getActual() != builder.getFantasySiteInfoOrThrow(site).getActual()) {
								changeDetected = true;
							}
						}
						if (changeDetected) {
							return playerDayStore.update(playerDay.getId(), playerDay1 ->
									playerDay1.toBuilder().putAllFantasySiteInfo(builder.getFantasySiteInfoMap()).build());
						}
					}
					return playerDay;
				})
				.collect(Collectors.toMap(PlayerDay::getPlayerId, Function.identity()));

		lineUpStore.list(Query.newBuilder().addFilter(Filter.newBuilder().setColumn("date").setValue(strDate)).build())
				.parallelStream()
				.forEach(lineUp -> {
					LineUp.Builder builder = lineUp.toBuilder()
							.setActual(lineUp.getPlayerIdList()
									.stream()
									.mapToDouble(playerId -> playerDaysMap.get(playerId).getFantasySiteInfoOrThrow(lineUp.getFantasySite()).getActual())
									.sum());
					if (lineUp.getActual() != builder.getActual()) {
						lineUpStore.update(lineUp.getId(), lineUp1 -> lineUp1.toBuilder()
								.setActual(builder.getActual())
								.build());
					}
				});

		return new ArrayList<>(playerDaysMap.values());
	}

	private double round(double n) {
		return Calculate.round(n, 3);
	}
}
