package com.digitald4.nbastats.storage;

import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.digitald4.nbastats.util.Constaints;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import org.joda.time.DateTime;

public class PlayerGameLogStore extends GenericStore<PlayerGameLog> {
	private final NBAApiDAO apiDAO;

	@Inject
	public PlayerGameLogStore(Provider<DAO> daoProvider, @Nullable NBAApiDAO apiDAO) {
		super(PlayerGameLog.class, daoProvider);
		this.apiDAO = apiDAO;
	}

	@Override
	public QueryResult<PlayerGameLog> list(Query query) {
		QueryResult<PlayerGameLog> queryResult = super.list(query);
		if (queryResult.getTotalSize() == 0) {
			int playerId = 0;
			String season = null;
			for (Filter filter : query.getFilters()) {
				if ("player_id".equals(filter.getColumn())) {
					playerId = filter.getVal();
				} else if ("season".equals(filter.getColumn())) {
					season = filter.getVal();
				}
			}
			if (playerId != 0 && season != null && apiDAO != null) {
				apiDAO.getGames(playerId, season, null)
						.parallelStream()
						.forEach(this::create);
				queryResult = super.list(query);
			}
		}
		return queryResult;
	}

	public PlayerGameLog get(int playerId, DateTime date) {
		String season = Constaints.getSeason(date);
		Query query = new Query().setFilters(
				new Filter().setColumn("player_id").setValue(String.valueOf(playerId)),
				new Filter().setColumn("season").setValue(season),
				new Filter().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE)));
		List<PlayerGameLog> gameLog = super.list(query).getResults();
		if (!gameLog.isEmpty()) {
			return gameLog.get(0);
		}

		return refreshGames(playerId, date.plusDays(1));
	}

	public PlayerGameLog refreshGames(int playerId, DateTime date) {
		String season = Constaints.getSeason(date);
		String dateStr = date.toString(Constaints.COMPUTER_DATE);
		List<PlayerGameLog> gameLog = super.list(new Query()
				.setFilters(
						new Filter().setColumn("player_id").setValue(String.valueOf(playerId)),
						new Filter().setColumn("season").setValue(season))
				.setOrderBys(new OrderBy().setColumn("date").setDesc(true))
				.setLimit(1)).getResults();
		DateTime dateFrom = null;
		if (!gameLog.isEmpty()) {
			dateFrom = DateTime.parse(gameLog.get(0).getDate(), Constaints.COMPUTER_DATE);
			dateFrom = dateFrom.plusDays(1);
		}
		PlayerGameLog[] ret = new PlayerGameLog[1];
		if ((dateFrom == null || dateFrom.isBefore(date)) && apiDAO != null) {
			apiDAO.getGames(playerId, season, dateFrom)
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
