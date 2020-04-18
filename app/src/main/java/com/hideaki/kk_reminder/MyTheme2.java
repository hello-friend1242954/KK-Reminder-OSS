package com.hideaki.kk_reminder;

import java.io.Serializable;

class MyTheme2 implements Serializable {

  private static final long serialVersionUID = -4213604700198080504L;
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
}
