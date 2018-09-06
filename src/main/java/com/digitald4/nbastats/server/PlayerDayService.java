package com.digitald4.nbastats.server;

import com.digitald4.common.server.JSONServiceImpl;
import com.digitald4.common.server.SingleProtoService;
import com.digitald4.common.util.ProtoUtil;
import com.digitald4.nbastats.compute.StatsProcessor;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.util.Constaints;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.protobuf.Empty;
import javax.inject.Inject;
import org.joda.time.DateTime;
import org.json.JSONObject;

@Api(
		name = "playerDay",
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
public class PlayerDayService extends SingleProtoService<PlayerDay> {
	private final StatsProcessor statsProcessor;

	@Inject
	public PlayerDayService(PlayerDayStore playerDayStore, StatsProcessor statsProcessor) {
		super(playerDayStore);
		this.statsProcessor = statsProcessor;
	}

	@ApiMethod(name = "processStats", httpMethod = ApiMethod.HttpMethod.GET)
	public Empty processStats(@Named("date") String date) {
		new Thread(() -> statsProcessor.processStats(DateTime.parse(date, Constaints.COMPUTER_DATE))).start();
		return Empty.getDefaultInstance();
	}

	static class PlayerDayJSONService extends JSONServiceImpl<PlayerDay> {

		private PlayerDayService playerDayService;
		public PlayerDayJSONService(PlayerDayService playerDayService) {
			super(PlayerDay.class, playerDayService,  false);
			this.playerDayService = playerDayService;
		}

		@Override
		public JSONObject performAction(String action, JSONObject request) {
			if (action.equals("processStats")) {
				return ProtoUtil.toJSON(playerDayService.processStats(request.getString("date")));
			}
			return super.performAction(action, request);
		}
	}
}
