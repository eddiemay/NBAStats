package com.digitald4.nbastats.server;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.server.SingleProtoService;
import com.digitald4.common.util.ProtoUtil;
import com.digitald4.nbastats.compute.StatsProcessor;
import com.digitald4.nbastats.proto.NBAStatsProtos.GameLog;
import com.digitald4.nbastats.storage.APIDAO;
import com.digitald4.nbastats.storage.GameLogStore;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import javax.servlet.ServletException;

public class ApiServiceServlet extends com.digitald4.common.server.ApiServiceServlet {

	static {
		ProtoUtil.init(GeneralData.getDescriptor(), GameLog.getDescriptor());
	}

	public ApiServiceServlet() throws ServletException {
		APIDAO apiDAO = null;
		PlayerStore playerStore = new PlayerStore(daoProvider, apiDAO);
		PlayerDayStore playerDayStore = new PlayerDayStore(daoProvider, apiDAO);
		GameLogStore gameLogStore = new GameLogStore(daoProvider, apiDAO);
		LineUpStore lineUpStore = new LineUpStore(daoProvider);
		StatsProcessor statsProcessor = new StatsProcessor(playerStore, gameLogStore, playerDayStore, lineUpStore);

		addService("player", new PlayerService(playerStore));
		addService("playerDay", new PlayerDayService(playerDayStore, statsProcessor));
		addService("GameLog", new SingleProtoService<GameLog>(gameLogStore) {
			@Override
			public boolean requiresLogin(String action) {
				return false;
			}
		});
		addService("lineUp", new LineUpService(lineUpStore, statsProcessor));
	}
}
