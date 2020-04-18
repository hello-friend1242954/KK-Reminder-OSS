package com.hideaki.kk_reminder;

import java.util.ArrayList;
import java.util.List;

class GeneralSettingsAdapter implements Cloneable {

  private GeneralSettings2 generalSettings;
  private boolean isCopiedFromOldVersion = false;

  GeneralSettingsAdapter() {

    generalSettings = new GeneralSettings2();
  }

  GeneralSettingsAdapter(Object obj) {
    
    if(obj instanceof GeneralSettings2) {
      generalSettings = (GeneralSettings2)obj;
    }
    else if(obj instanceof GeneralSettings) {
      isCopiedFromOldVersion = true;
      GeneralSettings oldGeneralSettings = (GeneralSettings)obj;
      generalSettings = new GeneralSettings2();

      List<NonScheduledList> oldNonScheduledLists = oldGeneralSettings.getNonScheduledLists();
      List<NonScheduledList2> newNonScheduledLists =
        generalSettings.getNonScheduledLists();
      for(NonScheduledList oldNonScheduledList : oldNonScheduledLists) {
        newNonScheduledLists.add(
          new NonScheduledListAdapter(oldNonScheduledList).getNonScheduledList()
        );
      }

      List<Tag> oldTagList = oldGeneralSettings.getTagList();
      List<Tag2> newTagList = generalSettings.getTagList();
      for(Tag oldTag : oldTagList) {
        newTagList.add(new TagAdapter(oldTag).getTag());
      }

      generalSettings.setItem(new ItemAdapter(oldGeneralSettings.getItem()).getItem());
      generalSettings.setTheme(new MyThemeAdapter(oldGeneralSettings.getTheme()).getTheme());
    }
    else {
      throw new IllegalArgumentException(
        "Arg generalSettings is not instance of GeneralSettings Class"
      );
    }
  }

  GeneralSettings2 getGeneralSettings() {

    return generalSettings;
  }

  boolean isCopiedFromOldVersion() {

    return isCopiedFromOldVersion;
  }

  List<NonScheduledListAdapter> getNonScheduledLists() {

    List<NonScheduledListAdapter> nonScheduledListList = new ArrayList<>();
    for(NonScheduledList2 nonScheduledList : generalSettings.getNonScheduledLists()) {
      nonScheduledListList.add(new NonScheduledListAdapter(nonScheduledList));
    }

    return nonScheduledListList;
  }

  List<TagAdapter> getTagList() {

    List<TagAdapter> tagList = new ArrayList<>();
    for(Tag2 tag : generalSettings.getTagList()) {
      tagList.add(new TagAdapter(tag));
    }

    return tagList;
  }

  TagAdapter getTagById(long id) {

    for(Tag2 tag : generalSettings.getTagList()) {
      if(tag.getId() == id) {
        return new TagAdapter(tag);
      }
    }
    return null;
  }

  ItemAdapter getItem() {

    return new ItemAdapter(generalSettings.getItem());
  }

  MyThemeAdapter getTheme() {

    return new MyThemeAdapter(generalSettings.getTheme());
  }

  void setIsCopiedFromOldVersion(boolean isCopiedFromOldVersion) {

    this.isCopiedFromOldVersion = isCopiedFromOldVersion;
  }

  void addNonScheduledList(int index, NonScheduledListAdapter nonScheduledList) {

    generalSettings.getNonScheduledLists().add(index, nonScheduledList.getNonScheduledList());
  }

  void addNonScheduledList(NonScheduledListAdapter nonScheduledList) {

    generalSettings.getNonScheduledLists().add(nonScheduledList.getNonScheduledList());
  }

  void removeNonScheduledList(int index) {

    generalSettings.getNonScheduledLists().remove(index);
  }

  void setNonScheduledLists(List<NonScheduledListAdapter> nonScheduledLists) {

    List<NonScheduledList2> nonScheduledList2List = new ArrayList<>();
    for(NonScheduledListAdapter nonScheduledList : nonScheduledLists) {
      nonScheduledList2List.add(nonScheduledList.getNonScheduledList());
    }
    generalSettings.setNonScheduledLists(nonScheduledList2List);
  }

  void setNonScheduledList(NonScheduledListAdapter list) {

    long id = list.getId();
    List<NonScheduledList2> nonScheduledLists = generalSettings.getNonScheduledLists();
    int size = nonScheduledLists.size();
    for(int i = 0; i < size; i++) {
      if(nonScheduledLists.get(i).getId() == id) {
        nonScheduledLists.set(i, list.getNonScheduledList());
        break;
      }
    }
  }

  void addTag(TagAdapter tag) {

    generalSettings.getTagList().add(tag.getTag());
  }

  void addTag(int index, TagAdapter tag) {

    generalSettings.getTagList().add(index, tag.getTag());
  }

  void removeTag(int index) {

    generalSettings.getTagList().remove(index);
  }

  void setTagList(List<TagAdapter> tagList) {

    List<Tag2> tag2List = new ArrayList<>();
    for(TagAdapter tag : tagList) {
      tag2List.add(tag.getTag());
    }
    generalSettings.setTagList(tag2List);
  }

  void setItem(ItemAdapter item) {

    generalSettings.setItem(item.getItem());
  }

  void setTheme(MyThemeAdapter theme) {

    generalSettings.setTheme(theme.getTheme());
  }
}
