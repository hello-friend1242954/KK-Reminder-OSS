package com.hideaki.kk_reminder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.constraintlayout.widget.ConstraintLayout;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;

public class ManuallySnoozeListAdapter extends BaseAdapter {

  private final List<String> snoozeList;
  private ManuallySnoozeActivity activity;
  private static boolean isManuallyChecked;
  static int checkedPosition;
  private String defaultSnoozeTime;
  private static boolean isFirst;

  ManuallySnoozeListAdapter(ManuallySnoozeActivity activity) {

    this.activity = activity;

    snoozeList =
      new ArrayList<>(Arrays.asList(activity.getResources().getStringArray(R.array.snooze_list)));
    defaultSnoozeTime = "";
    int defaultHour = activity.snoozeDefaultHour;
    if(defaultHour != 0) {
      defaultSnoozeTime +=
        activity.getResources().getQuantityString(R.plurals.hour, defaultHour, defaultHour);
      if(!LOCALE.equals(Locale.JAPAN)) {
        defaultSnoozeTime += " ";
      }
    }
    int defaultMinute = activity.snoozeDefaultMinute;
    if(defaultMinute != 0) {
      defaultSnoozeTime += activity
        .getResources()
        .getQuantityString(R.plurals.minute, defaultMinute, defaultMinute);
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

    isManuallyChecked = false;
    checkedPosition = 0;
    isFirst = true;
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

      if(checked && isManuallyChecked) {
        isFirst = false;
        checkedPosition = position;
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
            if(activity.customHour != 0) {
              title += activity
                .getResources()
                .getQuantityString(R.plurals.hour, activity.customHour, activity.customHour);
              if(!LOCALE.equals(Locale.JAPAN)) {
                title += " ";
              }
            }
            if(activity.customMinute != 0) {
              title += activity
                .getResources()
                .getQuantityString(
                  R.plurals.minute,
                  activity.customMinute,
                  activity.customMinute
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
      else if(position == checkedPosition && isManuallyChecked) {
        isFirst = false;
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
    // ダークモードでないときも背景が濃い色で白色が最も見やすかったため、
    // ダークモード時のテキストカラーと同じ色を設定した。
    viewHolder.howLong.setTextColor(activity.primaryTextMaterialDarkColor);
    viewHolder.howLong.setText(snoozeItem);

    // チェック状態の初期化
    if(position != checkedPosition) {
      isManuallyChecked = false;
      if(isFirst) {
        viewHolder.checkBox.setChecked(false, false);
      }
      else {
        viewHolder.checkBox.setChecked(false);
      }
    }
    else {
      isManuallyChecked = false;
      if(isFirst) {
        viewHolder.checkBox.setChecked(true, false);
      }
      else {
        viewHolder.checkBox.setChecked(true);
      }
    }
    isManuallyChecked = true;

    return convertView;
  }
}
