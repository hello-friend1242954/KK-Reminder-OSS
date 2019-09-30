package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.widget.TimePicker;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;

public class MinuteRepeatDurationPickerDialogFragment extends DialogFragment
    implements TimePickerDialog.OnTimeSetListener {

  private MainActivity activity;
  private MinuteRepeatEditFragment minuteRepeatEditFragment;

  MinuteRepeatDurationPickerDialogFragment(
      MinuteRepeatEditFragment minuteRepeatEditFragment
  ) {

    this.minuteRepeatEditFragment = minuteRepeatEditFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    int hour = MainEditFragment.minuteRepeat.getOrg_duration_hour();
    int minute = MainEditFragment.minuteRepeat.getOrg_duration_minute();

    activity = (MainActivity)getActivity();
    checkNotNull(activity);

    if(activity.isDarkMode) {
      return new TimePickerDialog(activity, this, hour, minute, true);
    }
    else {
      return new TimePickerDialog(
          activity,
          activity.dialog_style_id,
          this,
          hour,
          minute,
          true
      );
    }
  }

  @Override
  public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

    if(hourOfDay == 0 && minute == 0) {
      MainEditFragment.minuteRepeat.setOrg_duration_hour(24);
    }
    else {
      MainEditFragment.minuteRepeat.setOrg_duration_hour(hourOfDay);
    }
    MainEditFragment.minuteRepeat.setOrg_duration_minute(minute);

    String interval = "";
    int hour_repeat = MainEditFragment.minuteRepeat.getHour();
    if(hour_repeat != 0) {
      interval +=
          activity.getResources().getQuantityString(R.plurals.hour, hour_repeat, hour_repeat);
      if(!LOCALE.equals(Locale.JAPAN)) {
        interval += " ";
      }
    }
    int minute_repeat = MainEditFragment.minuteRepeat.getMinute();
    if(minute_repeat != 0) {
      interval +=
          activity.getResources().getQuantityString(R.plurals.minute, minute_repeat, minute_repeat);
      if(!LOCALE.equals(Locale.JAPAN)) {
        interval += " ";
      }
    }
    String duration = "";
    int duration_hour = MainEditFragment.minuteRepeat.getOrg_duration_hour();
    if(duration_hour != 0) {
      duration +=
          activity.getResources().getQuantityString(R.plurals.hour, duration_hour, duration_hour);
      if(!LOCALE.equals(Locale.JAPAN)) {
        duration += " ";
      }
    }
    int duration_minute = MainEditFragment.minuteRepeat.getOrg_duration_minute();
    if(duration_minute != 0) {
      duration += activity
          .getResources()
          .getQuantityString(R.plurals.minute, duration_minute, duration_minute);
      if(!LOCALE.equals(Locale.JAPAN)) {
        duration += " ";
      }
    }
    String label = activity.getString(R.string.repeat_minute_duration_format, interval, duration);

    MinuteRepeatEditFragment.label_str = label;
    minuteRepeatEditFragment.label.setSummary(label);
    minuteRepeatEditFragment.durationPicker.setTitle(duration);

    MainEditFragment.minuteRepeat.setLabel(label);
  }
}
