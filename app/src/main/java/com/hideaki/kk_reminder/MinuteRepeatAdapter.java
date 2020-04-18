package com.hideaki.kk_reminder;

import androidx.annotation.NonNull;

class MinuteRepeatAdapter implements Cloneable {

  private MinuteRepeat2 minuteRepeat;

  MinuteRepeatAdapter() {

    minuteRepeat = new MinuteRepeat2();
  }

  MinuteRepeatAdapter(Object obj) {

    if(obj instanceof MinuteRepeat2) {
      minuteRepeat = (MinuteRepeat2)obj;
    }
    else if(obj instanceof MinuteRepeat) {
      MinuteRepeat oldMinuteRepeat = (MinuteRepeat)obj;
      minuteRepeat = new MinuteRepeat2();
      minuteRepeat.setLabel(oldMinuteRepeat.getLabel());
      minuteRepeat.setHour(oldMinuteRepeat.getHour());
      minuteRepeat.setMinute(oldMinuteRepeat.getMinute());
      minuteRepeat.setCount(oldMinuteRepeat.getCount());
      minuteRepeat.setOrgCount(oldMinuteRepeat.getOrg_count());
      minuteRepeat.setOrgCount2(oldMinuteRepeat.getOrg_count2());
      minuteRepeat.setDuration(oldMinuteRepeat.getDuration());
      minuteRepeat.setOrgDurationHour(oldMinuteRepeat.getOrg_duration_hour());
      minuteRepeat.setOrgDurationMinute(oldMinuteRepeat.getOrg_duration_minute());
      minuteRepeat.setOrgDuration2(oldMinuteRepeat.getOrg_duration2());
      minuteRepeat.setWhichSet(oldMinuteRepeat.getWhich_setted());
    }
    else {
      throw new IllegalArgumentException("Arg minuteRepeat is not instance of MinuteRepeat Class");
    }
  }

  MinuteRepeat2 getMinuteRepeat() {

    return minuteRepeat;
  }

  String getLabel() {

    return minuteRepeat.getLabel();
  }

  int getHour() {

    return minuteRepeat.getHour();
  }

  int getMinute() {

    return minuteRepeat.getMinute();
  }

  long getInterval() {

    return minuteRepeat.getInterval();
  }

  int getCount() {

    return minuteRepeat.getCount();
  }

  int getOrgCount() {

    return minuteRepeat.getOrgCount();
  }

  int getOrgCount2() {

    return minuteRepeat.getOrgCount2();
  }

  long getDuration() {

    return minuteRepeat.getDuration();
  }

  int getOrgDurationHour() {

    return minuteRepeat.getOrgDurationHour();
  }

  int getOrgDurationMinute() {

    return minuteRepeat.getOrgDurationMinute();
  }

  long getOrgDuration() {

    return minuteRepeat.getOrgDuration();
  }

  long getOrgDuration2() {

    return minuteRepeat.getOrgDuration2();
  }

  int getWhichSet() {

    return minuteRepeat.getWhichSet();
  }

  void setLabel(String label) {

    minuteRepeat.setLabel(label);
  }

  void setHour(int hour) {

    minuteRepeat.setHour(hour);
  }

  void setMinute(int minute) {

    minuteRepeat.setMinute(minute);
  }

  void setCount(int count) {

    minuteRepeat.setCount(count);
  }

  void setOrgCount(int orgCount) {

    minuteRepeat.setOrgCount(orgCount);
  }

  void addOrgCount(int orgCount) {

    minuteRepeat.addOrgCount(orgCount);
  }

  void setOrgCount2(int orgCount2) {

    minuteRepeat.setOrgCount2(orgCount2);
  }

  void setDuration(long rest) {

    minuteRepeat.setDuration(rest);
  }

  void setOrgDurationHour(int orgDurationHour) {

    minuteRepeat.setOrgDurationHour(orgDurationHour);
  }

  void setOrgDurationMinute(int orgDurationMinute) {

    minuteRepeat.setOrgDurationMinute(orgDurationMinute);
  }

  void setOrgDuration2(long orgDuration2) {

    minuteRepeat.setOrgDuration2(orgDuration2);
  }

  void setWhichSet(int whichSet) {

    minuteRepeat.setWhichSet(whichSet);
  }

  @NonNull
  @Override
  public MinuteRepeatAdapter clone() {

    try {
      MinuteRepeatAdapter minuteRepeat = (MinuteRepeatAdapter)super.clone();
      minuteRepeat.minuteRepeat = this.minuteRepeat.clone();
      return minuteRepeat;
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  void clear() {

    minuteRepeat.clear();
  }

  void countClear() {

    minuteRepeat.countClear();
  }

  void durationClear() {

    minuteRepeat.durationClear();
  }
}
