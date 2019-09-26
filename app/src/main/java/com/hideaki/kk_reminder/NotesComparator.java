package com.hideaki.kk_reminder;

import java.util.Comparator;

public class NotesComparator implements Comparator<Notes> {

  @Override
  public int compare(Notes o1, Notes o2) {

    return Integer.compare(o1.getOrder(), o2.getOrder());
  }
}
