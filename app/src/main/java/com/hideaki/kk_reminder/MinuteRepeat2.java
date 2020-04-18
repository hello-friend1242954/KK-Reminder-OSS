package com.hideaki.kk_reminder;

import java.io.Serializable;

import androidx.annotation.NonNull;

import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.MINUTE;

public class MinuteRepeat2 implements Serializable, Cloneable {

  private static final long serialVersionUID = -1323179221037527822L;
  private String label = null;
  private int hour = 0;
  private int minute = 10;
  private int count = 3;
  private int orgCount = 3;
  private int orgCount2 = 3; // スナックバーから元に戻すときのために値を保持する
  private int durationHour = 1;
  private int durationMinute = 0;
  private int orgDurationHour = 1;
  private int orgDurationMinute = 0;
  private long orgDuration2 = 0; // スナックバーから元に戻すときのために値を保持する
  private int whichSet = 0;

  String getLabel() {

    return label;
  }

  int getHour() {

    return hour;
  }

  int getMinute() {

    return minute;
  }

  long getInterval() {

    return hour * HOUR + minute * MINUTE;
  }

  int getCount() {

    return count;
  }

  int getOrgCount() {

    return orgCount;
  }

  int getOrgCount2() {

    return orgCount2;
  }

  long getDuration() {

    return durationHour * HOUR + durationMinute * MINUTE;
  }

  int getOrgDurationHour() {

    return orgDurationHour;
  }

  int getOrgDurationMinute() {

    return orgDurationMinute;
  }

  long getOrgDuration() {

    return orgDurationHour * HOUR + orgDurationMinute * MINUTE;
  }

  long getOrgDuration2() {

    return orgDuration2;
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

  void setCount(int count) {

    this.count = count;
  }

  void setOrgCount(int orgCount) {

    this.orgCount = orgCount;
  }

  void addOrgCount(int orgCount) {

    this.orgCount += orgCount;
  }

  void setOrgCount2(int orgCount2) {

    this.orgCount2 = orgCount2;
  }

  void setDuration(long rest) {

    durationHour = (int)(rest / HOUR);
    durationMinute = (int)(rest / MINUTE - durationHour * 60);
  }

  void setOrgDurationHour(int orgDurationHour) {

    this.orgDurationHour = orgDurationHour;
  }

  void setOrgDurationMinute(int orgDurationMinute) {

    this.orgDurationMinute = orgDurationMinute;
  }

  void setOrgDuration2(long orgDuration2) {

    this.orgDuration2 = orgDuration2;
  }

  void setWhichSet(int whichSet) {

    this.whichSet = whichSet;
  }

  @NonNull
  @Override
  public MinuteRepeat2 clone() {

    try {
      return (MinuteRepeat2)super.clone();
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  void clear() {

    this.label = null;
    this.hour = 0;
    this.minute = 10;
    this.count = 3;
    this.orgCount = 3;
    this.orgCount2 = 3;
    this.durationHour = 1;
    this.durationMinute = 0;
    this.orgDurationHour = 1;
    this.orgDurationMinute = 0;
    this.orgDuration2 = 0;
    this.whichSet = 0;
  }

  void countClear() {

    this.durationHour = 1;
    this.durationMinute = 0;
    this.orgDurationHour = 1;
    this.orgDurationMinute = 0;
    this.orgDuration2 = 0;
  }

  void durationClear() {

    this.count = 3;
    this.orgCount = 3;
    this.orgCount2 = 3;
  }
}
