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

  private final String[] SCALE_LIST_JA = {"日", "週", "月", "年"};
  private final String[] SCALE_LIST_EN = {"Day", "Week", "Month", "Year"};

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

          int interval_val = interval.getValue();
          int scale_val = scale.getValue();

          MainEditFragment.dayRepeat.setInterval(interval_val);
          MainEditFragment.dayRepeat.setScale(scale_val);

          // Scaleの表示処理
          String intervalLabel = "";
          Resources res = getResources();
          dayRepeatCustomPickerFragment.rootPreferenceScreen.removeAll();
          dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
            dayRepeatCustomPickerFragment.picker
          );
          switch(MainEditFragment.dayRepeat.getScale()) {

            case 1: {

              if(LOCALE.equals(Locale.JAPAN) && interval_val == 1) {
                intervalLabel = getString(R.string.everyday);
              }
              else {
                intervalLabel =
                  res.getQuantityString(R.plurals.per_day, interval_val, interval_val);
              }

              break;
            }
            case 2: {

              dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                dayRepeatCustomPickerFragment.week
              );

              if(LOCALE.equals(Locale.JAPAN) && interval_val == 1) {
                intervalLabel = getString(R.string.everyweek);
              }
              else {
                intervalLabel =
                  res.getQuantityString(R.plurals.per_week, interval_val, interval_val);
              }

              break;
            }
            case 3: {

              dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                dayRepeatCustomPickerFragment.days_of_month
              );
              dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                dayRepeatCustomPickerFragment.on_the_month
              );
              if(MainEditFragment.dayRepeat.isDays_of_month_setted()) {
                dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                  dayRepeatCustomPickerFragment.days_of_month_picker
                );
              }
              else {
                dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                  dayRepeatCustomPickerFragment.onTheMonthPicker
                );
              }

              if(LOCALE.equals(Locale.JAPAN) && interval_val == 1) {
                intervalLabel = getString(R.string.everymonth);
              }
              else {
                intervalLabel =
                  res.getQuantityString(R.plurals.per_month, interval_val, interval_val);
              }

              // onTheMonthPickerのラベルを初期化
              String label = "";
              if(!MainEditFragment.dayRepeat.isDays_of_month_setted()) {
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
                      .DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat
                      .getOn_the_month()
                      .ordinal()] + "曜日";
                  }
                  else {
                    label += DayRepeatCustomPickerFragment
                      .DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat
                      .getOn_the_month()
                      .ordinal()];
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

              break;
            }
            case 4: {

              dayRepeatCustomPickerFragment.rootPreferenceScreen.addPreference(
                dayRepeatCustomPickerFragment.year);

              if(LOCALE.equals(Locale.JAPAN) && interval_val == 1) {
                intervalLabel = getString(R.string.everyyear);
              }
              else {
                intervalLabel =
                  res.getQuantityString(R.plurals.per_year, interval_val, interval_val);
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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accent_color);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accent_color);
      }
    });

    return dialog;
  }
}
