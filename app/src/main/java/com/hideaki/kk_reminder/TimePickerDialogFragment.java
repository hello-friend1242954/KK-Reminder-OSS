package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import static java.util.Objects.requireNonNull;

public class TimePickerDialogFragment extends DialogFragment
  implements TimePickerDialog.OnTimeSetListener {

  private MainEditFragment mainEditFragment;

  TimePickerDialogFragment(MainEditFragment mainEditFragment) {

    this.mainEditFragment = mainEditFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    int hour = MainEditFragment.finalCal.get(Calendar.HOUR_OF_DAY);
    int minute = MainEditFragment.finalCal.get(Calendar.MINUTE);

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

    MainEditFragment.finalCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
    MainEditFragment.finalCal.set(Calendar.MINUTE, minute);
    mainEditFragment.timePicker.setTitle(DateFormat.format("kk:mm", MainEditFragment.finalCal));
  }
}
