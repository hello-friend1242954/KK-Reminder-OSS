package com.hideaki.kk_reminder;

import java.util.Comparator;

public class ScheduledItemComparator implements Comparator<Item> {

  @Override
  public int compare(Item o1, Item o2) {

    return Long.compare(o1.getDate().getTimeInMillis(), o2.getDate().getTimeInMillis());
  }
}
