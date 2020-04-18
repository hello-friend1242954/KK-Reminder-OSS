package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TimePicker;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static java.util.Objects.requireNonNull;

public class MinuteRepeatIntervalPickerDialogFragment extends DialogFragment
  implements TimePickerDialog.OnTimeSetListener {

  private MainActivity activity;
  private MinuteRepeatEditFragment minuteRepeatEditFragment;

  MinuteRepeatIntervalPickerDialogFragment(
    MinuteRepeatEditFragment minuteRepeatEditFragment
  ) {

    this.minuteRepeatEditFragment = minuteRepeatEditFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    int hour = MainEditFragment.minuteRepeat.getHour();
    int minute = MainEditFragment.minuteRepeat.getMinute();

    activity = (MainActivity)getActivity();
    requireNonNull(activity);

    if(activity.isDarkMode) {
      return new TimePickerDialog(activity, this, hour, minute, true);
    }
    else {
      return new TimePickerDialog(
        activity,
        activity.dialogStyleId,
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
      MainEditFragment.minuteRepeat.setHour(24);
    }
    else {
      MainEditFragment.minuteRepeat.setHour(hourOfDay);
    }
    MainEditFragment.minuteRepeat.setMinute(minute);

    if(minuteRepeatEditFragment.count.isChecked()) {

      String interval = "";
      int hourRepeat = MainEditFragment.minuteRepeat.getHour();
      if(hourRepeat != 0) {
        interval +=
          activity.getResources().getQuantityString(R.plurals.hour, hourRepeat, hourRepeat);
        if(!LOCALE.equals(Locale.JAPAN)) {
          interval += " ";
        }
      }
      int minuteRepeat = MainEditFragment.minuteRepeat.getMinute();
      if(minuteRepeat != 0) {
        interval += activity
          .getResources()
          .getQuantityString(R.plurals.minute, minuteRepeat, minuteRepeat);
        if(!LOCALE.equals(Locale.JAPAN)) {
          interval += " ";
        }
      }
      int count = MainEditFragment.minuteRepeat.getOrgCount();
      String label = activity.getResources().getQuantityString(R.plurals.repeat_minute_count_format,
        count, interval, count
      );

      MinuteRepeatEditFragment.labelStr = label;
      minuteRepeatEditFragment.label.setSummary(label);
      minuteRepeatEditFragment.interval.setTitle(interval);

      MainEditFragment.minuteRepeat.setLabel(label);
    }
    else if(minuteRepeatEditFragment.duration.isChecked()) {

      String interval = "";
      int hourRepeat = MainEditFragment.minuteRepeat.getHour();
      if(hourRepeat != 0) {
        interval +=
          activity.getResources().getQuantityString(R.plurals.hour, hourRepeat, hourRepeat);
        if(!LOCALE.equals(Locale.JAPAN)) {
          interval += " ";
        }
      }
      int minuteRepeat = MainEditFragment.minuteRepeat.getMinute();
      if(minuteRepeat != 0) {
        interval += activity
          .getResources()
          .getQuantityString(R.plurals.minute, minuteRepeat, minuteRepeat);
        if(!LOCALE.equals(Locale.JAPAN)) {
          interval += " ";
        }
      }
      String duration = "";
      int durationHour = MainEditFragment.minuteRepeat.getOrgDurationHour();
      if(durationHour != 0) {
        duration +=
          activity.getResources().getQuantityString(R.plurals.hour, durationHour, durationHour);
        if(!LOCALE.equals(Locale.JAPAN)) {
          duration += " ";
        }
      }
      int durationMinute = MainEditFragment.minuteRepeat.getOrgDurationMinute();
      if(durationMinute != 0) {
        duration += activity
          .getResources()
          .getQuantityString(R.plurals.minute, durationMinute, durationMinute);
        if(!LOCALE.equals(Locale.JAPAN)) {
          duration += " ";
        }
      }
      String label = activity.getString(R.string.repeat_minute_duration_format, interval, duration);

      MinuteRepeatEditFragment.labelStr = label;
      minuteRepeatEditFragment.label.setSummary(label);
      minuteRepeatEditFragment.interval.setTitle(interval);

      MainEditFragment.minuteRepeat.setLabel(label);
    }
    else {
      String interval = "";
      int hourRepeat = MainEditFragment.minuteRepeat.getHour();
      if(hourRepeat != 0) {
        interval +=
          activity.getResources().getQuantityString(R.plurals.hour, hourRepeat, hourRepeat);
        if(!LOCALE.equals(Locale.JAPAN)) {
          interval += " ";
        }
      }
      int minuteRepeat = MainEditFragment.minuteRepeat.getMinute();
      if(minuteRepeat != 0) {
        interval += activity
          .getResources()
          .getQuantityString(R.plurals.minute, minuteRepeat, minuteRepeat);
        if(!LOCALE.equals(Locale.JAPAN)) {
          interval += " ";
        }
      }
      minuteRepeatEditFragment.interval.setTitle(interval);
    }
  }
}
