package com.hideaki.kk_reminder;

import java.io.Serializable;

class MyTheme implements Serializable {

  private static final long serialVersionUID = 1616767247996293375L;
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
}
