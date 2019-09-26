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
  private int mask_num;
  static int year;

  public DayRepeatCustomYearPickerPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
    setLayoutResource(R.layout.repeat_custom_year_picker);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);

    year = MainEditFragment.dayRepeat.getYear();

    TableLayout year_table = (TableLayout)holder.findViewById(R.id.year_table);
    if(year == 0) {
      mask_num = MainEditFragment.final_cal.get(Calendar.MONTH);
      year |= (1 << mask_num);
      MainEditFragment.dayRepeat.setYear(year);
    }
    int year_table_size = year_table.getChildCount();
    for(int i = 0; i < year_table_size; i++) {
      TableRow tableRow = (TableRow)year_table.getChildAt(i);
      int table_row_size = tableRow.getChildCount();
      for(int j = 0; j < table_row_size; j++) {
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
    mask_num = Integer.parseInt(checkBox.getTag().toString());

    if(checkBox.isChecked()) {
      year |= (1 << mask_num);
    }
    else {
      year &= ~(1 << mask_num);
      if(year == 0) {
        year |= (1 << mask_num);
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
