package com.hideaki.kk_reminder;

import java.util.Comparator;

public class DoneItemComparator implements Comparator<ItemAdapter> {

  @Override
  public int compare(ItemAdapter o1, ItemAdapter o2) {

    return Long.compare(o2.getDoneDate().getTimeInMillis(), o1.getDoneDate().getTimeInMillis());
  }
}
