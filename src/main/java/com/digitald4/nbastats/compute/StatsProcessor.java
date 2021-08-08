package com.digitald4.nbastats.compute;

import static com.digitald4.common.util.Calculate.standardDeviation;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.util.Calculate;
import com.digitald4.nbastats.model.LineUp;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.model.PlayerDay.FantasySiteInfo;
import com.digitald4.nbastats.model.PlayerDay.FantasySiteInfo.Projection;
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

	public ImmutableList<PlayerDay> processStats(DateTime date) {
		ImmutableList<Player> playerList = playerStore.list(Constaints.getSeason(date)).getResults();
		ImmutableMap<String, Player> playerMap = ImmutableMap.<String, Player>builder()
				.putAll(playerList.stream().collect(toImmutableMap(Player::getName, identity())))
				.putAll(
						playerList.stream()
								.filter(player -> player.getAka() != null && !player.getAka().isEmpty())
								.collect(toImmutableMap(Player::getAka, identity())))
				.build();

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
				.collect(toImmutableList());
	}

	private PlayerDay fillStats(PlayerDay playerDay) {
		String strDate = playerDay.getDate();
		DateTime date = DateTime.parse(strDate, Constaints.COMPUTER_DATE);
		playerGameLogStore.refreshGames(playerDay.getPlayerId(), date);
		ImmutableList<PlayerGameLog> games = playerGameLogStore
				.list(
						new Query()
								.setFilters(
										new Filter().setColumn("playerId").setValue(playerDay.getPlayerId()),
										new Filter().setColumn("season").setValue(Constaints.getSeason(date)),
										new Filter().setColumn("date").setOperator("<").setValue(strDate))
								.setOrderBys(new OrderBy().setColumn("date").setDesc(true))
								.setLimit(SAMPLE_SIZE))
				.getResults();
		if (games.size() < SAMPLE_SIZE) {
			games = ImmutableList.<PlayerGameLog>builder()
					.addAll(games)
					.addAll(
							playerGameLogStore.list(
									new Query()
											.setFilters(
													new Filter().setColumn("playerId").setValue(playerDay.getPlayerId()),
													new Filter().setColumn("season").setValue(Constaints.getPrevSeason(date)))
											.setOrderBys(new OrderBy().setColumn("date").setDesc(true))
											.setLimit(SAMPLE_SIZE - games.size())).getResults())
					.build();
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
				playerDay.getFantasySiteInfo(fantasyLeague.name).addProjections(
						ImmutableList.of(
								Projection.forValues("30 Game Average", average),
								Projection.forValues(
										"30 Game 75th Pct",
										round(standardDeviation(matrix[fantasyLeague.ordinal()]) * Z_SCORE_25P + average))));
			}

			if (sampleSize < SAMPLE_SIZE) {
				playerDay.setLowDataWarn(true);
				System.err.printf(
						"Not enough data for: %d - %s (%d)%n", playerDay.getPlayerId(), playerDay.getName(), games.size());
			}
		}
		return playerDay;
	}

	public ImmutableList<PlayerDay> updateActuals(DateTime date) {
		/*gameLogStore.delete(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE)))
				.build());*/
		String strDate = date.toString(Constaints.COMPUTER_DATE);
		// playerStore.refreshPlayerList(Constaints.getSeason(date));
		ImmutableMap<Integer, PlayerDay> playerDaysMap = playerDayStore.list(date)
				.getResults()
				.stream()
				.parallel()
				.filter(playerDay -> playerDay.getPlayerId() != 0)
				.map(playerDay -> {
					boolean changeDetected  = false;
					PlayerGameLog gameLog = playerGameLogStore.get(playerDay.getPlayerId(), date);
					if (gameLog != null) {
						for (FantasySiteInfo fantasySiteInfo : playerDay.getFantasySiteInfos()) {
							double actual = gameLog.getFantasySitePoints(fantasySiteInfo.getFantasySite());
							if (fantasySiteInfo.getActual() != actual) {
								fantasySiteInfo.setActual(actual);
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
				.collect(toImmutableMap(PlayerDay::getPlayerId, identity()));

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

		return ImmutableList.copyOf(playerDaysMap.values());
	}

	private double round(double n) {
		return Calculate.round(n, 3);
	}
}
