package com.digitald4.nbastats.server;

import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.digitald4.nbastats.storage.PlayerGameLogStore;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiNamespace;
import javax.inject.Inject;

@Api(
    name = "playerGameLogs",
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
public class PlayerGameLogService extends EntityServiceImpl<PlayerGameLog> {
  @Inject
  PlayerGameLogService(PlayerGameLogStore playerGameLogStore) {
    super(playerGameLogStore);
  }
}
