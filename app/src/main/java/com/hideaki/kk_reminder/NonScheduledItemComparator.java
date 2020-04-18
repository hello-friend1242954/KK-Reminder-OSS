package com.hideaki.kk_reminder;

import java.util.Comparator;

public class NonScheduledItemComparator implements Comparator<ItemAdapter> {

  @Override
  public int compare(ItemAdapter o1, ItemAdapter o2) {

    return Integer.compare(o1.getOrder(), o2.getOrder());
  }
}
