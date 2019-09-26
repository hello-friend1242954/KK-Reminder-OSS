package com.hideaki.kk_reminder;

import java.util.Comparator;

public class DoneItemComparator implements Comparator<Item> {

  @Override
  public int compare(Item o1, Item o2) {

    return Long.compare(o2.getDoneDate().getTimeInMillis(), o1.getDoneDate().getTimeInMillis());
  }
}
