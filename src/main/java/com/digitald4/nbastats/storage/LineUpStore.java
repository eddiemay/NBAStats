package com.digitald4.nbastats.storage;

import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;

public class LineUpStore extends GenericStore<LineUp> {
	public LineUpStore(Provider<DAO> daoProvider) {
		super(LineUp.class, daoProvider);
	}
}
