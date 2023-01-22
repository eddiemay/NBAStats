package com.digitald4.nbastats.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.common.server.APIConnector;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.digitald4.nbastats.model.PlayerGameLog.Result;
import com.digitald4.nbastats.model.PlayerGameLog.Venue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public class WebFetcherBasketballReference implements WebFetcher {
  // Gamg log url example https://www.basketball-reference.com/players/j/jamesle01/gamelog/2023
  private static final String GAME_LOG_URL =
      "https://www.basketball-reference.com/players/%s/%s%s01/gamelog/%d";
  private static final Pattern TIME_PLAYED = Pattern.compile("(\\d+):(\\d+)");
  private static final Pattern AGE = Pattern.compile("(\\d+)-(\\d+)");

  private final APIConnector apiConnector;
  @Inject
  public WebFetcherBasketballReference(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public ImmutableList<PlayerGameLog> getGames(Player player, String season, DateTime dateFrom) {
    String name = player.getName();
    String lastName = name.substring(name.lastIndexOf(" ") + 1);
    if (lastName.length() > 5) {
      lastName = lastName.substring(0, 5);
    }
    String firstName = name.substring(0, 2);
    int seasonEndYear =
        Integer.parseInt(season.substring(0, 2) + season.substring(season.length() - 2));
    String url = String.format(GAME_LOG_URL, lastName.charAt(0), lastName, firstName, seasonEndYear);
    Document doc = Jsoup.parse(apiConnector.sendGet(url).trim(), "", Parser.xmlParser());

    return doc.getElementsByTag("tr").stream()
        .filter(tr -> tr.id().startsWith("pgl_basic."))
        .map(tr -> tr.getElementsByTag("td"))
        .map(tds -> {
          int i = 0;
          boolean isRoad = tds.get(4).text().contains("@");
          return WebFetcher.fillFantasy(
              new PlayerGameLog()
                  .setPlayerId(player.getId())
                  .setDate(tds.get(++i).text())
                  .setAge(toAge(tds.get(++i).text()))
                  .setMatchUp(
                      String.format("%s %s %s",
                          tds.get(++i).text(),
                          tds.get(++i).text().equals("@") ? "@" : "vs.",
                          tds.get(++i).text()))
                  .setVenue(isRoad ? Venue.ROAD : Venue.HOME)
                  .setResult(tds.get(++i).text().startsWith("W") ? Result.W : Result.L)
                  .setStarted(tds.get(++i).text().equals("1"))
                  .setMinutes(toMinutes(tds.get(++i).text()))
                  .setFieldGoalsMade(Double.parseDouble(tds.get(++i).text()))
                  .setFieldGoalsAtts(Double.parseDouble(tds.get(++i).text()))
                  .setFieldGoalPercent(Double.parseDouble(tds.get(++i).text()))
                  .setThreePointersMade(Double.parseDouble(tds.get(++i).text()))
                  .setThreePointerAtts(Double.parseDouble(tds.get(++i).text()))
                  .setThreePointerPercent(Double.parseDouble(tds.get(++i).text()))
                  .setFreeThrowsMade(Double.parseDouble(tds.get(++i).text()))
                  .setFreeThrowAtts(Double.parseDouble(tds.get(++i).text()))
                  .setFreeThrowPercent(Double.parseDouble(tds.get(++i).text()))
                  .setOffensiveRebounds(Double.parseDouble(tds.get(++i).text()))
                  .setDefensiveRebounds(Double.parseDouble(tds.get(++i).text()))
                  .setRebounds(Double.parseDouble(tds.get(++i).text()))
                  .setAssists(Double.parseDouble(tds.get(++i).text()))
                  .setSteals(Double.parseDouble(tds.get(++i).text()))
                  .setBlocks(Double.parseDouble(tds.get(++i).text()))
                  .setTurnovers(Double.parseDouble(tds.get(++i).text()))
                  .setPersonalFouls(Double.parseDouble(tds.get(++i).text()))
                  .setPoints(Double.parseDouble(tds.get(++i).text()))
                  // Skip GameSc, don't know what that is.
                  .setPlusMinus(Double.parseDouble(tds.get(i + 2).text())));
        })
        .collect(toImmutableList());
  }

  private static double toAge(String value) {
    Matcher matcher = AGE.matcher(value);
    if (matcher.matches()) {
      return Double.parseDouble(matcher.group(1)) + Double.parseDouble(matcher.group(2)) / 365.2425;
    }

    throw new RuntimeException(String.format("%s does not match format #-#", value));
  }

  private static double toMinutes(String value) {
    Matcher matcher = TIME_PLAYED.matcher(value);
    if (matcher.matches()) {
      return Double.parseDouble(matcher.group(1)) + Double.parseDouble(matcher.group(2)) / 60.0;
    }

    throw new RuntimeException(String.format("%s does not match format #:#", value));
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
