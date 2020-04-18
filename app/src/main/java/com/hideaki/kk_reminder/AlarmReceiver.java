package com.hideaki.kk_reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.hideaki.kk_reminder.StartupReceiver.getDynamicContext;
import static com.hideaki.kk_reminder.StartupReceiver.getIsDirectBootContext;
import static com.hideaki.kk_reminder.UtilClass.BOOLEAN_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.BOOLEAN_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.BOOT_FROM_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.CHANNEL_ID;
import static com.hideaki.kk_reminder.UtilClass.CHILD_NOTIFICATION_ID;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_URI_SOUND;
import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.IS_ID_TABLE_FLOOD;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.MINUTE;
import static com.hideaki.kk_reminder.UtilClass.NOTIFICATION_ID_TABLE;
import static com.hideaki.kk_reminder.UtilClass.PARENT_NOTIFICATION_ID;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_HOUR;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.STRING_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.STRING_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.copySharedPreferences;
import static com.hideaki.kk_reminder.UtilClass.currentTimeMinutes;
import static com.hideaki.kk_reminder.UtilClass.deserialize;
import static com.hideaki.kk_reminder.UtilClass.getVibrationPattern;
import static com.hideaki.kk_reminder.UtilClass.serialize;
import static java.util.Objects.requireNonNull;

public class AlarmReceiver extends BroadcastReceiver {

  private static final int ID_TABLE_FLOOD_THRESHOLD = 1000;
  private static int shiftCount = 0;

  static {
    int threshold = ID_TABLE_FLOOD_THRESHOLD;
    while(threshold > 1) {
      threshold /= 10;
      shiftCount++;
    }
  }

  DBAccessor accessor = null;
  Context context;

  @Override
  public void onReceive(Context context, Intent intent) {

    this.context = context;
    ItemAdapter item = new ItemAdapter(deserialize(intent.getByteArrayExtra(ITEM)));
    requireNonNull(item);

    accessor = new DBAccessor(getDynamicContext(context), getIsDirectBootContext(context));

    int currentNightMode =
      context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

    // 通知の文字色に使うアクセントカラーの取得
    GeneralSettingsAdapter generalSettings = querySettingsDB();
    MyThemeAdapter theme = generalSettings.getTheme();
    int accentColor;
    theme.setIsColorPrimary(false);
    if(theme.getColor() == 0) {
      if(currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
        accentColor = ContextCompat.getColor(context, R.color.red6PrimaryColor);
      }
      else {
        accentColor = ContextCompat.getColor(context, R.color.red13PrimaryDarkColor);
      }
    }
    else {
      accentColor = theme.getColor();
    }
    theme.setIsColorPrimary(true);

    int time = item.getNotifyInterval().getTime();

    // Notification IDの生成
    SharedPreferences stringPreferences = getDynamicContext(context).getSharedPreferences(
      getIsDirectBootContext(context) ? STRING_GENERAL_COPY : STRING_GENERAL,
      MODE_PRIVATE
    );
    SharedPreferences booleanPreferences = getDynamicContext(context).getSharedPreferences(
      getIsDirectBootContext(context) ? BOOLEAN_GENERAL_COPY : BOOLEAN_GENERAL,
      MODE_PRIVATE
    );
    Set<String> idTable =
      stringPreferences.getStringSet(NOTIFICATION_ID_TABLE, new TreeSet<String>());
    requireNonNull(idTable);
    boolean isIdTableFlood = booleanPreferences.getBoolean(IS_ID_TABLE_FLOOD, false);
    int parentId = intent.getIntExtra(PARENT_NOTIFICATION_ID, 0);
    if(parentId == 0) {
      int parentPlusUnit = 1 << 10;
      parentId = 1 << (10 + (isIdTableFlood ? shiftCount : 0));
      int loopCount = 0;
      while(true) {
        loopCount++;
        String binaryId = Integer.toBinaryString(parentId);
        if(!idTable.contains(binaryId)) {
          idTable.add(binaryId);
          stringPreferences
            .edit()
            .putStringSet(NOTIFICATION_ID_TABLE, idTable)
            .apply();

          if(!getIsDirectBootContext(context)) {
            copySharedPreferences(context, false);
          }
          break;
        }
        else if(loopCount >= ID_TABLE_FLOOD_THRESHOLD - 1) {
          isIdTableFlood = !isIdTableFlood;
          parentId = 1 << (10 + (isIdTableFlood ? shiftCount : 0));
          idTable = new TreeSet<>();
          idTable.add(Integer.toBinaryString(parentId));

          stringPreferences
            .edit()
            .putStringSet(NOTIFICATION_ID_TABLE, idTable)
            .apply();

          booleanPreferences
            .edit()
            .putBoolean(IS_ID_TABLE_FLOOD, isIdTableFlood)
            .apply();

          if(!getIsDirectBootContext(context)) {
            copySharedPreferences(context, false);
          }
          break;
        }
        parentId += parentPlusUnit;
      }
    }

    int childId = intent.getIntExtra(CHILD_NOTIFICATION_ID, 0);
    childId++;

    // 通知をタップしたときにアクティビティを起動するインテントの作成
    Intent openActivity = new Intent(context, MainActivity.class);
    openActivity.setAction(BOOT_FROM_NOTIFICATION);
    PendingIntent sender = PendingIntent.getActivity(
      context, (int)System.currentTimeMillis(), openActivity, PendingIntent.FLAG_UPDATE_CURRENT
    );

    // バイブレーションの設定
    String vibrationStr = item.getVibrationPattern();
    long[] vibrationPattern = getVibrationPattern(vibrationStr);

    // 通知音の設定
    String uriString = item.getSoundUri();
    Uri sound = DEFAULT_URI_SOUND;
    if(uriString != null) {
      sound = Uri.parse(uriString);
    }

    // タスクごとに異なるNotificationChannelを利用して通知音を区別する
    NotificationManager manager =
      (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
    requireNonNull(manager);
    // あるIDで一度でも通知チャンネルを作ってしまうと、例えチャンネルを消しても再び作成したチャンネルのIDに
    // 同じものが使われていると通知チャンネルのアップデートができないので、使い捨てかつ重複しないように現在時刻
    // を通知チャンネルのIDとしている
    String channelId = intent.getStringExtra(CHANNEL_ID);
    if(channelId == null) {
      channelId = String.valueOf(item.getId()) + System.currentTimeMillis();
    }
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

      AudioAttributes attributes = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build();

      String channelName = LOCALE.equals(Locale.JAPAN) ? "タスク" : "Task";
      channelName += ": " + item.getDetail();
      NotificationChannel channel = new NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_HIGH
      );

      channel.setShowBadge(true);
      channel.setSound(sound, attributes);
      channel.setLightColor(Color.RED);
      channel.setVibrationPattern(vibrationPattern);
      channel.enableLights(true);
      channel.enableVibration(true);

      manager.createNotificationChannel(channel);
    }

    NotificationCompat.Builder builder = new NotificationCompat
      .Builder(context, channelId)
      .setContentTitle(context.getString(R.string.app_name))
      .setContentText(item.getDetail())
      .setSmallIcon(R.mipmap.ic_launcher_notification)
      .setLargeIcon(BitmapFactory.decodeResource(
        context.getResources(),
        R.mipmap.ic_launcher_notification
      ))
      .setWhen(System.currentTimeMillis())
      .setContentIntent(sender)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setSound(sound)
      .setLights(Color.RED, 2000, 1000)
      .setVibrate(vibrationPattern)
      .setColor(accentColor)
      .setColorized(true);

    // 手動スヌーズ用のIntentの設定

    // 完了
    Intent doneIntent = new Intent(context, DoneReceiver.class);
    doneIntent.putExtra(ITEM, serialize(item.getItem()));
    doneIntent.putExtra(PARENT_NOTIFICATION_ID, parentId);
    doneIntent.putExtra(CHILD_NOTIFICATION_ID, childId);
    doneIntent.putExtra(CHANNEL_ID, channelId);
    PendingIntent doneSender = PendingIntent.getBroadcast(
      context, (int)item.getId(), doneIntent, PendingIntent.FLAG_UPDATE_CURRENT
    );
    builder.addAction(R.mipmap.done, context.getString(R.string.done), doneSender);

    // デフォルトスヌーズ
    SharedPreferences intPreferences = getDynamicContext(context).getSharedPreferences(
      getIsDirectBootContext(context) ? INT_GENERAL_COPY : INT_GENERAL,
      MODE_PRIVATE
    );
    int hour = intPreferences.getInt(SNOOZE_DEFAULT_HOUR, 0);
    int minute = intPreferences.getInt(SNOOZE_DEFAULT_MINUTE, 15);
    String summary = "";
    if(hour != 0) {
      summary += context.getResources().getQuantityString(R.plurals.hour, hour, hour);
      if(!LOCALE.equals(Locale.JAPAN)) {
        summary += " ";
      }
    }
    if(minute != 0) {
      summary += context.getResources().getQuantityString(R.plurals.minute, minute, minute);
      if(!LOCALE.equals(Locale.JAPAN)) {
        summary += " ";
      }
    }
    summary += context.getString(R.string.snooze);

    Intent defaultSnoozeIntent = new Intent(context, DefaultManuallySnoozeReceiver.class);
    defaultSnoozeIntent.putExtra(ITEM, serialize(item.getItem()));
    defaultSnoozeIntent.putExtra(PARENT_NOTIFICATION_ID, parentId);
    defaultSnoozeIntent.putExtra(CHILD_NOTIFICATION_ID, childId);
    defaultSnoozeIntent.putExtra(CHANNEL_ID, channelId);
    PendingIntent defaultSnoozeSender = PendingIntent.getBroadcast(
      context, (int)item.getId(), defaultSnoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT
    );
    builder.addAction(R.mipmap.update, summary, defaultSnoozeSender);

    // 細かいスヌーズ
    Intent advancedSnoozeIntent = new Intent(context, ManuallySnoozeActivity.class);
    advancedSnoozeIntent.putExtra(ITEM, serialize(item.getItem()));
    advancedSnoozeIntent.putExtra(PARENT_NOTIFICATION_ID, parentId);
    advancedSnoozeIntent.putExtra(CHILD_NOTIFICATION_ID, childId);
    advancedSnoozeIntent.putExtra(CHANNEL_ID, channelId);
    PendingIntent snoozeSender = PendingIntent.getActivity(
      context, (int)item.getId(), advancedSnoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT
    );
    builder.addAction(
      R.mipmap.snooze,
      context.getString(R.string.more_advanced_snooze),
      snoozeSender
    );

    // 通知を発行
    manager.notify(parentId + childId, builder.build());

    // ロックされている場合、画面を点ける
    PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    requireNonNull(powerManager);
    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
      PowerManager.FULL_WAKE_LOCK |
        PowerManager.ACQUIRE_CAUSES_WAKEUP |
        PowerManager.ON_AFTER_RELEASE, "kk_reminder:Notification"
    );
    if(!powerManager.isInteractive()) {
      wakeLock.acquire(10000);
    }

    // 再帰通知処理
    if(time != 0) {
      item.getNotifyInterval().setTime(time - 1);
      Intent recursiveAlarm = new Intent(context, AlarmReceiver.class);
      recursiveAlarm.putExtra(ITEM, serialize(item.getItem()));
      recursiveAlarm.putExtra(PARENT_NOTIFICATION_ID, parentId);
      recursiveAlarm.putExtra(CHILD_NOTIFICATION_ID, childId);
      recursiveAlarm.putExtra(CHANNEL_ID, channelId);
      PendingIntent recursiveSender = PendingIntent.getBroadcast(
        context, (int)item.getId(), recursiveAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

      long resetSchedule = currentTimeMinutes()
        + item.getNotifyInterval().getHour() * HOUR
        + item.getNotifyInterval().getMinute() * MINUTE;

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      requireNonNull(alarmManager);

      alarmManager.setAlarmClock(
        new AlarmManager.AlarmClockInfo(resetSchedule, null), recursiveSender);

      updateDB(item, MyDatabaseHelper.TODO_TABLE);
    }
  }

  public void updateDB(ItemAdapter item, String table) {

    accessor.executeUpdate(item.getId(), serialize(item.getItem()), table);
  }

  public GeneralSettingsAdapter querySettingsDB() {

    Object generalSettings = null;
    do {
      try {
        generalSettings = deserialize(
          accessor.executeQueryById(1, MyDatabaseHelper.SETTINGS_TABLE)
        );
      }
      catch(SQLiteCantOpenDatabaseException e) {
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ex) {
          Log.e("querySettingsDB", Log.getStackTraceString(ex));
        }
      }
    }
    while(getIsDirectBootContext(context) && generalSettings == null);

    return new GeneralSettingsAdapter(generalSettings);
  }
}
