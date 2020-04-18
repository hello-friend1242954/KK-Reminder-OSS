package com.hideaki.kk_reminder;

import androidx.annotation.NonNull;

class NonScheduledListAdapter implements Cloneable {

  private NonScheduledList2 nonScheduledList;

  NonScheduledListAdapter() {

    nonScheduledList = new NonScheduledList2();
  }

  NonScheduledListAdapter(String title) {

    nonScheduledList = new NonScheduledList2(title);
  }

  NonScheduledListAdapter(Object obj) {

    if(obj instanceof NonScheduledList2) {
      nonScheduledList = (NonScheduledList2)obj;
    }
    else if(obj instanceof NonScheduledList) {
      NonScheduledList oldNonScheduledList = (NonScheduledList)obj;
      nonScheduledList = new NonScheduledList2();
      nonScheduledList.setId(oldNonScheduledList.getId());
      nonScheduledList.setTitle(oldNonScheduledList.getTitle());
      oldNonScheduledList.setColor_primary(false);
      nonScheduledList.setIsColorPrimary(false);
      nonScheduledList.setColor(oldNonScheduledList.getColor());
      nonScheduledList.setLightColor(oldNonScheduledList.getLightColor());
      nonScheduledList.setDarkColor(oldNonScheduledList.getDarkColor());
      nonScheduledList.setTextColor(oldNonScheduledList.getTextColor());
      nonScheduledList.setColorGroup(oldNonScheduledList.getColorGroup());
      nonScheduledList.setColorChild(oldNonScheduledList.getColorChild());
      oldNonScheduledList.setColor_primary(true);
      nonScheduledList.setIsColorPrimary(true);
      nonScheduledList.setColor(oldNonScheduledList.getColor());
      nonScheduledList.setLightColor(oldNonScheduledList.getLightColor());
      nonScheduledList.setDarkColor(oldNonScheduledList.getDarkColor());
      nonScheduledList.setTextColor(oldNonScheduledList.getTextColor());
      nonScheduledList.setColorGroup(oldNonScheduledList.getColorGroup());
      nonScheduledList.setColorChild(oldNonScheduledList.getColorChild());
      nonScheduledList.setOrder(oldNonScheduledList.getOrder());
      nonScheduledList.setWhichTagBelongs(oldNonScheduledList.getWhich_tag_belongs());
      nonScheduledList.setIsTodo(oldNonScheduledList.isTodo());
    }
    else {
      throw new IllegalArgumentException("Arg nonScheduledList is not instance of NonScheduledList Class");
    }
  }

  NonScheduledList2 getNonScheduledList() {

    return nonScheduledList;
  }

  long getId() {

    return nonScheduledList.getId();
  }

  String getTitle() {

    return nonScheduledList.getTitle();
  }

  boolean isColorPrimary() {

    return nonScheduledList.isColorPrimary();
  }

  int getColor() {

    return nonScheduledList.getColor();
  }

  int getLightColor() {

    return nonScheduledList.getLightColor();
  }

  int getDarkColor() {

    return nonScheduledList.getDarkColor();
  }

  int getTextColor() {

    return nonScheduledList.getTextColor();
  }

  int getColorGroup() {

    return nonScheduledList.getColorGroup();
  }

  int getColorChild() {

    return nonScheduledList.getColorChild();
  }

  int getOrder() {

    return nonScheduledList.getOrder();
  }

  long getWhichTagBelongs() {

    return nonScheduledList.getWhichTagBelongs();
  }

  boolean isTodo() {

    return nonScheduledList.isTodo();
  }

  void setId(long id) {

    nonScheduledList.setId(id);
  }

  void setTitle(String title) {

    nonScheduledList.setTitle(title);
  }

  void setIsColorPrimary(boolean isColorPrimary) {

    nonScheduledList.setIsColorPrimary(isColorPrimary);
  }

  void setColor(int color) {

    nonScheduledList.setColor(color);
  }

  void setLightColor(int lightColor) {

    nonScheduledList.setLightColor(lightColor);
  }

  void setDarkColor(int darkColor) {

    nonScheduledList.setDarkColor(darkColor);
  }

  void setTextColor(int textColor) {

    nonScheduledList.setTextColor(textColor);
  }

  void setColorGroup(int colorGroup) {

    nonScheduledList.setColorGroup(colorGroup);
  }

  void setColorChild(int colorChild) {

    nonScheduledList.setColorChild(colorChild);
  }

  void setOrder(int order) {

    nonScheduledList.setOrder(order);
  }

  void setWhichTagBelongs(long whichTagBelongs) {

    nonScheduledList.setWhichTagBelongs(whichTagBelongs);
  }

  void setIsTodo(boolean isTodo) {

    nonScheduledList.setIsTodo(isTodo);
  }

  @NonNull
  @Override
  public NonScheduledListAdapter clone() {

    try {
      NonScheduledListAdapter nonScheduledList = (NonScheduledListAdapter)super.clone();
      nonScheduledList.nonScheduledList = this.nonScheduledList.clone();
      return nonScheduledList;
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
