package com.hideaki.kk_reminder;

import java.io.Serializable;

public class NotifyInterval implements Serializable, Cloneable {

  private static final long serialVersionUID = -4030712214761391450L;
  private String label = null;
  private int hour;
  private int minute;
  private int time;
  private int org_time;
  private int which_setted;

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

  int getOrg_time() {

    return org_time;
  }

  int getWhich_setted() {

    return which_setted;
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

  void setOrg_time(int org_time) {

    this.org_time = org_time;
  }

  void addOrgTime(int org_time) {

    this.org_time += org_time;
  }

  void setWhich_setted(int which_setted) {

    this.which_setted = which_setted;
  }

  @Override
  public NotifyInterval clone() {

    NotifyInterval notifyInterval = null;

    try {
      notifyInterval = (NotifyInterval)super.clone();
    }
    catch(CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return notifyInterval;
  }
}
