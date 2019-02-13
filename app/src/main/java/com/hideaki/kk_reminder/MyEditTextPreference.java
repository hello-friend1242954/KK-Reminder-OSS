package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.PorterDuff;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.EditText;

import com.takisoft.fix.support.v7.preference.EditTextPreference;

import static com.hideaki.kk_reminder.UtilClass.setCursorDrawableColor;

public class MyEditTextPreference extends EditTextPreference {

  private MainActivity activity;

  public MyEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {

    super(context, attrs, defStyleAttr);
    activity = (MainActivity)((ContextWrapper)context).getBaseContext();
  }

  public MyEditTextPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
    activity = (MainActivity)((ContextWrapper)context).getBaseContext();
  }

  public MyEditTextPreference(Context context) {

    super(context);
    activity = (MainActivity)((ContextWrapper)context).getBaseContext();
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);

    EditText editText = getEditText();
    if(editText != null) {
      setCursorDrawableColor(editText, activity.accent_color);
      editText.getBackground().mutate().setColorFilter(activity.accent_color, PorterDuff.Mode.SRC_IN);
      editText.setHint(R.string.detail_hint);
      editText.requestFocus();
      editText.setSelection(editText.getText().length());
    }
  }
}