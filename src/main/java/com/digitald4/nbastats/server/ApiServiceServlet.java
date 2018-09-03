package com.digitald4.nbastats.server;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.server.JSONServiceImpl;
import com.digitald4.common.server.SingleProtoService;
import com.digitald4.common.util.ProtoUtil;
import com.digitald4.nbastats.compute.StatsProcessor;
import com.digitald4.nbastats.proto.NBAStatsProtos.GameLog;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import com.digitald4.nbastats.server.LineUpService.LineUpJSONService;
import com.digitald4.nbastats.server.PlayerDayService.PlayerDayJSONService;
import com.digitald4.nbastats.storage.GameLogStore;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;

public class ApiServiceServlet extends com.digitald4.common.server.ApiServiceServlet {

	static {
		ProtoUtil.init(GeneralData.getDescriptor(), GameLog.getDescriptor());
	}

	public ApiServiceServlet() {
		PlayerStore playerStore = new PlayerStore(daoProvider, null);
		PlayerDayStore playerDayStore = new PlayerDayStore(daoProvider, null);
		GameLogStore gameLogStore = new GameLogStore(daoProvider, null);
		LineUpStore lineUpStore = new LineUpStore(daoProvider);
		StatsProcessor statsProcessor = new StatsProcessor(playerStore, gameLogStore, playerDayStore, lineUpStore);

		addService("player", new JSONServiceImpl<>(Player.class, new PlayerService(playerStore), false));
		addService("playerDay", new PlayerDayJSONService(new PlayerDayService(playerDayStore, statsProcessor)));
		addService("GameLog", new JSONServiceImpl<>(GameLog.class, new SingleProtoService<>(gameLogStore), false));
		addService("lineUp", new LineUpJSONService(new LineUpService(lineUpStore, statsProcessor)));
	}
}
