package com.digitald4.nbastats.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.util.Constaints;
import com.digitald4.nbastats.util.WebFetcher;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;

public class PlayerDayStore extends GenericStore<PlayerDay, String> {
	private final WebFetcher webFetcher;

	@Inject
	public PlayerDayStore(Provider<DAO> daoProvider, WebFetcher webFetcher) {
		super(PlayerDay.class, daoProvider);
		this.webFetcher = webFetcher;
	}

	public QueryResult<PlayerDay> list(DateTime date) {
		return list(
				Query.forList().setFilters(Filter.of("date", date.toString(Constaints.COMPUTER_DATE))));
	}

	@Override
	public QueryResult<PlayerDay> list(Query.List query) {
		QueryResult<PlayerDay> queryResult = super.list(query);
		if (queryResult.getTotalSize() == 0 && query.getFilters().size() == 1
				&& query.getFilters().get(0).getColumn().equals("date")) {
			DateTime date = DateTime.parse(query.getFilters().get(0).getVal(), Constaints.COMPUTER_DATE);
			ImmutableList<PlayerDay> results =
					webFetcher.getGameDay(date).parallelStream().map(this::create).collect(toImmutableList());

			return QueryResult.of(results, results.size(), query);
		}

		return queryResult;
	}
}