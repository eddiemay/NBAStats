package com.digitald4.nbastats.server;

import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.Store;

public class NBAStatsService<T, I> extends EntityServiceImpl<T, I> {
  public NBAStatsService(Store<T, I> store) {
    super(store, null, false);
  }

  @Override
  protected void resolveLogin(String idToken, String method) {
    // No login required for this api so do nothing.
  }
}
