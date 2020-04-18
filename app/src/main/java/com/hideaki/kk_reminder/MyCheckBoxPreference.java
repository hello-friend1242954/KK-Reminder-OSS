package com.hideaki.kk_reminder;

import android.content.Context;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import android.util.AttributeSet;
import android.widget.TextView;

public class MyCheckBoxPreference extends CheckBoxPreference {

  private AnimCheckBox animCheckBox;
  private boolean isManuallyChecked;
  private MyCheckBoxPreferenceCheckedChangeListener listener = null;
  private boolean isFromOnClick;
  private boolean isChecked;

  @SuppressWarnings("unused")
  MyCheckBoxPreference(Context context) {

    super(context);
    setLayoutResource(R.layout.my_checkbox_preference_layout);
  }

  public MyCheckBoxPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
    setLayoutResource(R.layout.my_checkbox_preference_layout);
  }

  public MyCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {

    super(context, attrs, defStyleAttr);
    setLayoutResource(R.layout.my_checkbox_preference_layout);
  }

  @Override
  public void onAttached() {

    super.onAttached();
    if(listener == null) {
      throw new RuntimeException(
        getKey() + " must call setOnMyCheckBoxPreferenceCheckedChangeListener");
    }
    isChecked = super.isChecked();
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);

    TextView title = (TextView)holder.findViewById(R.id.item_name);
    animCheckBox = (AnimCheckBox)holder.findViewById(R.id.checkBox);
    animCheckBox.setOnCheckedChangeListener(null);
    animCheckBox.setChecked(isChecked, false);
    animCheckBox.setOnCheckedChangeListener(new AnimCheckBox.OnCheckedChangeListener() {
      @Override
      public void onChange(AnimCheckBox view, boolean checked) {

        isChecked = checked;
        if(isManuallyChecked) {
          listener.onCheckedChange(getKey(), checked);
        }
      }
    });
    title.setText(getTitle());
  }

  @Override
  protected void onClick() {

    isFromOnClick = true;
    super.onClick();
  }

  @Override
  public boolean isChecked() {

    return isChecked;
  }

  @Override
  public void setChecked(boolean checked) {

    if(!isFromOnClick) {
      isManuallyChecked = false;
    }
    else {
      isFromOnClick = false;
    }

    super.setChecked(checked);
    if(animCheckBox != null) {
      animCheckBox.setChecked(checked);
    }

    isManuallyChecked = true;
  }

  void setOnMyCheckBoxPreferenceCheckedChangeListener(MyCheckBoxPreferenceCheckedChangeListener listener) {

    this.listener = listener;
  }

  public interface MyCheckBoxPreferenceCheckedChangeListener {

    void onCheckedChange(String key, boolean checked);
  }
}