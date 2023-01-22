package com.digitald4.nbastats.model;

import com.digitald4.common.util.Calculate;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class PlayerGameLog {
  private static final String COMPOSITE_KEY = "%d-%s";
  private Integer playerId;
  private String date;
  private String season;
  private double age;
  private String matchUp;
  public enum Venue {HOME, ROAD, MUTUAL}
  private Venue venue;
  public enum Result {W, L, POSTPONED, CANCELLED}
  private Result result;
  private boolean started;
  private double minutes;
  private double fieldGoalsMade;
  private double fieldGoalsAtts;
  private double fieldGoalPercent;
  private double threePointersMade;
  private double threePointerAtts;
  private double threePointerPercent;
  private double freeThrowsMade;
  private double freeThrowAtts;
  private double freeThrowPercent;
  private double offensiveRebounds;
  private double defensiveRebounds;
  private double rebounds;
  private double assists;
  private double steals;
  private double blocks;
  private double turnovers;
  private double personalFouls;
  private double points;
  private boolean doubleDouble;
  private boolean tripleDouble;
  private double plusMinus;
  private double nBAFantasyPoints;
  private Map<String, Double> fantasySitePoints = new HashMap<>();

  public String getId() {
    return String.format(COMPOSITE_KEY, playerId, date);
  }

  @Deprecated
  public PlayerGameLog setId(String id) {
    return this;
  }

  @Deprecated
  public PlayerGameLog setId(Long id) {
    return this;
  }

  public Integer getPlayerId() {
    return playerId;
  }

  public PlayerGameLog setPlayerId(Integer playerId) {
    this.playerId = playerId;
    return this;
  }

  public String getDate() {
    return date;
  }

  public PlayerGameLog setDate(String date) {
    this.date = date;
    return this;
  }

  public String getSeason() {
    return season;
  }

  public PlayerGameLog setSeason(String season) {
    this.season = season;
    return this;
  }

  public double getAge() {
    return age;
  }

  public PlayerGameLog setAge(double age) {
    this.age = Calculate.round(age, 3);
    return this;
  }

  public String getMatchUp() {
    return matchUp;
  }

  public PlayerGameLog setMatchUp(String matchUp) {
    this.matchUp = matchUp;
    return this;
  }

  public Venue getVenue() {
    if (venue == null) {
      return getMatchUp().contains("@") ? Venue.ROAD : Venue.HOME;
    }

    return venue;
  }

  public PlayerGameLog setVenue(Venue venue) {
    this.venue = venue;
    return this;
  }

  public Result getResult() {
    return result;
  }

  public PlayerGameLog setResult(Result result) {
    this.result = result;
    return this;
  }

  public boolean isStarted() {
    return started;
  }

  public PlayerGameLog setStarted(boolean started) {
    this.started = started;
    return this;
  }

  public double getMinutes() {
    return minutes;
  }

  public PlayerGameLog setMinutes(double minutes) {
    this.minutes = minutes;
    return this;
  }

  public double getFieldGoalsMade() {
    return fieldGoalsMade;
  }

  public PlayerGameLog setFieldGoalsMade(double fieldGoalsMade) {
    this.fieldGoalsMade = fieldGoalsMade;
    return this;
  }

  public double getFieldGoalsAtts() {
    return fieldGoalsAtts;
  }

  public PlayerGameLog setFieldGoalsAtts(double fieldGoalsAtts) {
    this.fieldGoalsAtts = fieldGoalsAtts;
    return this;
  }

  public double getFieldGoalPercent() {
    if (fieldGoalPercent == 0.0 && fieldGoalsAtts > 0.0) {
      return Calculate.round(fieldGoalsMade / fieldGoalsAtts, 3);
    }

    return fieldGoalPercent;
  }

  public PlayerGameLog setFieldGoalPercent(double fieldGoalPercent) {
    this.fieldGoalPercent = fieldGoalPercent;
    return this;
  }

  public double getThreePointersMade() {
    return threePointersMade;
  }

  public PlayerGameLog setThreePointersMade(double threePointersMade) {
    this.threePointersMade = threePointersMade;
    return this;
  }

  public double getThreePointerAtts() {
    return threePointerAtts;
  }

  public PlayerGameLog setThreePointerAtts(double threePointerAtts) {
    this.threePointerAtts = threePointerAtts;
    return this;
  }

  public double getThreePointerPercent() {
    if (threePointerPercent == 0.0 && threePointerAtts > 0.0) {
      return Calculate.round(threePointersMade / threePointerAtts, 3);
    }

    return threePointerPercent;
  }

  public PlayerGameLog setThreePointerPercent(double threePointerPercent) {
    this.threePointerPercent = threePointerPercent;
    return this;
  }

  public double getFreeThrowsMade() {
    return freeThrowsMade;
  }

  public PlayerGameLog setFreeThrowsMade(double freeThrowsMade) {
    this.freeThrowsMade = freeThrowsMade;
    return this;
  }

  public double getFreeThrowAtts() {
    return freeThrowAtts;
  }

  public PlayerGameLog setFreeThrowAtts(double freeThrowAtts) {
    this.freeThrowAtts = freeThrowAtts;
    return this;
  }

  public double getFreeThrowPercent() {
    if (freeThrowPercent == 0.0 && freeThrowAtts > 0) {
      return Calculate.round(freeThrowsMade / freeThrowAtts, 3);
    }

    return freeThrowPercent;
  }

  public PlayerGameLog setFreeThrowPercent(double freeThrowPercent) {
    this.freeThrowPercent = freeThrowPercent;
    return this;
  }

  public double getOffensiveRebounds() {
    return offensiveRebounds;
  }

  public PlayerGameLog setOffensiveRebounds(double offensiveRebounds) {
    this.offensiveRebounds = offensiveRebounds;
    return this;
  }

  public double getDefensiveRebounds() {
    return defensiveRebounds;
  }

  public PlayerGameLog setDefensiveRebounds(double defensiveRebounds) {
    this.defensiveRebounds = defensiveRebounds;
    return this;
  }

  public double getPersonalFouls() {
    return personalFouls;
  }

  public PlayerGameLog setPersonalFouls(double personalFouls) {
    this.personalFouls = personalFouls;
    return this;
  }

  public double getPoints() {
    return points;
  }

  public PlayerGameLog setPoints(double points) {
    this.points = points;
    return this;
  }

  public double getRebounds() {
    return rebounds;
  }

  public PlayerGameLog setRebounds(double rebounds) {
    this.rebounds = rebounds;
    return this;
  }

  public double getAssists() {
    return assists;
  }

  public PlayerGameLog setAssists(double assists) {
    this.assists = assists;
    return this;
  }

  public double getBlocks() {
    return blocks;
  }

  public PlayerGameLog setBlocks(double blocks) {
    this.blocks = blocks;
    return this;
  }

  public double getSteals() {
    return steals;
  }

  public PlayerGameLog setSteals(double steals) {
    this.steals = steals;
    return this;
  }

  @Deprecated
  public Integer getMade3s() {
    return null;
  }

  @Deprecated
  public PlayerGameLog setMade3s(int made3s) {
    this.threePointersMade = made3s;
    return this;
  }

  public double getTurnovers() {
    return turnovers;
  }

  public PlayerGameLog setTurnovers(double turnovers) {
    this.turnovers = turnovers;
    return this;
  }

  public boolean isDoubleDouble() {
    return doubleDouble;
  }

  public PlayerGameLog setDoubleDouble(boolean doubleDouble) {
    this.doubleDouble = doubleDouble;
    return this;
  }

  public boolean isTripleDouble() {
    return tripleDouble;
  }

  public PlayerGameLog setTripleDouble(boolean tripleDouble) {
    this.tripleDouble = tripleDouble;
    return this;
  }

  public double getPlusMinus() {
    return plusMinus;
  }

  public PlayerGameLog setPlusMinus(double plusMinus) {
    this.plusMinus = plusMinus;
    return this;
  }

  public double getNBAFantasyPoints() {
    return nBAFantasyPoints;
  }

  public PlayerGameLog setNBAFantasyPoints(double nBAFantasyPoints) {
    this.nBAFantasyPoints = nBAFantasyPoints;
    return this;
  }

  public Map<String, Double> getFantasySitePoints() {
    return fantasySitePoints;
  }

  public double getFantasySitePoints(String fantasySite) {
    return getFantasySitePoints().getOrDefault(fantasySite, 0.0);
  }

  public PlayerGameLog setFantasySitePoints(Map<String, Double> fantasySitePoints) {
    this.fantasySitePoints = fantasySitePoints;
    return this;
  }

  public PlayerGameLog setFantasySitePoints(String fantasySite, double points) {
    fantasySitePoints.put(fantasySite, points);
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PlayerGameLog)) {
      return super.equals(obj);
    }
    PlayerGameLog other = (PlayerGameLog) obj;

    return toString().equals(other.toString());
  }

  @Override
  public String toString() {
    return new JSONObject(this).toString();
  }
}
