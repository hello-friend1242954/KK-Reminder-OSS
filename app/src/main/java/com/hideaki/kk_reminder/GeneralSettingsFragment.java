package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.takisoft.fix.support.v7.preference.PreferenceCategory;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.transition.Fade;
import androidx.transition.Transition;

import static com.hideaki.kk_reminder.UtilClass.IS_DARK_MODE;
import static com.hideaki.kk_reminder.UtilClass.IS_DARK_THEME_FOLLOW_SYSTEM;
import static com.hideaki.kk_reminder.UtilClass.PLAY_SLIDE_ANIMATION;
import static java.util.Objects.requireNonNull;

public class GeneralSettingsFragment extends BasePreferenceFragmentCompat
  implements Preference.OnPreferenceClickListener,
  MyCheckBoxPreference.MyCheckBoxPreferenceCheckedChangeListener {

  static final String TAG = GeneralSettingsFragment.class.getSimpleName();

  private MainActivity activity;
  private CheckBoxPreference animation;
  private CheckBoxPreference darkTheme;
  private CheckBoxPreference darkThemeFollowSystem;

  public static GeneralSettingsFragment newInstance() {

    return new GeneralSettingsFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
    if(activity.drawerLayout != null) {
      activity.drawerLayout.closeDrawer(GravityCompat.START);
    }
    else {
      FragmentManager manager = getFragmentManager();
      requireNonNull(manager);
      manager
        .beginTransaction()
        .remove(this)
        .commit();
    }
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.general_settings_edit);

    PreferenceScreen defaultControlTime = (PreferenceScreen)findPreference("control_time");
    PreferenceScreen defaultTextSize = (PreferenceScreen)findPreference("text_size");
    PreferenceScreen defaultNewTask = (PreferenceScreen)findPreference("new_task");
    PreferenceScreen manuallySnooze = (PreferenceScreen)findPreference("manually_snooze");
    animation = (CheckBoxPreference)findPreference("animation");
    PreferenceCategory adsCategory = (PreferenceCategory)findPreference("ads_category");
    PreferenceScreen disableAds = (PreferenceScreen)findPreference("disable_ads");
    PreferenceScreen primaryColor = (PreferenceScreen)findPreference("primary_color");
    PreferenceScreen secondaryColor = (PreferenceScreen)findPreference("secondary_color");
    darkTheme = (CheckBoxPreference)findPreference("dark_theme");
    darkThemeFollowSystem = (CheckBoxPreference)findPreference("dark_theme_follow_system");
    PreferenceScreen backup = (PreferenceScreen)findPreference("backup");
    PreferenceScreen about = (PreferenceScreen)findPreference("this_app");

    defaultControlTime.setOnPreferenceClickListener(this);
    defaultTextSize.setOnPreferenceClickListener(this);
    defaultNewTask.setOnPreferenceClickListener(this);
    manuallySnooze.setOnPreferenceClickListener(this);
    ((MyCheckBoxPreference)animation).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    disableAds.setOnPreferenceClickListener(this);
    primaryColor.setOnPreferenceClickListener(this);
    secondaryColor.setOnPreferenceClickListener(this);
    ((MyCheckBoxPreference)darkTheme).setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    ((MyCheckBoxPreference)darkThemeFollowSystem)
      .setOnMyCheckBoxPreferenceCheckedChangeListener(this);
    backup.setOnPreferenceClickListener(this);
    about.setOnPreferenceClickListener(this);

    if(activity.isPremium) {
      getPreferenceScreen().removePreference(adsCategory);
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

    super.onViewCreated(view, savedInstanceState);

    // 設定項目間の区切り線の非表示
    setDivider(new ColorDrawable(Color.TRANSPARENT));
    setDividerHeight(0);
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

    activity.drawerToggle.setDrawerIndicatorEnabled(true);
    actionBar.setTitle(R.string.settings);

    // チェック状態の初期化
    if(activity.isPlaySlideAnimation) {
      animation.setChecked(true);
    }
    else {
      animation.setChecked(false);
    }

    if(activity.isDarkMode) {
      darkTheme.setChecked(true);
    }
    else {
      darkTheme.setChecked(false);
    }

    if(activity.isDarkThemeFollowSystem) {
      darkThemeFollowSystem.setChecked(true);
    }
    else {
      darkThemeFollowSystem.setChecked(false);
    }

    return view;
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {

    switch(preference.getKey()) {

      case "control_time": {
        transitionFragment(DefaultControlTimeEditFragment.newInstance());
        return true;
      }
      case "text_size": {
        transitionFragment(DefaultTextSizeEditFragment.newInstance());
        return true;
      }
      case "new_task": {
        activity.showMainEditFragment(activity.generalSettings.getItem());
        return true;
      }
      case "manually_snooze": {
        transitionFragment(DefaultManuallySnoozeFragment.newInstance());
        return true;
      }
      case "disable_ads": {
        activity.promotionDialog.show();
        return true;
      }
      case "primary_color": {
        ColorPickerListAdapter.isGeneralSettings = true;
        activity.showColorPickerListViewFragment();
        return true;
      }
      case "secondary_color": {
        ColorPickerListAdapter.isGeneralSettings = true;
        activity.generalSettings.getTheme().setIsColorPrimary(false);
        activity.showColorPickerListViewFragment();
        return true;
      }
      case "backup": {
        BackupAndRestoreFragment backupAndRestoreFragment = BackupAndRestoreFragment.newInstance();
        transitionFragment(backupAndRestoreFragment);
        return true;
      }
      case "this_app": {
        activity.showAboutThisAppFragment();
        return true;
      }

    }
    return false;
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

    switch(key) {
      case "animation": {
        animation.setChecked(checked);
        if(activity.isPlaySlideAnimation != checked) {
          activity.setBooleanGeneralInSharedPreferences(
            PLAY_SLIDE_ANIMATION, checked
          );
        }
        break;
      }
      case "dark_theme": {
        darkTheme.setChecked(checked);
        if(activity.isDarkMode != checked) {
          activity.setBooleanGeneralInSharedPreferences(IS_DARK_MODE, checked);
        }
        initDarkMode();
        break;
      }
      case "dark_theme_follow_system": {
        darkThemeFollowSystem.setChecked(checked);
        if(activity.isDarkThemeFollowSystem != checked) {
          activity.setBooleanGeneralInSharedPreferences(
            IS_DARK_THEME_FOLLOW_SYSTEM, checked
          );
        }
        initDarkMode();
        break;
      }
      default: {
        throw new IllegalStateException("Such a key not exist!: " + key);
      }
    }
  }

  private void initDarkMode() {

    int currentNightMode =
      getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    if(!activity.isDarkThemeFollowSystem) {
      if(activity.isDarkMode && currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
      }
      else if(!activity.isDarkMode && currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
      }
    }
    else {
      if(activity.isDarkMode && currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
        activity.setBooleanGeneralInSharedPreferences(IS_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        activity.recreate();
      }
      else if(!activity.isDarkMode && currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
        activity.setBooleanGeneralInSharedPreferences(IS_DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        activity.recreate();
      }
      else {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        activity.recreate();
      }
    }
  }
}