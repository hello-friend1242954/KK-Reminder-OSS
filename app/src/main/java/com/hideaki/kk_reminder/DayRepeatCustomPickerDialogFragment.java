package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
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

public class DayRepeatCustomPickerDialogFragment extends DialogFragment {

  private static final String[] SCALE_LIST_JA = {"日", "週", "月", "年"};
  private static final String[] SCALE_LIST_EN = {"Day", "Week", "Month", "Year"};

  private DayRepeatCustomPickerFragment dayRepeatCustomPickerFragment;
  private NumberPicker interval;
  private NumberPicker scale;

  DayRepeatCustomPickerDialogFragment(
    DayRepeatCustomPickerFragment dayRepeatCustomPickerFragment
  ) {

    this.dayRepeatCustomPickerFragment = dayRepeatCustomPickerFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    View view = View.inflate(getContext(), R.layout.repeat_custom_picker, null);
    final MainActivity activity = (MainActivity)getActivity();
    requireNonNull(activity);

    // intervalの実装
    interval = view.findViewById(R.id.interval);
    interval.setDisplayedValues(null);
    interval.setMaxValue(1000);
    interval.setMinValue(1);
    interval.setValue(MainEditFragment.dayRepeat.getInterval());

    // scaleの実装
    scale = view.findViewById(R.id.scale);
    scale.setDisplayedValues(null);
    scale.setMaxValue(SCALE_LIST_JA.length);
    scale.setMinValue(1);
    scale.setValue(MainEditFragment.dayRepeat.getScale());
    if(LOCALE.equals(Locale.JAPAN)) {
      scale.setDisplayedValues(SCALE_LIST_JA);
    }
    else {
      scale.setDisplayedValues(SCALE_LIST_EN);
    }

    final AlertDialog dialog = new AlertDialog.Builder(activity)
      .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {

          int intervalValue = interval.getValue();
          int scaleValue = scale.getValue();

          MainEditFragment.dayRepeat.setInterval(intervalValue);
          MainEditFragment.dayRepeat.setScale(scaleValue);

          // Scaleの表示処理
          String intervalLabel = "";
          Resources res = getResources();
          dayRepeatCustomPickerFragment.rootPreferenceScreen.removeAll();
          dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
            dayRepeatCustomPickerFragment.picker
          );
          switch(MainEditFragment.dayRepeat.getScale()) {

            case 1: {

              if(LOCALE.equals(Locale.JAPAN) && intervalValue == 1) {
                intervalLabel = getString(R.string.everyday);
              }
              else {
                intervalLabel =
                  res.getQuantityString(R.plurals.per_day, intervalValue, intervalValue);
              }

              break;
            }
            case 2: {

              dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                dayRepeatCustomPickerFragment.week
              );

              if(LOCALE.equals(Locale.JAPAN) && intervalValue == 1) {
                intervalLabel = getString(R.string.every_week);
              }
              else {
                intervalLabel =
                  res.getQuantityString(R.plurals.per_week, intervalValue, intervalValue);
              }

              break;
            }
            case 3: {

              dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                dayRepeatCustomPickerFragment.daysOfMonth
              );
              dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                dayRepeatCustomPickerFragment.onTheMonth
              );
              if(MainEditFragment.dayRepeat.isDaysOfMonthSet()) {
                dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                  dayRepeatCustomPickerFragment.daysOfMonthPicker
                );
              }
              else {
                dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                  dayRepeatCustomPickerFragment.onTheMonthPicker
                );
              }

              if(LOCALE.equals(Locale.JAPAN) && intervalValue == 1) {
                intervalLabel = getString(R.string.every_month);
              }
              else {
                intervalLabel =
                  res.getQuantityString(R.plurals.per_month, intervalValue, intervalValue);
              }

              // onTheMonthPickerのラベルを初期化
              String label = "";
              if(!MainEditFragment.dayRepeat.isDaysOfMonthSet()) {
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
                      .DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat
                      .getOnTheMonth()
                      .ordinal()] + "曜日";
                  }
                  else {
                    label += DayRepeatCustomPickerFragment
                      .DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat
                      .getOnTheMonth()
                      .ordinal()];
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

              break;
            }
            case 4: {

              dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                dayRepeatCustomPickerFragment.year);

              if(LOCALE.equals(Locale.JAPAN) && intervalValue == 1) {
                intervalLabel = getString(R.string.every_year);
              }
              else {
                intervalLabel =
                  res.getQuantityString(R.plurals.per_year, intervalValue, intervalValue);
              }

              break;
            }
          }

          // Pickerのラベルを更新
          dayRepeatCustomPickerFragment.picker.setTitle(intervalLabel);
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
