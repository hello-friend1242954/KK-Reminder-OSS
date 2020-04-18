package com.hideaki.kk_reminder;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class DayRepeat2 implements Serializable, Cloneable {

  private static final long serialVersionUID = -527908335190331358L;
  private String label = null;
  private int interval = 0;
  private int scale = 0;
  private boolean isDay = false;
  private int week = 0; // 月～日曜日の7個のパラメータの設定状況のフラグをビットで表す
  private int daysOfMonth = 0; // 1日～月末までの最大31個のパラメータの設定状況のフラグをビットで表す
  private int ordinalNumber = 0;
  private Week onTheMonth = null;
  // 一度ordinal_numberに一致する週数の月曜日を見つけたら、この変数が5になるまで(金曜日になるまで)Repeatを
  // 毎日に固定する
  private int weekdayNum = 0;
  private boolean isDaysOfMonthSet = true;
  private int year = 0; // 1～12月の12個のパラメータの設定状況のフラグをビットで表す
  private int dayOfMonthOfYear = 0;
  private int whichSet = 0; // day, week, month, yearの4つについて設定状況のフラグをビットで表す
  private int whichTemplate = 0;

  String getLabel() {

    return label;
  }

  int getInterval() {

    return interval;
  }

  int getScale() {

    return scale;
  }

  boolean isDay() {

    return isDay;
  }

  int getWeek() {

    return week;
  }

  int getDaysOfMonth() {

    return daysOfMonth;
  }

  int getOrdinalNumber() {

    return ordinalNumber;
  }

  Week getOnTheMonth() {

    return onTheMonth;
  }

  int getWeekdayNum() {

    return weekdayNum;
  }

  boolean isDaysOfMonthSet() {

    return isDaysOfMonthSet;
  }

  int getYear() {

    return year;
  }

  int getDayOfMonthOfYear() {

    return dayOfMonthOfYear;
  }

  int getWhichSet() {

    return whichSet;
  }

  int getWhichTemplate() {

    return whichTemplate;
  }

  void setLabel(String label) {

    this.label = label;
  }

  void setInterval(int interval) {

    this.interval = interval;
  }

  void setScale(int scale) {

    this.scale = scale;
  }

  void setIsDay(boolean isDay) {

    this.isDay = isDay;
  }

  void setWeek(int week) {

    this.week = week;
  }

  void setDaysOfMonth(int daysOfMonth) {

    this.daysOfMonth = daysOfMonth;
  }

  void setOrdinalNumber(int ordinalNumber) {

    this.ordinalNumber = ordinalNumber;
  }

  void setOnTheMonth(Week onTheMonth) {

    this.onTheMonth = onTheMonth;
  }

  void setWeekdayNum(int weekdayNum) {

    this.weekdayNum = weekdayNum;
  }

  void setIsDaysOfMonthSet(boolean isDaysOfMonthSet) {

    this.isDaysOfMonthSet = isDaysOfMonthSet;
  }

  void setYear(int year) {

    this.year = year;
  }

  void setDayOfMonthOfYear(int dayOfMonthOfYear) {

    this.dayOfMonthOfYear = dayOfMonthOfYear;
  }

  void setWhichSet(int whichSet) {

    this.whichSet = whichSet;
  }

  void setWhichTemplate(int whichTemplate) {

    this.whichTemplate = whichTemplate;
  }

  @NonNull
  @Override
  public DayRepeat2 clone() {

    try {
      return (DayRepeat2)super.clone();
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  void clear() {

    this.label = null;
    this.interval = 0;
    this.scale = 0;
    this.isDay = false;
    this.week = 0;
    this.daysOfMonth = 0;
    this.ordinalNumber = 0;
    this.onTheMonth = null;
    this.weekdayNum = 0;
    this.isDaysOfMonthSet = true;
    this.year = 0;
    this.dayOfMonthOfYear = 0;
    this.whichSet = 0;
    this.whichTemplate = 0;
  }

  void dayClear() {

    this.week = 0;
    this.daysOfMonth = 0;
    this.ordinalNumber = 0;
    this.onTheMonth = null;
    this.isDaysOfMonthSet = true;
    this.year = 0;
    this.dayOfMonthOfYear = 0;
  }

  void weekClear() {

    this.isDay = false;
    this.daysOfMonth = 0;
    this.ordinalNumber = 0;
    this.onTheMonth = null;
    this.isDaysOfMonthSet = true;
    this.year = 0;
    this.dayOfMonthOfYear = 0;
  }

  void daysOfMonthClear() {

    this.isDay = false;
    this.week = 0;
    this.ordinalNumber = 0;
    this.onTheMonth = null;
    this.year = 0;
    this.dayOfMonthOfYear = 0;
  }

  void onTheMonthClear() {

    this.isDay = false;
    this.week = 0;
    this.daysOfMonth = 0;
    this.year = 0;
    this.dayOfMonthOfYear = 0;
  }

  void yearClear() {

    this.isDay = false;
    this.week = 0;
    this.daysOfMonth = 0;
    this.ordinalNumber = 0;
    this.onTheMonth = null;
    this.isDaysOfMonthSet = true;
  }
}
