package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.PorterDuff;

import androidx.preference.PreferenceViewHolder;

import android.graphics.PorterDuffColorFilter;
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
      setCursorDrawableColor(editText);
      editText.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(
          activity.accent_color,
          PorterDuff.Mode.SRC_IN
      ));
      editText.setHint(R.string.detail_hint);
    }
  }
}