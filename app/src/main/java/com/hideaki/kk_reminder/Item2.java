package com.hideaki.kk_reminder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;

import static com.hideaki.kk_reminder.UtilClass.DEFAULT_VIBRATION_PATTERN;
import static com.hideaki.kk_reminder.UtilClass.LINE_SEPARATOR;

public class Item2 implements Serializable, Cloneable {

  private static final long serialVersionUID = -6212304336551543417L;
  private long id = Calendar.getInstance().getTimeInMillis();
  private String detail;
  private Calendar date = Calendar.getInstance();
  // 子ビューのコントロールパネルで時間を変えたときだけ時間を元に戻すのに用いる
  private Calendar orgDate = (Calendar)date.clone();
  private Calendar orgDate2 = (Calendar)date.clone(); // MinuteRepeatで元の時間を保持しておくのに用いる
  private long whichTagBelongs;
  private NotifyInterval2 notifyInterval = new NotifyInterval2();
  private DayRepeat2 dayRepeat = new DayRepeat2();
  private MinuteRepeat2 minuteRepeat = new MinuteRepeat2();
  private String soundUri;
  private List<Notes2> notesList = new ArrayList<>();
  private boolean isChecklistMode;
  private long alteredTime; // 子ビューのコントロールパネルで時間を変えたとき、変えた総時間を保持する
  private long orgAlteredTime; // リピート設定による変更をスナックバーから元に戻すのに用いる
  private boolean isAlarmStopped;
  private boolean isOrgAlarmStopped; // リピート設定による変更をスナックバーから元に戻すのに用いる
  private long whichListBelongs;
  private int order = -1;
  private boolean isSelected;
  private Calendar doneDate = Calendar.getInstance();
  private String vibrationPattern = DEFAULT_VIBRATION_PATTERN;

  Item2() {

  }

  long getId() {

    return id;
  }

  String getDetail() {

    return detail;
  }

  Calendar getDate() {

    return date;
  }

  Calendar getOrgDate() {

    return orgDate;
  }

  Calendar getOrgDate2() {

    return orgDate2;
  }

  long getWhichTagBelongs() {

    return whichTagBelongs;
  }

  NotifyInterval2 getNotifyInterval() {

    return notifyInterval;
  }

  DayRepeat2 getDayRepeat() {

    return dayRepeat;
  }

  MinuteRepeat2 getMinuteRepeat() {

    return minuteRepeat;
  }

  String getSoundUri() {

    return soundUri;
  }

  List<Notes2> getNotesList() {

    return notesList;
  }

  String getNotesString() {

    StringBuilder stringBuilder = new StringBuilder();
    for(Notes2 notes : notesList) {
      stringBuilder.append(notes.getString()).append(LINE_SEPARATOR);
    }

    return stringBuilder.toString();
  }

  boolean isChecklistMode() {

    return isChecklistMode;
  }

  long getAlteredTime() {

    return alteredTime;
  }

  long getOrgAlteredTime() {

    return orgAlteredTime;
  }

  boolean isAlarmStopped() {

    return isAlarmStopped;
  }

  boolean isOrgAlarmStopped() {

    return isOrgAlarmStopped;
  }

  long getWhichListBelongs() {

    return whichListBelongs;
  }

  int getOrder() {

    return order;
  }

  boolean isSelected() {

    return isSelected;
  }

  Calendar getDoneDate() {

    return doneDate;
  }

  String getVibrationPattern() {

    return vibrationPattern;
  }

  void setId(long id) {

    this.id = id;
  }

  void setDetail(String detail) {

    this.detail = detail;
  }

  void setDate(Calendar date) {

    this.date = date;
  }

  void setOrgDate(Calendar orgDate) {

    this.orgDate = orgDate;
  }

  void setOrgDate2(Calendar orgDate2) {

    this.orgDate2 = orgDate2;
  }

  void setWhichTagBelongs(long whichTagBelongs) {

    this.whichTagBelongs = whichTagBelongs;
  }

  void setNotifyInterval(NotifyInterval2 notifyInterval) {

    this.notifyInterval = notifyInterval;
  }

  void setDayRepeat(DayRepeat2 dayRepeat) {

    this.dayRepeat = dayRepeat;
  }

  void setMinuteRepeat(MinuteRepeat2 minuteRepeat) {

    this.minuteRepeat = minuteRepeat;
  }

  void setSoundUri(String soundUri) {

    this.soundUri = soundUri;
  }

  void setNotesList(List<Notes2> notesList) {

    this.notesList = notesList;
  }

  void setIsChecklistMode(boolean isChecklistMode) {

    this.isChecklistMode = isChecklistMode;
  }

  void addAlteredTime(long alteredTime) {

    this.alteredTime += alteredTime;
  }

  void setAlteredTime(long alteredTime) {

    this.alteredTime = alteredTime;
  }

  void setOrgAlteredTime(long orgAlteredTime) {

    this.orgAlteredTime = orgAlteredTime;
  }

  void setIsAlarmStopped(boolean isAlarmStopped) {

    this.isAlarmStopped = isAlarmStopped;
  }

  void setIsOrgAlarmStopped(boolean isOrgAlarmStopped) {

    this.isOrgAlarmStopped = isOrgAlarmStopped;
  }

  void setWhichListBelongs(long whichListBelongs) {

    this.whichListBelongs = whichListBelongs;
  }

  void setOrder(int order) {

    this.order = order;
  }

  void setIsSelected(boolean isSelected) {

    this.isSelected = isSelected;
  }

  void setDoneDate(Calendar doneDate) {

    this.doneDate = doneDate;
  }

  void setVibrationPattern(String vibrationPattern) {

    this.vibrationPattern = vibrationPattern;
  }

  @NonNull
  @Override
  public Item2 clone() {

    try {
      Item2 item = (Item2)super.clone();
      item.setDate((Calendar)this.date.clone());
      item.setOrgDate((Calendar)this.orgDate.clone());
      item.setOrgDate2((Calendar)this.orgDate2.clone());
      item.setNotifyInterval(this.notifyInterval.clone());
      item.setDayRepeat(this.dayRepeat.clone());
      item.setMinuteRepeat(this.minuteRepeat.clone());
      item.setNotesList(new ArrayList<>(this.notesList));
      item.setDoneDate((Calendar)this.doneDate.clone());

      return item;
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}