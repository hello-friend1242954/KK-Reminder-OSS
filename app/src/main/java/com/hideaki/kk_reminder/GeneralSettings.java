package com.hideaki.kk_reminder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class GeneralSettings implements Serializable {

  private static final long serialVersionUID = 1607411965678706774L;
  private List<NonScheduledList> nonScheduledLists = new ArrayList<>();
  private List<Tag> tagList = new ArrayList<>();
  private Item item = new Item();
  private MyTheme theme = new MyTheme();

  List<NonScheduledList> getNonScheduledLists() {

    return nonScheduledLists;
  }

  List<Tag> getTagList() {

    return tagList;
  }

  Tag getTagById(long id) {

    for(Tag tag : tagList) {
      if(tag.getId() == id) {
        return tag;
      }
    }
    return null;
  }

  Item getItem() {

    return item;
  }

  MyTheme getTheme() {

    return theme;
  }

  void setNonScheduledLists(List<NonScheduledList> nonScheduledLists) {

    this.nonScheduledLists = nonScheduledLists;
  }

  void setNonScheduledList(NonScheduledList list) {

    long id = list.getId();
    int size = nonScheduledLists.size();
    for(int i = 0; i < size; i++) {
      NonScheduledList thisList = nonScheduledLists.get(i);
      if(thisList.getId() == id) {
        nonScheduledLists.set(i, list);
        break;
      }
    }
  }

  void setTagList(List<Tag> tagList) {

    this.tagList = tagList;
  }

  void setItem(Item item) {

    this.item = item;
  }

  void setTheme(MyTheme theme) {

    this.theme = theme;
  }
}