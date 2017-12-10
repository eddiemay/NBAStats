package com.digitald4.nbastats.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.joda.time.DateTime;

public class PlayerDayStore extends GenericStore<PlayerDay> {

	private final APIDAO apiDAO;
	private final PlayerStore playerStore;
	public PlayerDayStore(Provider<DAO> cacheDAOProvider, APIDAO apiDAO, PlayerStore playerStore) {
		super(PlayerDay.class, cacheDAOProvider);
		this.apiDAO = apiDAO;
		this.playerStore = playerStore;
	}

	public QueryResult<PlayerDay> list(DateTime date) {
		QueryResult<PlayerDay> queryResult = list(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("as_of_date").setValue(FormatText.formatDate(date)))
				.build());
		if (queryResult.getTotalSize() == 0) {
			List<PlayerDay> playerDays = apiDAO.getGameDay(date).stream().parallel()
					.map(this::create)
					.collect(Collectors.toList());
			return QueryResult.<PlayerDay>newBuilder()
					.setResultList(playerDays)
					.setTotalSize(playerDays.size())
					.build();
		}
		return queryResult;
	}

	public List<PlayerDay> processStats(DateTime date) {
		List<Player> playerList = playerStore.list().getResultList();
		Map<String, Player> playerMap = playerList.stream()
				.parallel()
				.collect(Collectors.toMap(Player::getName, Function.identity()));
		playerMap.putAll(playerList.stream()
				.parallel()
				.filter(player -> !player.getAKA().isEmpty())
				.collect(Collectors.toMap(Player::getAKA, Function.identity())));
		List<PlayerDay> playerDays = list(date).getResultList().stream()
				//.parallel()
				.map(playerDay -> {
					if (playerDay.getStatsCount() == 0) {
						Player player = playerMap.computeIfAbsent(playerDay.getName(), playerStore::getByName);
						if (player != null) {
							PlayerDay statsFilled = apiDAO.fillStats(playerDay.toBuilder()
									.setPlayerId(player.getPlayerId())
									.build());
							return update(playerDay.getId(), current -> current.toBuilder()
									.setPlayerId(player.getPlayerId())
									.clearStats()
									.putAllStats(statsFilled.getStatsMap())
									.build());
						}
					}
					return playerDay;
				})
				.collect(Collectors.toList());
		return playerDays;
	}
}