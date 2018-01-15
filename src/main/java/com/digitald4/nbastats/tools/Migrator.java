package com.digitald4.nbastats.tools;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOSQLImpl;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.storage.LineUpStore;
import java.util.stream.Collectors;

public class Migrator {
	private final LineUpStore lineUpStore;
	public Migrator(LineUpStore lineUpStore) {
		this.lineUpStore = lineUpStore;
	}

	public void execute() {
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		DAO dao = new DAOSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
				"jdbc:mysql://localhost/NBAStats?autoReconnect=true",
				"dd4_user", "getSchooled85"));
		Provider<DAO> daoProvider = () -> dao;
		LineUpStore lineUpStore = new LineUpStore(daoProvider);
		Migrator migrator = new Migrator(lineUpStore);
		migrator.execute();
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
	}
}
