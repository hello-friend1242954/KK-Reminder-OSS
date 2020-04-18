package com.hideaki.kk_reminder;

import androidx.annotation.NonNull;

class NotesAdapter implements Cloneable {

  private Notes2 notes;

  NotesAdapter() {

    notes = new Notes2();
  }

  NotesAdapter(String string, boolean isChecked, int order) {

    notes = new Notes2(string, isChecked, order);
  }

  NotesAdapter(Object obj) {

    if(obj instanceof Notes2) {
      notes = (Notes2)obj;
    }
    else if(obj instanceof Notes) {
      Notes oldNotes = (Notes)obj;
      notes = new Notes2(oldNotes.getString(), oldNotes.isChecked(), oldNotes.getOrder());
    }
    else {
      throw new IllegalArgumentException("Arg notes is not instance of Notes Class");
    }
  }

  Notes2 getNotes() {

    return notes;
  }

  String getString() {

    return notes.getString();
  }

  int getOrder() {

    return notes.getOrder();
  }

  boolean isChecked() {

    return notes.isChecked();
  }

  void setString(String string) {

    notes.setString(string);
  }

  void setOrder(int order) {

    notes.setOrder(order);
  }

  void setIsChecked(boolean isChecked) {

    notes.setIsChecked(isChecked);
  }

  @NonNull
  @Override
  public NotesAdapter clone() {

    try {
      NotesAdapter notes = (NotesAdapter)super.clone();
      notes.notes = this.notes.clone();
      return notes;
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
