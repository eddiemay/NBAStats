package com.digitald4.nbastats.server;

import com.digitald4.common.server.SingleProtoService;
import com.digitald4.nbastats.compute.StatsProcessor;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
import com.digitald4.nbastats.proto.NBAStatsProtos.ProcessStatsRequest;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.util.Constaints;
import com.google.protobuf.Empty;
import org.joda.time.DateTime;
import org.json.JSONObject;

public class LineUpService extends SingleProtoService<LineUp> {
	private final StatsProcessor statsProcessor;
	public LineUpService(LineUpStore lineUpStore, StatsProcessor statsProcessor) {
		super(lineUpStore);
		this.statsProcessor = statsProcessor;
	}

	public JSONObject updateActuals(JSONObject jsonRequest) {
		return convertToJSON(updateActuals(transformJSONRequest(ProcessStatsRequest.getDefaultInstance(), jsonRequest)));
	}

	public Empty updateActuals(ProcessStatsRequest request) {
		DateTime date = DateTime.parse(request.getDate(), Constaints.COMPUTER_DATE);
		new Thread(() -> statsProcessor.updateActuals(date)).start();
		return Empty.getDefaultInstance();
	}

	@Override
	public boolean requiresLogin(String action) {
		return false;
	}

	@Override
	public JSONObject performAction(String action, JSONObject request) {
		if (action.equals("updateActuals")) {
			return updateActuals(request);
		}
		return super.performAction(action, request);
	}
}
