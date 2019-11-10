package com.hideaki.kk_reminder;

import java.io.Serializable;

import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.MINUTE;

public class MinuteRepeat implements Serializable, Cloneable {

  private static final long serialVersionUID = 6882023574179727992L;
  private String label = null;
  private int hour = 0;
  private int minute = 10;
  private int count = 3;
  private int org_count = 3;
  private int org_count2 = 3; // スナックバーから元に戻すときのために値を保持する
  private int duration_hour = 1;
  private int duration_minute = 0;
  private int org_duration_hour = 1;
  private int org_duration_minute = 0;
  private long org_duration2 = 0; // スナックバーから元に戻すときのために値を保持する
  private int which_setted = 0;

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

  int getOrg_count() {

    return org_count;
  }

  int getOrg_count2() {

    return org_count2;
  }

  long getDuration() {

    return duration_hour * HOUR + duration_minute * MINUTE;
  }

  int getOrg_duration_hour() {

    return org_duration_hour;
  }

  int getOrg_duration_minute() {

    return org_duration_minute;
  }

  long getOrgDuration() {

    return org_duration_hour * HOUR + org_duration_minute * MINUTE;
  }

  long getOrg_duration2() {

    return org_duration2;
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

  void setCount(int count) {

    this.count = count;
  }

  void setOrg_count(int org_count) {

    this.org_count = org_count;
  }

  void addOrg_count(int org_count) {

    this.org_count += org_count;
  }

  void setOrg_count2(int org_count2) {

    this.org_count2 = org_count2;
  }

  void setDuration(long rest) {

    duration_hour = (int)(rest / HOUR);
    duration_minute = (int)(rest / MINUTE - duration_hour * 60);
  }

  void setOrg_duration_hour(int org_duration_hour) {

    this.org_duration_hour = org_duration_hour;
  }

  void setOrg_duration_minute(int org_duration_minute) {

    this.org_duration_minute = org_duration_minute;
  }

  void setOrg_duration2(long org_duration2) {

    this.org_duration2 = org_duration2;
  }

  void setWhich_setted(int which_setted) {

    this.which_setted = which_setted;
  }

  @Override
  public MinuteRepeat clone() {

    MinuteRepeat minuteRepeat = null;

    try {
      minuteRepeat = (MinuteRepeat)super.clone();
    }
    catch(CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return minuteRepeat;
  }

  void clear() {

    this.label = null;
    this.hour = 0;
    this.minute = 10;
    this.count = 3;
    this.org_count = 3;
    this.org_count2 = 3;
    this.duration_hour = 1;
    this.duration_minute = 0;
    this.org_duration_hour = 1;
    this.org_duration_minute = 0;
    this.org_duration2 = 0;
    this.which_setted = 0;
  }

  void countClear() {

    this.duration_hour = 1;
    this.duration_minute = 0;
    this.org_duration_hour = 1;
    this.org_duration_minute = 0;
    this.org_duration2 = 0;
  }

  void durationClear() {

    this.count = 3;
    this.org_count = 3;
    this.org_count2 = 3;
  }
}
