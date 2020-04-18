package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TimePicker;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static java.util.Objects.requireNonNull;

public class NotifyIntervalDurationPickerDialogFragment extends DialogFragment
  implements TimePickerDialog.OnTimeSetListener {

  private NotifyIntervalEditFragment notifyIntervalEditFragment;

  NotifyIntervalDurationPickerDialogFragment(
    NotifyIntervalEditFragment notifyIntervalEditFragment
  ) {

    this.notifyIntervalEditFragment = notifyIntervalEditFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    int hour = MainEditFragment.notifyInterval.getHour();
    int minute = MainEditFragment.notifyInterval.getMinute();

    MainActivity activity = (MainActivity)getActivity();
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
      MainEditFragment.notifyInterval.setHour(24);
    }
    else {
      MainEditFragment.notifyInterval.setHour(hourOfDay);
    }
    MainEditFragment.notifyInterval.setMinute(minute);

    NotifyIntervalAdapter interval = MainEditFragment.notifyInterval;
    Resources res = getResources();
    String summary;
    String title = "";
    if(LOCALE.equals(Locale.JAPAN)) {
      summary = getString(R.string.unless_complete_task);
      if(interval.getHour() != 0) {
        String tmp = res.getQuantityString(R.plurals.hour, interval.getHour(), interval.getHour());
        summary += tmp;
        title += tmp;
      }
      if(interval.getMinute() != 0) {
        String tmp =
          res.getQuantityString(R.plurals.minute, interval.getMinute(), interval.getMinute());
        summary += tmp;
        title += tmp;
      }
      summary += getString(R.string.per);
      if(interval.getOrgTime() == -1) {
        summary += getString(R.string.infinite_times_notify);
      }
      else {
        summary += res.getQuantityString(R.plurals.times_notify, interval.getOrgTime(),
          interval.getOrgTime()
        );
      }
    }
    else {
      summary = "Notify every ";
      if(interval.getHour() != 0) {
        String tmp =
          res.getQuantityString(R.plurals.hour, interval.getHour(), interval.getHour()) + " ";
        summary += tmp;
        title += tmp;
      }
      if(interval.getMinute() != 0) {
        String tmp =
          res.getQuantityString(R.plurals.minute, interval.getMinute(), interval.getMinute()) +
            " ";
        summary += tmp;
        title += tmp;
      }
      if(interval.getOrgTime() != -1) {
        summary += res.getQuantityString(R.plurals.times_notify, interval.getOrgTime(),
          interval.getOrgTime()
        ) + " ";
      }
      summary += getString(R.string.unless_complete_task);
    }

    notifyIntervalEditFragment.duration.setTitle(title);

    if(interval.getOrgTime() == 0) {
      notifyIntervalEditFragment.label.setSummary(getString(R.string.non_notify));

      MainEditFragment.notifyInterval.setLabel(getString(R.string.none));
    }
    else {
      notifyIntervalEditFragment.label.setSummary(summary);

      MainEditFragment.notifyInterval.setLabel(summary);
    }
  }
}
