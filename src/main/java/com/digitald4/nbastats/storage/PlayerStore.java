package com.digitald4.nbastats.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
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

	public Player getByName(String name) {
		QueryResult<Player> result = list(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("name").setValue(name)).build());
		if (result.getTotalSize() == 0) {
			System.err.println("Could not find player named: " + name);
			/*synchronized (this) {
				result = list(Query.newBuilder()
						.addFilter(Filter.newBuilder().setColumn("name").setValue(name)).build());
				if (result.getTotalSize() == 0) {
					Player[] player = new Player[1];
					refreshPlayerList().forEach(p -> {
						if (name.equals(p.getName())) {
							player[0] = p;
						}
					});
					return player[0];
				}
			}*/
			return null;
		}
		return result.getResultList().get(0);
	}

	public QueryResult<Player> list() {
		QueryResult<Player> queryResult = list(Query.getDefaultInstance());
		if (queryResult.getTotalSize() == 0) {
			List<Player> players = refreshPlayerList();
			return QueryResult.<Player>newBuilder()
					.setResultList(players)
					.setTotalSize(players.size())
					.build();
		}
		return queryResult;
	}

	public List<Player> refreshPlayerList() {
		Map<Integer, Player> playerMap = list(Query.getDefaultInstance()).getResultList()
				.stream()
				.collect(Collectors.toMap(Player::getPlayerId, Function.identity()));
		return apiDAO.listAllPlayers()
				.stream()
				.parallel()
				.map(player -> playerMap.computeIfAbsent(player.getPlayerId(), playerId -> create(player)))
				.collect(Collectors.toList());
	}
}
