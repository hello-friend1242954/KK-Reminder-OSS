package com.hideaki.kk_reminder;

import androidx.annotation.NonNull;

class DayRepeatAdapter implements Cloneable {

  private DayRepeat2 dayRepeat;

  DayRepeatAdapter() {

    dayRepeat = new DayRepeat2();
  }

  DayRepeatAdapter(Object obj) {

    if(obj instanceof DayRepeat2) {
      dayRepeat = (DayRepeat2)obj;
    }
    else if(obj instanceof DayRepeat) {
      DayRepeat oldDayRepeat = (DayRepeat)obj;
      dayRepeat = new DayRepeat2();
      dayRepeat.setLabel(oldDayRepeat.getLabel());
      dayRepeat.setInterval(oldDayRepeat.getInterval());
      dayRepeat.setScale(oldDayRepeat.getScale());
      dayRepeat.setIsDay(oldDayRepeat.isDay());
      dayRepeat.setWeek(oldDayRepeat.getWeek());
      dayRepeat.setDaysOfMonth(oldDayRepeat.getDays_of_month());
      dayRepeat.setOrdinalNumber(oldDayRepeat.getOrdinal_number());
      dayRepeat.setOnTheMonth(oldDayRepeat.getOn_the_month());
      dayRepeat.setWeekdayNum(oldDayRepeat.getWeekday_num());
      dayRepeat.setIsDaysOfMonthSet(oldDayRepeat.isDays_of_month_setted());
      dayRepeat.setYear(oldDayRepeat.getYear());
      dayRepeat.setDayOfMonthOfYear(oldDayRepeat.getDay_of_month_of_year());
      dayRepeat.setWhichSet(oldDayRepeat.getSetted());
      dayRepeat.setWhichTemplate(oldDayRepeat.getWhich_template());
    }
    else {
      throw new IllegalArgumentException("Arg dayRepeat is not instance of DayRepeat Class");
    }
  }

  DayRepeat2 getDayRepeat() {

    return dayRepeat;
  }

  String getLabel() {

    return dayRepeat.getLabel();
  }

  int getInterval() {

    return dayRepeat.getInterval();
  }

  int getScale() {

    return dayRepeat.getScale();
  }

  boolean isDay() {

    return dayRepeat.isDay();
  }

  int getWeek() {

    return dayRepeat.getWeek();
  }

  int getDaysOfMonth() {

    return dayRepeat.getDaysOfMonth();
  }

  int getOrdinalNumber() {

    return dayRepeat.getOrdinalNumber();
  }

  Week getOnTheMonth() {

    return dayRepeat.getOnTheMonth();
  }

  int getWeekdayNum() {

    return dayRepeat.getWeekdayNum();
  }

  boolean isDaysOfMonthSet() {

    return dayRepeat.isDaysOfMonthSet();
  }

  int getYear() {

    return dayRepeat.getYear();
  }

  int getDayOfMonthOfYear() {

    return dayRepeat.getDayOfMonthOfYear();
  }

  int getWhichSet() {

    return dayRepeat.getWhichSet();
  }

  int getWhichTemplate() {

    return dayRepeat.getWhichTemplate();
  }

  void setLabel(String label) {

    dayRepeat.setLabel(label);
  }

  void setInterval(int interval) {

    dayRepeat.setInterval(interval);
  }

  void setScale(int scale) {

    dayRepeat.setScale(scale);
  }

  void setIsDay(boolean isDay) {

    dayRepeat.setIsDay(isDay);
  }

  void setWeek(int week) {

    dayRepeat.setWeek(week);
  }

  void setDaysOfMonth(int daysOfMonth) {

    dayRepeat.setDaysOfMonth(daysOfMonth);
  }

  void setOrdinalNumber(int ordinalNumber) {

    dayRepeat.setOrdinalNumber(ordinalNumber);
  }

  void setOnTheMonth(Week onTheMonth) {

    dayRepeat.setOnTheMonth(onTheMonth);
  }

  void setWeekdayNum(int weekdayNum) {

    dayRepeat.setWeekdayNum(weekdayNum);
  }

  void setIsDaysOfMonthSet(boolean isDaysOfMonthSet) {

    dayRepeat.setIsDaysOfMonthSet(isDaysOfMonthSet);
  }

  void setYear(int year) {

    dayRepeat.setYear(year);
  }

  void setDayOfMonthOfYear(int dayOfMonthOfYear) {

    dayRepeat.setDayOfMonthOfYear(dayOfMonthOfYear);
  }

  void setWhichSet(int whichSet) {

    dayRepeat.setWhichSet(whichSet);
  }

  void setWhichTemplate(int whichTemplate) {

    dayRepeat.setWhichTemplate(whichTemplate);
  }

  @NonNull
  @Override
  public DayRepeatAdapter clone() {

    try {
      DayRepeatAdapter dayRepeat = (DayRepeatAdapter)super.clone();
      dayRepeat.dayRepeat = this.dayRepeat.clone();
      return dayRepeat;
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  void clear() {

    dayRepeat.clear();
  }

  void dayClear() {

    dayRepeat.dayClear();
  }

  void weekClear() {

    dayRepeat.weekClear();
  }

  void daysOfMonthClear() {

    dayRepeat.daysOfMonthClear();
  }

  void onTheMonthClear() {

    dayRepeat.onTheMonthClear();
  }

  void yearClear() {

    dayRepeat.yearClear();
  }
}
