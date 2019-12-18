package com.hideaki.kk_reminder;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceScreen;
import androidx.transition.Fade;
import androidx.transition.Transition;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static java.util.Objects.requireNonNull;

public class DayRepeatEditFragment extends BasePreferenceFragmentCompat
  implements MyCheckBoxPreference.MyCheckBoxPreferenceCheckedChangeListener {

  private static final String[] MONTH_LIST_EN = {
    "Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.", "Jul.",
    "Aug.", "Sep.", "Oct.", "Nov.", "Dec."
  };
  private CheckBoxPreference never;
  CheckBoxPreference everyday;
  CheckBoxPreference everyweekday;
  CheckBoxPreference everyweek;
  CheckBoxPreference everymonth;
  CheckBoxPreference everyyear;
  CheckBoxPreference custom;
  PreferenceScreen label;
  static String label_str_everyweek;
  static String label_str_everymonth;
  static String label_str_everyyear;
  static String label_str_custom;
  private int mask_num;
  private MainActivity activity;

  public static DayRepeatEditFragment newInstance() {

    return new DayRepeatEditFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.repeat_edit);
    setHasOptionsMenu(true);

    never = (CheckBoxPreference)findPreference("never");
    everyday = (CheckBoxPreference)findPreference("everyday");
    everyweekday = (CheckBoxPreference)findPreference("everyweekday");
    everyweek = (CheckBoxPreference)findPreference("everyweek");
    everymonth = (CheckBoxPreference)findPreference("everymonth");
    everyyear = (CheckBoxPreference)findPreference("everyyear");
    custom = (CheckBoxPreference)findPreference("custom");
    label = (PreferenceScreen)findPreference("label");

    ((MyCheckBoxPreference)never).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyday).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyweekday).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyweek).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everymonth).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)everyyear).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)custom).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
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

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    requireNonNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.repeat_day_unit);

    label_str_everyweek = getString(R.string.everyweek);
    if(LOCALE.equals(Locale.JAPAN)) {
      label_str_everyweek += DateFormat.format("E曜日", MainEditFragment.final_cal);
    }
    else {
      label_str_everyweek += DateFormat.format(" on E", MainEditFragment.final_cal);
    }
    everyweek.setTitle(label_str_everyweek);

    label_str_everymonth = getString(R.string.everymonth);
    if(LOCALE.equals(Locale.JAPAN)) {
      label_str_everymonth += MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH) + "日";
    }
    else {
      int day_of_month = MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH);
      label_str_everymonth += " on the " + day_of_month;
      if(day_of_month == 1) {
        label_str_everymonth += "st";
      }
      else if(day_of_month == 2) {
        label_str_everymonth += "nd";
      }
      else if(day_of_month == 3) {
        label_str_everymonth += "rd";
      }
      else {
        label_str_everymonth += "th";
      }
    }
    everymonth.setTitle(label_str_everymonth);

    label_str_everyyear = getString(R.string.everyyear);
    if(LOCALE.equals(Locale.JAPAN)) {
      label_str_everyyear += DateFormat.format("M月d日", MainEditFragment.final_cal);
    }
    else {
      int day_of_month = MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH);
      label_str_everyyear += " on the " + day_of_month;
      if(day_of_month == 1) {
        label_str_everyyear += "st";
      }
      else if(day_of_month == 2) {
        label_str_everyyear += "nd";
      }
      else if(day_of_month == 3) {
        label_str_everyyear += "rd";
      }
      else {
        label_str_everyyear += "th";
      }

      label_str_everyyear += " of " + MONTH_LIST_EN[MainEditFragment.final_cal.get(Calendar.MONTH)];
    }

    everyyear.setTitle(label_str_everyyear);

    // チェック状態の初期化
    never.setChecked(false);
    everyday.setChecked(false);
    everyweekday.setChecked(false);
    everyweek.setChecked(false);
    everymonth.setChecked(false);
    everyyear.setChecked(false);
    custom.setChecked(false);
    switch(MainEditFragment.dayRepeat.getWhich_template()) {
      case 0: {
        never.setChecked(true);
        break;
      }
      case 1: {
        everyday.setChecked(true);
        break;
      }
      case 1 << 1: {
        everyweekday.setChecked(true);
        break;
      }
      case 1 << 2: {
        everyweek.setChecked(true);

        MainEditFragment.dayRepeat.setLabel(label_str_everyweek);
        mask_num = MainEditFragment.final_cal.get(Calendar.DAY_OF_WEEK);
        mask_num = mask_num < 2 ? mask_num + 5 : mask_num - 2;
        MainEditFragment.dayRepeat.setWeek(1 << mask_num);
        break;
      }
      case 1 << 3: {
        everymonth.setChecked(true);

        MainEditFragment.dayRepeat.setLabel(label_str_everymonth);
        mask_num = MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH);
        MainEditFragment.dayRepeat.setDays_of_month(1 << (mask_num - 1));
        break;
      }
      case 1 << 4: {
        everyyear.setChecked(true);

        MainEditFragment.dayRepeat.setLabel(label_str_everyyear);
        mask_num = MainEditFragment.final_cal.get(Calendar.MONTH);
        MainEditFragment.dayRepeat.setYear(1 << mask_num);
        MainEditFragment.dayRepeat.setDay_of_month_of_year(MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH));
        break;
      }
      case 1 << 5: {
        custom.setChecked(true);
        break;
      }
    }

    // ラベルの初期化
    String label_str = MainEditFragment.dayRepeat.getLabel();
    if(label_str == null || label_str.equals(getString(R.string.none))) {
      label.setSummary(R.string.non_repeat);
    }
    else {
      label.setSummary(MainEditFragment.dayRepeat.getLabel());
    }

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {

    FragmentManager manager = getFragmentManager();
    requireNonNull(manager);
    manager.popBackStack();
    return super.onOptionsItemSelected(item);
  }

  private void transitionFragment(PreferenceFragmentCompat next) {

    Transition transition = new Fade()
      .setDuration(300);
    this.setExitTransition(transition);
    next.setEnterTransition(transition);
    FragmentManager manager = getFragmentManager();
    requireNonNull(manager);
    manager
      .beginTransaction()
      .remove(this)
      .add(R.id.content, next)
      .addToBackStack(null)
      .commit();
  }

  @Override
  public void onCheckedChange(String key, boolean checked) {

    never.setChecked(false);
    everyday.setChecked(false);
    everyweekday.setChecked(false);
    everyweek.setChecked(false);
    everymonth.setChecked(false);
    everyyear.setChecked(false);
    custom.setChecked(false);

    switch(key) {
      case "never": {
        never.setChecked(true);
        label.setSummary(R.string.non_repeat);

        MainEditFragment.dayRepeat.setLabel(getString(R.string.none));
        MainEditFragment.dayRepeat.setSetted(0);
        MainEditFragment.dayRepeat.setWhich_template(0);

        break;
      }
      case "everyday": {
        everyday.setChecked(true);
        label.setSummary(R.string.everyday);

        MainEditFragment.dayRepeat.setLabel(getString(R.string.everyday));
        MainEditFragment.dayRepeat.setSetted(1);
        MainEditFragment.dayRepeat.setWhich_template(1);
        MainEditFragment.dayRepeat.setInterval(1);
        MainEditFragment.dayRepeat.setDay(true);

        break;
      }
      case "everyweekday": {
        everyweekday.setChecked(true);
        label.setSummary(R.string.everyweekday);

        MainEditFragment.dayRepeat.setLabel(getString(R.string.everyweekday));
        MainEditFragment.dayRepeat.setSetted(1 << 1);
        MainEditFragment.dayRepeat.setWhich_template(1 << 1);
        MainEditFragment.dayRepeat.setInterval(1);
        MainEditFragment.dayRepeat.setWeek(Integer.parseInt("11111", 2));

        break;
      }
      case "everyweek": {
        everyweek.setChecked(true);
        label.setSummary(label_str_everyweek);

        MainEditFragment.dayRepeat.setLabel(label_str_everyweek);
        MainEditFragment.dayRepeat.setSetted(1 << 1);
        MainEditFragment.dayRepeat.setWhich_template(1 << 2);
        MainEditFragment.dayRepeat.setInterval(1);
        mask_num = MainEditFragment.final_cal.get(Calendar.DAY_OF_WEEK);
        mask_num = mask_num < 2 ? mask_num + 5 : mask_num - 2;
        MainEditFragment.dayRepeat.setWeek(1 << mask_num);

        break;
      }
      case "everymonth": {
        everymonth.setChecked(true);
        label.setSummary(label_str_everymonth);

        MainEditFragment.dayRepeat.setLabel(label_str_everymonth);
        MainEditFragment.dayRepeat.setSetted(1 << 2);
        MainEditFragment.dayRepeat.setWhich_template(1 << 3);
        MainEditFragment.dayRepeat.setInterval(1);
        mask_num = MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH);
        MainEditFragment.dayRepeat.setDays_of_month(1 << (mask_num - 1));
        if(!MainEditFragment.dayRepeat.isDays_of_month_setted()) {
          MainEditFragment.dayRepeat.setDays_of_month_setted(true);
        }

        break;
      }
      case "everyyear": {
        everyyear.setChecked(true);
        label.setSummary(label_str_everyyear);

        MainEditFragment.dayRepeat.setLabel(label_str_everyyear);
        MainEditFragment.dayRepeat.setSetted(1 << 3);
        MainEditFragment.dayRepeat.setWhich_template(1 << 4);
        MainEditFragment.dayRepeat.setInterval(1);
        mask_num = MainEditFragment.final_cal.get(Calendar.MONTH);
        MainEditFragment.dayRepeat.setYear(1 << mask_num);
        MainEditFragment.dayRepeat.setDay_of_month_of_year(MainEditFragment.final_cal.get(Calendar.DAY_OF_MONTH));

        break;
      }
      case "custom": {
        custom.setChecked(true);
        DayRepeatCustomPickerFragment fragment = DayRepeatCustomPickerFragment.newInstance();
        fragment.setDayRepeatEditFragment(this);
        transitionFragment(fragment);

        break;
      }
    }
  }
}
