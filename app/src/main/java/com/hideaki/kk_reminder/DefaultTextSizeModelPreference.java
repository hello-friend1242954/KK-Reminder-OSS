package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;

public class DefaultTextSizeModelPreference extends Preference {

  private MainActivity activity;

  public DefaultTextSizeModelPreference(Context context, AttributeSet attrs) {

    super(context, attrs);
    setLayoutResource(R.layout.child_layout);
    activity = (MainActivity)((ContextWrapper)context).getBaseContext();
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {

    super.onBindViewHolder(holder);
    CardView childCard = (CardView)holder.findViewById(R.id.child_card);
    ImageView clockImage = (ImageView)holder.findViewById(R.id.clock_image);
    TextView date = (TextView)holder.findViewById(R.id.date);
    TextView detail = (TextView)holder.findViewById(R.id.detail);
    TextView repeat = (TextView)holder.findViewById(R.id.repeat);
    ImageView tag = (ImageView)holder.findViewById(R.id.tag_pallet);

    Calendar now = Calendar.getInstance();
    now.add(Calendar.MINUTE, 1);
    String setTime;
    if(LOCALE.equals(Locale.JAPAN)) {
      setTime = (String)DateFormat.format("M月d日(E) k:mm", now);
    }
    else {
      setTime = (String)DateFormat.format("M/d (E) k:mm", now);
    }

    setTime += " (";
    if(!LOCALE.equals(Locale.JAPAN)) {
      setTime += " ";
    }
    setTime += activity.getString(R.string.within_one_minute);
    if(!LOCALE.equals(Locale.JAPAN)) {
      setTime += " ";
    }
    setTime += ")";

    if(activity.isDarkMode) {
      childCard.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      clockImage.setColorFilter(ContextCompat.getColor(
        activity,
        R.color.green5PrimaryColor
      ));
      TextView[] textViews = {
        date, detail, repeat
      };
      for(TextView textView : textViews) {
        textView.setTextColor(activity.secondaryTextMaterialDarkColor);
      }
    }
    date.setText(setTime);
    detail.setText(R.string.default_detail);
    detail.setTextSize(activity.textSize);
    repeat.setText(R.string.non_repeat);
    tag.setVisibility(View.GONE);
  }
}
