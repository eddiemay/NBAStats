package com.digitald4.nbastats.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.util.Constaints;
import java.util.List;
import java.util.stream.Collectors;
import org.joda.time.DateTime;

public class PlayerDayStore extends GenericStore<PlayerDay> {
	private final APIDAO apiDAO;
	public PlayerDayStore(Provider<DAO> daoProvider, APIDAO apiDAO) {
		super(PlayerDay.class, daoProvider);
		this.apiDAO = apiDAO;
	}

	public QueryResult<PlayerDay> list(DateTime date) {
		return list(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE)))
				.build());
	}

	@Override
	public QueryResult<PlayerDay> list(Query query) {
		QueryResult<PlayerDay> queryResult = super.list(query);
		if (queryResult.getResultCount() == 0 && query.getFilterCount() == 1
				&& query.getFilter(0).getColumn().equals("date")) {
			DateTime date = DateTime.parse(query.getFilter(0).getValue(), Constaints.COMPUTER_DATE);
			List<PlayerDay> playerDays = apiDAO.getGameDay(date).stream().parallel()
					.map(this::create)
					.collect(Collectors.toList());
			queryResult = QueryResult.<PlayerDay>newBuilder()
					.setResultList(playerDays)
					.setTotalSize(playerDays.size())
					.build();
		}
		return queryResult;
	}
}