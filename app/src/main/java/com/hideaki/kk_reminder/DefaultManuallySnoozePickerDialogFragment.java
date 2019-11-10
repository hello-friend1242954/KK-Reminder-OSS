package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.widget.TimePicker;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_HOUR;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_MINUTE;

public class DefaultManuallySnoozePickerDialogFragment extends DialogFragment
  implements TimePickerDialog.OnTimeSetListener {

  private MainActivity activity;
  private DefaultManuallySnoozeFragment defaultManuallySnoozeFragment;

  DefaultManuallySnoozePickerDialogFragment(
    DefaultManuallySnoozeFragment defaultManuallySnoozeFragment
  ) {

    this.defaultManuallySnoozeFragment = defaultManuallySnoozeFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    activity = (MainActivity)getActivity();
    checkNotNull(activity);

    int hour = activity.snooze_default_hour;
    int minute = activity.snooze_default_minute;

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

    int hour;
    if(hourOfDay == 0 && minute == 0) {
      hour = 24;
    }
    else {
      hour = hourOfDay;
    }

    String summary = "";
    Resources res = getResources();
    if(hour != 0) {
      summary += res.getQuantityString(R.plurals.hour, hour, hour);
      if(!LOCALE.equals(Locale.JAPAN)) {
        summary += " ";
      }
    }
    if(minute != 0) {
      summary += res.getQuantityString(R.plurals.minute, minute, minute);
      if(!LOCALE.equals(Locale.JAPAN)) {
        summary += " ";
      }
    }
    defaultManuallySnoozeFragment.label.setTitle(summary);

    activity.setIntGeneralInSharedPreferences(SNOOZE_DEFAULT_HOUR, hour);
    activity.setIntGeneralInSharedPreferences(SNOOZE_DEFAULT_MINUTE, minute);
  }
}
