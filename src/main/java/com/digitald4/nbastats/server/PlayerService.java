package com.digitald4.nbastats.server;

import com.digitald4.common.storage.QueryResult;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.storage.PlayerStore;
import com.google.api.server.spi.config.*;
import javax.inject.Inject;

@Api(
		name = "players",
		version = "v1",
		namespace = @ApiNamespace(
				ownerDomain = "nbastats.digitald4.com",
				ownerName = "nbastats.digitald4.com"
		)
)
public class PlayerService extends NBAStatsService<Player, Long> {
	private final PlayerStore playerStore;

	@Inject
	public PlayerService(PlayerStore playerStore) {
		super(playerStore);
		this.playerStore = playerStore;
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "season/{season}")
	public QueryResult<Player> bySeason(@Named("season") Integer season) {
		return playerStore.list(season);
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "echo")
	public Player echo(Player player) {
		return player;
	}
}
