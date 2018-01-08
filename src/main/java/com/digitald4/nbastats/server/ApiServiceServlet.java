package com.digitald4.nbastats.server;

import com.digitald4.common.server.SingleProtoService;
import com.digitald4.common.tools.DataImporter;
import com.digitald4.nbastats.compute.StatsProcessor;
import com.digitald4.nbastats.proto.NBAStatsProtos.GameLog;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
import com.digitald4.nbastats.storage.APIDAO;
import com.digitald4.nbastats.storage.GameLogStore;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import javax.servlet.ServletException;

public class ApiServiceServlet extends com.digitald4.common.server.ApiServiceServlet {

	public ApiServiceServlet() throws ServletException {
		APIDAO apiDAO = new APIDAO(new DataImporter(null, null));
		PlayerStore playerStore = new PlayerStore(daoProvider, apiDAO);
		PlayerDayStore playerDayStore = new PlayerDayStore(daoProvider, apiDAO);
		GameLogStore gameLogStore = new GameLogStore(daoProvider, apiDAO);
		StatsProcessor statsProcessor = new StatsProcessor(playerStore, gameLogStore, playerDayStore);

		addService("player", new PlayerService(playerStore));
		addService("playerDay", new PlayerDayService(playerDayStore, statsProcessor));
		addService("GameLog", new SingleProtoService<GameLog>(gameLogStore) {
			@Override
			public boolean requiresLogin(String action) {
				return false;
			}
		});
		addService("lineUp", new SingleProtoService<LineUp>(new LineUpStore(daoProvider)) {
			@Override
			public boolean requiresLogin(String action) {
				return false;
			}
		});
	}
}
