package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBar;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static com.google.common.base.Preconditions.checkNotNull;

public class HelpAndFeedbackFragment extends BasePreferenceFragmentCompat
    implements Preference.OnPreferenceClickListener {

  static final String TAG = HelpAndFeedbackFragment.class.getSimpleName();
  private MainActivity activity;

  public static HelpAndFeedbackFragment newInstance() {

    return new HelpAndFeedbackFragment();
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
      checkNotNull(manager);
      manager
          .beginTransaction()
          .remove(this)
          .commit();
    }
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.help_and_feedback);

    PreferenceScreen contact = (PreferenceScreen)findPreference("contact");
    PreferenceScreen feedback = (PreferenceScreen)findPreference("feedback");
    PreferenceScreen request = (PreferenceScreen)findPreference("request");

    contact.setOnPreferenceClickListener(this);
    feedback.setOnPreferenceClickListener(this);
    request.setOnPreferenceClickListener(this);
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
    checkNotNull(view);

    if(activity.isDarkMode) {
      view.setBackgroundColor(activity.backgroundMaterialDarkColor);
    }
    else {
      view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    }

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    checkNotNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(true);
    actionBar.setTitle(R.string.nav_help_and_feedback);

    return view;
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {

    switch(preference.getKey()) {
      case "contact":
      case "request": {

        Intent intent = new Intent()
            .setAction(Intent.ACTION_SENDTO)
            .setType("text/plain")
            .setData(Uri.parse("mailto:bisigness100@gmail.com"));
        if(preference.getKey().equals("contact")) {
          intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.contact));
        }
        else {
          intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.request));
        }
        startActivity(intent);
        return true;
      }
      case "feedback": {

        Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
        googlePlayIntent.setData(Uri.parse("market://details?id=com.hideaki.kk_reminder"));
        startActivity(googlePlayIntent);
        return true;
      }
    }
    return false;
  }
}