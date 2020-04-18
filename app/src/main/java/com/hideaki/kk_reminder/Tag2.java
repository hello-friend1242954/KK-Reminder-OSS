package com.hideaki.kk_reminder;

import java.io.Serializable;
import java.util.Calendar;

class Tag2 implements Serializable {

  private static final long serialVersionUID = -6271299797697385550L;
  private long id;
  private String name;
  private int primaryColor;
  private int primaryLightColor;
  private int primaryDarkColor;
  private int primaryTextColor;
  private int colorOrderGroup = -1;
  private int colorOrderChild = 5;
  private int order;

  Tag2() {

    this.id = Calendar.getInstance().getTimeInMillis();
  }

  Tag2(long id) {

    this.id = id;
  }

  long getId() {

    return id;
  }

  String getName() {

    return name;
  }

  int getPrimaryColor() {

    return primaryColor;
  }

  int getPrimaryLightColor() {

    return primaryLightColor;
  }

  int getPrimaryDarkColor() {

    return primaryDarkColor;
  }

  int getPrimaryTextColor() {

    return primaryTextColor;
  }

  int getColorOrderGroup() {

    return colorOrderGroup;
  }

  int getColorOrderChild() {

    return colorOrderChild;
  }

  int getOrder() {

    return order;
  }

  void setId(long id) {

    this.id = id;
  }

  void setName(String name) {

    this.name = name;
  }

  void setPrimaryColor(int primaryColor) {

    this.primaryColor = primaryColor;
  }

  void setPrimaryLightColor(int primaryLightColor) {

    this.primaryLightColor = primaryLightColor;
  }

  void setPrimaryDarkColor(int primaryDarkColor) {

    this.primaryDarkColor = primaryDarkColor;
  }

  void setPrimaryTextColor(int primaryTextColor) {

    this.primaryTextColor = primaryTextColor;
  }

  void setColorOrderGroup(int colorOrderGroup) {

    this.colorOrderGroup = colorOrderGroup;
  }

  void setColorOrderChild(int colorOrderChild) {

    this.colorOrderChild = colorOrderChild;
  }

  void setOrder(int order) {

    this.order = order;
  }
}
