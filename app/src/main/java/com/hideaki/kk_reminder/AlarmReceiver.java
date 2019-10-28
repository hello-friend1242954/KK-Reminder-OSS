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

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.google.common.base.Preconditions.checkNotNull;
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
import static com.hideaki.kk_reminder.UtilClass.serialize;

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
    Item item = (Item)deserialize(intent.getByteArrayExtra(ITEM));
    checkNotNull(item);

    accessor = new DBAccessor(getDynamicContext(context), getIsDirectBootContext(context));

    int currentNightMode =
        context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

    // 通知の文字色に使うアクセントカラーの取得
    GeneralSettings generalSettings = querySettingsDB();
    MyTheme theme = generalSettings.getTheme();
    int accent_color;
    theme.setColor_primary(false);
    if(theme.getColor() == 0) {
      if(currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
        accent_color = ContextCompat.getColor(context, R.color.red6PrimaryColor);
      }
      else {
        accent_color = ContextCompat.getColor(context, R.color.red13PrimaryDarkColor);
      }
    }
    else {
      accent_color = theme.getColor();
    }
    theme.setColor_primary(true);

    int time = item.getNotify_interval().getTime();

    // Notification IDの生成
    SharedPreferences stringPreferences = getDynamicContext(context).getSharedPreferences(
        getIsDirectBootContext(context) ? STRING_GENERAL_COPY : STRING_GENERAL,
        MODE_PRIVATE
    );
    SharedPreferences booleanPreferences = getDynamicContext(context).getSharedPreferences(
        getIsDirectBootContext(context) ? BOOLEAN_GENERAL_COPY : BOOLEAN_GENERAL,
        MODE_PRIVATE
    );
    Set<String> id_table =
        stringPreferences.getStringSet(NOTIFICATION_ID_TABLE, new TreeSet<String>());
    checkNotNull(id_table);
    boolean isIdTableFlood = booleanPreferences.getBoolean(IS_ID_TABLE_FLOOD, false);
    int parent_id = intent.getIntExtra(PARENT_NOTIFICATION_ID, 0);
    if(parent_id == 0) {
      int parent_plus_unit = 1 << 10;
      parent_id = 1 << (10 + (isIdTableFlood ? shiftCount : 0));
      int loopCount = 0;
      while(true) {
        loopCount++;
        String binaryId = Integer.toBinaryString(parent_id);
        if(!id_table.contains(binaryId)) {
          id_table.add(binaryId);
          stringPreferences
              .edit()
              .putStringSet(NOTIFICATION_ID_TABLE, id_table)
              .apply();

          if(!getIsDirectBootContext(context)) {
            copySharedPreferences(context, false);
          }
          break;
        }
        else if(loopCount >= ID_TABLE_FLOOD_THRESHOLD - 1) {
          isIdTableFlood = !isIdTableFlood;
          parent_id = 1 << (10 + (isIdTableFlood ? shiftCount : 0));
          id_table = new TreeSet<>();
          id_table.add(Integer.toBinaryString(parent_id));

          stringPreferences
              .edit()
              .putStringSet(NOTIFICATION_ID_TABLE, id_table)
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
        parent_id += parent_plus_unit;
      }
    }

    int child_id = intent.getIntExtra(CHILD_NOTIFICATION_ID, 0);
    child_id++;

    // 通知をタップしたときにアクティビティを起動するインテントの作成
    Intent open_activity = new Intent(context, MainActivity.class);
    open_activity.setAction(BOOT_FROM_NOTIFICATION);
    PendingIntent sender = PendingIntent.getActivity(
        context, (int)System.currentTimeMillis(), open_activity, PendingIntent.FLAG_UPDATE_CURRENT
    );

    // 通知音の設定
    String uriString = item.getSoundUri();
    Uri sound = DEFAULT_URI_SOUND;
    if(uriString != null) {
      sound = Uri.parse(uriString);
    }

    // タスクごとに異なるNotificationChannelを利用して通知音を区別する
    NotificationManager manager =
        (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
    checkNotNull(manager);
    // あるIDで一度でも通知チャンネルを作ってしまうと、例えチャンネルを消しても再び作成したチャンネルのIDに
    // 同じものが使われていると通知チャンネルのアップデートができないので、使い捨てかつ重複しないように現在時刻
    // を通知チャンネルのIDとしている
    String channelId = intent.getStringExtra(CHANNEL_ID);
    if(channelId == null) {
      channelId = String.valueOf(System.currentTimeMillis());
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
        .setVibrate(new long[]{0, 500})
        .setColor(accent_color)
        .setColorized(true);

    // 手動スヌーズ用のIntentの設定

    // 完了
    Intent doneIntent = new Intent(context, DoneReceiver.class);
    doneIntent.putExtra(ITEM, serialize(item));
    doneIntent.putExtra(PARENT_NOTIFICATION_ID, parent_id);
    doneIntent.putExtra(CHILD_NOTIFICATION_ID, child_id);
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
    defaultSnoozeIntent.putExtra(ITEM, serialize(item));
    defaultSnoozeIntent.putExtra(PARENT_NOTIFICATION_ID, parent_id);
    defaultSnoozeIntent.putExtra(CHILD_NOTIFICATION_ID, child_id);
    defaultSnoozeIntent.putExtra(CHANNEL_ID, channelId);
    PendingIntent defaultSnoozeSender = PendingIntent.getBroadcast(
        context, (int)item.getId(), defaultSnoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT
    );
    builder.addAction(R.mipmap.update, summary, defaultSnoozeSender);

    // 細かいスヌーズ
    Intent advancedSnoozeIntent = new Intent(context, ManuallySnoozeActivity.class);
    advancedSnoozeIntent.putExtra(ITEM, serialize(item));
    advancedSnoozeIntent.putExtra(PARENT_NOTIFICATION_ID, parent_id);
    advancedSnoozeIntent.putExtra(CHILD_NOTIFICATION_ID, child_id);
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
    manager.notify(parent_id + child_id, builder.build());

    // ロックされている場合、画面を点ける
    PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    checkNotNull(powerManager);
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
      item.getNotify_interval().setTime(time - 1);
      Intent recursive_alarm = new Intent(context, AlarmReceiver.class);
      recursive_alarm.putExtra(ITEM, serialize(item));
      recursive_alarm.putExtra(PARENT_NOTIFICATION_ID, parent_id);
      recursive_alarm.putExtra(CHILD_NOTIFICATION_ID, child_id);
      recursive_alarm.putExtra(CHANNEL_ID, channelId);
      PendingIntent recursive_sender = PendingIntent.getBroadcast(
          context, (int)item.getId(), recursive_alarm, PendingIntent.FLAG_UPDATE_CURRENT);

      long reset_schedule = currentTimeMinutes()
          + item.getNotify_interval().getHour() * HOUR
          + item.getNotify_interval().getMinute() * MINUTE;

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      checkNotNull(alarmManager);

      alarmManager.setAlarmClock(
          new AlarmManager.AlarmClockInfo(reset_schedule, null), recursive_sender);

      updateDB(item, MyDatabaseHelper.TODO_TABLE);
    }
  }

  public void updateDB(Item item, String table) {

    accessor.executeUpdate(item.getId(), serialize(item), table);
  }

  public GeneralSettings querySettingsDB() {

    GeneralSettings generalSettings = null;
    do {
      try {
        generalSettings = (GeneralSettings)deserialize(
            accessor.executeQueryById(1, MyDatabaseHelper.SETTINGS_TABLE)
        );
      }
      catch(SQLiteCantOpenDatabaseException e) {
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ex) {
          ex.printStackTrace();
        }
      }
    }
    while(getIsDirectBootContext(context) && generalSettings == null);

    return generalSettings;
  }
}
