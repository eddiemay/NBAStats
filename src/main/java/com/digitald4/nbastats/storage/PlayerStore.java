package com.digitald4.nbastats.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.proto.DD4Protos.Query.OrderBy;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlayerStore extends GenericStore<Player> {

	private final APIDAO apiDAO;
	public PlayerStore(Provider<DAO> daoProvider, APIDAO apiDAO) {
		super(Player.class, daoProvider);
		this.apiDAO = apiDAO;
	}

	public QueryResult<Player> list(String season) {
		QueryResult<Player> queryResult = super.list(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("season").setValue(season))
				.addOrderBy(OrderBy.newBuilder().setColumn("name"))
				.build());

		if (queryResult.size() == 0 && apiDAO != null) {
			List<Player> players = refreshPlayerList(season);
			return new QueryResult<>(players, players.size());
		}
		return queryResult;
	}

	private List<Player> refreshPlayerList(String season) {
		Map<Integer, Player> playerMap =
				list(Query.newBuilder().addFilter(Filter.newBuilder().setColumn("season").setValue(season)).build())
						.stream()
						.collect(Collectors.toMap(Player::getPlayerId, Function.identity()));
		return apiDAO.listAllPlayers(season)
				.stream()
				.parallel()
				.map(player -> playerMap.computeIfAbsent(player.getPlayerId(), playerId -> create(player)))
				.collect(Collectors.toList());
	}
}
