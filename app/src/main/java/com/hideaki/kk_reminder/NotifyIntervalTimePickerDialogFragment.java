package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;

public class NotifyIntervalTimePickerDialogFragment extends DialogFragment {

  private EditText time;

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    View view = View.inflate(getContext(), R.layout.notify_interval_time_picker, null);
    final MainActivity activity = (MainActivity)getActivity();
    checkNotNull(activity);

    time = view.findViewById(R.id.time);
    time.requestFocus();
    time.setText(String.valueOf(MainEditFragment.notifyInterval.getOrg_time()));
    time.setSelection(time.getText().length());

    ImageView plus = view.findViewById(R.id.plus);
    plus.setColorFilter(activity.accent_color);
    GradientDrawable drawable = (GradientDrawable)plus.getBackground();
    drawable = (GradientDrawable)drawable.mutate();
    drawable.setStroke(3, activity.accent_color);
    drawable.setCornerRadius(8.0f);

    plus.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        if("".equals(time.getText().toString())) {
          time.setText(String.valueOf(MainEditFragment.notifyInterval.getOrg_time()));
        }
        if(Integer.parseInt(time.getText().toString()) < 999) {
          time.setText(String.valueOf(Integer.parseInt(time.getText().toString()) + 1));
          time.setSelection(time.getText().length());
        }
      }
    });

    ImageView minus = view.findViewById(R.id.minus);
    minus.setColorFilter(activity.accent_color);
    drawable = (GradientDrawable)minus.getBackground();
    drawable = (GradientDrawable)drawable.mutate();
    drawable.setStroke(3, activity.accent_color);
    drawable.setCornerRadius(8.0f);

    minus.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        if("".equals(time.getText().toString())) {
          time.setText(String.valueOf(MainEditFragment.notifyInterval.getOrg_time()));
        }
        if(Integer.parseInt(time.getText().toString()) > -1) {
          time.setText(String.valueOf(Integer.parseInt(time.getText().toString()) - 1));
          time.setSelection(time.getText().length());
        }
      }
    });

    return new AlertDialog.Builder(activity)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            if("".equals(time.getText().toString())) {
              time.setText(String.valueOf(MainEditFragment.notifyInterval.getOrg_time()));
            }
            int set_time = Integer.parseInt(time.getText().toString());
            if(set_time > -2) {
              NotifyInterval interval = MainEditFragment.notifyInterval;

              interval.setOrg_time(set_time);

              if(interval.getOrg_time() != 0) {
                Resources res = activity.getResources();
                String summary;
                if(LOCALE.equals(Locale.JAPAN)) {
                  summary = activity.getString(R.string.unless_complete_task);
                  if(interval.getHour() != 0) {
                    summary += res.getQuantityString(R.plurals.hour, interval.getHour(), interval.getHour());
                  }
                  if(interval.getMinute() != 0) {
                    summary += res.getQuantityString(R.plurals.minute, interval.getMinute(), interval.getMinute());
                  }
                  summary += activity.getString(R.string.per);
                  if(interval.getOrg_time() == -1) {
                    summary += activity.getString(R.string.infinite_times_notify);
                  }
                  else {
                    summary += res.getQuantityString(R.plurals.times_notify, interval.getOrg_time(),
                        interval.getOrg_time());
                  }
                }
                else {
                  summary = "Notify every ";
                  if(interval.getHour() != 0) {
                    summary += res.getQuantityString(R.plurals.hour, interval.getHour(), interval.getHour());
                    if(!LOCALE.equals(Locale.JAPAN)) summary += " ";
                  }
                  if(interval.getMinute() != 0) {
                    summary += res.getQuantityString(R.plurals.minute, interval.getMinute(), interval.getMinute());
                    if(!LOCALE.equals(Locale.JAPAN)) summary += " ";
                  }
                  if(interval.getOrg_time() != -1) {
                    summary += res.getQuantityString(R.plurals.times_notify, interval.getOrg_time(),
                        interval.getOrg_time()) + " ";
                  }
                  summary += activity.getString(R.string.unless_complete_task);
                }

                NotifyIntervalEditFragment.label.setSummary(summary);

                MainEditFragment.notifyInterval.setLabel(summary);
              }
              else {
                NotifyIntervalEditFragment.label.setSummary(activity.getString(R.string.non_notify));

                MainEditFragment.notifyInterval.setLabel(activity.getString(R.string.none));
              }

              //項目のタイトル部に現在の設定値を表示
              int org_time = interval.getOrg_time();
              NotifyIntervalEditFragment.time.setTitle(getResources().getQuantityString(R.plurals.times, org_time, org_time));
            }
          }
        })
        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {}
        })
        .setView(view)
        .create();
  }
}