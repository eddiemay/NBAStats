package com.digitald4.nbastats.storage;

import com.digitald4.common.model.HasProto;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.ModelStore;
import com.digitald4.nbastats.model.LineUp;
import javax.inject.Inject;
import javax.inject.Provider;

public class LineUpStore extends ModelStore<LineUp> {
  @Inject
  public LineUpStore(Provider<DAO<HasProto>> daoProvider) {
    super(LineUp.class, daoProvider);
  }
}