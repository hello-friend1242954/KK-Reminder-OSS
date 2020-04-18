package com.hideaki.kk_reminder;

class MyThemeAdapter {

  private MyTheme2 theme;

  MyThemeAdapter() {

    theme = new MyTheme2();
  }

  MyThemeAdapter(Object obj) {

    if(obj instanceof MyTheme2) {
      theme = (MyTheme2)obj;
    }
    else if(obj instanceof MyTheme) {
      MyTheme oldTheme = (MyTheme)obj;
      theme = new MyTheme2();
      oldTheme.setColor_primary(false);
      theme.setIsColorPrimary(false);
      theme.setColor(oldTheme.getColor());
      theme.setLightColor(oldTheme.getLightColor());
      theme.setDarkColor(oldTheme.getDarkColor());
      theme.setTextColor(oldTheme.getTextColor());
      theme.setColorGroup(oldTheme.getColorGroup());
      theme.setColorChild(oldTheme.getColorChild());
      oldTheme.setColor_primary(true);
      theme.setIsColorPrimary(true);
      theme.setColor(oldTheme.getColor());
      theme.setLightColor(oldTheme.getLightColor());
      theme.setDarkColor(oldTheme.getDarkColor());
      theme.setTextColor(oldTheme.getTextColor());
      theme.setColorGroup(oldTheme.getColorGroup());
      theme.setColorChild(oldTheme.getColorChild());
    }
    else {
      throw new IllegalArgumentException("Arg theme is not instance of MyTheme Class");
    }
  }

  MyTheme2 getTheme() {

    return theme;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  boolean isColorPrimary() {

    return theme.isColorPrimary();
  }

  int getColor() {

    return theme.getColor();
  }

  int getLightColor() {

    return theme.getLightColor();
  }

  int getDarkColor() {

    return theme.getDarkColor();
  }

  int getTextColor() {

    return theme.getTextColor();
  }

  int getColorGroup() {

    return theme.getColorGroup();
  }

  int getColorChild() {

    return theme.getColorChild();
  }

  void setIsColorPrimary(boolean isColorPrimary) {

    theme.setIsColorPrimary(isColorPrimary);
  }

  void setColor(int color) {

    theme.setColor(color);
  }

  void setLightColor(int lightColor) {

    theme.setLightColor(lightColor);
  }

  void setDarkColor(int darkColor) {

    theme.setDarkColor(darkColor);
  }

  void setTextColor(int textColor) {

    theme.setTextColor(textColor);
  }

  void setColorGroup(int colorGroup) {

    theme.setColorGroup(colorGroup);
  }

  void setColorChild(int colorChild) {

    theme.setColorChild(colorChild);
  }
}
