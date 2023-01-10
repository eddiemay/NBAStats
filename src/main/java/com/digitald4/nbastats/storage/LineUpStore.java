package com.digitald4.nbastats.storage;

import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.nbastats.model.LineUp;
import javax.inject.Inject;
import javax.inject.Provider;

public class LineUpStore extends GenericStore<LineUp, Long> {
  @Inject
  public LineUpStore(Provider<DAO> daoProvider) {
    super(LineUp.class, daoProvider);
  }
}