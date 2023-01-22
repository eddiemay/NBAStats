package com.digitald4.nbastats.machinelearning;

import com.digitald4.common.server.APIConnector;
import com.digitald4.nbastats.model.Player;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.digitald4.nbastats.util.WebFetcher;
import com.digitald4.nbastats.util.WebFetcherBasketballReference;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class FetchFantasyTrainingData {

  private final WebFetcher webFetcher;
  @Inject
  public FetchFantasyTrainingData(WebFetcher webFetcher) {
    this.webFetcher = webFetcher;
  }

  public void output(Player player) {
    ImmutableList<PlayerGameLog> gameLogs = webFetcher.getGames(player, "2022-23", null);
    System.out.printf("Found %d games\n", gameLogs.size());

    System.out.println(
        "Player,Date,Mins,FGM,FGA,3PM,3PA,FTM,FTA,ORB,DRB,REB,AST,STL,BLK,TOV,PF,+/-,PTS,DD,TD,FDP,DKP");
    gameLogs.forEach(
        gl -> System.out.printf(
            // "%s,%s,%.1f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%d,%d,%.1f,%.1f\n",
            "%s,%s,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%d,%d,%.1f,%.1f\n",
            gl.getPlayerId(),
            gl.getDate(),
            gl.getMinutes(),
            gl.getFieldGoalsMade(),
            gl.getFieldGoalsAtts(),
            gl.getThreePointersMade(),
            gl.getThreePointerAtts(),
            gl.getFreeThrowsMade(),
            gl.getFreeThrowAtts(),
            gl.getOffensiveRebounds(),
            gl.getDefensiveRebounds(),
            gl.getRebounds(),
            gl.getAssists(),
            gl.getSteals(),
            gl.getBlocks(),
            gl.getTurnovers(),
            gl.getPersonalFouls(),
            gl.getPlusMinus(),
            gl.getPoints(),
            gl.isDoubleDouble() ? 1 : 0,
            gl.isTripleDouble() ? 1 : 0,
            gl.getFantasySitePoints("fanduel"),
            gl.getFantasySitePoints("draftkings")));
  }

  public static void main(String[] args) {
    APIConnector apiConnector = new APIConnector("https://fantasy-predictor.appspot.com/_ah/api", "v1");
    FetchFantasyTrainingData dataFetcher =
        new FetchFantasyTrainingData(new WebFetcherBasketballReference(apiConnector));
    dataFetcher.output(new Player().setId(2544).setName("LeBron James"));
  }
}
