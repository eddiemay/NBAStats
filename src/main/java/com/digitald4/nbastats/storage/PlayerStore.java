package com.digitald4.nbastats.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.List;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.util.WebFetcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

public class PlayerStore extends GenericStore<Player, Long> {
	private final WebFetcher webFetcher;

	@Inject
	public PlayerStore(Provider<DAO> daoProvider, @Nullable WebFetcher webFetcher) {
		super(Player.class, daoProvider);
		this.webFetcher = webFetcher;
	}

	public QueryResult<Player> list(Integer season) {
		return list(Query.forList().setFilters(Filter.of("season", season)));
	}

	@Override
	public QueryResult<Player> list(List query) {
		Optional<Filter> seasonFilter = query.getFilters().stream().filter(f -> f.getColumn().equals("season")).findAny();
		if (seasonFilter.isPresent()) {
			Integer season = Integer.parseInt(seasonFilter.get().getValue().toString());
			query.setFilters(query.getFilters().stream().filter(f -> !f.getColumn().equals("season")).collect(toImmutableList()))
					.addFilter(Filter.of("maxSeason", ">=", season))
					.setLimit(2048);

			QueryResult<Player> queryResult = super.list(query);
			ImmutableList<Player> endResult = queryResult.getItems().stream()
					.filter(p -> p.getMinSeason() <= season)
					.collect(toImmutableList());

			return QueryResult.of(Player.class, endResult, endResult.size(), query);
		}
		return super.list(query);
	}

	public QueryResult<Player> refreshPlayerList(Integer season) {
		if (webFetcher == null) {
			throw new DD4StorageException("ApiDAO required to refresh player list");
		}

		Query.List query = Query.forList().setFilters(Filter.of("season", season));
		ImmutableMap<Long, Player> playerMap = list(query)
				.getItems().stream().collect(toImmutableMap(Player::getId, Function.identity()));

		ImmutableList<Player> players = webFetcher.listAllPlayers(season).stream()
				.parallel()
				.map(player -> playerMap.getOrDefault(player.getId(), create(player)))
				.collect(toImmutableList());

		return QueryResult.of(Player.class, players, players.size(), query);
	}
}
