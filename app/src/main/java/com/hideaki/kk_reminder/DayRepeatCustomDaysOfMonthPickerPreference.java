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
  private int max_days_of_month;
  private CheckableTextView day;
  private int mask_num;
  static int days_of_month;

  public DayRepeatCustomDaysOfMonthPickerPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
    setLayoutResource(R.layout.repeat_custom_days_of_month_picker);
    activity = (MainActivity)((ContextWrapper)context).getBaseContext();
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);

    // その月の最大日数に応じた日にちの表示処理
    days_of_month = MainEditFragment.dayRepeat.getDays_of_month();
    TableRow last_table_row = (TableRow)holder.findViewById(R.id.last_table_row);
    max_days_of_month = MainEditFragment.final_cal.getActualMaximum(Calendar.DAY_OF_MONTH);

    last_table_row.setVisibility(View.VISIBLE);
    for(int i = 0; i < 3; i++) {
      day = (CheckableTextView)last_table_row.getChildAt(i);
      day.setVisibility(View.VISIBLE);
    }

    if(max_days_of_month <= 28) {
      last_table_row.setVisibility(View.GONE);
    }
    else {
      for(int i = 3 - (31 - max_days_of_month); i < 3; i++) {
        day = (CheckableTextView)last_table_row.getChildAt(i);
        day.setVisibility(View.GONE);
      }
    }

    TableLayout calendar_table = (TableLayout)holder.findViewById(R.id.calendar_table);
    if(days_of_month == 0) {
      mask_num = MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH);
      days_of_month |= (1 << (mask_num - 1));
      MainEditFragment.dayRepeat.setDays_of_month(days_of_month);
    }
    int calendar_table_size = calendar_table.getChildCount();
    for(int i = 0; i < calendar_table_size; i++) {
      TableRow tableRow = (TableRow)calendar_table.getChildAt(i);
      int table_row_size = tableRow.getChildCount();
      for(int j = 0; j < table_row_size; j++) {
        day = (CheckableTextView)tableRow.getChildAt(j);
        if((days_of_month & (1 << (i * 7 + j))) != 0) {
          day.setBackgroundColor(activity.accent_color);
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
    mask_num = Integer.parseInt(day.getText().toString());

    if(day.isChecked()) {
      days_of_month |= (1 << (mask_num - 1));
      if(mask_num == max_days_of_month && mask_num != 31) {
        days_of_month |= (1 << 30);
      }
      day.setBackgroundColor(activity.accent_color);
    }
    else {
      days_of_month &= ~(1 << (mask_num - 1));
      if(mask_num == max_days_of_month && mask_num != 31) {
        days_of_month &= ~(1 << 30);
      }
      if(days_of_month == 0) {
        days_of_month |= (1 << (mask_num - 1));
        if(mask_num == max_days_of_month && mask_num != 31) {
          days_of_month |= (1 << 30);
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

    MainEditFragment.dayRepeat.setDays_of_month(days_of_month);
  }
}
