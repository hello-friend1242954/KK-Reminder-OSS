package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.setCursorDrawableColor;

public class MinuteRepeatCountPickerDialogFragment extends DialogFragment {

  private EditText count;
  private MinuteRepeatEditFragment minuteRepeatEditFragment;

  MinuteRepeatCountPickerDialogFragment(MinuteRepeatEditFragment minuteRepeatEditFragment) {

    this.minuteRepeatEditFragment = minuteRepeatEditFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    View view = View.inflate(getContext(), R.layout.minute_repeat_count_picker, null);
    final MainActivity activity = (MainActivity)getActivity();
    checkNotNull(activity);

    count = view.findViewById(R.id.count);
    setCursorDrawableColor(count);
    count.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(
        activity.accent_color,
        PorterDuff.Mode.SRC_IN
    ));
    count.setText(String.valueOf(MainEditFragment.minuteRepeat.getOrg_count()));
    count.setSelection(count.getText().length());

    ImageView plus = view.findViewById(R.id.plus);
    plus.setColorFilter(activity.accent_color);
    GradientDrawable drawable = (GradientDrawable)plus.getBackground();
    drawable = (GradientDrawable)drawable.mutate();
    drawable.setStroke(3, activity.accent_color);
    drawable.setCornerRadius(8.0f);

    plus.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        if("".equals(count.getText().toString())) {
          count.setText(String.valueOf(MainEditFragment.minuteRepeat.getOrg_count()));
        }
        if(Integer.parseInt(count.getText().toString()) < 999) {
          count.setText(String.valueOf(Integer.parseInt(count.getText().toString()) + 1));
          count.setSelection(count.getText().length());
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

        if("".equals(count.getText().toString())) {
          count.setText(String.valueOf(MainEditFragment.minuteRepeat.getOrg_count()));
        }
        if(Integer.parseInt(count.getText().toString()) > 1) {
          count.setText(String.valueOf(Integer.parseInt(count.getText().toString()) - 1));
          count.setSelection(count.getText().length());
        }
      }
    });

    final AlertDialog dialog = new AlertDialog.Builder(activity)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            if("".equals(count.getText().toString())) {
              count.setText(String.valueOf(MainEditFragment.minuteRepeat.getOrg_count()));
            }
            int set_count = Integer.parseInt(count.getText().toString());
            if(set_count > 0) {
              MainEditFragment.minuteRepeat.setOrg_count(set_count);

              String interval = "";
              int hour = MainEditFragment.minuteRepeat.getHour();
              if(hour != 0) {
                interval += activity.getResources().getQuantityString(R.plurals.hour, hour, hour);
                if(!LOCALE.equals(Locale.JAPAN)) {
                  interval += " ";
                }
              }
              int minute = MainEditFragment.minuteRepeat.getMinute();
              if(minute != 0) {
                interval +=
                    activity.getResources().getQuantityString(R.plurals.minute, minute, minute);
                if(!LOCALE.equals(Locale.JAPAN)) {
                  interval += " ";
                }
              }
              int count = MainEditFragment.minuteRepeat.getOrg_count();
              String label =
                  activity.getResources().getQuantityString(R.plurals.repeat_minute_count_format,
                      count, interval, count
                  );
              MinuteRepeatEditFragment.label_str = label;
              minuteRepeatEditFragment.label.setSummary(label);

              MainEditFragment.minuteRepeat.setLabel(label);

              // 項目のタイトル部に現在の設定値を表示
              minuteRepeatEditFragment.count_picker.setTitle(getResources().getQuantityString(
                  R.plurals.times,
                  count,
                  count
              ));
            }
          }
        })
        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

          }
        })
        .setView(view)
        .create();

    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialogInterface) {

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accent_color);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accent_color);
      }
    });

    // ダイアログ表示時にソフトキーボードを自動で表示
    count.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {

        if(hasFocus) {
          Window dialogWindow = dialog.getWindow();
          checkNotNull(dialogWindow);

          dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
      }
    });
    count.requestFocus();

    return dialog;
  }
}
