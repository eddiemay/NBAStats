package com.digitald4.nbastats.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitald4.common.server.APIConnector;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.digitald4.nbastats.model.PlayerGameLog.Result;
import com.digitald4.nbastats.model.PlayerGameLog.Venue;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.mockito.Mock;

public class WebFetcherBasketballReferenceTest {
  private final @Mock APIConnector apiConnector = mock(APIConnector.class);
  private final WebFetcher webFetcher = new WebFetcherBasketballReference(apiConnector);

  @Test
  public void getGames_correctUrl() {
    when(apiConnector.sendGet(anyString()))
        .thenReturn("<html><body><table><tr></tr></table></body></html>");

    webFetcher.getGames(
        new Player().setId(2544).setName("LeBron James"), "2022-23", null);
    verify(apiConnector)
        .sendGet("https://www.basketball-reference.com/players/J/JamesLe01/gamelog/2023");

    webFetcher.getGames(
        new Player().setId(2408).setName("Kobe Bryant"), "2015-16", null);
    verify(apiConnector)
        .sendGet("https://www.basketball-reference.com/players/B/BryanKo01/gamelog/2016");
  }

  @Test
  public void getGames_correctData() {
    when(apiConnector.sendGet(anyString())).thenReturn("<html><body><table>" +
        "<tr id=\"pgl_basic.1394\" data-row=\"36\" class=\"rowSum\"><th scope=\"row\" class=\"right \" data-stat=\"ranker\" csk=\"36\">36</th><td class=\"right endpoint tooltip\" data-endpoint=\"/players/pgl_cum_stats.cgi?player=jamesle01&amp;year=2023&amp;date_game=2022-12-30&amp;is_playoff_game=N\" data-stat=\"game_season\"><strong>28</strong></td><td class=\"left \" data-stat=\"date_game\"><a href=\"/boxscores/202212300ATL.html\">2022-12-30</a></td><td class=\"right \" data-stat=\"age\">38-000</td><td class=\"left \" data-stat=\"team_id\"><a href=\"/teams/LAL/2023.html\">LAL</a></td><td class=\"center \" data-stat=\"game_location\">@</td><td class=\"left \" data-stat=\"opp_id\"><a href=\"/teams/ATL/2023.html\">ATL</a></td><td class=\"center \" data-stat=\"game_result\" csk=\"9\">W (+9)</td><td class=\"right \" data-stat=\"gs\">1</td><td class=\"right \" data-stat=\"mp\" csk=\"2379\">39:39</td><td class=\"right \" data-stat=\"fg\">18</td><td class=\"right \" data-stat=\"fga\">27</td><td class=\"right \" data-stat=\"fg_pct\">.667</td><td class=\"right \" data-stat=\"fg3\">4</td><td class=\"right \" data-stat=\"fg3a\">6</td><td class=\"right \" data-stat=\"fg3_pct\">.667</td><td class=\"right \" data-stat=\"ft\">7</td><td class=\"right \" data-stat=\"fta\">9</td><td class=\"right \" data-stat=\"ft_pct\">.778</td><td class=\"right \" data-stat=\"orb\">2</td><td class=\"right \" data-stat=\"drb\">8</td><td class=\"right \" data-stat=\"trb\">10</td><td class=\"right \" data-stat=\"ast\">10</td><td class=\"right iz\" data-stat=\"stl\">0</td><td class=\"right \" data-stat=\"blk\">1</td><td class=\"right \" data-stat=\"tov\">5</td><td class=\"right iz\" data-stat=\"pf\">0</td><td class=\"right \" data-stat=\"pts\">47</td><td class=\"right \" data-stat=\"game_score\">40.3</td><td class=\"right \" data-stat=\"plus_minus\">+12</td></tr>" +
        "<tr id=\"pgl_basic.1400\" data-row=\"45\"><th scope=\"row\" class=\"right \" data-stat=\"ranker\" csk=\"44\">44</th><td class=\"right endpoint tooltip\" data-endpoint=\"/players/pgl_cum_stats.cgi?player=jamesle01&amp;year=2023&amp;date_game=2023-01-16&amp;is_playoff_game=N\" data-stat=\"game_season\"><strong>34</strong></td><td class=\"left \" data-stat=\"date_game\"><a href=\"/boxscores/202301160LAL.html\">2023-01-16</a></td><td class=\"right \" data-stat=\"age\">38-017</td><td class=\"left \" data-stat=\"team_id\"><a href=\"/teams/LAL/2023.html\">LAL</a></td><td class=\"center iz\" data-stat=\"game_location\"></td><td class=\"left \" data-stat=\"opp_id\"><a href=\"/teams/HOU/2023.html\">HOU</a></td><td class=\"center \" data-stat=\"game_result\" csk=\"8\">W (+8)</td><td class=\"right \" data-stat=\"gs\">0</td><td class=\"right \" data-stat=\"mp\" csk=\"2151\">35:51</td><td class=\"right \" data-stat=\"fg\">16</td><td class=\"right \" data-stat=\"fga\">26</td><td class=\"right \" data-stat=\"fg_pct\">.615</td><td class=\"right \" data-stat=\"fg3\">5</td><td class=\"right \" data-stat=\"fg3a\">10</td><td class=\"right \" data-stat=\"fg3_pct\">.500</td><td class=\"right \" data-stat=\"ft\">11</td><td class=\"right \" data-stat=\"fta\">12</td><td class=\"right \" data-stat=\"ft_pct\">.917</td><td class=\"right \" data-stat=\"orb\">1</td><td class=\"right \" data-stat=\"drb\">7</td><td class=\"right \" data-stat=\"trb\">8</td><td class=\"right \" data-stat=\"ast\">9</td><td class=\"right iz\" data-stat=\"stl\">0</td><td class=\"right iz\" data-stat=\"blk\">0</td><td class=\"right iz\" data-stat=\"tov\">0</td><td class=\"right \" data-stat=\"pf\">1</td><td class=\"right \" data-stat=\"pts\">48</td><td class=\"right \" data-stat=\"game_score\">44.5</td><td class=\"right \" data-stat=\"plus_minus\">+19</td></tr>" +
        "<tr id=\"pgl_basic.1401\" data-row=\"46\"><th scope=\"row\" class=\"right \" data-stat=\"ranker\" csk=\"45\">45</th><td class=\"right endpoint tooltip\" data-endpoint=\"/players/pgl_cum_stats.cgi?player=jamesle01&amp;year=2023&amp;date_game=2023-01-18&amp;is_playoff_game=N\" data-stat=\"game_season\"><strong>35</strong></td><td class=\"left \" data-stat=\"date_game\"><a href=\"/boxscores/202301180LAL.html\">2023-01-18</a></td><td class=\"right \" data-stat=\"age\">38-019</td><td class=\"left \" data-stat=\"team_id\"><a href=\"/teams/LAL/2023.html\">LAL</a></td><td class=\"center iz\" data-stat=\"game_location\"></td><td class=\"left \" data-stat=\"opp_id\"><a href=\"/teams/SAC/2023.html\">SAC</a></td><td class=\"center \" data-stat=\"game_result\" csk=\"-5\">L (-5)</td><td class=\"right \" data-stat=\"gs\">1</td><td class=\"right \" data-stat=\"mp\" csk=\"2190\">36:30</td><td class=\"right \" data-stat=\"fg\">11</td><td class=\"right \" data-stat=\"fga\">25</td><td class=\"right \" data-stat=\"fg_pct\">.440</td><td class=\"right \" data-stat=\"fg3\">2</td><td class=\"right \" data-stat=\"fg3a\">9</td><td class=\"right \" data-stat=\"fg3_pct\">.222</td><td class=\"right \" data-stat=\"ft\">8</td><td class=\"right \" data-stat=\"fta\">9</td><td class=\"right \" data-stat=\"ft_pct\">.889</td><td class=\"right iz\" data-stat=\"orb\">0</td><td class=\"right \" data-stat=\"drb\">8</td><td class=\"right \" data-stat=\"trb\">8</td><td class=\"right \" data-stat=\"ast\">9</td><td class=\"right \" data-stat=\"stl\">1</td><td class=\"right \" data-stat=\"blk\">1</td><td class=\"right \" data-stat=\"tov\">1</td><td class=\"right iz\" data-stat=\"pf\">0</td><td class=\"right \" data-stat=\"pts\">32</td><td class=\"right \" data-stat=\"game_score\">27.9</td><td class=\"right \" data-stat=\"plus_minus\">-16</td></tr>" +
        "</table></body></html>");

    ImmutableList<PlayerGameLog> gameLogs = webFetcher.getGames(
        new Player().setId(2544).setName("LeBron James"), "2022-23", null);

    assertThat(gameLogs).containsExactly(
        createGamelog("2022-12-30", 38.0, "LAL @ ATL", Venue.ROAD,
            Result.W, true, 39.65, 18, 27, 4, 6, 7, 9,
            2, 8, 10, 0, 1, 5, 0, 12),
        createGamelog("2023-01-16", 38.047, "LAL vs. HOU", Venue.HOME,
            Result.W, false, 35.85, 16, 26, 5, 10, 11, 12,
            1, 7, 9, 0, 0, 0, 1, 19),
        createGamelog("2023-01-18", 38.052, "LAL vs. SAC", Venue.HOME,
            Result.L, true, 36.5, 11, 25, 2, 9, 8, 9,
            0, 8, 9, 1, 1, 1, 0, -16));
  }

  private static PlayerGameLog createGamelog(
      String date, double age, String matchUp, Venue venue, Result result, boolean started,
      double minutes, double fgm, double fga, double tpm, double tpa, double ftm, double fta,
      double orb, double drb, double ast, double stl, double blk, double tov, double pf, double pm) {
    return WebFetcher.fillFantasy(
        new PlayerGameLog()
            .setPlayerId(2544)
            .setDate(date)
            .setAge(age)
            .setMatchUp(matchUp)
            .setVenue(venue)
            .setResult(result)
            .setStarted(started)
            .setMinutes(minutes)
            .setFieldGoalsMade(fgm)
            .setFieldGoalsAtts(fga)
            .setThreePointersMade(tpm)
            .setThreePointerAtts(tpa)
            .setFreeThrowsMade(ftm)
            .setFreeThrowAtts(fta)
            .setOffensiveRebounds(orb)
            .setDefensiveRebounds(drb)
            .setRebounds(orb + drb)
            .setAssists(ast)
            .setSteals(stl)
            .setBlocks(blk)
            .setTurnovers(tov)
            .setPersonalFouls(pf)
            .setPoints(fgm * 2 + tpm + ftm)
            .setPlusMinus(pm));
  }
}
