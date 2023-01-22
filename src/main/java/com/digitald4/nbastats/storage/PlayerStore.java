package com.digitald4.nbastats.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.util.WebFetcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

public class PlayerStore extends GenericStore<Player, Integer> {
	private final WebFetcher webFetcher;

	@Inject
	public PlayerStore(Provider<DAO> daoProvider, @Nullable WebFetcher webFetcher) {
		super(Player.class, daoProvider);
		this.webFetcher = webFetcher;
	}

	public QueryResult<Player> list(String season) {
		QueryResult<Player> queryResult = list(Query.forList().setFilters(Filter.of("season", season)));
		if (queryResult.getTotalSize() == 0) {
			queryResult = refreshPlayerList(season);
		}

		return queryResult;
	}

	public QueryResult<Player> refreshPlayerList(String season) {
		if (webFetcher == null) {
			throw new DD4StorageException("ApiDAO required to refresh player list");
		}

		Query.List query = Query.forList().setFilters(Filter.of("season", season));
		ImmutableMap<Integer, Player> playerMap = list(query)
				.getItems().stream().collect(toImmutableMap(Player::getId, Function.identity()));

		ImmutableList<Player> players = webFetcher.listAllPlayers(season).stream()
				.parallel()
				.map(player -> playerMap.getOrDefault(player.getId(), create(player)))
				.collect(toImmutableList());

		return QueryResult.of(players, players.size(), query);
	}
}
