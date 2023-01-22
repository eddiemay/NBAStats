package com.digitald4.nbastats.storage;

import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.digitald4.nbastats.util.Constaints;
import com.digitald4.nbastats.util.WebFetcher;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import org.joda.time.DateTime;

public class PlayerGameLogStore extends GenericStore<PlayerGameLog, String> {
	private final PlayerStore playerStore;
	private final WebFetcher webFetcher;

	@Inject
	public PlayerGameLogStore(Provider<DAO> daoProvider, PlayerStore playerStore, WebFetcher webFetcher) {
		super(PlayerGameLog.class, daoProvider);
		this.webFetcher = webFetcher;
		this.playerStore = playerStore;
	}

	@Override
	public QueryResult<PlayerGameLog> list(Query.List query) {
		QueryResult<PlayerGameLog> queryResult = super.list(query);
		if (queryResult.getTotalSize() == 0) {
			Integer playerId = null;
			String season = null;
			for (Filter filter : query.getFilters()) {
				if ("playerId".equals(filter.getColumn())) {
					playerId = filter.getVal();
				} else if ("season".equals(filter.getColumn())) {
					season = filter.getVal();
				}
			}
			if (playerId != null && season != null) {
				webFetcher.getGames(playerStore.get(playerId), season, null)
						.parallelStream()
						.forEach(this::create);
				queryResult = super.list(query);
			}
		}
		return queryResult;
	}

	public PlayerGameLog get(int playerId, DateTime date) {
		String season = Constaints.getSeason(date);
		List<PlayerGameLog> gameLog = list(
				Query.forList().setFilters(
						Filter.of("playerId", playerId),
						Filter.of("season", season),
						Filter.of("date", date.toString(Constaints.COMPUTER_DATE)))).getItems();
		if (!gameLog.isEmpty()) {
			return gameLog.get(0);
		}

		return refreshGames(playerId, date.plusDays(1));
	}

	public PlayerGameLog refreshGames(int playerId, DateTime date) {
		String season = Constaints.getSeason(date);
		String dateStr = date.toString(Constaints.COMPUTER_DATE);
		List<PlayerGameLog> gameLog = list(
				Query.forList()
						.setFilters(Filter.of("playerId", playerId), Filter.of("season", season))
						.setOrderBys(OrderBy.of("date", true))
						.setLimit(1)).getItems();
		DateTime dateFrom = null;
		if (!gameLog.isEmpty()) {
			dateFrom = DateTime.parse(gameLog.get(0).getDate(), Constaints.COMPUTER_DATE);
			dateFrom = dateFrom.plusDays(1);
		}
		PlayerGameLog[] ret = new PlayerGameLog[1];
		if ((dateFrom == null || dateFrom.isBefore(date)) && webFetcher != null) {
			Player player = playerStore.get(playerId);
			webFetcher.getGames(player, season, dateFrom)
					.parallelStream()
					.forEach(game -> {
						create(game);
						if (dateStr.equals(game.getDate())) {
							ret[0] = game;
						}
					});
		}

		return ret[0];
	}
}
