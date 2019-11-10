package com.hideaki.kk_reminder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.StartupReceiver.getDynamicContext;
import static com.hideaki.kk_reminder.StartupReceiver.getIsDirectBootContext;
import static com.hideaki.kk_reminder.UtilClass.ACTION_IN_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.BOOLEAN_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.BOOLEAN_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.BOOT_FROM_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.CHANNEL_ID;
import static com.hideaki.kk_reminder.UtilClass.CHILD_NOTIFICATION_ID;
import static com.hideaki.kk_reminder.UtilClass.CREATED;
import static com.hideaki.kk_reminder.UtilClass.DESTROYED;
import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.IS_DARK_THEME_FOLLOW_SYSTEM;
import static com.hideaki.kk_reminder.UtilClass.IS_DARK_MODE;
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
  int snooze_default_hour;
  int snooze_default_minute;
  int custom_hour;
  int custom_minute;
  String summary;
  boolean isDarkMode;
  TextView time;
  int primaryTextMaterialDarkColor;
  int secondaryTextMaterialDarkColor;
  private boolean isDarkThemeFollowSystem;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    // AlarmReceiverからItemとNotificationIDを受け取る
    Intent intent = getIntent();
    item = (Item)deserialize(intent.getByteArrayExtra(ITEM));
    checkNotNull(item);

    // SharedPreferencesからダークモードかどうかを取得
    SharedPreferences booleanPreferences =
      getDynamicContext(this).getSharedPreferences(
        getIsDirectBootContext(this) ? BOOLEAN_GENERAL_COPY : BOOLEAN_GENERAL,
        MODE_PRIVATE
      );
    isDarkMode = booleanPreferences.getBoolean(IS_DARK_MODE, false);
    isDarkThemeFollowSystem = booleanPreferences.getBoolean(IS_DARK_THEME_FOLLOW_SYSTEM, true);

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
    String channelId = intent.getStringExtra(CHANNEL_ID);
    NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    checkNotNull(manager);

    for(int i = 1; i <= child_id; i++) {
      manager.cancel(parent_id + i);
    }
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.deleteNotificationChannel(channelId);
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

    // ダークモードの設定
    int currentNightMode =
      getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    if(!isDarkThemeFollowSystem) {
      if(isDarkMode && currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
      }
      else if(!isDarkMode && currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
      }
    }

    // 色の設定
    int primaryDarkMaterialDarkColor =
      ContextCompat.getColor(this, R.color.primaryDarkMaterialDark);
    primaryTextMaterialDarkColor =
      ContextCompat.getColor(this, R.color.primaryTextMaterialDark);
    secondaryTextMaterialDarkColor =
      ContextCompat.getColor(this, R.color.secondaryTextMaterialDark);

    setContentView(R.layout.manually_snooze_layout);

    // ListViewの設定
    ManuallySnoozeListAdapter manuallySnoozeListAdapter
      = new ManuallySnoozeListAdapter(this);
    listView = findViewById(R.id.listView);
    listView.setAdapter(manuallySnoozeListAdapter);

    // customレイアウトとして表示するfooterの設定
    footer = View.inflate(this, R.layout.manually_snooze_custom_layout, null);
    custom_hour = snooze_default_hour;
    custom_minute = snooze_default_minute;
    time = footer.findViewById(R.id.time);
    TextView description = footer.findViewById(R.id.description);
    // ダークモードでないときも背景が濃い色で白色が最も見やすかったため、
    // ダークモード時のテキストカラーと同じ色を設定した。
    time.setTextColor(primaryTextMaterialDarkColor);
    description.setTextColor(primaryTextMaterialDarkColor);
    time.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        ManuallySnoozePickerDiaglogFragment dialog = new ManuallySnoozePickerDiaglogFragment();
        dialog.show(getSupportFragmentManager(), "manually_snooze_picker");
      }
    });

    // listView以外のレイアウトの設定
    ImageView backArrow = findViewById(R.id.back_arrow);
    title = findViewById(R.id.title);
    ImageView launchActivity = findViewById(R.id.launch_activity);
    ImageView done = findViewById(R.id.done);
    if(isDarkMode) {
      backArrow.setColorFilter(secondaryTextMaterialDarkColor);
      launchActivity.setColorFilter(secondaryTextMaterialDarkColor);
      done.setColorFilter(secondaryTextMaterialDarkColor);
      // ステータスバーの色
      Window window = getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(primaryDarkMaterialDarkColor);
    }

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
    // ダークモードでないときも背景が濃い色で白色が最も見やすかったため、
    // ダークモード時のテキストカラーと同じ色を設定した。
    title.setTextColor(primaryTextMaterialDarkColor);
    title.setText(summary);
    time.setText(summary);

    backArrow.setOnClickListener(this);
    launchActivity.setOnClickListener(this);
    done.setOnClickListener(this);
  }

  @Override
  protected void onResume() {

    super.onResume();

    int currentNightMode =
      getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    if(isDarkThemeFollowSystem) {
      if(isDarkMode && currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
        setIsDarkModeInSharedPreferences(false);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        recreate();
      }
      else if(!isDarkMode && currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
        setIsDarkModeInSharedPreferences(true);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        recreate();
      }
    }
  }

  public void setIsDarkModeInSharedPreferences(boolean value) {

    isDarkMode = value;

    getDynamicContext(this).getSharedPreferences(
      getIsDirectBootContext(this) ? BOOLEAN_GENERAL_COPY : BOOLEAN_GENERAL,
      Context.MODE_PRIVATE
    )
      .edit()
      .putBoolean(IS_DARK_MODE, value)
      .apply();
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
}