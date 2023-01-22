package com.digitald4.nbastats.util;

import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;

public interface WebFetcher {
  ImmutableList<PlayerGameLog> getGames(Player player, String season, DateTime dateFrom);
  ImmutableList<Player> listAllPlayers(String season);
  ImmutableList<PlayerDay> getGameDay(DateTime date);

  static PlayerGameLog fillFantasy(PlayerGameLog stats) {
    int doubles = 0;
    if (stats.getPoints() >= 10) doubles++;
    if (stats.getRebounds() >= 10) doubles++;
    if (stats.getAssists() >= 10) doubles++;
    if (stats.getBlocks() >= 10) doubles++;
    if (stats.getSteals() >= 10) doubles++;
    return stats
        .setDoubleDouble(doubles >= 2)
        .setTripleDouble(doubles >= 3)
        .setFantasySitePoints("draftkings",
            stats.getPoints()
                + stats.getThreePointersMade() * .5
                + stats.getRebounds() * 1.25
                + stats.getAssists() * 1.5
                + stats.getSteals() * 2
                + stats.getBlocks() * 2
                - stats.getTurnovers() * .5
                + (stats.isDoubleDouble() ? 1.5 : 0)
                + (stats.isTripleDouble() ? 1.5 : 0))
        .setFantasySitePoints("fanduel",
            stats.getPoints()
                + stats.getRebounds() * 1.2
                + stats.getAssists() * 1.5
                + stats.getBlocks() * 3
                + stats.getSteals() * 3
                - stats.getTurnovers());
  }
}
