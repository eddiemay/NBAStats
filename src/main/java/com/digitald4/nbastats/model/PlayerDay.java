package com.digitald4.nbastats.model;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public class PlayerDay {
  private long id;
  private int playerId;
  private String date;
  private String name;

  public enum Status {
    STATUS_UNKNOWN,
    ACTIVE,
    IN_ACTIVE,
    INJURIED,
    QUESTIONABLE
  }
  private Status status;

  private String team = "";
  private String opponent = "";
  private Map<String, FantasySiteInfo> fantasySiteInfos = new HashMap<>();
  private boolean lowDataWarn;

  public long getId() {
    return id;
  }

  public PlayerDay setId(long id) {
    this.id = id;
    return this;
  }

  public int getPlayerId() {
    return playerId;
  }

  public PlayerDay setPlayerId(int playerId) {
    this.playerId = playerId;
    return this;
  }

  public String getDate() {
    return date;
  }

  public PlayerDay setDate(String date) {
    this.date = date;
    return this;
  }

  public String getName() {
    return name;
  }

  public PlayerDay setName(String name) {
    this.name = name;
    return this;
  }

  public Status getStatus() {
    return status;
  }

  public PlayerDay setStatus(Status status)  {
    this.status = status;
    return this;
  }

  public String getTeam() {
    return team;
  }

  public PlayerDay setTeam(String team) {
    this.team = team;
    return this;
  }

  public String getOpponent() {
    return opponent;
  }

  public PlayerDay setOpponent(String opponent) {
    this.opponent = opponent;
    return this;
  }

  public Map<String, FantasySiteInfo> getFantasySiteInfos() {
    return fantasySiteInfos;
  }

  public FantasySiteInfo getFantasySiteInfo(String fantasySite) {
    return fantasySiteInfos.computeIfAbsent(fantasySite, site -> new FantasySiteInfo());
  }

  public PlayerDay setFantasySiteInfos(Map<String, FantasySiteInfo> fantasySiteInfos) {
    this.fantasySiteInfos = fantasySiteInfos;
    return this;
  }

  public boolean getLowDataWarn() {
    return lowDataWarn;
  }

  public PlayerDay setLowDataWarn(boolean lowDataWarn) {
    this.lowDataWarn = lowDataWarn;
    return this;
  }

  public static class FantasySiteInfo {
    public enum Position { POS_UNKNOWN, PG, SG, SF, PF, C }
    private ImmutableList<Position> positions;
    private int cost;
    private ImmutableMap<String, Double> projections;
    private double actual;

    public ImmutableList<Position> getPositions() {
      return positions;
    }

    public FantasySiteInfo setPositions(Iterable<Position> positions) {
      this.positions = ImmutableList.copyOf(positions);
      return this;
    }

    public int getCost() {
      return cost;
    }

    public FantasySiteInfo setCost(int cost) {
      this.cost = cost;
      return this;
    }

    public ImmutableMap<String, Double> getProjections() {
      return projections;
    }

    public double getProjection(String source) {
      return projections.getOrDefault(source, 0.0);
    }

    public FantasySiteInfo setProjections(Map<String, Double> projections) {
      this.projections = ImmutableMap.copyOf(projections);
      return this;
    }

    public double getActual() {
      return actual;
    }

    public FantasySiteInfo setActual(double actual) {
      this.actual = actual;
      return this;
    }
  }
}
