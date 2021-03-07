package com.digitald4.nbastats.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.server.APIConnector;
import com.digitald4.nbastats.model.Player;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class NBAApiDAOTest {
  private static final String TEAM_YEARS_RESULT = "{\"resource\":\"commonteamyears\",\"parameters\":{\"LeagueID\":\"00\"},"
      + "\"resultSets\":[{\"name\":\"TeamYears\",\"headers\":[\"LEAGUE_ID\",\"TEAM_ID\",\"MIN_YEAR\",\"MAX_YEAR\",\"ABBREVIATION\"],"
      + "\"rowSet\":[[\"00\",1610612762,\"1974\",\"2020\",\"UTA\"],[\"00\",1610612737,\"1949\",\"2020\",\"ATL\"],"
      + "[\"00\",1610612738,\"1946\",\"2020\",\"BOS\"],[\"00\",1610612747,\"1948\",\"2020\",\"LAL\"],"
      + "[\"00\",1610610024,\"1947\",\"1954\",null],[\"00\",1610610024,\"1947\",\"1949\",null]]}]}";
  private static final String TEAM_ROSTER_QUERY =
      "https://stats.nba.com/stats/commonteamroster?LeagueID=00&Season=2020-21&TeamID=1610612750";
  private static final String TEAM_ROSTER_RESULT = "{\"resource\":\"commonteamroster\","
      + "\"parameters\":{\"TeamID\":1610612750,\"LeagueID\":\"00\",\"Season\":\"2020-21\"},"
      + "\"resultSets\":[{\"name\":\"CommonTeamRoster\","
      + "\"headers\":[\"TeamID\",\"SEASON\",\"LeagueID\",\"PLAYER\",\"PLAYER_SLUG\",\"NUM\",\"POSITION\",\"HEIGHT\","
      + "\"WEIGHT\",\"BIRTH_DATE\",\"AGE\",\"EXP\",\"SCHOOL\",\"PLAYER_ID\"],\"rowSet\":["
      + "[1610612750,\"2020\",\"00\",\"D'Angelo Russell\",\"dangelo-russell\",\"0\",\"G\",\"6-4\",\"193\",\"FEB 23, 1996\",24.0,\"5\",\"Ohio State\",1626156],"
      + "[1610612750,\"2020\",\"00\",\"Anthony Edwards\",\"anthony-edwards\",\"1\",\"G\",\"6-4\",\"225\",\"AUG 05, 2001\",19.0,\"R\",\"Georgia\",1630162],"
      + "[1610612750,\"2020\",\"00\",\"Jaden McDaniels\",\"jaden-mcdaniels\",\"3\",\"F\",\"6-9\",\"185\",\"SEP 29, 2000\",20.0,\"R\",\"Washington\",1630183],"
      + "[1610612750,\"2020\",\"00\",\"Jaylen Nowell\",\"jaylen-nowell\",\"4\",\"G\",\"6-4\",\"201\",\"JUL 09, 1999\",21.0,\"1\",\"Washington\",1629669],"
      + "[1610612750,\"2020\",\"00\",\"Malik Beasley\",\"malik-beasley\",\"5\",\"G\",\"6-4\",\"187\",\"NOV 26, 1996\",24.0,\"4\",\"Florida State\",1627736]]}]}";

  private final @Mock APIConnector apiConnector = mock(APIConnector.class);
  private final NBAApiDAO apiDao = new NBAApiDAO(apiConnector);

  @Before
  public void setup() {
    when(apiConnector.sendGet(NBAApiDAO.COMMON_TEAM_YEARS)).thenReturn(TEAM_YEARS_RESULT);
    when(apiConnector.sendGet(TEAM_ROSTER_QUERY)).thenReturn(TEAM_ROSTER_RESULT);
  }

  @Test
  public void getActiveTeamIds() {
    ImmutableList<Long> teamIds = apiDao.getActiveTeamIds();
    assertThat(teamIds).containsExactly(1610612762L, 1610612737L, 1610612738L, 1610612747L);
  }

  @Test
  public void getTeamRoster() {
    ImmutableList<String> playerNames = apiDao.getTeamRoster("2020-21", 1610612750).stream()
        .map(Player::getName)
        .collect(toImmutableList());
    assertThat(playerNames).containsExactly(
        "D'Angelo Russell", "Anthony Edwards", "Jaden McDaniels", "Jaylen Nowell", "Malik Beasley");
  }

  @Test
  public void testNBADataResult() {
    NBAApiDAO.NBADataResult result = new NBAApiDAO.NBADataResult(TEAM_YEARS_RESULT);

    assertEquals("LEAGUE_ID", result.getHeaders().get(0));
    assertEquals("TEAM_ID", result.getHeaders().get(1));
    assertEquals("MIN_YEAR", result.getHeaders().get(2));
    assertEquals("MAX_YEAR", result.getHeaders().get(3));
    assertEquals("ABBREVIATION", result.getHeaders().get(4));

    assertEquals(6, result.getResultCount());
    assertEquals(1610612762L, result.getLong("TEAM_ID", 0));
    assertEquals("ATL", result.getString("ABBREVIATION", 1));
    assertEquals("1946", result.getString("MIN_YEAR", 2));
    assertEquals("LAL", result.getString("ABBREVIATION", 3));
  }
}
