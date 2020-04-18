package com.hideaki.kk_reminder;

class TagAdapter {

  private Tag2 tag;

  TagAdapter() {

    tag = new Tag2();
  }

  TagAdapter(long id) {

    tag = new Tag2(id);
  }

  TagAdapter(Object obj) {

    if(obj instanceof Tag2) {
      tag = (Tag2)obj;
    }
    else if(obj instanceof Tag) {
      Tag oldTag = (Tag)obj;
      tag = new Tag2(oldTag.getId());
      tag.setName(oldTag.getName());
      tag.setPrimaryColor(oldTag.getPrimary_color());
      tag.setPrimaryLightColor(oldTag.getPrimary_light_color());
      tag.setPrimaryDarkColor(oldTag.getPrimary_dark_color());
      tag.setPrimaryTextColor(oldTag.getPrimary_text_color());
      tag.setColorOrderGroup(oldTag.getColor_order_group());
      tag.setColorOrderChild(oldTag.getColor_order_child());
      tag.setOrder(oldTag.getOrder());
    }
    else {
      throw new IllegalArgumentException("Arg tag is not instance of Tag Class");
    }
  }

  Tag2 getTag() {

    return tag;
  }

  long getId() {

    return tag.getId();
  }

  String getName() {

    return tag.getName();
  }

  int getPrimaryColor() {

    return tag.getPrimaryColor();
  }

  int getPrimaryLightColor() {

    return tag.getPrimaryLightColor();
  }

  int getPrimaryDarkColor() {

    return tag.getPrimaryDarkColor();
  }

  int getPrimaryTextColor() {

    return tag.getPrimaryTextColor();
  }

  int getColorOrderGroup() {

    return tag.getColorOrderGroup();
  }

  int getColorOrderChild() {

    return tag.getColorOrderChild();
  }

  int getOrder() {

    return tag.getOrder();
  }

  void setId(long id) {

    tag.setId(id);
  }

  void setName(String name) {

    tag.setName(name);
  }

  void setPrimaryColor(int primaryColor) {

    tag.setPrimaryColor(primaryColor);
  }

  void setPrimaryLightColor(int primaryLightColor) {

    tag.setPrimaryLightColor(primaryLightColor);
  }

  void setPrimaryDarkColor(int primaryDarkColor) {

    tag.setPrimaryDarkColor(primaryDarkColor);
  }

  void setPrimaryTextColor(int primaryTextColor) {

    tag.setPrimaryTextColor(primaryTextColor);
  }

  void setColorOrderGroup(int colorOrderGroup) {

    tag.setColorOrderGroup(colorOrderGroup);
  }

  void setColorOrderChild(int colorOrderChild) {

    tag.setColorOrderChild(colorOrderChild);
  }

  void setOrder(int order) {

    tag.setOrder(order);
  }
}
