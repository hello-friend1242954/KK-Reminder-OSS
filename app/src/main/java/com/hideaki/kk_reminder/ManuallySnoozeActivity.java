package com.hideaki.kk_reminder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.StartupReceiver.getDynamicContext;
import static com.hideaki.kk_reminder.StartupReceiver.getIsDirectBootContext;
import static com.hideaki.kk_reminder.UtilClass.ACTION_IN_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.BOOT_FROM_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.CHILD_NOTIFICATION_ID;
import static com.hideaki.kk_reminder.UtilClass.CREATED;
import static com.hideaki.kk_reminder.UtilClass.DESTROYED;
import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.MINUTE;
import static com.hideaki.kk_reminder.UtilClass.NOTIFICATION_ID_TABLE;
import static com.hideaki.kk_reminder.UtilClass.PARENT_NOTIFICATION_ID;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_HOUR;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.STRING_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.STRING_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.copyDatabase;
import static com.hideaki.kk_reminder.UtilClass.copySharedPreferences;
import static com.hideaki.kk_reminder.UtilClass.currentTimeMinutes;
import static com.hideaki.kk_reminder.UtilClass.deserialize;
import static com.hideaki.kk_reminder.UtilClass.serialize;

public class ManuallySnoozeActivity extends AppCompatActivity implements View.OnClickListener {

  private DBAccessor accessor = null;
  TextView title;
  ListView listView;
  View footer;
  private Item item;
  private static final List<String> hour_list = new ArrayList<>();
  private static final List<String> minute_list = new ArrayList<>();
  private NumberPicker hour_picker;
  private NumberPicker minute_picker;
  int snooze_default_hour;
  int snooze_default_minute;
  int custom_hour;
  int custom_minute;
  String summary;

  static {

    if(LOCALE.equals(Locale.JAPAN)) {
      for(int i = 0; i < 24; i++) {
        hour_list.add(i + "時間");
      }

      for(int i = 0; i < 60; i++) {
        minute_list.add(i + "分");
      }
    }
    else {
      for(int i = 0; i < 24; i++) {
        if(i == 0 || i == 1) {
          hour_list.add(i + " hour");
        }
        else {
          hour_list.add(i + " hours");
        }
      }

      for(int i = 0; i < 60; i++) {
        if(i == 0 || i == 1) {
          minute_list.add(i + " minute");
        }
        else {
          minute_list.add(i + " minutes");
        }
      }
    }
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.manually_snooze_layout);

    // AlarmReceiverからItemとNotificationIDを受け取る
    Intent intent = getIntent();
    item = (Item)deserialize(intent.getByteArrayExtra(ITEM));
    checkNotNull(item);

    // SharedPreferencesからデフォルトのスヌーズ時間を取得
    SharedPreferences intPreferences =
        getDynamicContext(this).getSharedPreferences(
            getIsDirectBootContext(this) ? INT_GENERAL_COPY : INT_GENERAL,
            MODE_PRIVATE
        );
    snooze_default_hour = intPreferences.getInt(SNOOZE_DEFAULT_HOUR, 0);
    snooze_default_minute = intPreferences.getInt(SNOOZE_DEFAULT_MINUTE, 15);

    // 通知を既読する
    SharedPreferences stringPreferences =
        getDynamicContext(this).getSharedPreferences(
            getIsDirectBootContext(this) ? STRING_GENERAL_COPY : STRING_GENERAL,
            MODE_PRIVATE
        );
    Set<String> id_table =
        stringPreferences.getStringSet(NOTIFICATION_ID_TABLE, new TreeSet<String>());
    int parent_id = intent.getIntExtra(PARENT_NOTIFICATION_ID, 0);
    int child_id = intent.getIntExtra(CHILD_NOTIFICATION_ID, 0);
    NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    checkNotNull(manager);

    for(int i = 1; i <= child_id; i++) {
      manager.cancel(parent_id + i);
    }
    checkNotNull(id_table);
    id_table.remove(Integer.toBinaryString(parent_id));
    stringPreferences
        .edit()
        .putStringSet(NOTIFICATION_ID_TABLE, id_table)
        .apply();

    if(!getIsDirectBootContext(this)) {
      copySharedPreferences(this, false);
    }

    // ListViewの設定
    ManuallySnoozeListAdapter manuallySnoozeListAdapter
        = new ManuallySnoozeListAdapter(this);
    listView = findViewById(R.id.listView);
    listView.setAdapter(manuallySnoozeListAdapter);

    // customレイアウトとして表示するfooterの設定
    footer = View.inflate(this, R.layout.manually_snooze_custom_layout, null);
    custom_hour = snooze_default_hour;
    custom_minute = snooze_default_minute;
    initPicker();

    // listView以外のレイアウトの設定
    ImageView backArrow = findViewById(R.id.back_arrow);
    title = findViewById(R.id.title);
    ImageView launchActivity = findViewById(R.id.launch_activity);
    ImageView done = findViewById(R.id.done);

    // タイトルの設定
    summary = "";
    if(custom_hour != 0) {
      summary += getResources().getQuantityString(R.plurals.hour, custom_hour, custom_hour);
      if(!LOCALE.equals(Locale.JAPAN)) {
        summary += " ";
      }
    }
    if(custom_minute != 0) {
      summary += getResources().getQuantityString(R.plurals.minute, custom_minute, custom_minute);
      if(!LOCALE.equals(Locale.JAPAN)) {
        summary += " ";
      }
    }
    summary += getString(R.string.snooze);
    title.setText(summary);

    backArrow.setOnClickListener(this);
    launchActivity.setOnClickListener(this);
    done.setOnClickListener(this);
  }

  public void setAlarm(Item item) {

    if(
        item.getDate().getTimeInMillis() > System.currentTimeMillis() &&
            item.getWhich_list_belongs() == 0
    ) {
      item.getNotify_interval().setTime(item.getNotify_interval().getOrg_time());
      Intent intent = new Intent(this, AlarmReceiver.class);
      byte[] ob_array = serialize(item);
      intent.putExtra(ITEM, ob_array);
      PendingIntent sender = PendingIntent.getBroadcast(
          this, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
      checkNotNull(alarmManager);

      alarmManager.setAlarmClock(
          new AlarmManager.AlarmClockInfo(
              item.getDate().getTimeInMillis(),
              null
          ),
          sender
      );
    }
  }

  public void deleteAlarm(Item item) {

    if(isAlarmSetted(item)) {
      Intent intent = new Intent(this, AlarmReceiver.class);
      PendingIntent sender = PendingIntent.getBroadcast(
          this, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
      checkNotNull(alarmManager);

      alarmManager.cancel(sender);
      sender.cancel();
    }
  }

  public boolean isAlarmSetted(Item item) {

    Intent intent = new Intent(this, AlarmReceiver.class);
    PendingIntent sender = PendingIntent.getBroadcast(
        this, (int)item.getId(), intent, PendingIntent.FLAG_NO_CREATE);

    return sender != null;
  }


  public void updateDB(Item item, String table) {

    accessor.executeUpdate(item.getId(), serialize(item), table);
  }

  @Override
  public void onClick(View v) {

    switch(v.getId()) {

      case R.id.back_arrow: {

        this.finish();
        break;
      }
      case R.id.launch_activity: {

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(BOOT_FROM_NOTIFICATION);
        startActivity(intent);
        break;
      }
      case R.id.done: {

        // チェックされた項目に応じた時間スヌーズする
        int checked_position = ManuallySnoozeListAdapter.checked_position;

        long default_snooze = snooze_default_hour * HOUR + snooze_default_minute * MINUTE;
        long custom_snooze = custom_hour * HOUR + custom_minute * MINUTE;
        long[] how_long = {
            default_snooze,
            15 * MINUTE,
            30 * MINUTE,
            HOUR,
            3 * HOUR,
            10 * HOUR,
            24 * HOUR,
            custom_snooze
        };

        if(item.getTime_altered() == 0) {
          item.setOrg_date((Calendar)item.getDate().clone());
        }
        item.getDate().setTimeInMillis(currentTimeMinutes() + how_long[checked_position]);
        item.addTime_altered(how_long[checked_position]);

        // 更新
        deleteAlarm(item);
        setAlarm(item);
        accessor =
            new DBAccessor(getDynamicContext(this), getIsDirectBootContext(this));
        updateDB(item, MyDatabaseHelper.TODO_TABLE);

        // データベースを端末暗号化ストレージへコピーする
        if(!getIsDirectBootContext(this)) {
          copyDatabase(this, false);
        }

        SharedPreferences preferences =
            getDynamicContext(this).getSharedPreferences(
                getIsDirectBootContext(this) ? INT_GENERAL_COPY : INT_GENERAL,
                MODE_PRIVATE
            );
        int created = preferences.getInt(CREATED, -1);
        int destroyed = preferences.getInt(DESTROYED, -1);
        if(created > destroyed) {
          sendBroadcast(new Intent(ACTION_IN_NOTIFICATION));
        }

        this.finish();
        break;
      }
    }
  }

  private void initPicker() {

    // hour_pickerの実装
    hour_picker = footer.findViewById(R.id.hour);
    hour_picker.setDisplayedValues(null);
    hour_picker.setMaxValue(23);
    hour_picker.setMinValue(0);
    hour_picker.setValue(custom_hour);
    hour_picker.setDisplayedValues(hour_list.toArray(new String[0]));
    hour_picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
      @Override
      public void onValueChange(NumberPicker picker, int oldVal, final int newVal) {

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {

            if(newVal == hour_picker.getValue()) {

              if(hour_picker.getValue() == 0 && minute_picker.getValue() == 0) {
                custom_hour = 24;
              }
              else {
                custom_hour = hour_picker.getValue();
              }

              summary = "";
              if(custom_hour != 0) {
                summary += getResources().getQuantityString(
                    R.plurals.hour,
                    custom_hour,
                    custom_hour
                );
                if(!LOCALE.equals(Locale.JAPAN)) {
                  summary += " ";
                }
              }
              if(custom_minute != 0) {
                summary += getResources().getQuantityString(
                    R.plurals.minute,
                    custom_minute,
                    custom_minute
                );
                if(!LOCALE.equals(Locale.JAPAN)) {
                  summary += " ";
                }
              }
              summary += getString(R.string.snooze);
              title.setText(summary);
            }
          }
        }, 100);
      }
    });

    // minute_pickerの実装
    minute_picker = footer.findViewById(R.id.minute);
    minute_picker.setDisplayedValues(null);
    minute_picker.setMaxValue(59);
    minute_picker.setMinValue(0);
    minute_picker.setValue(custom_minute);
    minute_picker.setDisplayedValues(minute_list.toArray(new String[0]));
    minute_picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
      @Override
      public void onValueChange(NumberPicker picker, int oldVal, final int newVal) {

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {

            if(newVal == minute_picker.getValue()) {

              if(hour_picker.getValue() == 0 && minute_picker.getValue() == 0) {
                custom_hour = 24;
              }
              else {
                custom_hour = hour_picker.getValue();
              }
              custom_minute = minute_picker.getValue();

              summary = "";
              if(custom_hour != 0) {
                summary += getResources().getQuantityString(
                    R.plurals.hour,
                    custom_hour,
                    custom_hour
                );
                if(!LOCALE.equals(Locale.JAPAN)) {
                  summary += " ";
                }
              }
              if(custom_minute != 0) {
                summary += getResources().getQuantityString(
                    R.plurals.minute,
                    custom_minute,
                    custom_minute
                );
                if(!LOCALE.equals(Locale.JAPAN)) {
                  summary += " ";
                }
              }
              summary += getString(R.string.snooze);
              title.setText(summary);
            }
          }
        }, 100);
      }
    });
  }
}