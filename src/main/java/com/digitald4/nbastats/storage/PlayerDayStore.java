package com.digitald4.nbastats.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class PlayerDayStore extends GenericStore<PlayerDay> {
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern(APIDAO.API_DATE_FORMAT);

	private final APIDAO apiDAO;
	private final PlayerStore playerStore;
	public PlayerDayStore(Provider<DAO> daoProvider, APIDAO apiDAO, PlayerStore playerStore) {
		super(PlayerDay.class, daoProvider);
		this.apiDAO = apiDAO;
		this.playerStore = playerStore;
	}

	public QueryResult<PlayerDay> list(DateTime date) {
		return list(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("as_of_date").setValue(date.toString(DATE_FORMAT)))
				.build());
	}

	@Override
	public QueryResult<PlayerDay> list(Query query) {
		if (query.getFilterCount() == 0) {
			query = query.toBuilder()
					.addFilter(Filter.newBuilder().setColumn("as_of_date").setValue(DateTime.now().toString(DATE_FORMAT)))
					.build();
		}
		QueryResult<PlayerDay> queryResult = super.list(query);
		if (queryResult.getTotalSize() == 0 && query.getFilterCount() == 1
				&& query.getFilter(0).getColumn().equals("as_of_date")) {
			DateTime date = DateTime.parse(query.getFilter(0).getValue(), DATE_FORMAT);
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

	public List<PlayerDay> processStats(DateTime date) {
		List<Player> playerList = playerStore.list(Query.getDefaultInstance()).getResultList();
		Map<String, Player> playerMap = playerList.stream()
				.parallel()
				.collect(Collectors.toMap(Player::getName, Function.identity()));
		playerMap.putAll(playerList.stream()
				.parallel()
				.filter(player -> !player.getAKA().isEmpty())
				.collect(Collectors.toMap(Player::getAKA, Function.identity())));
		return list(date).getResultList().stream()
				//.parallel()
				.map(playerDay -> {
					if (playerDay.getStatsCount() == 0) {
						Player player = playerMap.get(playerDay.getName());
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
	}
}