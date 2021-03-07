package com.digitald4.nbastats.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.nbastats.model.Player;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

public class PlayerStore extends GenericStore<Player> {
	private final NBAApiDAO apiDAO;

	@Inject
	public PlayerStore(Provider<DAO> daoProvider, @Nullable NBAApiDAO apiDAO) {
		super(Player.class, daoProvider);
		this.apiDAO = apiDAO;
	}

	public QueryResult<Player> list(String season) {
		QueryResult<Player> queryResult =
				list(new Query().setFilters(new Filter().setColumn("season").setOperator("=").setValue(season)));
		if (queryResult.getTotalSize() == 0 && apiDAO != null) {
			queryResult = refreshPlayerList(season);
		}

		return queryResult;
	}

	public QueryResult<Player> refreshPlayerList(String season) {
		if (apiDAO == null) {
			throw new DD4StorageException("ApiDAO required to refresh player list");
		}

		ImmutableMap<Integer, Player> playerMap =
				list(new Query().setFilters(new Filter().setColumn("season").setOperator("=").setValue(season)))
				.getResults()
				.stream()
				.collect(toImmutableMap(Player::getPlayerId, Function.identity()));

		return new QueryResult<>(apiDAO.listAllPlayers(season)
				.stream()
				.parallel()
				.map(player -> playerMap.getOrDefault(player.getPlayerId(), create(player)))
				.collect(toImmutableList()));
	}
}
