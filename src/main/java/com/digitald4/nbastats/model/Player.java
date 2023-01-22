package com.digitald4.nbastats.model;

public class Player {
  private Integer id;
  private String season;
  private String name;
  private String aka;

  public Integer getId() {
    return id;
  }

  public Player setId(Integer id) {
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

  @Deprecated
  public Integer getPlayerId() {
    return id;
  }

  @Deprecated
  public Player setPlayerId(Integer playerId) {
    this.id = playerId;
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
}
