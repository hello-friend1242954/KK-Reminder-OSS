package com.hideaki.kk_reminder;

import java.io.Serializable;
import java.util.Calendar;

import androidx.annotation.NonNull;

public class NonScheduledList2 implements Serializable, Cloneable {

  private static final long serialVersionUID = -790943097657293664L;
  private long id = Calendar.getInstance().getTimeInMillis();
  private String title;
  private boolean isColorPrimary = true;
  private int primaryColor;
  private int primaryLightColor;
  private int primaryDarkColor;
  private int primaryTextColor;
  private int primaryColorGroup = -1;
  private int primaryColorChild = 5;
  private int secondaryColor;
  private int secondaryLightColor;
  private int secondaryDarkColor;
  private int secondaryTextColor;
  private int secondaryColorGroup = -1;
  private int secondaryColorChild = 5;
  private int order;
  private long whichTagBelongs;
  private boolean isTodo = true;

  NonScheduledList2() {

  }

  NonScheduledList2(String title) {

    this.title = title;
  }

  long getId() {

    return id;
  }

  String getTitle() {

    return title;
  }

  boolean isColorPrimary() {

    return isColorPrimary;
  }

  int getColor() {

    return isColorPrimary ? primaryColor : secondaryColor;
  }

  int getLightColor() {

    return isColorPrimary ? primaryLightColor : secondaryLightColor;
  }

  int getDarkColor() {

    return isColorPrimary ? primaryDarkColor : secondaryDarkColor;
  }

  int getTextColor() {

    return isColorPrimary ? primaryTextColor : secondaryTextColor;
  }

  int getColorGroup() {

    return isColorPrimary ? primaryColorGroup : secondaryColorGroup;
  }

  int getColorChild() {

    return isColorPrimary ? primaryColorChild : secondaryColorChild;
  }

  int getOrder() {

    return order;
  }

  long getWhichTagBelongs() {

    return whichTagBelongs;
  }

  boolean isTodo() {

    return isTodo;
  }

  void setId(long id) {

    this.id = id;
  }

  void setTitle(String title) {

    this.title = title;
  }

  void setIsColorPrimary(boolean isColorPrimary) {

    this.isColorPrimary = isColorPrimary;
  }

  void setColor(int color) {

    if(isColorPrimary) {
      this.primaryColor = color;
    }
    else {
      this.secondaryColor = color;
    }
  }

  void setLightColor(int lightColor) {

    if(isColorPrimary) {
      this.primaryLightColor = lightColor;
    }
    else {
      this.secondaryLightColor = lightColor;
    }
  }

  void setDarkColor(int darkColor) {

    if(isColorPrimary) {
      this.primaryDarkColor = darkColor;
    }
    else {
      this.secondaryDarkColor = darkColor;
    }
  }

  void setTextColor(int textColor) {

    if(isColorPrimary) {
      this.primaryTextColor = textColor;
    }
    else {
      this.secondaryTextColor = textColor;
    }
  }

  void setColorGroup(int colorGroup) {

    if(isColorPrimary) {
      this.primaryColorGroup = colorGroup;
    }
    else {
      this.secondaryColorGroup = colorGroup;
    }
  }

  void setColorChild(int colorChild) {

    if(isColorPrimary) {
      this.primaryColorChild = colorChild;
    }
    else {
      this.secondaryColorChild = colorChild;
    }
  }

  void setOrder(int order) {

    this.order = order;
  }

  void setWhichTagBelongs(long whichTagBelongs) {

    this.whichTagBelongs = whichTagBelongs;
  }

  void setIsTodo(boolean isTodo) {

    this.isTodo = isTodo;
  }

  @NonNull
  @Override
  public NonScheduledList2 clone() {

    try {
      return (NonScheduledList2)super.clone();
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
