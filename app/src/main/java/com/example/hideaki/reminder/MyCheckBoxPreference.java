package com.example.hideaki.reminder;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class MyCheckBoxPreference extends CheckBoxPreference {

  public MyCheckBoxPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
  }

  @Override
  protected void onBindView(View view) {

    super.onBindView(view);

    CheckBox checkBox = view.findViewById(android.R.id.checkbox);
    checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        buttonView.jumpDrawablesToCurrentState();
      }
    });
  }
}
