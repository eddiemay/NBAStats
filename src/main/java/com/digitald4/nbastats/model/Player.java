package com.digitald4.nbastats.model;

import com.digitald4.common.model.ModelObject;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import java.time.Instant;

public class Player extends ModelObject<Long> {
  private String basketRefPlayerId;
  private String name;
  private String aka;
  private Instant dateOfBirth;
  private String number;
  private String position;
  private String height;
  private String weight;
  private String school;
  private String country;
  private Integer minSeason;
  private Integer maxSeason;

  public Player setId(Long id) {
    super.setId(id);
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

  public String getBasketRefPlayerId() {
    return basketRefPlayerId;
  }

  public Player setBasketRefPlayerId(String basketRefPlayerId) {
    this.basketRefPlayerId = basketRefPlayerId;
    return this;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  public Instant getDateOfBirth() {
    return dateOfBirth;
  }

  public Player setDateOfBirth(Instant dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
    return this;
  }

  @ApiResourceProperty
  public Long dateOfBirth() {
    return dateOfBirth == null ? null : dateOfBirth.toEpochMilli();
  }

  public Player setDateOfBirth(long dateOfBirth) {
    this.dateOfBirth = Instant.ofEpochMilli(dateOfBirth);
    return this;
  }

  public String getNumber() {
    return number;
  }

  public Player setNumber(String number) {
    this.number = number;
    return this;
  }

  public String getPosition() {
    return position;
  }

  public Player setPosition(String position) {
    this.position = position;
    return this;
  }

  public String getHeight() {
    return height;
  }

  public Player setHeight(String height) {
    this.height = height;
    return this;
  }

  public String getWeight() {
    return weight;
  }

  public Player setWeight(String weight) {
    this.weight = weight;
    return this;
  }

  public String getSchool() {
    return school;
  }

  public Player setSchool(String school) {
    this.school = school;
    return this;
  }

  public String getCountry() {
    return country;
  }

  public Player setCountry(String country) {
    this.country = country;
    return this;
  }

  public Integer getMinSeason() {
    return minSeason;
  }

  public Player setMinSeason(Integer minSeason) {
    this.minSeason = minSeason;
    return this;
  }

  public Integer getMaxSeason() {
    return maxSeason;
  }

  public Player setMaxSeason(Integer maxSeason) {
    this.maxSeason = maxSeason;
    return this;
  }
}
