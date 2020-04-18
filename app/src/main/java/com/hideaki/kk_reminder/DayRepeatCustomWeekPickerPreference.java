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
  private int maskNum;
  static int week;

  public DayRepeatCustomWeekPickerPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
    setLayoutResource(R.layout.repeat_custom_week_picker);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);

    week = MainEditFragment.dayRepeat.getWeek();

    TableLayout weekTable = (TableLayout)holder.findViewById(R.id.week_table);
    if(week == 0) {
      int dayOfWeek = MainEditFragment.finalCal.get(Calendar.DAY_OF_WEEK);
      maskNum = dayOfWeek == 1 ? dayOfWeek + 5 : dayOfWeek - 2;
      week |= (1 << maskNum);
      MainEditFragment.dayRepeat.setWeek(week);
    }
    int weekTableSize = weekTable.getChildCount();
    for(int i = 0; i < weekTableSize; i++) {
      TableRow tableRow = (TableRow)weekTable.getChildAt(i);
      int tableRowSize = tableRow.getChildCount();
      for(int j = 0; j < tableRowSize; j++) {
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
    maskNum = Integer.parseInt(checkBox.getTag().toString());

    if(checkBox.isChecked()) {
      week |= (1 << maskNum);
    }
    else {
      week &= ~(1 << maskNum);
      if(week == 0) {
        week |= (1 << maskNum);
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
