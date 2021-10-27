package com.digitald4.nbastats.server;

import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.nbastats.compute.StatsProcessor;
import com.digitald4.nbastats.model.LineUp;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.util.Constaints;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.protobuf.Empty;
import javax.inject.Inject;
import org.joda.time.DateTime;

@Api(
		name = "lineUps",
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
public class LineUpService extends NBAStatsService<LineUp> {
	private final StatsProcessor statsProcessor;

	@Inject
	LineUpService(LineUpStore lineUpStore, StatsProcessor statsProcessor) {
		super(lineUpStore);
		this.statsProcessor = statsProcessor;
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "updateActuals/{date}")
	Empty updateActuals(@Named("date") String date) {
		new Thread(() -> statsProcessor.updateActuals(DateTime.parse(date, Constaints.COMPUTER_DATE))).start();
		return Empty.getDefaultInstance();
	}
}
