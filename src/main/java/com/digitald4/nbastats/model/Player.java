package com.digitald4.nbastats.model;

import com.digitald4.common.model.HasProto;
import com.digitald4.nbastats.proto.NBAStatsProtos;

public class Player implements HasProto<NBAStatsProtos.Player> {
  private long id;
  private String season = "";
  private int playerId;
  private String name = "";
  private String aka = "";

  public long getId() {
    return id;
  }

  public Player setId(long id) {
    this.id = id;
    return this;
  }

  public String getSeason() {
    return season;
  }

  public Player setSeason(String season) {
    this.season = season;
    return this;
  }

  public int getPlayerId() {
    return playerId;
  }

  public Player setPlayerId(int playerId) {
    this.playerId = playerId;
    return this;
  }

  public String getName() {
    return name;
  }

  public Player setName(String name) {
    this.name = name;
    return this;
  }

  public String getAka() {
    return aka;
  }

  public Player setAka(String aka) {
    this.aka = aka;
    return this;
  }

  @Override
  public NBAStatsProtos.Player toProto() {
    return NBAStatsProtos.Player.newBuilder()
        .setId(getId())
        .setSeason(getSeason())
        .setPlayerId(getPlayerId())
        .setName(getName())
        .setAka(getAka())
        .build();
  }

  @Override
  public Player fromProto(NBAStatsProtos.Player proto) {
    return setId(proto.getId())
        .setSeason(proto.getSeason())
        .setPlayerId(proto.getPlayerId())
        .setName(proto.getName())
        .setAka(proto.getAka());
  }

  public static Player from(NBAStatsProtos.Player proto) {
    return new Player().fromProto(proto);
  }
}
