package com.digitald4.nbastats.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.common.model.HasProto;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.util.Constaints;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import org.joda.time.DateTime;

public class PlayerDayStore extends ModelStore<PlayerDay> {
	private final APIDAO apiDAO;

	@Inject
	public PlayerDayStore(Provider<DAO<HasProto>> daoProvider, @Nullable APIDAO apiDAO) {
		super(PlayerDay.class, daoProvider);
		this.apiDAO = apiDAO;
	}

	public QueryResult<PlayerDay> list(DateTime date) {
		return list(
				new Query().setFilters(new Filter().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE))));
	}

	@Override
	public QueryResult<PlayerDay> list(Query query) {
		QueryResult<PlayerDay> queryResult = super.list(query);
		if (queryResult.getTotalSize() == 0 && apiDAO != null && query.getFilters().size() == 1
				&& query.getFilters().get(0).getColumn().equals("date")) {
			DateTime date = DateTime.parse(query.getFilters().get(0).getValue(), Constaints.COMPUTER_DATE);
			return new QueryResult<>(apiDAO.getGameDay(date).stream()
					.parallel()
					.map(PlayerDay::fromProto)
					.map(this::create)
					.collect(toImmutableList()));
		}

		return queryResult;
	}
}