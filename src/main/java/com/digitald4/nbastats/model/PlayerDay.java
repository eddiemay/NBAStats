package com.digitald4.nbastats.model;

import static com.google.common.collect.Streams.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;

public class PlayerDay {
  private static final String COMPOSITE_KEY = "%d-%s";
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
  private boolean lowDataWarning;

  public String getId() {
    return String.format(COMPOSITE_KEY, playerId, date);
  }

  public PlayerDay setId(String id) {
    return this;
  }

  public PlayerDay setId(long id) {
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

  public ImmutableList<FantasySiteInfo> getFantasySiteInfos() {
    return ImmutableList.copyOf(fantasySiteInfos.values());
  }

  public PlayerDay setFantasySiteInfos(Iterable<FantasySiteInfo> fantasySiteInfos) {
    this.fantasySiteInfos = stream(fantasySiteInfos).collect(toMap(FantasySiteInfo::getFantasySite, identity()));
    return this;
  }

  public FantasySiteInfo getFantasySiteInfo(String fantasySite) {
    return fantasySiteInfos.computeIfAbsent(fantasySite, site -> new FantasySiteInfo().setFantasySite(fantasySite));
  }

  public boolean getLowDataWarning() {
    return lowDataWarning;
  }

  public PlayerDay setLowDataWarn(boolean lowDataWarning) {
    this.lowDataWarning = lowDataWarning;
    return this;
  }

  public static class FantasySiteInfo {
    public enum Position {POS_UNKNOWN, PG, SG, SF, PF, C}

    private String fantasySite;
    private ImmutableList<String> positions;
    private int cost;
    private Map<String, Projection> projections = new HashMap<>();
    private double actual;

    public String getFantasySite() {
      return fantasySite;
    }

    public FantasySiteInfo setFantasySite(String fantasySite) {
      this.fantasySite = fantasySite;
      return this;
    }

    public ImmutableList<String> getPositions() {
      return positions;
    }

    public FantasySiteInfo setPositions(Iterable<String> positions) {
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

    public ImmutableList<Projection> getProjections() {
      return ImmutableList.copyOf(projections.values());
    }

    public FantasySiteInfo setProjections(Iterable<Projection> projections) {
      this.projections = stream(projections).collect(toMap(Projection::getBasis, identity()));
      return this;
    }

    public Projection getProjection(String basis) {
      return projections.computeIfAbsent(basis, b -> new Projection().setBasis(basis));
    }

    public FantasySiteInfo addProjections(Iterable<Projection> projections) {
      this.projections.putAll(stream(projections).collect(toMap(Projection::getBasis, identity())));
      return this;
    }

    public double getActual() {
      return actual;
    }

    public FantasySiteInfo setActual(double actual) {
      this.actual = actual;
      return this;
    }

    public static class Projection {
      private String basis;
      private double projected;

      public String getBasis() {
        return basis;
      }

      public Projection setBasis(String basis) {
        this.basis = basis;
        return this;
      }

      public double getProjected() {
        return projected;
      }

      public Projection setProjected(double projected) {
        this.projected = projected;
        return this;
      }

      public static Projection forValues(String basis, double projected) {
        return new Projection().setBasis(basis).setProjected(projected);
      }
    }
  }
}
