package com.digitald4.nbastats.server;

import com.digitald4.common.server.service.SingleProtoService;
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
		),
		// [START_EXCLUDE]
		issuers = {
				@ApiIssuer(
						name = "firebase",
						issuer = "https://securetoken.google.com/fantasy-predictor",
						jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
		}
		// [END_EXCLUDE]
)
public class PlayerService extends SingleProtoService<Player> {
	private final PlayerStore playerStore;

	@Inject
	public PlayerService(PlayerStore playerStore) {
		super(playerStore);
		this.playerStore = playerStore;
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "season/{season}")
	public QueryResult<Player> bySeason(@Named("season") String season) {
		return playerStore.list(season);
	}
}
