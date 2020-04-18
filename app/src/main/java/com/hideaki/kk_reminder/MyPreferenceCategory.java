package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.preference.PreferenceViewHolder;

import android.util.AttributeSet;
import android.widget.TextView;

import com.takisoft.fix.support.v7.preference.PreferenceCategory;

public class MyPreferenceCategory extends PreferenceCategory {

  private MainActivity activity;

  @SuppressWarnings("unused")
  public MyPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {

    super(context, attrs, defStyleAttr);
    activity = (MainActivity)((ContextWrapper)context).getBaseContext();
  }

  @SuppressWarnings("unused")
  public MyPreferenceCategory(Context context, AttributeSet attrs) {

    super(context, attrs);
    activity = (MainActivity)((ContextWrapper)context).getBaseContext();
  }

  @SuppressWarnings("unused")
  public MyPreferenceCategory(Context context) {

    super(context);
    activity = (MainActivity)((ContextWrapper)context).getBaseContext();
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);
    TextView titleView = (TextView)holder.findViewById(android.R.id.title);
    if(titleView != null) {
      titleView.setTextColor(activity.accentColor);
    }
  }
}