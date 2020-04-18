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

public class DayRepeatCustomYearPickerPreference extends Preference
  implements AnimCheckBox.OnCheckedChangeListener, View.OnClickListener {

  private AnimCheckBox checkBox;
  private int maskNum;
  static int year;

  public DayRepeatCustomYearPickerPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
    setLayoutResource(R.layout.repeat_custom_year_picker);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);

    year = MainEditFragment.dayRepeat.getYear();

    TableLayout yearTable = (TableLayout)holder.findViewById(R.id.year_table);
    if(year == 0) {
      maskNum = MainEditFragment.finalCal.get(Calendar.MONTH);
      year |= (1 << maskNum);
      MainEditFragment.dayRepeat.setYear(year);
    }
    int yearTableSize = yearTable.getChildCount();
    for(int i = 0; i < yearTableSize; i++) {
      TableRow tableRow = (TableRow)yearTable.getChildAt(i);
      int tableRowSize = tableRow.getChildCount();
      for(int j = 0; j < tableRowSize; j++) {
        ConstraintLayout constraintLayout = (ConstraintLayout)tableRow.getChildAt(j);
        checkBox = (AnimCheckBox)constraintLayout.getChildAt(0);
        if((year & (1 << (i * 4 + j))) != 0) {
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
      year |= (1 << maskNum);
    }
    else {
      year &= ~(1 << maskNum);
      if(year == 0) {
        year |= (1 << maskNum);
        checkBox.setChecked(true);
      }
    }

    MainEditFragment.dayRepeat.setYear(year);
  }

  @Override
  public void onClick(View v) {

    checkBox = (AnimCheckBox)((ConstraintLayout)v).getChildAt(0);
    checkBox.setChecked(!checkBox.isChecked());
  }
}
