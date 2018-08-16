package com.example.hideaki.reminder;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

public class RepeatCustomOnTheMonthPickerPreference extends Preference {

  private final String[] ORDINAL_NUMBER_LIST = {"第一", "第二", "第三", "第四", "最終"};
  private static final String[] DAY_OF_WEEK_LIST = {"月曜日", "火曜日", "水曜日", "木曜日", "金曜日",
      "土曜日", "日曜日", "平日", "週末"};

  private NumberPicker ordinal_number;
  private NumberPicker day_of_week;

  public RepeatCustomOnTheMonthPickerPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
  }

  @Override
  protected View onCreateView(ViewGroup parent) {

    super.onCreateView(parent);
    View view = View.inflate(getContext(), R.layout.repeat_custom_on_the_month_picker, null);
    return view;
  }

  @Override
  protected void onBindView(View view) {

    super.onBindView(view);

    //ordinal_numberの実装
    ordinal_number = view.findViewById(R.id.ordinal_number);
    ordinal_number.setDisplayedValues(null);
    ordinal_number.setMaxValue(ORDINAL_NUMBER_LIST.length);
    ordinal_number.setMinValue(1);
    if(MainEditFragment.repeat.getOrdinal_number() == 0) {
      MainEditFragment.repeat.setOrdinal_number(1);
    }
    ordinal_number.setValue(MainEditFragment.repeat.getOrdinal_number());
    ordinal_number.setDisplayedValues(ORDINAL_NUMBER_LIST);
    ordinal_number.setOnScrollListener(new NumberPicker.OnScrollListener() {
      @Override
      public void onScrollStateChange(NumberPicker view, int scrollState) {
        switch(scrollState) {
          case SCROLL_STATE_IDLE:
            MainEditFragment.repeat.setOrdinal_number(ordinal_number.getValue());
        }
      }
    });

    //day_on_weekの実装
    day_of_week = view.findViewById(R.id.day_on_week);
    day_of_week.setDisplayedValues(null);
    day_of_week.setMaxValue(DAY_OF_WEEK_LIST.length - 1);
    day_of_week.setMinValue(0);
    if(MainEditFragment.repeat.getOn_the_month() == null) {
      MainEditFragment.repeat.setOn_the_month(Week.MON);
    }
    day_of_week.setValue(MainEditFragment.repeat.getOn_the_month().ordinal());
    day_of_week.setDisplayedValues(DAY_OF_WEEK_LIST);
    day_of_week.setOnScrollListener(new NumberPicker.OnScrollListener() {
      @Override
      public void onScrollStateChange(NumberPicker view, int scrollState) {
        switch(scrollState) {
          case SCROLL_STATE_IDLE:
            MainEditFragment.repeat.setOn_the_month(Week.values()[day_of_week.getValue()]);
        }
      }
    });
  }
}
