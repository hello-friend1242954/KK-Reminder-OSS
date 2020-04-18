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

public class ManuallySnoozePickerDialogFragment extends DialogFragment
  implements TimePickerDialog.OnTimeSetListener {

  private ManuallySnoozeActivity activity;

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    activity = (ManuallySnoozeActivity)getActivity();
    requireNonNull(activity);

    int hour = activity.customHour;
    int minute = activity.customMinute;

    return new TimePickerDialog(activity, this, hour, minute, true);
  }

  @Override
  public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {

    if(hourOfDay == 0 && minute == 0) {
      activity.customHour = 24;
    }
    else {
      activity.customHour = hourOfDay;
    }
    activity.customMinute = minute;

    activity.summary = "";
    if(activity.customHour != 0) {
      activity.summary += getResources().getQuantityString(
        R.plurals.hour,
        activity.customHour,
        activity.customHour
      );
      if(!LOCALE.equals(Locale.JAPAN)) {
        activity.summary += " ";
      }
    }
    if(activity.customMinute != 0) {
      activity.summary += getResources().getQuantityString(
        R.plurals.minute,
        activity.customMinute,
        activity.customMinute
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
