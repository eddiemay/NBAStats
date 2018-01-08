package com.digitald4.nbastats.server;

import com.digitald4.common.server.SingleProtoService;
import com.digitald4.nbastats.compute.StatsProcessor;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.proto.NBAStatsProtos.ProcessStatsRequest;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.util.Constaints;
import com.google.protobuf.Empty;
import org.joda.time.DateTime;
import org.json.JSONObject;

public class PlayerDayService extends SingleProtoService<PlayerDay> {
	private final PlayerDayStore playerDayStore;
	private final StatsProcessor statsProcessor;
	public PlayerDayService(PlayerDayStore playerDayStore, StatsProcessor statsProcessor) {
		super(playerDayStore);
		this.playerDayStore = playerDayStore;
		this.statsProcessor = statsProcessor;
	}

	public JSONObject processStats(JSONObject jsonRequest) {
		return convertToJSON(processStats(transformJSONRequest(ProcessStatsRequest.getDefaultInstance(), jsonRequest)));
	}

	public Empty processStats(ProcessStatsRequest request) {
		DateTime date = DateTime.parse(request.getDate(), Constaints.COMPUTER_DATE);
		new Thread(() -> statsProcessor.processStats(date)).start();
		return Empty.getDefaultInstance();
	}

	@Override
	public boolean requiresLogin(String action) {
		return false;
	}

	@Override
	public JSONObject performAction(String action, JSONObject request) {
		if (action.equals("processStats")) {
			return processStats(request);
		}
		return super.performAction(action, request);
	}
}
