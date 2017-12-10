package com.digitald4.nbastats.tools;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOSQLImpl;
import com.digitald4.common.tools.DataImporter;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.storage.APIDAO;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import org.joda.time.DateTime;

public class ProcessLocal {

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		DAO dao = new DAOSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
				"jdbc:mysql://localhost/NBAStats?autoReconnect=true",
				"dd4_user", "getSchooled85"));
		Provider<DAO> daoProvider = () -> dao;
		APIDAO apiDAO = new APIDAO(new DataImporter(dao, null));
		PlayerDayStore playerDayStore = new PlayerDayStore(daoProvider, apiDAO,
				new PlayerStore(daoProvider, apiDAO));
		LineUpStore lineUpStore = new LineUpStore(daoProvider, playerDayStore);
		//System.out.println(playerDayStore.processStats(DateTime.now()).size());
		System.out.println("Valid Line Ups: " + lineUpStore.processDraftKings(DateTime.parse("2017-12-02")).size());
		//lineUpStore.processDraftKings(DateTime.now().minusHours(8));
		// System.out.println(apiDAO.fillStats(PlayerDay.newBuilder().setName("Marcus Smart").setPlayerId(203935).setAsOfDate("11/30/2017").build()));
		// System.out.println(apiDAO.fillStats(PlayerDay.newBuilder().setName("Lonzo Ball").setPlayerId(1628366).setAsOfDate("11/30/2017")).build());
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
	}
}
