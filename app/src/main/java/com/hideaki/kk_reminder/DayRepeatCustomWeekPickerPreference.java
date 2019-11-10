package com.hideaki.kk_reminder;

import android.content.Context;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import android.util.AttributeSet;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.Calendar;

public class DayRepeatCustomWeekPickerPreference extends Preference
  implements AnimCheckBox.OnCheckedChangeListener, View.OnClickListener {

  private AnimCheckBox checkBox;
  private int mask_num;
  static int week;

  public DayRepeatCustomWeekPickerPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
    setLayoutResource(R.layout.repeat_custom_week_picker);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);

    week = MainEditFragment.dayRepeat.getWeek();

    TableLayout week_table = (TableLayout)holder.findViewById(R.id.week_table);
    if(week == 0) {
      int day_of_week = MainEditFragment.final_cal.get(Calendar.DAY_OF_WEEK);
      mask_num = day_of_week == 1 ? day_of_week + 5 : day_of_week - 2;
      week |= (1 << mask_num);
      MainEditFragment.dayRepeat.setWeek(week);
    }
    int week_table_size = week_table.getChildCount();
    for(int i = 0; i < week_table_size; i++) {
      TableRow tableRow = (TableRow)week_table.getChildAt(i);
      int table_row_size = tableRow.getChildCount();
      for(int j = 0; j < table_row_size; j++) {
        ConstraintLayout constraintLayout = (ConstraintLayout)tableRow.getChildAt(j);
        checkBox = (AnimCheckBox)constraintLayout.getChildAt(0);
        if((week & (1 << (i * 5 + j))) != 0) {
          checkBox.setChecked(true, false);
        }
        constraintLayout.setOnClickListener(this);
        checkBox.setOnCheckedChangeListener(this);
      }
    }
  }

  @Override
  public void onChange(AnimCheckBox view, boolean checked) {

    checkBox = view;
    mask_num = Integer.parseInt(checkBox.getTag().toString());

    if(checkBox.isChecked()) {
      week |= (1 << mask_num);
    }
    else {
      week &= ~(1 << mask_num);
      if(week == 0) {
        week |= (1 << mask_num);
        checkBox.setChecked(true);
      }
    }

    MainEditFragment.dayRepeat.setWeek(week);
  }

  @Override
  public void onClick(View v) {

    checkBox = (AnimCheckBox)((ConstraintLayout)v).getChildAt(0);
    checkBox.setChecked(!checkBox.isChecked());
  }
}
