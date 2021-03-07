package com.digitald4.nbastats.server;

import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.nbastats.compute.StatsProcessor;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.util.Constaints;
import com.google.api.server.spi.config.*;
import com.google.protobuf.Empty;
import javax.inject.Inject;
import org.joda.time.DateTime;

@Api(
		name = "playerdays",
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
public class PlayerDayService extends EntityServiceImpl<PlayerDay> {
	private final StatsProcessor statsProcessor;

	@Inject
	PlayerDayService(PlayerDayStore playerDayStore, StatsProcessor statsProcessor) {
		super(playerDayStore);
		this.statsProcessor = statsProcessor;
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "processStats/{date}")
	public Empty processStats(@Named("date") String date) {
		new Thread(() -> statsProcessor.processStats(DateTime.parse(date, Constaints.COMPUTER_DATE))).start();
		return Empty.getDefaultInstance();
	}
}
