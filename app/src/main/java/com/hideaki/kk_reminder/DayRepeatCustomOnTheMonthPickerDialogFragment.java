package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.widget.NumberPicker;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;

public class DayRepeatCustomOnTheMonthPickerDialogFragment extends DialogFragment {

  private static final String[] ORDINAL_NUMBER_LIST_JA = {"第一", "第二", "第三", "第四", "最終"};
  private static final String[] ORDINAL_NUMBER_LIST_EN = {"1st", "2nd", "3rd", "4th", "Last"};
  private static final String[] DAY_OF_WEEK_LIST_JA = {
      "月曜日", "火曜日", "水曜日", "木曜日",
      "金曜日", "土曜日", "日曜日", "平日", "週末"
  };
  private static final String[] DAY_OF_WEEK_LIST_EN = {
      "Mon", "Tue", "Wed", "Thu", "Fri",
      "Sat", "Sun", "Weekday", "Weekend Day"
  };

  private DayRepeatCustomPickerFragment dayRepeatCustomPickerFragment;
  private NumberPicker ordinal_number;
  private NumberPicker day_of_week;

  DayRepeatCustomOnTheMonthPickerDialogFragment(
      DayRepeatCustomPickerFragment dayRepeatCustomPickerFragment
  ) {

    this.dayRepeatCustomPickerFragment = dayRepeatCustomPickerFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    View view = View.inflate(getContext(), R.layout.repeat_custom_on_the_month_picker, null);
    final MainActivity activity = (MainActivity)getActivity();
    checkNotNull(activity);

    // ordinal_numberの実装
    ordinal_number = view.findViewById(R.id.ordinal_number);
    ordinal_number.setDisplayedValues(null);
    ordinal_number.setMaxValue(ORDINAL_NUMBER_LIST_JA.length);
    ordinal_number.setMinValue(1);
    ordinal_number.setValue(MainEditFragment.dayRepeat.getOrdinal_number());
    if(LOCALE.equals(Locale.JAPAN)) {
      ordinal_number.setDisplayedValues(ORDINAL_NUMBER_LIST_JA);
    }
    else {
      ordinal_number.setDisplayedValues(ORDINAL_NUMBER_LIST_EN);
    }

    // day_on_weekの実装
    day_of_week = view.findViewById(R.id.day_on_week);
    day_of_week.setDisplayedValues(null);
    day_of_week.setMaxValue(DAY_OF_WEEK_LIST_JA.length - 1);
    day_of_week.setMinValue(0);
    day_of_week.setValue(MainEditFragment.dayRepeat.getOn_the_month().ordinal());
    if(LOCALE.equals(Locale.JAPAN)) {
      day_of_week.setDisplayedValues(DAY_OF_WEEK_LIST_JA);
    }
    else {
      day_of_week.setDisplayedValues(DAY_OF_WEEK_LIST_EN);
    }

    final AlertDialog dialog = new AlertDialog.Builder(activity)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            MainEditFragment.dayRepeat.setOrdinal_number(ordinal_number.getValue());
            MainEditFragment.dayRepeat.setOn_the_month(Week.values()[day_of_week.getValue()]);

            // onTheMonthPickerのラベルを更新
            String label = "";
            if(MainEditFragment.dayRepeat.getOrdinal_number() < 5) {
              if(LOCALE.equals(Locale.JAPAN)) {
                label += "第" + MainEditFragment.dayRepeat.getOrdinal_number();
              }
              else {
                int ordinal_num = MainEditFragment.dayRepeat.getOrdinal_number();
                String ordinal_str = ordinal_num + "";
                if(ordinal_num == 1) {
                  ordinal_str += "st";
                }
                else if(ordinal_num == 2) {
                  ordinal_str += "nd";
                }
                else if(ordinal_num == 3) {
                  ordinal_str += "rd";
                }
                else {
                  ordinal_str += "th";
                }
                label += ordinal_str + " ";
              }
            }
            else {
              if(LOCALE.equals(Locale.JAPAN)) {
                label += "最終週の";
              }
              else {
                label += "Last ";
              }
            }

            if(MainEditFragment.dayRepeat.getOn_the_month().ordinal() < 7) {
              if(LOCALE.equals(Locale.JAPAN)) {
                label += DayRepeatCustomPickerFragment
                    .DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat.getOn_the_month().ordinal()] +
                    "曜日";
              }
              else {
                label += DayRepeatCustomPickerFragment
                    .DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat.getOn_the_month().ordinal()];
              }
            }
            else if(MainEditFragment.dayRepeat.getOn_the_month().ordinal() == 7) {
              if(LOCALE.equals(Locale.JAPAN)) {
                label += "平日";
              }
              else {
                label += "Weekday";
              }
            }
            else if(MainEditFragment.dayRepeat.getOn_the_month().ordinal() == 8) {
              if(LOCALE.equals(Locale.JAPAN)) {
                label += "週末";
              }
              else {
                label += "Weekend day";
              }
            }

            dayRepeatCustomPickerFragment.onTheMonthPicker.setTitle(label);
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

    return dialog;
  }
}
