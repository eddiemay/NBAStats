package com.digitald4.nbastats.model;

import com.digitald4.common.model.HasProto;
import com.digitald4.nbastats.proto.NBAStatsProtos;
import com.google.common.collect.ImmutableList;

public class LineUp implements HasProto<NBAStatsProtos.LineUp> {
  private long id;
  private String date;
  private String fantasySite;
  private String projectionMethod;
  private ImmutableList<Integer> playerIds;
  private double totalSalary;
  private double projected;
  private double actual;
  private boolean selected;

  public long getId() {
    return id;
  }

  public LineUp setId(long id) {
    this.id = id;
    return this;
  }

  public String getDate() {
    return date;
  }

  public LineUp setDate(String date) {
    this.date = date;
    return this;
  }

  public String getFantasySite() {
    return fantasySite;
  }

  public LineUp setFantasySite(String fantasySite) {
    this.fantasySite = fantasySite;
    return this;
  }

  public String getProjectionMethod() {
    return projectionMethod;
  }

  public LineUp setProjectionMethod(String projectionMethod) {
    this.projectionMethod = projectionMethod;
    return this;
  }

  public double getTotalSalary() {
    return totalSalary;
  }

  public LineUp setTotalSalary(double totalSalary) {
    this.totalSalary = totalSalary;
    return this;
  }

  public double getProjected() {
    return projected;
  }

  public LineUp setProjected(double projected) {
    this.projected = projected;
    return this;
  }

  public double getActual() {
    return actual;
  }

  public LineUp setActual(double actual) {
    this.actual = actual;
    return this;
  }

  public ImmutableList<Integer> getPlayerIds() {
    return playerIds;
  }

  public LineUp setPlayerIds(Iterable<Integer> playerIds) {
    this.playerIds = ImmutableList.copyOf(playerIds);
    return this;
  }

  public boolean getSelected() {
    return selected;
  }

  public LineUp setSelected(boolean selected) {
    this.selected = selected;
    return this;
  }

  @Override
  public NBAStatsProtos.LineUp toProto() {
    return NBAStatsProtos.LineUp.newBuilder()
        .setId(getId())
        .setDate(getDate())
        .setFantasySite(getFantasySite())
        .setProjectionMethod(getProjectionMethod())
        .addAllPlayerId(getPlayerIds())
        .setTotalSalary(getTotalSalary())
        .setProjected(getProjected())
        .setActual(getActual())
        .setSelected(getSelected())
        .build();
  }

  @Override
  public HasProto<NBAStatsProtos.LineUp> update(NBAStatsProtos.LineUp proto) {
    return new LineUp()
        .setId(proto.getId())
        .setDate(proto.getDate())
        .setFantasySite(proto.getFantasySite())
        .setProjectionMethod(proto.getProjectionMethod())
        .setPlayerIds(proto.getPlayerIdList())
        .setTotalSalary(proto.getTotalSalary())
        .setProjected(proto.getProjected())
        .setActual(proto.getActual())
        .setSelected(proto.getSelected());
  }
}
