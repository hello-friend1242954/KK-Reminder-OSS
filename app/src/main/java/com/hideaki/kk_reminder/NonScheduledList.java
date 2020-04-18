package com.hideaki.kk_reminder;

import java.io.Serializable;
import java.util.Calendar;

public class NonScheduledList implements Serializable, Cloneable {

  private static final long serialVersionUID = 5371508300422121626L;
  private final long id = Calendar.getInstance().getTimeInMillis();
  private String title;
  private boolean color_primary = true;
  private int primary_color;
  private int primary_light_color;
  private int primary_dark_color;
  private int primary_text_color;
  private int primary_color_group = -1;
  private int primary_color_child = 5;
  private int secondary_color;
  private int secondary_light_color;
  private int secondary_dark_color;
  private int secondary_text_color;
  private int secondary_color_group = -1;
  private int secondary_color_child = 5;
  private int order;
  private long which_tag_belongs;
  private boolean todo = true;

  NonScheduledList() {

  }

  NonScheduledList(String title) {

    this.title = title;
  }

  long getId() {

    return id;
  }

  String getTitle() {

    return title;
  }

  boolean isColor_primary() {

    return color_primary;
  }

  int getColor() {

    return color_primary ? primary_color : secondary_color;
  }

  int getLightColor() {

    return color_primary ? primary_light_color : secondary_light_color;
  }

  int getDarkColor() {

    return color_primary ? primary_dark_color : secondary_dark_color;
  }

  int getTextColor() {

    return color_primary ? primary_text_color : secondary_text_color;
  }

  int getColorGroup() {

    return color_primary ? primary_color_group : secondary_color_group;
  }

  int getColorChild() {

    return color_primary ? primary_color_child : secondary_color_child;
  }

  int getOrder() {

    return order;
  }

  long getWhich_tag_belongs() {

    return which_tag_belongs;
  }

  boolean isTodo() {

    return todo;
  }

  void setTitle(String title) {

    this.title = title;
  }

  void setColor_primary(boolean color_primary) {

    this.color_primary = color_primary;
  }

  void setColor(int color) {

    if(color_primary) {
      this.primary_color = color;
    }
    else {
      this.secondary_color = color;
    }
  }

  void setLightColor(int light_color) {

    if(color_primary) {
      this.primary_light_color = light_color;
    }
    else {
      this.secondary_light_color = light_color;
    }
  }

  void setDarkColor(int dark_color) {

    if(color_primary) {
      this.primary_dark_color = dark_color;
    }
    else {
      this.secondary_dark_color = dark_color;
    }
  }

  void setTextColor(int text_color) {

    if(color_primary) {
      this.primary_text_color = text_color;
    }
    else {
      this.secondary_text_color = text_color;
    }
  }

  void setColorGroup(int color_group) {

    if(color_primary) {
      this.primary_color_group = color_group;
    }
    else {
      this.secondary_color_group = color_group;
    }
  }

  void setColorChild(int color_child) {

    if(color_primary) {
      this.primary_color_child = color_child;
    }
    else {
      this.secondary_color_child = color_child;
    }
  }

  void setOrder(int order) {

    this.order = order;
  }

  void setWhich_tag_belongs(long which_tag_belongs) {

    this.which_tag_belongs = which_tag_belongs;
  }

  void setTodo(boolean todo) {

    this.todo = todo;
  }

  @Override
  public NonScheduledList clone() {

    NonScheduledList nonScheduledList = null;

    try {
      nonScheduledList = (NonScheduledList)super.clone();
    }
    catch(CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return nonScheduledList;
  }
}
