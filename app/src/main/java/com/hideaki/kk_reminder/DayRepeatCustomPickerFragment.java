package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.appcompat.widget.Toolbar;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;

public class DayRepeatCustomPickerFragment extends BasePreferenceFragmentCompat
    implements
    Preference.OnPreferenceClickListener,
    MyCheckBoxPreference.MyCheckBoxPreferenceCheckedChangeListener {

  static final String[] DAY_OF_WEEK_LIST_JA = {"月", "火", "水", "木", "金", "土", "日"};
  static final String[] DAY_OF_WEEK_LIST_EN = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
  private static final String[] MONTH_LIST_EN = {
      "Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.",
      "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."
  };

  PreferenceScreen rootPreferenceScreen;
  PreferenceScreen picker;
  Preference week;
  CheckBoxPreference days_of_month;
  Preference days_of_month_picker;
  CheckBoxPreference on_the_month;
  PreferenceScreen onTheMonthPicker;
  Preference year;
  private MainActivity activity;
  private DayRepeatEditFragment dayRepeatEditFragment;

  void setDayRepeatEditFragment(DayRepeatEditFragment dayRepeatEditFragment) {

    this.dayRepeatEditFragment = dayRepeatEditFragment;
  }

  public static DayRepeatCustomPickerFragment newInstance() {

    return new DayRepeatCustomPickerFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.repeat_custom_item);
    setHasOptionsMenu(true);

    rootPreferenceScreen = getPreferenceScreen();
    picker = (PreferenceScreen)findPreference("picker");
    picker.setOnPreferenceClickListener(this);
    week = findPreference("week");
    days_of_month = (CheckBoxPreference)findPreference("days_of_month");
    ((MyCheckBoxPreference)days_of_month).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    days_of_month.setChecked(MainEditFragment.dayRepeat.isDays_of_month_setted());
    days_of_month.setOnPreferenceClickListener(this);
    days_of_month_picker = findPreference("days_of_month_picker");
    on_the_month = (CheckBoxPreference)findPreference("on_the_month");
    ((MyCheckBoxPreference)on_the_month).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    on_the_month.setChecked(!MainEditFragment.dayRepeat.isDays_of_month_setted());
    on_the_month.setOnPreferenceClickListener(this);
    onTheMonthPicker = (PreferenceScreen)findPreference("on_the_month_picker");
    onTheMonthPicker.setOnPreferenceClickListener(this);
    year = findPreference("year");
    rootPreferenceScreen.removeAll();
    rootPreferenceScreen.addPreference(picker);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {

    View view = super.onCreateView(inflater, container, savedInstanceState);
    checkNotNull(view);

    if(activity.isDarkMode) {
      view.setBackgroundColor(activity.backgroundMaterialDarkColor);
    }
    else {
      view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    }
    view.setFocusableInTouchMode(true);
    view.requestFocus();
    view.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
          registerCustomRepeat();
        }
        return false;
      }
    });

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    checkNotNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.custom);

    int interval = MainEditFragment.dayRepeat.getInterval();
    int scale = MainEditFragment.dayRepeat.getScale();

    // Intervalの初期化
    if(interval == 0) {
      interval = 1;
      MainEditFragment.dayRepeat.setInterval(1);
    }

    // Scaleの初期化
    if(scale == 0) {
      scale = 1;
      MainEditFragment.dayRepeat.setScale(1);
      MainEditFragment.dayRepeat.setDay(true);
    }

    // Ordinal Numberの初期化
    if(MainEditFragment.dayRepeat.getOrdinal_number() == 0) {
      MainEditFragment.dayRepeat.setOrdinal_number(1);
    }

    // On the monthの初期化
    if(MainEditFragment.dayRepeat.getOn_the_month() == null) {
      MainEditFragment.dayRepeat.setOn_the_month(Week.MON);
    }

    // Scaleの表示処理
    String intervalLabel = "";
    Resources res = getResources();
    rootPreferenceScreen.removeAll();
    rootPreferenceScreen.addPreference(picker);
    switch(scale) {

      case 1: {

        if(LOCALE.equals(Locale.JAPAN) && interval == 1) {
          intervalLabel = getString(R.string.everyday);
        }
        else {
          intervalLabel = res.getQuantityString(R.plurals.per_day, interval, interval);
        }

        break;
      }
      case 2: {

        rootPreferenceScreen.addPreference(week);

        if(LOCALE.equals(Locale.JAPAN) && interval == 1) {
          intervalLabel = getString(R.string.everyweek);
        }
        else {
          intervalLabel = res.getQuantityString(R.plurals.per_week, interval, interval);
        }

        break;
      }
      case 3: {

        rootPreferenceScreen.addPreference(days_of_month);
        rootPreferenceScreen.addPreference(on_the_month);
        if(MainEditFragment.dayRepeat.isDays_of_month_setted()) {
          rootPreferenceScreen.addPreference(days_of_month_picker);
        }
        else {
          rootPreferenceScreen.addPreference(onTheMonthPicker);
        }

        if(LOCALE.equals(Locale.JAPAN) && interval == 1) {
          intervalLabel = getString(R.string.everymonth);
        }
        else {
          intervalLabel = res.getQuantityString(R.plurals.per_month, interval, interval);
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
              label += DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat.getOn_the_month().ordinal()] +
                  "曜日";
            }
            else {
              label += DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat.getOn_the_month().ordinal()];
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

          onTheMonthPicker.setTitle(label);
        }

        break;
      }
      case 4: {

        rootPreferenceScreen.addPreference(year);

        if(LOCALE.equals(Locale.JAPAN) && interval == 1) {
          intervalLabel = getString(R.string.everyyear);
        }
        else {
          intervalLabel = res.getQuantityString(R.plurals.per_year, interval, interval);
        }

        break;
      }
    }

    // Pickerのラベルの初期化
    picker.setTitle(intervalLabel);

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {

    registerCustomRepeat();

    FragmentManager manager = getFragmentManager();
    checkNotNull(manager);
    manager.popBackStack();
    return super.onOptionsItemSelected(item);
  }

  private void registerCustomRepeat() {

    boolean match_to_template = false;
    int scale = MainEditFragment.dayRepeat.getScale();

    if(MainEditFragment.dayRepeat.getInterval() == 1) {
      switch(scale) {

        case 1: {

          match_to_template = true;
          dayRepeatEditFragment.everyday.setChecked(true);
          dayRepeatEditFragment.custom.setChecked(false);
          MainEditFragment.dayRepeat.setSetted(1);
          MainEditFragment.dayRepeat.setWhich_template(1);
          MainEditFragment.dayRepeat.setLabel(getString(R.string.everyday));
          dayRepeatEditFragment.label.setSummary(R.string.everyday);

          break;
        }
        case 2: {

          int cal_day_of_week = MainEditFragment.final_cal.get(Calendar.DAY_OF_WEEK);
          if(DayRepeatCustomWeekPickerPreference.week == (1 << (cal_day_of_week - 2)) ||
              DayRepeatCustomWeekPickerPreference.week == (1 << (cal_day_of_week + 5))) {
            match_to_template = true;
            dayRepeatEditFragment.everyweek.setChecked(true);
            dayRepeatEditFragment.custom.setChecked(false);
            MainEditFragment.dayRepeat.setSetted(1 << 1);
            MainEditFragment.dayRepeat.setWhich_template(1 << 2);
            MainEditFragment.dayRepeat.setLabel(DayRepeatEditFragment.label_str_everyweek);
          }

          if(DayRepeatCustomWeekPickerPreference.week == Integer.parseInt("11111", 2)) {
            match_to_template = true;
            dayRepeatEditFragment.everyweekday.setChecked(true);
            dayRepeatEditFragment.custom.setChecked(false);
            MainEditFragment.dayRepeat.setSetted(1 << 1);
            MainEditFragment.dayRepeat.setWhich_template(1 << 1);
            MainEditFragment.dayRepeat.setLabel(getString(R.string.everyweekday));
            dayRepeatEditFragment.label.setSummary(R.string.everyweekday);
          }

          break;
        }
        case 3: {

          if(MainEditFragment.dayRepeat.isDays_of_month_setted()) {
            int cal_day_of_month = MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH);
            if(DayRepeatCustomDaysOfMonthPickerPreference.days_of_month ==
                (1 << (cal_day_of_month - 1))) {
              match_to_template = true;
              dayRepeatEditFragment.everymonth.setChecked(true);
              dayRepeatEditFragment.custom.setChecked(false);
              MainEditFragment.dayRepeat.setSetted(1 << 2);
              MainEditFragment.dayRepeat.setWhich_template(1 << 3);
              MainEditFragment.dayRepeat.setLabel(DayRepeatEditFragment.label_str_everymonth);
            }
          }

          break;
        }
        case 4: {

          int cal_month = MainEditFragment.final_cal.get(Calendar.MONTH);
          if(DayRepeatCustomYearPickerPreference.year == (1 << cal_month)) {
            match_to_template = true;
            dayRepeatEditFragment.everyyear.setChecked(true);
            dayRepeatEditFragment.custom.setChecked(false);
            MainEditFragment.dayRepeat.setSetted(1 << 3);
            MainEditFragment.dayRepeat.setWhich_template(1 << 4);
            MainEditFragment.dayRepeat.setLabel(DayRepeatEditFragment.label_str_everyyear);
            MainEditFragment.dayRepeat.setDay_of_month_of_year(MainEditFragment.final_cal.get(
                Calendar.DAY_OF_MONTH));
          }

          break;
        }
      }
    }

    if(!match_to_template) {
      MainEditFragment.dayRepeat.setWhich_template(1 << 5);
      if(dayRepeatEditFragment.everyday.isChecked()) {
        dayRepeatEditFragment.everyday.setChecked(false);
      }
      if(dayRepeatEditFragment.everyweekday.isChecked()) {
        dayRepeatEditFragment.everyweekday.setChecked(false);
      }
      if(dayRepeatEditFragment.everyweek.isChecked()) {
        dayRepeatEditFragment.everyweek.setChecked(false);
      }
      if(dayRepeatEditFragment.everymonth.isChecked()) {
        dayRepeatEditFragment.everymonth.setChecked(false);
      }
      if(dayRepeatEditFragment.everyyear.isChecked()) {
        dayRepeatEditFragment.everyyear.setChecked(false);
      }
      if(!dayRepeatEditFragment.custom.isChecked()) {
        dayRepeatEditFragment.custom.setChecked(true);
      }

      Resources res = getResources();
      String label = "";
      String tmp = "";
      StringBuilder stringBuilder;
      int interval = MainEditFragment.dayRepeat.getInterval();

      switch(scale) {

        case 1: {

          MainEditFragment.dayRepeat.setSetted(1);
          label = res.getQuantityString(R.plurals.per_day, interval, interval);

          break;
        }
        case 2: {

          MainEditFragment.dayRepeat.setSetted(1 << 1);
          if(interval == 1 && LOCALE.equals(Locale.JAPAN)) {
            label = "毎週";
          }
          else {
            label = res.getQuantityString(R.plurals.per_week, interval, interval);
            if(LOCALE.equals(Locale.JAPAN)) {
              label += "、";
            }
            else {
              label += " on ";
            }
          }

          stringBuilder = new StringBuilder(tmp);
          int week = MainEditFragment.dayRepeat.getWeek();
          for(int i = 0; i < 7; i++) {
            if((week & (1 << i)) != 0) {
              if(LOCALE.equals(Locale.JAPAN)) {
                stringBuilder.append(DAY_OF_WEEK_LIST_JA[i]).append(", ");
              }
              else {
                stringBuilder.append(DAY_OF_WEEK_LIST_EN[i]).append(", ");
              }
            }
          }
          tmp = stringBuilder.substring(0, stringBuilder.length() - 2);
          if(LOCALE.equals(Locale.JAPAN)) {
            label += tmp + "曜日";
          }
          else {
            label += tmp;
          }

          break;
        }
        case 3: {

          MainEditFragment.dayRepeat.setSetted(1 << 2);
          if(interval == 1 && LOCALE.equals(Locale.JAPAN)) {
            label = "毎月";
          }
          else {
            label = res.getQuantityString(R.plurals.per_month, interval, interval);
            if(LOCALE.equals(Locale.JAPAN)) {
              label += "、";
            }
            else {
              label += " on the ";
            }
          }

          if(MainEditFragment.dayRepeat.isDays_of_month_setted()) {
            stringBuilder = new StringBuilder(tmp);
            int size = MainEditFragment.final_cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int days_of_month = MainEditFragment.dayRepeat.getDays_of_month();
            for(int i = 0; i < size; i++) {
              if((days_of_month & (1 << i)) != 0) {
                stringBuilder.append(i + 1).append(", ");
              }
            }
            if((days_of_month & (1 << 30)) != 0) {
              if(LOCALE.equals(Locale.JAPAN)) {
                stringBuilder.append("最終, ");
              }
              else {
                stringBuilder.append("Last, ");
              }
            }
            tmp = stringBuilder.substring(0, stringBuilder.length() - 2);
            if(LOCALE.equals(Locale.JAPAN)) {
              label += tmp + "日";
            }
            else {
              label += tmp;
            }
          }
          else if(!MainEditFragment.dayRepeat.isDays_of_month_setted()) {
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
                label +=
                    DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat.getOn_the_month().ordinal()] +
                        "曜日";
              }
              else {
                label +=
                    DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat.getOn_the_month().ordinal()];
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
          }

          break;
        }
        case 4: {

          MainEditFragment.dayRepeat.setSetted(1 << 3);
          MainEditFragment.dayRepeat.setDay_of_month_of_year(MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH));
          if(interval == 1 && LOCALE.equals(Locale.JAPAN)) {
            label = "毎年";
          }
          else {
            label = res.getQuantityString(R.plurals.per_year, interval, interval);
            if(LOCALE.equals(Locale.JAPAN)) {
              label += "、";
            }
            else {
              label += " on the ";
              int day_of_month = MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH);
              label += day_of_month + "";
              if(day_of_month == 1) {
                label += "st";
              }
              else if(day_of_month == 2) {
                label += "nd";
              }
              else if(day_of_month == 3) {
                label += "rd";
              }
              else {
                label += "th";
              }
              label += " of ";
            }
          }

          stringBuilder = new StringBuilder(tmp);
          int year = MainEditFragment.dayRepeat.getYear();
          for(int i = 0; i < 12; i++) {
            if((year & (1 << i)) != 0) {
              if(LOCALE.equals(Locale.JAPAN)) {
                stringBuilder.append(i + 1).append(", ");
              }
              else {
                stringBuilder.append(MONTH_LIST_EN[i]).append(", ");
              }
            }
          }
          tmp = stringBuilder.substring(0, stringBuilder.length() - 2);

          if(LOCALE.equals(Locale.JAPAN)) {
            label += tmp + "月の" + MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH) + "日";
          }
          else {
            label += tmp;
          }

          break;
        }
      }

      DayRepeatEditFragment.label_str_custom = label;
      MainEditFragment.dayRepeat.setLabel(label);
    }
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {

    switch(preference.getKey()) {
      case "picker": {

        DayRepeatCustomPickerDialogFragment dialog =
            new DayRepeatCustomPickerDialogFragment(this);
        dialog.show(activity.getSupportFragmentManager(), "day_repeat_custom_picker");

        return true;
      }
      case "on_the_month_picker": {

        DayRepeatCustomOnTheMonthPickerDialogFragment dialog =
            new DayRepeatCustomOnTheMonthPickerDialogFragment(this);
        dialog.show(activity.getSupportFragmentManager(), "day_repeat_custom_on_the_month_picker");

        return true;
      }
    }

    return false;
  }

  @Override
  public void onCheckedChange(String key, boolean checked) {

    switch(key) {
      case "days_of_month": {
        if(days_of_month.isChecked()) {
          MainEditFragment.dayRepeat.setDays_of_month_setted(true);
          on_the_month.setChecked(false);
          rootPreferenceScreen.addPreference(days_of_month_picker);
          rootPreferenceScreen.removePreference(onTheMonthPicker);
        }
        else {
          days_of_month.setChecked(true);
        }

        break;
      }
      case "on_the_month": {
        if(on_the_month.isChecked()) {
          MainEditFragment.dayRepeat.setDays_of_month_setted(false);
          days_of_month.setChecked(false);
          rootPreferenceScreen.addPreference(onTheMonthPicker);
          rootPreferenceScreen.removePreference(days_of_month_picker);

          // onTheMonthPickerのラベルを初期化
          String label = "";
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
              label += DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat.getOn_the_month().ordinal()] +
                  "曜日";
            }
            else {
              label += DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat.getOn_the_month().ordinal()];
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

          onTheMonthPicker.setTitle(label);
        }
        else {
          on_the_month.setChecked(true);
        }

        break;
      }
    }
  }
}