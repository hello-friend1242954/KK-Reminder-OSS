package com.hideaki.kk_reminder;

import java.io.Serializable;

public class Notes implements Serializable, Cloneable {

  private static final long serialVersionUID = 8676811177232193986L;
  private String string;
  private int order;
  private boolean checked;

  public Notes() {

  }

  Notes(String string, boolean checked, int order) {

    this.string = string;
    this.checked = checked;
    this.order = order;
  }

  String getString() {

    return string;
  }

  int getOrder() {

    return order;
  }

  boolean isChecked() {

    return checked;
  }

  void setString(String string) {

    this.string = string;
  }

  void setOrder(int order) {

    this.order = order;
  }

  void setChecked(boolean checked) {

    this.checked = checked;
  }

  @Override
  public Notes clone() {

    Notes notes = null;

    try {
      notes = (Notes)super.clone();
    }
    catch(CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return notes;
  }
}
