package com.digitald4.nbastats.model;

import com.digitald4.common.model.HasProto;
import com.digitald4.nbastats.proto.NBAStatsProtos;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class PlayerGameLog implements HasProto<NBAStatsProtos.GameLog> {
  private long id;
  private int playerId;
  private String season = "";
  private String date = "";
  private String matchUp = "";
  private String result = "";
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
  private ImmutableMap<String, Double> fantasySitePoints = ImmutableMap.of();

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

  public ImmutableMap<String, Double> getFantasySitePoints() {
    return fantasySitePoints;
  }

  public double getFantasySitePoints(String fantasySite) {
    return getFantasySitePoints().getOrDefault(fantasySite, 0.0);
  }

  public PlayerGameLog setFantasySitePoints(Map<String, Double> fantasySitePoints) {
    this.fantasySitePoints = ImmutableMap.copyOf(fantasySitePoints);
    return this;
  }

  @Override
  public NBAStatsProtos.GameLog toProto() {
    return NBAStatsProtos.GameLog.newBuilder()
        .setId(getId())
        .setPlayerId(getPlayerId())
        .setSeason(getSeason())
        .setDate(getDate())
        .setMatchUp(getMatchUp())
        .setResult(getResult())
        .setMinutes(getMinutes())
        .setPoints(getPoints())
        .setRebounds(getRebounds())
        .setAssists(getAssists())
        .setBlocks(getBlocks())
        .setSteals(getSteals())
        .setMade3S(getMade3s())
        .setTurnovers(getTurnovers())
        .setDoubleDouble(isDoubleDouble())
        .setTripleDouble(isTripleDouble())
        .setPlusMinus(getPlusMinus())
        .setNBAFantasyPoints(getNBAFantasyPoints())
        .putAllFantasySitePoints(getFantasySitePoints())
        .build();
  }

  @Override
  public PlayerGameLog fromProto(NBAStatsProtos.GameLog proto) {
    return setId(proto.getId())
        .setPlayerId(proto.getPlayerId())
        .setSeason(proto.getSeason())
        .setDate(proto.getDate())
        .setMatchUp(proto.getMatchUp())
        .setResult(proto.getResult())
        .setMinutes(proto.getMinutes())
        .setPoints(proto.getPoints())
        .setRebounds(proto.getRebounds())
        .setAssists(proto.getAssists())
        .setBlocks(proto.getBlocks())
        .setSteals(proto.getSteals())
        .setMade3s(proto.getMade3S())
        .setTurnovers(proto.getTurnovers())
        .setDoubleDouble(proto.getDoubleDouble())
        .setTripleDouble(proto.getTripleDouble())
        .setPlusMinus(proto.getPlusMinus())
        .setNBAFantasyPoints(proto.getNBAFantasyPoints())
        .setFantasySitePoints(proto.getFantasySitePointsMap());
  }

  public static PlayerGameLog from(NBAStatsProtos.GameLog proto) {
    return new PlayerGameLog().fromProto(proto);
  }
}
