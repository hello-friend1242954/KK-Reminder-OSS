package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static java.util.Objects.requireNonNull;

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
  CheckBoxPreference daysOfMonth;
  Preference daysOfMonthPicker;
  CheckBoxPreference onTheMonth;
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
    daysOfMonth = (CheckBoxPreference)findPreference("days_of_month");
    ((MyCheckBoxPreference)daysOfMonth).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    daysOfMonth.setChecked(MainEditFragment.dayRepeat.isDaysOfMonthSet());
    daysOfMonth.setOnPreferenceClickListener(this);
    daysOfMonthPicker = findPreference("days_of_month_picker");
    onTheMonth = (CheckBoxPreference)findPreference("on_the_month");
    ((MyCheckBoxPreference)onTheMonth).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    onTheMonth.setChecked(!MainEditFragment.dayRepeat.isDaysOfMonthSet());
    onTheMonth.setOnPreferenceClickListener(this);
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
    requireNonNull(view);

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
    requireNonNull(actionBar);

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
      MainEditFragment.dayRepeat.setIsDay(true);
    }

    // Ordinal Numberの初期化
    if(MainEditFragment.dayRepeat.getOrdinalNumber() == 0) {
      MainEditFragment.dayRepeat.setOrdinalNumber(1);
    }

    // On the monthの初期化
    if(MainEditFragment.dayRepeat.getOnTheMonth() == null) {
      MainEditFragment.dayRepeat.setOnTheMonth(Week.MON);
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
          intervalLabel = getString(R.string.every_week);
        }
        else {
          intervalLabel = res.getQuantityString(R.plurals.per_week, interval, interval);
        }

        break;
      }
      case 3: {

        rootPreferenceScreen.addPreference(daysOfMonth);
        rootPreferenceScreen.addPreference(onTheMonth);
        if(MainEditFragment.dayRepeat.isDaysOfMonthSet()) {
          rootPreferenceScreen.addPreference(daysOfMonthPicker);
        }
        else {
          rootPreferenceScreen.addPreference(onTheMonthPicker);
        }

        if(LOCALE.equals(Locale.JAPAN) && interval == 1) {
          intervalLabel = getString(R.string.every_month);
        }
        else {
          intervalLabel = res.getQuantityString(R.plurals.per_month, interval, interval);
        }

        // onTheMonthPickerのラベルを初期化
        String label = "";
        if(!MainEditFragment.dayRepeat.isDaysOfMonthSet()) {
          if(MainEditFragment.dayRepeat.getOrdinalNumber() < 5) {
            if(LOCALE.equals(Locale.JAPAN)) {
              label += "第" + MainEditFragment.dayRepeat.getOrdinalNumber();
            }
            else {
              int ordinalNumber = MainEditFragment.dayRepeat.getOrdinalNumber();
              String ordinalStr = ordinalNumber + "";
              if(ordinalNumber == 1) {
                ordinalStr += "st";
              }
              else if(ordinalNumber == 2) {
                ordinalStr += "nd";
              }
              else if(ordinalNumber == 3) {
                ordinalStr += "rd";
              }
              else {
                ordinalStr += "th";
              }
              label += ordinalStr + " ";
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

          if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() < 7) {
            if(LOCALE.equals(Locale.JAPAN)) {
              label += DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat.getOnTheMonth().ordinal()] +
                "曜日";
            }
            else {
              label += DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat.getOnTheMonth().ordinal()];
            }
          }
          else if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() == 7) {
            if(LOCALE.equals(Locale.JAPAN)) {
              label += "平日";
            }
            else {
              label += "Weekday";
            }
          }
          else if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() == 8) {
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
          intervalLabel = getString(R.string.every_year);
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
    requireNonNull(manager);
    manager.popBackStack();
    return super.onOptionsItemSelected(item);
  }

  private void registerCustomRepeat() {

    boolean matchToTemplate = false;
    int scale = MainEditFragment.dayRepeat.getScale();

    if(MainEditFragment.dayRepeat.getInterval() == 1) {
      switch(scale) {

        case 1: {

          matchToTemplate = true;
          dayRepeatEditFragment.everyday.setChecked(true);
          dayRepeatEditFragment.custom.setChecked(false);
          MainEditFragment.dayRepeat.setWhichSet(1);
          MainEditFragment.dayRepeat.setWhichTemplate(1);
          MainEditFragment.dayRepeat.setLabel(getString(R.string.everyday));
          dayRepeatEditFragment.label.setSummary(R.string.everyday);

          break;
        }
        case 2: {

          int calDayOfWeek = MainEditFragment.finalCal.get(Calendar.DAY_OF_WEEK);
          if(DayRepeatCustomWeekPickerPreference.week == (1 << (calDayOfWeek - 2)) ||
            DayRepeatCustomWeekPickerPreference.week == (1 << (calDayOfWeek + 5))) {
            matchToTemplate = true;
            dayRepeatEditFragment.everyWeek.setChecked(true);
            dayRepeatEditFragment.custom.setChecked(false);
            MainEditFragment.dayRepeat.setWhichSet(1 << 1);
            MainEditFragment.dayRepeat.setWhichTemplate(1 << 2);
            MainEditFragment.dayRepeat.setLabel(DayRepeatEditFragment.labelStrEveryWeek);
          }

          if(DayRepeatCustomWeekPickerPreference.week == Integer.parseInt("11111", 2)) {
            matchToTemplate = true;
            dayRepeatEditFragment.everyWeekday.setChecked(true);
            dayRepeatEditFragment.custom.setChecked(false);
            MainEditFragment.dayRepeat.setWhichSet(1 << 1);
            MainEditFragment.dayRepeat.setWhichTemplate(1 << 1);
            MainEditFragment.dayRepeat.setLabel(getString(R.string.every_weekday));
            dayRepeatEditFragment.label.setSummary(R.string.every_weekday);
          }

          break;
        }
        case 3: {

          if(MainEditFragment.dayRepeat.isDaysOfMonthSet()) {
            int calDayOfMonth = MainEditFragment.finalCal.get(Calendar.DAY_OF_MONTH);
            if(DayRepeatCustomDaysOfMonthPickerPreference.daysOfMonth ==
              (1 << (calDayOfMonth - 1))) {
              matchToTemplate = true;
              dayRepeatEditFragment.everyMonth.setChecked(true);
              dayRepeatEditFragment.custom.setChecked(false);
              MainEditFragment.dayRepeat.setWhichSet(1 << 2);
              MainEditFragment.dayRepeat.setWhichTemplate(1 << 3);
              MainEditFragment.dayRepeat.setLabel(DayRepeatEditFragment.labelStrEveryMonth);
            }
          }

          break;
        }
        case 4: {

          int calMonth = MainEditFragment.finalCal.get(Calendar.MONTH);
          if(DayRepeatCustomYearPickerPreference.year == (1 << calMonth)) {
            matchToTemplate = true;
            dayRepeatEditFragment.everyYear.setChecked(true);
            dayRepeatEditFragment.custom.setChecked(false);
            MainEditFragment.dayRepeat.setWhichSet(1 << 3);
            MainEditFragment.dayRepeat.setWhichTemplate(1 << 4);
            MainEditFragment.dayRepeat.setLabel(DayRepeatEditFragment.labelStrEveryYear);
            MainEditFragment.dayRepeat.setDayOfMonthOfYear(MainEditFragment.finalCal.get(
              Calendar.DAY_OF_MONTH));
          }

          break;
        }
      }
    }

    if(!matchToTemplate) {
      MainEditFragment.dayRepeat.setWhichTemplate(1 << 5);
      if(dayRepeatEditFragment.everyday.isChecked()) {
        dayRepeatEditFragment.everyday.setChecked(false);
      }
      if(dayRepeatEditFragment.everyWeekday.isChecked()) {
        dayRepeatEditFragment.everyWeekday.setChecked(false);
      }
      if(dayRepeatEditFragment.everyWeek.isChecked()) {
        dayRepeatEditFragment.everyWeek.setChecked(false);
      }
      if(dayRepeatEditFragment.everyMonth.isChecked()) {
        dayRepeatEditFragment.everyMonth.setChecked(false);
      }
      if(dayRepeatEditFragment.everyYear.isChecked()) {
        dayRepeatEditFragment.everyYear.setChecked(false);
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

          MainEditFragment.dayRepeat.setWhichSet(1);
          label = res.getQuantityString(R.plurals.per_day, interval, interval);

          break;
        }
        case 2: {

          MainEditFragment.dayRepeat.setWhichSet(1 << 1);
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

          MainEditFragment.dayRepeat.setWhichSet(1 << 2);
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

          if(MainEditFragment.dayRepeat.isDaysOfMonthSet()) {
            stringBuilder = new StringBuilder(tmp);
            int size = MainEditFragment.finalCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int daysOfMonth = MainEditFragment.dayRepeat.getDaysOfMonth();
            for(int i = 0; i < size; i++) {
              if((daysOfMonth & (1 << i)) != 0) {
                stringBuilder.append(i + 1).append(", ");
              }
            }
            if((daysOfMonth & (1 << 30)) != 0) {
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
          else if(!MainEditFragment.dayRepeat.isDaysOfMonthSet()) {
            if(MainEditFragment.dayRepeat.getOrdinalNumber() < 5) {
              if(LOCALE.equals(Locale.JAPAN)) {
                label += "第" + MainEditFragment.dayRepeat.getOrdinalNumber();
              }
              else {
                int ordinalNumber = MainEditFragment.dayRepeat.getOrdinalNumber();
                String ordinalStr = ordinalNumber + "";
                if(ordinalNumber == 1) {
                  ordinalStr += "st";
                }
                else if(ordinalNumber == 2) {
                  ordinalStr += "nd";
                }
                else if(ordinalNumber == 3) {
                  ordinalStr += "rd";
                }
                else {
                  ordinalStr += "th";
                }
                label += ordinalStr + " ";
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

            if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() < 7) {
              if(LOCALE.equals(Locale.JAPAN)) {
                label +=
                  DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat.getOnTheMonth().ordinal()] +
                    "曜日";
              }
              else {
                label +=
                  DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat.getOnTheMonth().ordinal()];
              }
            }
            else if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() == 7) {
              if(LOCALE.equals(Locale.JAPAN)) {
                label += "平日";
              }
              else {
                label += "Weekday";
              }
            }
            else if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() == 8) {
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

          MainEditFragment.dayRepeat.setWhichSet(1 << 3);
          MainEditFragment.dayRepeat.setDayOfMonthOfYear(MainEditFragment.finalCal.get(Calendar.DAY_OF_MONTH));
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
              int dayOfMonth = MainEditFragment.finalCal.get(Calendar.DAY_OF_MONTH);
              label += dayOfMonth + "";
              if(dayOfMonth == 1) {
                label += "st";
              }
              else if(dayOfMonth == 2) {
                label += "nd";
              }
              else if(dayOfMonth == 3) {
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
            label += tmp + "月の" + MainEditFragment.finalCal.get(Calendar.DAY_OF_MONTH) + "日";
          }
          else {
            label += tmp;
          }

          break;
        }
      }

      DayRepeatEditFragment.labelStrCustom = label;
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
        if(daysOfMonth.isChecked()) {
          MainEditFragment.dayRepeat.setIsDaysOfMonthSet(true);
          onTheMonth.setChecked(false);
          rootPreferenceScreen.addPreference(daysOfMonthPicker);
          rootPreferenceScreen.removePreference(onTheMonthPicker);
        }
        else {
          daysOfMonth.setChecked(true);
        }

        break;
      }
      case "on_the_month": {
        if(onTheMonth.isChecked()) {
          MainEditFragment.dayRepeat.setIsDaysOfMonthSet(false);
          daysOfMonth.setChecked(false);
          rootPreferenceScreen.addPreference(onTheMonthPicker);
          rootPreferenceScreen.removePreference(daysOfMonthPicker);

          // onTheMonthPickerのラベルを初期化
          String label = "";
          if(MainEditFragment.dayRepeat.getOrdinalNumber() < 5) {
            if(LOCALE.equals(Locale.JAPAN)) {
              label += "第" + MainEditFragment.dayRepeat.getOrdinalNumber();
            }
            else {
              int ordinalNumber = MainEditFragment.dayRepeat.getOrdinalNumber();
              String ordinalStr = ordinalNumber + "";
              if(ordinalNumber == 1) {
                ordinalStr += "st";
              }
              else if(ordinalNumber == 2) {
                ordinalStr += "nd";
              }
              else if(ordinalNumber == 3) {
                ordinalStr += "rd";
              }
              else {
                ordinalStr += "th";
              }
              label += ordinalStr + " ";
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

          if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() < 7) {
            if(LOCALE.equals(Locale.JAPAN)) {
              label += DAY_OF_WEEK_LIST_JA[MainEditFragment.dayRepeat.getOnTheMonth().ordinal()] +
                "曜日";
            }
            else {
              label += DAY_OF_WEEK_LIST_EN[MainEditFragment.dayRepeat.getOnTheMonth().ordinal()];
            }
          }
          else if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() == 7) {
            if(LOCALE.equals(Locale.JAPAN)) {
              label += "平日";
            }
            else {
              label += "Weekday";
            }
          }
          else if(MainEditFragment.dayRepeat.getOnTheMonth().ordinal() == 8) {
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
          onTheMonth.setChecked(true);
        }

        break;
      }
    }
  }
}