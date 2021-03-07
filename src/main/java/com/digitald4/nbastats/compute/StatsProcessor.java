package com.digitald4.nbastats.compute;

import static com.digitald4.common.util.Calculate.standardDeviation;

import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.util.Calculate;
import com.digitald4.nbastats.model.LineUp;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerGameLogStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import com.digitald4.nbastats.util.Constaints;
import com.digitald4.nbastats.util.Constaints.FantasyLeague;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class StatsProcessor {
	private static final boolean OVER_WRITE = false;
	private static final int SAMPLE_SIZE = 30;
	// Sets the data at 25% from the center which is the 25th percentile, or 75th percentile on the right side.
	private static final double Z_SCORE_25P = .675;

	private final PlayerStore playerStore;
	private final PlayerGameLogStore playerGameLogStore;
	private final PlayerDayStore playerDayStore;
	private final LineUpStore lineUpStore;

	@Inject
	public StatsProcessor(PlayerStore playerStore, PlayerGameLogStore playerGameLogStore,
												PlayerDayStore playerDayStore, LineUpStore lineUpStore) {
		this.playerStore = playerStore;
		this.playerGameLogStore = playerGameLogStore;
		this.playerDayStore = playerDayStore;
		this.lineUpStore = lineUpStore;
	}

	public List<PlayerDay> processStats(DateTime date) {
		ImmutableList<Player> playerList = playerStore.list(Constaints.getSeason(date)).getResults();
		Map<String, Player> playerMap = playerList.stream()
				.parallel()
				.collect(Collectors.toMap(Player::getName, Function.identity()));
		playerMap.putAll(playerList.stream()
				.parallel()
				.filter(player -> !player.getAka().isEmpty())
				.collect(Collectors.toMap(Player::getAka, Function.identity())));

		return playerDayStore.list(date).getResults().stream()
				.parallel()
				.map(playerDay -> {
					if (playerDay.getFantasySiteInfo(FantasyLeague.FAN_DUEL.name).getProjections().size() < 5 || OVER_WRITE) {
						Player player = playerMap.get(playerDay.getName());
						if (player != null) {
							PlayerDay statsFilled = fillStats(playerDay.setPlayerId(player.getPlayerId()));
							System.out.println("About to update: " + player.getName());
							return playerDayStore.update(playerDay.getId(), current -> current
									.setPlayerId(player.getPlayerId())
									.setFantasySiteInfos(statsFilled.getFantasySiteInfos()));
						}
					}
					return playerDay;
				})
				.collect(Collectors.toList());
	}

	private PlayerDay fillStats(PlayerDay playerDay) {
		String strDate = playerDay.getDate();
		DateTime date = DateTime.parse(strDate, Constaints.COMPUTER_DATE);
		playerGameLogStore.refreshGames(playerDay.getPlayerId(), date);
		List<PlayerGameLog> games = playerGameLogStore.list(
				new Query().setFilters(
						new Filter().setColumn("player_id").setValue(String.valueOf(playerDay.getPlayerId())),
						new Filter().setColumn("season").setValue(Constaints.getSeason(date)),
						new Filter().setColumn("date").setOperator("<").setValue(strDate))
				.setOrderBys(new OrderBy().setColumn("date").setDesc(true))
				.setLimit(SAMPLE_SIZE))
				.getResults();
		if (games.size() < SAMPLE_SIZE) {
			games.addAll(
					playerGameLogStore.list(new Query()
							.setFilters(
									new Filter().setColumn("player_id").setValue(String.valueOf(playerDay.getPlayerId())),
									new Filter().setColumn("season").setValue(Constaints.getPrevSeason(date)))
							.setOrderBys(new OrderBy().setColumn("date").setDesc(true))
							.setLimit(SAMPLE_SIZE - games.size())).getResults());
		}
		if (games.size() > 0) {
			int sampleSize = games.size();
			double[][] matrix = new double[FantasyLeague.values().length][sampleSize];
			double[] totals = new double[FantasyLeague.values().length];
			int g = 0;
			for (PlayerGameLog game : games) {
				for (FantasyLeague fantasyLeague : FantasyLeague.values()) {
					totals[fantasyLeague.ordinal()] +=
							matrix[fantasyLeague.ordinal()][g] = game.getFantasySitePoints(fantasyLeague.name);
				}
				g++;
			}

			for (FantasyLeague fantasyLeague : FantasyLeague.values()) {
				double average = totals[fantasyLeague.ordinal()] / sampleSize;
				playerDay.getFantasySiteInfo(fantasyLeague.name).setProjections(
						ImmutableMap.of(
								"30 Game Average", average,
								"30 Game 75th Pct", round(standardDeviation(matrix[fantasyLeague.ordinal()]) * Z_SCORE_25P + average)));
			}

			if (sampleSize < SAMPLE_SIZE) {
				playerDay.setLowDataWarn(true);
				System.err.println(
						String.format(
								"Not enough data for: %d - %s (%d)", playerDay.getPlayerId(), playerDay.getName(), games.size()));
			}
		}
		return playerDay;
	}

	public List<PlayerDay> updateActuals(DateTime date) {
		/*gameLogStore.delete(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE)))
				.build());*/
		String strDate = date.toString(Constaints.COMPUTER_DATE);
		// playerStore.refreshPlayerList(Constaints.getSeason(date));
		Map<Integer, PlayerDay> playerDaysMap = playerDayStore.list(date)
				.getResults()
				.stream()
				.parallel()
				.filter(playerDay -> playerDay.getPlayerId() != 0)
				.map(playerDay -> {
					boolean changeDetected  = false;
					PlayerGameLog gameLog = playerGameLogStore.get(playerDay.getPlayerId(), date);
					if (gameLog != null) {
						for (String site : playerDay.getFantasySiteInfos().keySet()) {
							playerDay.getFantasySiteInfo(site).setActual(gameLog.getFantasySitePoints(site));
							if (playerDay.getFantasySiteInfo(site).getActual() != playerDay.getFantasySiteInfo(site).getActual()) {
								changeDetected = true;
							}
						}
						if (changeDetected) {
							return playerDayStore.update(
									playerDay.getId(), playerDay1 -> playerDay1.setFantasySiteInfos(playerDay.getFantasySiteInfos()));
						}
					}
					return playerDay;
				})
				.collect(Collectors.toMap(PlayerDay::getPlayerId, Function.identity()));

		lineUpStore.list(new Query().setFilters(new Filter().setColumn("date").setValue(strDate)))
				.getResults()
				.parallelStream()
				.forEach(lineUp -> {
					LineUp modified = new LineUp()
							.setActual(lineUp.getPlayerIds()
									.stream()
									.mapToDouble(
											playerId -> playerDaysMap.get(playerId).getFantasySiteInfo(lineUp.getFantasySite()).getActual())
									.sum());
					if (lineUp.getActual() != modified.getActual()) {
						lineUpStore.update(lineUp.getId(), lineUp1 -> lineUp1.setActual(modified.getActual()));
					}
				});

		return new ArrayList<>(playerDaysMap.values());
	}

	private double round(double n) {
		return Calculate.round(n, 3);
	}
}
