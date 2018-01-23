package com.digitald4.nbastats.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.proto.DD4Protos.Query.OrderBy;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.proto.NBAStatsProtos.GameLog;
import com.digitald4.nbastats.util.Constaints;
import java.util.List;
import org.joda.time.DateTime;

public class GameLogStore extends GenericStore<GameLog> {
	private final APIDAO apiDAO;
	public GameLogStore(Provider<DAO> daoProvider, APIDAO apiDAO) {
		super(GameLog.class, daoProvider);
		this.apiDAO = apiDAO;
	}

	public QueryResult<GameLog> list(Query query) {
		QueryResult<GameLog> queryResult = super.list(query);
		if (queryResult.size() == 0) {
			int playerId = 0;
			String season = null;
			for (Filter filter : query.getFilterList()) {
				if ("player_id".equals(filter.getColumn())) {
					playerId = Integer.parseInt(filter.getValue());
				} else if ("season".equals(filter.getColumn())) {
					season = filter.getValue();
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

	public GameLog get(int playerId, DateTime date) {
		Query query = Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("player_id").setValue(String.valueOf(playerId)))
				.addFilter(Filter.newBuilder().setColumn("season").setValue(Constaints.getSeason(date)))
				.addFilter(Filter.newBuilder().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE)))
				.build();
		List<GameLog> gameLog = super.list(query);
		if (gameLog.size() > 0) {
			return gameLog.get(0);
		}
		refreshGames(playerId, date);
		gameLog = super.list(query);
		if (gameLog.size() > 0) {
			return gameLog.get(0);
		}
		return null;
	}

	public void refreshGames(int playerId, DateTime date) {
		String season = Constaints.getSeason(date);
		List<GameLog> gameLog = super.list(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("player_id").setValue(String.valueOf(playerId)))
				.addFilter(Filter.newBuilder().setColumn("season").setValue(season))
				.addOrderBy(OrderBy.newBuilder().setColumn("date").setDesc(true))
				.setLimit(1)
				.build());
		DateTime dateFrom = null;
		if (gameLog.size() > 0) {
			dateFrom = DateTime.parse(gameLog.get(0).getDate(), Constaints.COMPUTER_DATE);
			dateFrom = dateFrom.plusDays(1);
		}
		if ((dateFrom == null || dateFrom.isBefore(date)) && apiDAO != null) {
			apiDAO.getGames(playerId, season, dateFrom)
					.parallelStream()
					.forEach(this::create);
		}
	}
}
