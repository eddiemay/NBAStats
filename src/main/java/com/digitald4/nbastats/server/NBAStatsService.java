package com.digitald4.nbastats.server;

import com.digitald4.common.model.Session;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.Store;

public class NBAStatsService<T, I> extends EntityServiceImpl<T, I> {
  public NBAStatsService(Store<T, I> store) {
    super(store, null);
  }

  @Override
  protected Session resolveLogin(String idToken, boolean requiresLogin) {
    // No login required for this api so do nothing.
    return null;
  }
}
