package com.hideaki.kk_reminder;

import androidx.constraintlayout.widget.ConstraintLayout;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;

public class ManuallySnoozeListAdapter extends BaseAdapter {

  private final List<String> snoozeList;
  private ManuallySnoozeActivity activity;
  private static boolean manually_checked;
  static int checked_position;
  private String defaultSnoozeTime;
  private static boolean is_first;

  ManuallySnoozeListAdapter(ManuallySnoozeActivity activity) {

    this.activity = activity;

    snoozeList =
        new ArrayList<>(Arrays.asList(activity.getResources().getStringArray(R.array.snooze_list)));
    defaultSnoozeTime = "";
    int default_hour = activity.snooze_default_hour;
    if(default_hour != 0) {
      defaultSnoozeTime +=
          activity.getResources().getQuantityString(R.plurals.hour, default_hour, default_hour);
      if(!LOCALE.equals(Locale.JAPAN)) {
        defaultSnoozeTime += " ";
      }
    }
    int default_minute = activity.snooze_default_minute;
    if(default_minute != 0) {
      defaultSnoozeTime += activity
          .getResources()
          .getQuantityString(R.plurals.minute, default_minute, default_minute);
      if(!LOCALE.equals(Locale.JAPAN)) {
        defaultSnoozeTime += " ";
      }
    }
    if(LOCALE.equals(Locale.JAPAN)) {
      snoozeList.add(
          0,
          activity.getString(R.string.default_snooze) + " (" + defaultSnoozeTime + ")"
      );
    }
    else {
      snoozeList.add(
          0,
          activity.getString(R.string.default_snooze) + " (" + defaultSnoozeTime + ")"
      );
    }

    manually_checked = false;
    checked_position = 0;
    is_first = true;
  }

  private static class ViewHolder {

    ConstraintLayout snoozeItem;
    AnimCheckBox checkBox;
    TextView howLong;
  }

  private class MyOnClickListener
      implements View.OnClickListener, AnimCheckBox.OnCheckedChangeListener {

    private int position;
    private ViewHolder viewHolder;

    MyOnClickListener(int position, ViewHolder viewHolder) {

      this.position = position;
      this.viewHolder = viewHolder;
    }

    @Override
    public void onClick(View v) {

      if(!viewHolder.checkBox.isChecked()) {
        viewHolder.checkBox.setChecked(true);
      }
      else {
        viewHolder.checkBox.setChecked(false);
      }
    }

    @Override
    public void onChange(AnimCheckBox view, boolean checked) {

      if(checked && manually_checked) {
        is_first = false;
        checked_position = position;
        if(position == snoozeList.size() - 1 && activity.listView.getFooterViewsCount() == 0) {
          activity.listView.addFooterView(activity.footer);
        }
        else if(activity.listView.getFooterViewsCount() != 0) {
          activity.listView.removeFooterView(activity.footer);
        }

        // 選んだスヌーズ時間に応じてタイトルを変える
        String snooze = activity.getString(R.string.snooze);
        String title;
        if(position == 0) {
          title = defaultSnoozeTime + snooze;
          activity.title.setText(title);
        }
        else if(position == snoozeList.size() - 1) {
          title = activity.summary;
          if(title == null || title.equals("")) {

            title = "";
            if(activity.custom_hour != 0) {
              title += activity
                  .getResources()
                  .getQuantityString(R.plurals.hour, activity.custom_hour, activity.custom_hour);
              if(!LOCALE.equals(Locale.JAPAN)) {
                title += " ";
              }
            }
            if(activity.custom_minute != 0) {
              title += activity
                  .getResources()
                  .getQuantityString(R.plurals.minute,
                      activity.custom_minute,
                      activity.custom_minute
                  );
              if(!LOCALE.equals(Locale.JAPAN)) {
                title += " ";
              }
            }
            title += snooze;
          }
          activity.title.setText(title);
        }
        else {
          title = getItem(position).toString();
          if(!LOCALE.equals(Locale.JAPAN)) {
            title += " ";
          }
          title += snooze;
          activity.title.setText(title);
        }

        notifyDataSetChanged();
      }
      else if(position == checked_position && manually_checked) {
        is_first = false;
        notifyDataSetChanged();
      }
    }
  }

  @Override
  public int getCount() {

    return snoozeList.size();
  }

  @Override
  public Object getItem(int position) {

    return snoozeList.get(position);
  }

  @Override
  public long getItemId(int position) {

    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    final ViewHolder viewHolder;

    if(convertView == null || convertView.getTag() == null) {
      convertView = View.inflate(parent.getContext(), R.layout.manually_snooze_list_layout, null);

      viewHolder = new ViewHolder();
      viewHolder.snoozeItem = convertView.findViewById(R.id.snooze_item);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.howLong = convertView.findViewById(R.id.how_long);

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)convertView.getTag();
    }

    // 現在のビュー位置でのcolor_nameの取得とリスナーの初期化
    String snoozeItem = (String)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(position, viewHolder);

    // 各リスナーの設定
    viewHolder.snoozeItem.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    // 各種表示処理
    viewHolder.howLong.setText(snoozeItem);

    // チェック状態の初期化
    if(position != checked_position) {
      manually_checked = false;
      if(is_first) {
        viewHolder.checkBox.setChecked(false, false);
      }
      else {
        viewHolder.checkBox.setChecked(false);
      }
    }
    else {
      manually_checked = false;
      if(is_first) {
        viewHolder.checkBox.setChecked(true, false);
      }
      else {
        viewHolder.checkBox.setChecked(true);
      }
    }
    manually_checked = true;

    return convertView;
  }
}
