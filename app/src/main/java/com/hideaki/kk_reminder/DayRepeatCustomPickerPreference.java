package com.hideaki.kk_reminder;

import android.content.Context;
import android.os.Handler;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import java.util.Calendar;
import java.util.Locale;

public class DayRepeatCustomPickerPreference extends Preference {

  private final String[] SCALE_LIST_JA = {"日", "週", "月", "年"};
  private final String[] SCALE_LIST_EN = {"Day", "Week", "Month", "Year"};

  static boolean day = false;
  static boolean week = false;
  static boolean month = false;
  static boolean year = false;
  private NumberPicker interval;
  private NumberPicker scale;
  private int mask_num;
  private static Locale locale = Locale.getDefault();

  public DayRepeatCustomPickerPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
  }

  @Override
  protected View onCreateView(ViewGroup parent) {

    super.onCreateView(parent);
    return View.inflate(getContext(), R.layout.repeat_custom_picker, null);
  }

  @Override
  protected void onBindView(View view) {

    super.onBindView(view);

    //intervalの実装
    interval = view.findViewById(R.id.interval);
    interval.setDisplayedValues(null);
    interval.setMaxValue(1000);
    interval.setMinValue(1);
    if(MainEditFragment.dayRepeat.getInterval() == 0) {
      MainEditFragment.dayRepeat.setInterval(1);
    }
    interval.setValue(MainEditFragment.dayRepeat.getInterval());
    interval.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
      @Override
      public void onValueChange(NumberPicker picker, int oldVal, final int newVal) {

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {

            if(newVal == interval.getValue()) {
              MainEditFragment.dayRepeat.setInterval(interval.getValue());
            }
          }
        }, 100);
      }
    });

    //scaleの実装
    scale = view.findViewById(R.id.scale);
    scale.setDisplayedValues(null);
    scale.setMaxValue(SCALE_LIST_JA.length);
    scale.setMinValue(1);
    if(MainEditFragment.dayRepeat.getScale() == 0) {
      MainEditFragment.dayRepeat.setScale(1);
      MainEditFragment.dayRepeat.setDay(true);
      day = true;
      week = false;
      month = false;
      year = false;
    }
    scale.setValue(MainEditFragment.dayRepeat.getScale());
    if(locale.equals(Locale.JAPAN)) {
      scale.setDisplayedValues(SCALE_LIST_JA);
    }
    else scale.setDisplayedValues(SCALE_LIST_EN);
    if(MainEditFragment.dayRepeat.getScale() == 1) {
      day = true;
    }
    else if(MainEditFragment.dayRepeat.getScale() == 2) {
      week = true;
      DayRepeatCustomPickerFragment.addWeekPreference();
    }
    else if(MainEditFragment.dayRepeat.getScale() == 3) {
      month = true;
      DayRepeatCustomPickerFragment.addDaysOfMonthPreference();
      DayRepeatCustomPickerFragment.addOnTheMonthPreference();
      if(MainEditFragment.dayRepeat.isDays_of_month_setted()) {
        DayRepeatCustomPickerFragment.addDaysOfMonthPickerPreference();
      }
      else {
        DayRepeatCustomPickerFragment.addOnTheMonthPickerPreference();
      }
    }
    else if(MainEditFragment.dayRepeat.getScale() == 4) {
      year = true;
      DayRepeatCustomPickerFragment.addYearPreference();
    }
    scale.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
      @Override
      public void onValueChange(NumberPicker picker, int oldVal, final int newVal) {

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {

            if(newVal == scale.getValue()) {

              MainEditFragment.dayRepeat.setScale(scale.getValue());
              if(MainEditFragment.dayRepeat.getScale() == 1) {

                MainEditFragment.dayRepeat.setDay(true);

                if(week) {
                  DayRepeatCustomPickerFragment.removeWeekPreference();
                  week = false;
                }
                else if(month) {
                  DayRepeatCustomPickerFragment.removeDaysOfMonthPreference();
                  DayRepeatCustomPickerFragment.removeOnTheMonthPreference();
                  if(MainEditFragment.dayRepeat.isDays_of_month_setted()) {
                    DayRepeatCustomPickerFragment.removeDaysOfMonthPickerPreference();
                  }
                  else {
                    DayRepeatCustomPickerFragment.removeOnTheMonthPickerPreference();
                  }
                  month = false;
                }
                else if(year) {
                  DayRepeatCustomPickerFragment.removeYearPreference();
                  year = false;
                }

                day = true;
              }
              else if(MainEditFragment.dayRepeat.getScale() == 2) {

                if(day) day = false;
                else if(month) {
                  DayRepeatCustomPickerFragment.removeDaysOfMonthPreference();
                  DayRepeatCustomPickerFragment.removeOnTheMonthPreference();
                  if(MainEditFragment.dayRepeat.isDays_of_month_setted()) {
                    DayRepeatCustomPickerFragment.removeDaysOfMonthPickerPreference();
                  }
                  else {
                    DayRepeatCustomPickerFragment.removeOnTheMonthPickerPreference();
                  }
                  month = false;
                }
                else if(year) {
                  DayRepeatCustomPickerFragment.removeYearPreference();
                  year = false;
                }

                DayRepeatCustomPickerFragment.addWeekPreference();
                week = true;
              }
              else if(MainEditFragment.dayRepeat.getScale() == 3) {

                if(day) day = false;
                else if(week) {
                  DayRepeatCustomPickerFragment.removeWeekPreference();
                  week = false;
                }
                else if(year) {
                  DayRepeatCustomPickerFragment.removeYearPreference();
                  year = false;
                }

                DayRepeatCustomPickerFragment.addDaysOfMonthPreference();
                DayRepeatCustomPickerFragment.addOnTheMonthPreference();
                if(MainEditFragment.dayRepeat.isDays_of_month_setted()) {
                  DayRepeatCustomPickerFragment.addDaysOfMonthPickerPreference();
                }
                else {
                  DayRepeatCustomPickerFragment.addOnTheMonthPickerPreference();
                }
                month = true;

                if(MainEditFragment.dayRepeat.getDays_of_month() == 0) {
                  mask_num = MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH);
                  MainEditFragment.dayRepeat.setDays_of_month(1 << (mask_num - 1));
                  DayRepeatCustomPickerFragment.days_of_month.setChecked(true);
                  DayRepeatCustomPickerFragment.on_the_month.setChecked(false);
                  MainEditFragment.dayRepeat.setDays_of_month_setted(true);
                }
              }
              else if(MainEditFragment.dayRepeat.getScale() == 4) {

                if(day) day = false;
                else if(week) {
                  DayRepeatCustomPickerFragment.removeWeekPreference();
                  week = false;
                }
                else if(month) {
                  DayRepeatCustomPickerFragment.removeDaysOfMonthPreference();
                  DayRepeatCustomPickerFragment.removeOnTheMonthPreference();
                  if(MainEditFragment.dayRepeat.isDays_of_month_setted()) {
                    DayRepeatCustomPickerFragment.removeDaysOfMonthPickerPreference();
                  }
                  else {
                    DayRepeatCustomPickerFragment.removeOnTheMonthPickerPreference();
                  }
                  month = false;
                }

                DayRepeatCustomPickerFragment.addYearPreference();
                year = true;
              }
            }
          }
        }, 100);
      }
    });
  }
}