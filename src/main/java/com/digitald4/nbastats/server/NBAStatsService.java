package com.digitald4.nbastats.server;

import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.FakeLoginResolver;
import com.digitald4.common.storage.Store;

public class NBAStatsService<T> extends EntityServiceImpl<T> {
  public NBAStatsService(Store<T> store) {
    super(store, new FakeLoginResolver(), false);
  }
}
