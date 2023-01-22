package com.digitald4.nbastats.util;

import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;

public class FakeWebFetcher implements WebFetcher {

  @Override
  public ImmutableList<PlayerGameLog> getGames(Player player, String season, DateTime dateFrom) {
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<Player> listAllPlayers(String season) {
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<PlayerDay> getGameDay(DateTime date) {
    return ImmutableList.of();
  }
}
