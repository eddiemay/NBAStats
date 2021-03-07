package com.digitald4.nbastats.model;

import java.util.HashMap;
import java.util.Map;

public class PlayerGameLog {
  private long id;
  private int playerId;
  private String season;
  private String date;
  private String matchUp;
  private String result;
  private double minutes;
  private int points;
  private int rebounds;
  private int assists;
  private int blocks;
  private int steals;
  private int made3s;
  private int turnovers;
  private boolean doubleDouble;
  private boolean tripleDouble;
  private int plusMinus;
  private double nBAFantasyPoints;
  private Map<String, Double> fantasySitePoints = new HashMap<>();

  public long getId() {
    return id;
  }

  public PlayerGameLog setId(long id) {
    this.id = id;
    return this;
  }

  public int getPlayerId() {
    return playerId;
  }

  public PlayerGameLog setPlayerId(int playerId) {
    this.playerId = playerId;
    return this;
  }

  public String getSeason() {
    return season;
  }

  public PlayerGameLog setSeason(String season) {
    this.season = season;
    return this;
  }

  public String getDate() {
    return date;
  }

  public PlayerGameLog setDate(String date) {
    this.date = date;
    return this;
  }

  public String getMatchUp() {
    return matchUp;
  }

  public PlayerGameLog setMatchUp(String matchUp) {
    this.matchUp = matchUp;
    return this;
  }

  public String getResult() {
    return result;
  }

  public PlayerGameLog setResult(String result) {
    this.result = result;
    return this;
  }

  public double getMinutes() {
    return minutes;
  }

  public PlayerGameLog setMinutes(double minutes) {
    this.minutes = minutes;
    return this;
  }

  public int getPoints() {
    return points;
  }

  public PlayerGameLog setPoints(int points) {
    this.points = points;
    return this;
  }

  public int getRebounds() {
    return rebounds;
  }

  public PlayerGameLog setRebounds(int rebounds) {
    this.rebounds = rebounds;
    return this;
  }

  public int getAssists() {
    return assists;
  }

  public PlayerGameLog setAssists(int assists) {
    this.assists = assists;
    return this;
  }

  public int getBlocks() {
    return blocks;
  }

  public PlayerGameLog setBlocks(int blocks) {
    this.blocks = blocks;
    return this;
  }

  public int getSteals() {
    return steals;
  }

  public PlayerGameLog setSteals(int steals) {
    this.steals = steals;
    return this;
  }

  public int getMade3s() {
    return made3s;
  }

  public PlayerGameLog setMade3s(int made3s) {
    this.made3s = made3s;
    return this;
  }

  public int getTurnovers() {
    return turnovers;
  }

  public PlayerGameLog setTurnovers(int turnovers) {
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

  public int getPlusMinus() {
    return plusMinus;
  }

  public PlayerGameLog setPlusMinus(int plusMinus) {
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
}
