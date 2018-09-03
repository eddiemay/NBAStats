package com.digitald4.nbastats.server;

import com.digitald4.common.server.SingleProtoService;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import com.digitald4.nbastats.storage.PlayerStore;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiNamespace;


@Api(
		name = "player",
		version = "v1",
		namespace =
		@ApiNamespace(
				ownerDomain = "nbastats.digitald4.com",
				ownerName = "nbastats.digitald4.com",
				packagePath = ""
		),
		// [START_EXCLUDE]
		issuers = {
				@ApiIssuer(
						name = "firebase",
						issuer = "https://securetoken.google.com/fantasy-predictor",
						jwksUri =
								"https://www.googleapis.com/service_accounts/v1/metadata/x509/securetoken@system"
										+ ".gserviceaccount.com"
				)
		}
		// [END_EXCLUDE]
)
public class PlayerService extends SingleProtoService<Player> {
	public PlayerService(PlayerStore playerStore) {
		super(playerStore);
	}
}
