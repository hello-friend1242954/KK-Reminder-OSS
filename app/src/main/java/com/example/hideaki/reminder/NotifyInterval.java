package com.example.hideaki.reminder;

import java.io.Serializable;

public class NotifyInterval implements Serializable {
  private static final long serialVersionUID = -5698716596687514734L;
  int hour;
  int minute;
  int time;

  public NotifyInterval(int hour, int minute, int time) {
    this.hour = hour;
    this.minute = minute;
    this.time = time;
  }

  public int getHour() {
    return hour;
  }

  public int getMinute() {
    return minute;
  }

  public int getTime() {
    return time;
  }

  public void setHour(int hour) {
    this.hour = hour;
  }

  public void setMinute(int minute) {
    this.minute = minute;
  }

  public void setTime(int time) {
    this.time = time;
  }
}