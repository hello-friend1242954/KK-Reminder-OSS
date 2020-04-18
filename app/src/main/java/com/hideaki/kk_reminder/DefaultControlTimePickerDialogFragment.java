package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_1_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_1_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_2_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_2_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_3_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_3_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_1_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_1_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_2_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_2_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_3_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_3_MINUTE;
import static java.util.Objects.requireNonNull;

public class DefaultControlTimePickerDialogFragment extends DialogFragment
  implements TimePickerDialog.OnTimeSetListener {

  private MainActivity activity;
  private boolean isMinus;
  private int which;
  private boolean isSetWhichTimeControllerCalled = false;
  private TextView target;

  void setWhichTimeController(TextView target, boolean isMinus, int which) {

    this.isMinus = isMinus;
    this.which = which;
    this.target = target;
    isSetWhichTimeControllerCalled = true;
  }

  @Override
  public void show(@NonNull FragmentManager manager, String tag) {

    if(isSetWhichTimeControllerCalled) {
      super.show(manager, tag);
    }
    else {
      throw new IllegalStateException("You must call 'setWhichTimeController' first!");
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    activity = (MainActivity)getActivity();
    requireNonNull(activity);

    int hour;
    int minute;
    switch(which) {
      case 1: {
        hour = isMinus ? activity.minusTime1Hour : activity.plusTime1Hour;
        minute = isMinus ? activity.minusTime1Minute : activity.plusTime1Minute;
        break;
      }
      case 2: {
        hour = isMinus ? activity.minusTime2Hour : activity.plusTime2Hour;
        minute = isMinus ? activity.minusTime2Minute : activity.plusTime2Minute;
        break;
      }
      case 3: {
        hour = isMinus ? activity.minusTime3Hour : activity.plusTime3Hour;
        minute = isMinus ? activity.minusTime3Minute : activity.plusTime3Minute;
        break;
      }
      default: {
        throw new IllegalStateException("Such a control num not exists! : " + which);
      }
    }

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

  @SuppressWarnings("LocalVariableNamingConvention")
  @Override
  public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

    String HOUR_TAG;
    String MINUTE_TAG;
    switch(which) {
      case 1: {
        HOUR_TAG = isMinus ? DEFAULT_MINUS_TIME_1_HOUR : DEFAULT_PLUS_TIME_1_HOUR;
        MINUTE_TAG = isMinus ? DEFAULT_MINUS_TIME_1_MINUTE : DEFAULT_PLUS_TIME_1_MINUTE;
        break;
      }
      case 2: {
        HOUR_TAG = isMinus ? DEFAULT_MINUS_TIME_2_HOUR : DEFAULT_PLUS_TIME_2_HOUR;
        MINUTE_TAG = isMinus ? DEFAULT_MINUS_TIME_2_MINUTE : DEFAULT_PLUS_TIME_2_MINUTE;
        break;
      }
      case 3: {
        HOUR_TAG = isMinus ? DEFAULT_MINUS_TIME_3_HOUR : DEFAULT_PLUS_TIME_3_HOUR;
        MINUTE_TAG = isMinus ? DEFAULT_MINUS_TIME_3_MINUTE : DEFAULT_PLUS_TIME_3_MINUTE;
        break;
      }
      default: {
        throw new IllegalStateException("Such a control num not exists! : " + which);
      }
    }

    activity.setIntGeneralInSharedPreferences(HOUR_TAG, hourOfDay);
    activity.setIntGeneralInSharedPreferences(MINUTE_TAG, minute);

    target.setText(activity.getControlTimeText(isMinus, which));
  }
}
