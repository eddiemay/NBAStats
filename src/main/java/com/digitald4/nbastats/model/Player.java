package com.digitald4.nbastats.model;

public class Player {
  private long id;
  private String season;
  private int playerId;
  private String name;
  private String aka;

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
}
