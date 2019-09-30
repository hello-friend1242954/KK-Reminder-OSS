package com.hideaki.kk_reminder;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;

public class AboutThisAppFragment extends Fragment {

  static final String TAG = AboutThisAppFragment.class.getSimpleName();
  private MainActivity activity;
  private WebView webView;

  public static AboutThisAppFragment newInstance() {

    return new AboutThisAppFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    this.setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      Bundle savedInstanceState
  ) {

    View view = inflater.inflate(R.layout.about_this_app_layout, container, false);
    if(activity.isDarkMode) {
      view.setBackgroundColor(activity.backgroundMaterialDarkColor);
    }
    else {
      view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    }

    webView = view.findViewById(R.id.webView);
    if(LOCALE.equals(Locale.JAPAN)) {
      if(activity.isDarkMode) {
        webView.loadUrl("file:///android_asset/about_this_app_ja_dark.html");
      }
      else {
        webView.loadUrl("file:///android_asset/about_this_app_ja.html");
      }
    }
    else {
      if(activity.isDarkMode) {
        webView.loadUrl("file:///android_asset/about_this_app_en_dark.html");
      }
      else {
        webView.loadUrl("file:///android_asset/about_this_app_en.html");
      }
    }

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    checkNotNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.about_this_app);

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {

    FragmentManager manager = getFragmentManager();
    checkNotNull(manager);
    manager.popBackStack();
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onDestroyView() {

    super.onDestroyView();
    webView.stopLoading();
    webView.setWebChromeClient(null);
    webView.setWebViewClient(null);
    webView.destroy();
    webView = null;
  }
}