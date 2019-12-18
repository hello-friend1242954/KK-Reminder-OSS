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

public class ManuallySnoozePickerDiaglogFragment extends DialogFragment
  implements TimePickerDialog.OnTimeSetListener {

  private ManuallySnoozeActivity activity;

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    activity = (ManuallySnoozeActivity)getActivity();
    requireNonNull(activity);

    int hour = activity.custom_hour;
    int minute = activity.custom_minute;

    return new TimePickerDialog(activity, this, hour, minute, true);
  }

  @Override
  public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {

    if(hourOfDay == 0 && minute == 0) {
      activity.custom_hour = 24;
    }
    else {
      activity.custom_hour = hourOfDay;
    }
    activity.custom_minute = minute;

    activity.summary = "";
    if(activity.custom_hour != 0) {
      activity.summary += getResources().getQuantityString(
        R.plurals.hour,
        activity.custom_hour,
        activity.custom_hour
      );
      if(!LOCALE.equals(Locale.JAPAN)) {
        activity.summary += " ";
      }
    }
    if(activity.custom_minute != 0) {
      activity.summary += getResources().getQuantityString(
        R.plurals.minute,
        activity.custom_minute,
        activity.custom_minute
      );
      if(!LOCALE.equals(Locale.JAPAN)) {
        activity.summary += " ";
      }
    }
    activity.summary += getString(R.string.snooze);
    activity.title.setText(activity.summary);
    activity.time.setText(activity.summary);
  }
}
