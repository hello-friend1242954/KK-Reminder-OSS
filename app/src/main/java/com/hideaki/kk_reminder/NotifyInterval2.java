package com.hideaki.kk_reminder;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class NotifyInterval2 implements Serializable, Cloneable {

  private static final long serialVersionUID = 6692802841373105942L;
  private String label = null;
  private int hour;
  private int minute;
  private int time;
  private int orgTime;
  private int whichSet;

  String getLabel() {

    return label;
  }

  int getHour() {

    return hour;
  }

  int getMinute() {

    return minute;
  }

  int getTime() {

    return time;
  }

  int getOrgTime() {

    return orgTime;
  }

  int getWhichSet() {

    return whichSet;
  }

  void setLabel(String label) {

    this.label = label;
  }

  void setHour(int hour) {

    this.hour = hour;
  }

  void setMinute(int minute) {

    this.minute = minute;
  }

  void setTime(int time) {

    this.time = time;
  }

  void setOrgTime(int orgTime) {

    this.orgTime = orgTime;
  }

  void addOrgTime(int orgTime) {

    this.orgTime += orgTime;
  }

  void setWhichSet(int whichSet) {

    this.whichSet = whichSet;
  }

  @NonNull
  @Override
  public NotifyInterval2 clone() {

    try {
      return (NotifyInterval2)super.clone();
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
