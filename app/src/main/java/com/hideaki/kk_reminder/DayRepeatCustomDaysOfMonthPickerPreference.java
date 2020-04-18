package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import android.util.AttributeSet;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.Calendar;

public class DayRepeatCustomDaysOfMonthPickerPreference extends Preference
  implements View.OnClickListener {

  private MainActivity activity;
  private int maxDaysOfMonth;
  private CheckableTextView day;
  private int maskNum;
  static int daysOfMonth;

  public DayRepeatCustomDaysOfMonthPickerPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
    setLayoutResource(R.layout.repeat_custom_days_of_month_picker);
    activity = (MainActivity)((ContextWrapper)context).getBaseContext();
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);

    // その月の最大日数に応じた日にちの表示処理
    daysOfMonth = MainEditFragment.dayRepeat.getDaysOfMonth();
    TableRow lastTableRow = (TableRow)holder.findViewById(R.id.last_table_row);
    maxDaysOfMonth = MainEditFragment.finalCal.getActualMaximum(Calendar.DAY_OF_MONTH);

    lastTableRow.setVisibility(View.VISIBLE);
    for(int i = 0; i < 3; i++) {
      day = (CheckableTextView)lastTableRow.getChildAt(i);
      day.setVisibility(View.VISIBLE);
    }

    if(maxDaysOfMonth <= 28) {
      lastTableRow.setVisibility(View.GONE);
    }
    else {
      for(int i = 3 - (31 - maxDaysOfMonth); i < 3; i++) {
        day = (CheckableTextView)lastTableRow.getChildAt(i);
        day.setVisibility(View.GONE);
      }
    }

    TableLayout calendarTable = (TableLayout)holder.findViewById(R.id.calendar_table);
    if(daysOfMonth == 0) {
      maskNum = MainEditFragment.finalCal.get(Calendar.DAY_OF_MONTH);
      daysOfMonth |= (1 << (maskNum - 1));
      MainEditFragment.dayRepeat.setDaysOfMonth(daysOfMonth);
    }
    int calendarTableSize = calendarTable.getChildCount();
    for(int i = 0; i < calendarTableSize; i++) {
      TableRow tableRow = (TableRow)calendarTable.getChildAt(i);
      int tableRowSize = tableRow.getChildCount();
      for(int j = 0; j < tableRowSize; j++) {
        day = (CheckableTextView)tableRow.getChildAt(j);
        if((daysOfMonth & (1 << (i * 7 + j))) != 0) {
          day.setBackgroundColor(activity.accentColor);
          day.setChecked(true);
        }
        day.setOnClickListener(this);
      }
    }
  }

  @Override
  public void onClick(View v) {

    day = (CheckableTextView)v;
    day.setChecked(!day.isChecked());
    maskNum = Integer.parseInt(day.getText().toString());

    if(day.isChecked()) {
      daysOfMonth |= (1 << (maskNum - 1));
      if(maskNum == maxDaysOfMonth && maskNum != 31) {
        daysOfMonth |= (1 << 30);
      }
      day.setBackgroundColor(activity.accentColor);
    }
    else {
      daysOfMonth &= ~(1 << (maskNum - 1));
      if(maskNum == maxDaysOfMonth && maskNum != 31) {
        daysOfMonth &= ~(1 << 30);
      }
      if(daysOfMonth == 0) {
        daysOfMonth |= (1 << (maskNum - 1));
        if(maskNum == maxDaysOfMonth && maskNum != 31) {
          daysOfMonth |= (1 << 30);
        }
        day.setChecked(true);
      }
      else {
        if(activity.isDarkMode) {
          day.setBackgroundColor(activity.backgroundMaterialDarkColor);
        }
        else {
          day.setBackgroundColor(ContextCompat.getColor(
            getContext(),
            android.R.color.background_light
          ));
        }
      }
    }

    MainEditFragment.dayRepeat.setDaysOfMonth(daysOfMonth);
  }
}
