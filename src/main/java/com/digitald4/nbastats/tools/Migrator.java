package com.digitald4.nbastats.tools;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.storage.DAOCloudDS;
import com.digitald4.common.storage.DAORouterImpl;
import com.digitald4.common.storage.DAOSQLImpl;
import com.digitald4.common.storage.DAOHasProto;
import com.digitald4.nbastats.storage.LineUpStore;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import java.time.Clock;

public class Migrator {
	private final LineUpStore lineUpStore;
	public Migrator(LineUpStore lineUpStore) {
		this.lineUpStore = lineUpStore;
	}

	public void execute() {
		if (lineUpStore == null) {
			throw new RuntimeException("LineUpStore must not be null.");
		}
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		DAOSQLImpl messageDAO = new DAOSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
				"jdbc:mysql://localhost/NBAStats?autoReconnect=true",
				"dd4_user", "getSchooled85"));
		DAOCloudDS modelDao =
				new DAOCloudDS(DatastoreServiceFactory.getDatastoreService(), Clock.systemUTC());
		DAORouterImpl dao = new DAORouterImpl(messageDAO, new DAOHasProto(messageDAO), modelDao);
		LineUpStore lineUpStore = new LineUpStore(() -> dao);
		Migrator migrator = new Migrator(lineUpStore);
		migrator.execute();
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
	}
}
