package com.example.hideaki.reminder;

import java.io.Serializable;
import java.util.Calendar;

public class Item implements Serializable {
  private static final long serialVersionUID = -3034988800437990975L;
  private final long id = Calendar.getInstance().getTimeInMillis();
  private String detail;
  private Calendar date = Calendar.getInstance();
  //子ビューのコントロールパネルで時間を変えたときだけ時間を元に戻すのに用いる
  private Calendar org_date = (Calendar)date.clone();
  private Tag tag = null;
  private NotifyInterval notify_interval = new NotifyInterval();
  private Repeat repeat = new Repeat();
  private String notes;
  private long time_altered; //子ビューのコントロールパネルで時間を変えたとき、変えた総時間を保持する
  private long org_time_altered; //リピート設定による変更をスナックバーから元に戻すのに用いる
  private boolean alarm_stopped;
  private boolean org_alarm_stopped; //リピート設定による変更をスナックバーから元に戻すのに用いる

  public Item() {
  }

  public long getId() {
    return id;
  }

  public String getDetail() {
    return detail;
  }

  public Calendar getOrg_date() {
    return org_date;
  }

  public Calendar getDate() {
    return date;
  }

  public Tag getTag() {
    return tag;
  }

  public NotifyInterval getNotify_interval() {
    return notify_interval;
  }

  public Repeat getRepeat() {
    return repeat;
  }

  public String getNotes() {
    return notes;
  }

  public long getTime_altered() {
    return time_altered;
  }

  public long getOrg_time_altered() {
    return org_time_altered;
  }

  public boolean isAlarm_stopped() {
    return alarm_stopped;
  }

  public boolean isOrg_alarm_stopped() {
    return org_alarm_stopped;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public void setDate(Calendar date) {
    this.date = date;
  }

  public void setOrg_date(Calendar org_date) {
    this.org_date = org_date;
  }

  public void setTag(Tag tag) {
    this.tag = tag;
  }

  public void setNotify_interval(NotifyInterval notify_interval) {
    this.notify_interval = notify_interval;
  }

  public void setRepeat(Repeat repeat) {
    this.repeat = repeat;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public void addTime_altered(long time_altered) {
    this.time_altered += time_altered;
  }

  public void setTime_altered(long time_altered) {
    this.time_altered = time_altered;
  }

  public void setOrg_time_altered(long org_time_altered) {
    this.org_time_altered = org_time_altered;
  }

  public void setAlarm_stopped(boolean alarm_stopped) {
    this.alarm_stopped = alarm_stopped;
  }

  public void setOrg_alarm_stopped(boolean org_alarm_stopped) {
    this.org_alarm_stopped = org_alarm_stopped;
  }
}