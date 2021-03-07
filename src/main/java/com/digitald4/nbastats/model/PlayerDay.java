package com.digitald4.nbastats.model;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.digitald4.common.model.HasProto;
import com.digitald4.nbastats.proto.NBAStatsProtos;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public class PlayerDay implements HasProto<NBAStatsProtos.PlayerDay> {
  private long id;
  private int playerId;
  private String date = "";
  private String name = "";
  private NBAStatsProtos.PlayerDay.Status status;
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

  public NBAStatsProtos.PlayerDay.Status getStatus() {
    return status;
  }

  public PlayerDay setStatus(NBAStatsProtos.PlayerDay.Status status)  {
    this.status = status;
    return this;
  }

  public PlayerDay setDate(NBAStatsProtos.PlayerDay.Status status) {
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

  @Override
  public NBAStatsProtos.PlayerDay toProto() {
    return NBAStatsProtos.PlayerDay.newBuilder()
        .setId(getId())
        .setPlayerId(getPlayerId())
        .setDate(getDate())
        .setName(getName())
        .setStatus(getStatus())
        .setTeam(getTeam())
        .setOpponent(getOpponent())
        .putAllFantasySiteInfo(getFantasySiteInfos()
            .entrySet().stream().collect(toImmutableMap(Map.Entry::getKey, e -> e.getValue().toProto())))
        .setLowDataWarn(getLowDataWarn())
        .build();
  }

  @Override
  public PlayerDay fromProto(NBAStatsProtos.PlayerDay proto) {
    return setId(proto.getId())
        .setPlayerId(proto.getPlayerId())
        .setDate(proto.getDate())
        .setName(proto.getName())
        .setStatus(proto.getStatus())
        .setTeam(proto.getTeam())
        .setOpponent(proto.getOpponent())
        .setFantasySiteInfos(proto.getFantasySiteInfoMap().entrySet().stream()
            .collect(toImmutableMap(Map.Entry::getKey, e -> FantasySiteInfo.fromProto(e.getValue()))))
        .setLowDataWarn(proto.getLowDataWarn());
  }

  public static PlayerDay from(NBAStatsProtos.PlayerDay proto) {
    return new PlayerDay().fromProto(proto);
  }

  public static class FantasySiteInfo {
    private ImmutableList<NBAStatsProtos.Position> positions;
    private int cost;
    private ImmutableMap<String, Double> projections;
    private double actual;

    public ImmutableList<NBAStatsProtos.Position> getPositions() {
      return positions;
    }

    public FantasySiteInfo setPositions(Iterable<NBAStatsProtos.Position> positions) {
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

    public NBAStatsProtos.PlayerDay.FantasySiteInfo toProto() {
      return NBAStatsProtos.PlayerDay.FantasySiteInfo.newBuilder()
          .addAllPosition(getPositions())
          .setCost(getCost())
          .putAllProjection(getProjections())
          .setActual(getActual())
          .build();
    }

    public static FantasySiteInfo fromProto(NBAStatsProtos.PlayerDay.FantasySiteInfo proto) {
      return new FantasySiteInfo()
          .setPositions(proto.getPositionList())
          .setCost(proto.getCost())
          .setProjections(proto.getProjectionMap())
          .setActual(proto.getActual());
    }
  }
}
