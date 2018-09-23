package com.digitald4.nbastats.server;

import com.digitald4.common.server.JSONServiceImpl;
import com.digitald4.common.server.SingleProtoService;
import com.digitald4.common.util.ProtoUtil;
import com.digitald4.nbastats.compute.StatsProcessor;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
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
import org.json.JSONObject;

@Api(
		name = "lineups",
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
public class LineUpService extends SingleProtoService<LineUp> {
	private final StatsProcessor statsProcessor;

	@Inject
	public LineUpService(LineUpStore lineUpStore, StatsProcessor statsProcessor) {
		super(lineUpStore);
		this.statsProcessor = statsProcessor;
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "updateActuals/{date}")
	public Empty updateActuals(@Named("date") String date) {
		new Thread(() -> statsProcessor.updateActuals(DateTime.parse(date, Constaints.COMPUTER_DATE))).start();
		return Empty.getDefaultInstance();
	}

	static class LineUpJSONService extends JSONServiceImpl<LineUp> {

		private LineUpService lineUpService;
		LineUpJSONService(LineUpService lineUpService) {
			super(lineUpService, false);
			this.lineUpService = lineUpService;
		}

		@Override
		public JSONObject performAction(String action, JSONObject request) {
			if (action.equals("updateActuals")) {
				return ProtoUtil.toJSON(lineUpService.updateActuals(request.getString("date")));
			}
			return super.performAction(action, request);
		}
	}
}
