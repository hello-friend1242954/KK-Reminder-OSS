package com.hideaki.kk_reminder;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class Notes2 implements Serializable, Cloneable {

  private static final long serialVersionUID = 2176002554282362696L;
  private String string;
  private int order;
  private boolean isChecked;

  Notes2() {

  }

  Notes2(String string, boolean isChecked, int order) {

    this.string = string;
    this.isChecked = isChecked;
    this.order = order;
  }

  String getString() {

    return string;
  }

  int getOrder() {

    return order;
  }

  boolean isChecked() {

    return isChecked;
  }

  void setString(String string) {

    this.string = string;
  }

  void setOrder(int order) {

    this.order = order;
  }

  void setIsChecked(boolean isChecked) {

    this.isChecked = isChecked;
  }

  @NonNull
  @Override
  public Notes2 clone() {

    try {
      return (Notes2)super.clone();
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
