package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static java.util.Objects.requireNonNull;

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
  private NumberPicker ordinalNumber;
  private NumberPicker dayOfWeek;

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
    requireNonNull(activity);

    // ordinal_numberの実装
    ordinalNumber = view.findViewById(R.id.ordinal_number);
    ordinalNumber.setDisplayedValues(null);
    ordinalNumber.setMaxValue(ORDINAL_NUMBER_LIST_JA.length);
    ordinalNumber.setMinValue(1);
    ordinalNumber.setValue(MainEditFragment.dayRepeat.getOrdinalNumber());
    if(LOCALE.equals(Locale.JAPAN)) {
      ordinalNumber.setDisplayedValues(ORDINAL_NUMBER_LIST_JA);
    }
    else {
      ordinalNumber.setDisplayedValues(ORDINAL_NUMBER_LIST_EN);
    }

    // day_on_weekの実装
    dayOfWeek = view.findViewById(R.id.day_on_week);
    dayOfWeek.setDisplayedValues(null);
    dayOfWeek.setMaxValue(DAY_OF_WEEK_LIST_JA.length - 1);
    dayOfWeek.setMinValue(0);
    dayOfWeek.setValue(MainEditFragment.dayRepeat.getOnTheMonth().ordinal());
    if(LOCALE.equals(Locale.JAPAN)) {
      dayOfWeek.setDisplayedValues(DAY_OF_WEEK_LIST_JA);
    }
    else {
      dayOfWeek.setDisplayedValues(DAY_OF_WEEK_LIST_EN);
    }

    final AlertDialog dialog = new AlertDialog.Builder(activity)
      .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {

          MainEditFragment.dayRepeat.setOrdinalNumber(ordinalNumber.getValue());
          MainEditFragment.dayRepeat.setOnTheMonth(Week.values()[dayOfWeek.getValue()]);

          // onTheMonthPickerのラベルを更新
          String label = "";
          if(MainEditFragment.dayRepeat.getOrdinalNumber() < 5) {
            if(LOCALE.equals(Locale.JAPAN)) {
              label += "第" + MainEditFragment.dayRepeat.getOrdinalNumber();
            }
            else {
              int ordinalNumber = MainEditFragment.dayRepeat.getOrdinalNumber();
              String ordinalStr = ordinalNumber + "";
              if(ordinalNumber == 1) {
                ordinalStr += "st";
              }
              else if(ordinalNumber == 2) {
                ordinalStr += "nd";
              }
              else if(ordinalNumber == 3) {
                ordinalStr += "rd";
              }
              else {
                ordinalStr += "th";
              }
              label += ordinalStr + " ";
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

          if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() < 7) {
            if(LOCALE.equals(Locale.JAPAN)) {
              label += DayRepeatCustomPickerFragment
                .DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat.getOnTheMonth().ordinal()] +
                "曜日";
            }
            else {
              label += DayRepeatCustomPickerFragment
                .DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat.getOnTheMonth().ordinal()];
            }
          }
          else if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() == 7) {
            if(LOCALE.equals(Locale.JAPAN)) {
              label += "平日";
            }
            else {
              label += "Weekday";
            }
          }
          else if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() == 8) {
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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
      }
    });

    return dialog;
  }
}
