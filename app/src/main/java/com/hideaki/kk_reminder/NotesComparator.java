package com.hideaki.kk_reminder;

import java.util.Comparator;

public class NotesComparator implements Comparator<NotesAdapter> {

  @Override
  public int compare(NotesAdapter o1, NotesAdapter o2) {

    return Integer.compare(o1.getOrder(), o2.getOrder());
  }
}
