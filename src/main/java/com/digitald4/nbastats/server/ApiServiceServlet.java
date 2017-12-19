package com.digitald4.nbastats.server;

import com.digitald4.common.server.SingleProtoService;
import com.digitald4.common.tools.DataImporter;
import com.digitald4.nbastats.storage.APIDAO;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import javax.servlet.ServletException;

public class ApiServiceServlet extends com.digitald4.common.server.ApiServiceServlet {

	public ApiServiceServlet() throws ServletException {
		APIDAO apiDAO = new APIDAO(new DataImporter(null, null));
		PlayerStore playerStore = new PlayerStore(dataAccessObjectProvider, apiDAO);
		PlayerDayStore playerDayStore = new PlayerDayStore(dataAccessObjectProvider, apiDAO, playerStore);
		LineUpStore lineUpStore = new LineUpStore(dataAccessObjectProvider, playerDayStore);

		addService("player", new SingleProtoService<>(playerStore).setDefaultRequiresLogin(false));
		addService("playerDay", new SingleProtoService<>(playerDayStore).setDefaultRequiresLogin(false));
		addService("lineUp", new SingleProtoService<>(lineUpStore).setDefaultRequiresLogin(false));
	}
}
