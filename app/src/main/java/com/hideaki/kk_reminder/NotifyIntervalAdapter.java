package com.hideaki.kk_reminder;

import androidx.annotation.NonNull;

class NotifyIntervalAdapter implements Cloneable {

  private NotifyInterval2 notifyInterval;

  NotifyIntervalAdapter() {

    notifyInterval = new NotifyInterval2();
  }

  NotifyIntervalAdapter(Object obj) {

    if(obj instanceof NotifyInterval2) {
      notifyInterval = (NotifyInterval2)obj;
    }
    else if(obj instanceof NotifyInterval) {
      NotifyInterval oldNotifyInterval = (NotifyInterval)obj;
      notifyInterval = new NotifyInterval2();
      notifyInterval.setLabel(oldNotifyInterval.getLabel());
      notifyInterval.setHour(oldNotifyInterval.getHour());
      notifyInterval.setMinute(oldNotifyInterval.getMinute());
      notifyInterval.setTime(oldNotifyInterval.getTime());
      notifyInterval.setOrgTime(oldNotifyInterval.getOrg_time());
      notifyInterval.setWhichSet(oldNotifyInterval.getWhich_setted());
    }
    else {
      throw new IllegalArgumentException(
        "Arg notifyInterval is not instance of NotifyInterval Class"
      );
    }
  }

  NotifyInterval2 getNotifyInterval() {

    return notifyInterval;
  }

  String getLabel() {

    return notifyInterval.getLabel();
  }

  int getHour() {

    return notifyInterval.getHour();
  }

  int getMinute() {

    return notifyInterval.getMinute();
  }

  int getTime() {

    return notifyInterval.getTime();
  }

  int getOrgTime() {

    return notifyInterval.getOrgTime();
  }

  int getWhichSet() {

    return notifyInterval.getWhichSet();
  }

  void setLabel(String label) {

    notifyInterval.setLabel(label);
  }

  void setHour(int hour) {

    notifyInterval.setHour(hour);
  }

  void setMinute(int minute) {

    notifyInterval.setMinute(minute);
  }

  void setTime(int time) {

    notifyInterval.setTime(time);
  }

  void setOrgTime(int orgTime) {

    notifyInterval.setOrgTime(orgTime);
  }

  void addOrgTime(int orgTime) {

    notifyInterval.addOrgTime(orgTime);
  }

  void setWhichSet(int whichSet) {

    notifyInterval.setWhichSet(whichSet);
  }

  @NonNull
  @Override
  public NotifyIntervalAdapter clone() {

    try {
      NotifyIntervalAdapter notifyInterval = (NotifyIntervalAdapter)super.clone();
      notifyInterval.notifyInterval = this.notifyInterval.clone();
      return notifyInterval;
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
