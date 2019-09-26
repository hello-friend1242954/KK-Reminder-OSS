package com.hideaki.kk_reminder;

import android.content.Context;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import android.util.AttributeSet;
import android.widget.TextView;

public class MyCheckBoxPreference extends CheckBoxPreference {

  private AnimCheckBox animCheckBox;
  private boolean manually_checked;
  private MyCheckBoxPreferenceCheckedChangeListener listener = null;
  private boolean from_on_click;
  private boolean is_checked;

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
    is_checked = super.isChecked();
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);

    TextView title = (TextView)holder.findViewById(R.id.item_name);
    animCheckBox = (AnimCheckBox)holder.findViewById(R.id.checkBox);
    animCheckBox.setOnCheckedChangeListener(null);
    animCheckBox.setChecked(is_checked, false);
    animCheckBox.setOnCheckedChangeListener(new AnimCheckBox.OnCheckedChangeListener() {
      @Override
      public void onChange(AnimCheckBox view, boolean checked) {

        is_checked = checked;
        if(manually_checked) {
          listener.onCheckedChange(getKey(), checked);
        }
      }
    });
    title.setText(getTitle());
  }

  @Override
  protected void onClick() {

    from_on_click = true;
    super.onClick();
  }

  @Override
  public boolean isChecked() {

    return is_checked;
  }

  @Override
  public void setChecked(boolean checked) {

    if(!from_on_click) {
      manually_checked = false;
    }
    else {
      from_on_click = false;
    }

    super.setChecked(checked);
    if(animCheckBox != null) {
      animCheckBox.setChecked(checked);
    }

    manually_checked = true;
  }

  void setOnMyCheckBoxPreferenceCheckedChangeListener(MyCheckBoxPreferenceCheckedChangeListener listener) {

    this.listener = listener;
  }

  public interface MyCheckBoxPreferenceCheckedChangeListener {

    void onCheckedChange(String key, boolean checked);
  }
}