package com.hideaki.kk_reminder;

import java.util.Comparator;

public class ScheduledItemComparator implements Comparator<ItemAdapter> {

  @Override
  public int compare(ItemAdapter o1, ItemAdapter o2) {

    return Long.compare(o1.getDate().getTimeInMillis(), o2.getDate().getTimeInMillis());
  }
}
