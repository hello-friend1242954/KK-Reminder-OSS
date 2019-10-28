package com.hideaki.kk_reminder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.StartupReceiver.getDynamicContext;
import static com.hideaki.kk_reminder.StartupReceiver.getIsDirectBootContext;
import static com.hideaki.kk_reminder.UtilClass.ACTION_IN_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.CHANNEL_ID;
import static com.hideaki.kk_reminder.UtilClass.CHILD_NOTIFICATION_ID;
import static com.hideaki.kk_reminder.UtilClass.CREATED;
import static com.hideaki.kk_reminder.UtilClass.DESTROYED;
import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
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

public class DefaultManuallySnoozeReceiver extends BroadcastReceiver {

  private DBAccessor accessor = null;
  private Context context;

  @Override
  public void onReceive(Context context, Intent intent) {

    this.context = context;
    Item item = (Item)deserialize(intent.getByteArrayExtra(ITEM));
    checkNotNull(item);

    accessor = new DBAccessor(getDynamicContext(context), getIsDirectBootContext(context));

    // 通知を既読する
    SharedPreferences stringPreferences = getDynamicContext(context).getSharedPreferences(
        getIsDirectBootContext(context) ? STRING_GENERAL_COPY : STRING_GENERAL,
        MODE_PRIVATE
    );
    Set<String> id_table =
        stringPreferences.getStringSet(NOTIFICATION_ID_TABLE, new TreeSet<String>());
    checkNotNull(id_table);
    int parent_id = intent.getIntExtra(PARENT_NOTIFICATION_ID, 0);
    int child_id = intent.getIntExtra(CHILD_NOTIFICATION_ID, 0);
    String channelId = intent.getStringExtra(CHANNEL_ID);
    NotificationManager manager =
        (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
    checkNotNull(manager);

    for(int i = 1; i <= child_id; i++) {
      manager.cancel(parent_id + i);
    }
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.deleteNotificationChannel(channelId);
    }
    id_table.remove(Integer.toBinaryString(parent_id));
    stringPreferences
        .edit()
        .putStringSet(NOTIFICATION_ID_TABLE, id_table)
        .apply();

    if(!getIsDirectBootContext(context)) {
      copySharedPreferences(context, false);
    }

    SharedPreferences intPreferences = getDynamicContext(context).getSharedPreferences(
        getIsDirectBootContext(context) ? INT_GENERAL_COPY : INT_GENERAL,
        MODE_PRIVATE
    );
    int snooze_default_hour = intPreferences.getInt(SNOOZE_DEFAULT_HOUR, 0);
    int snooze_default_minute = intPreferences.getInt(SNOOZE_DEFAULT_MINUTE, 15);
    long default_snooze = snooze_default_hour * HOUR + snooze_default_minute * MINUTE;

    if(item.getTime_altered() == 0) {
      item.setOrg_date((Calendar)item.getDate().clone());
    }
    item.getDate().setTimeInMillis(currentTimeMinutes() + default_snooze);
    item.addTime_altered(default_snooze);

    // 更新
    deleteAlarm(item);
    setAlarm(item);
    updateDB(item, MyDatabaseHelper.TODO_TABLE);

    // データベースを端末暗号化ストレージへコピーする
    if(!getIsDirectBootContext(context)) {
      copyDatabase(context, false);
    }

    int created = intPreferences.getInt(CREATED, -1);
    int destroyed = intPreferences.getInt(DESTROYED, -1);
    if(created > destroyed) {
      context.sendBroadcast(new Intent(ACTION_IN_NOTIFICATION));
    }
  }

  public void setAlarm(Item item) {

    if(item.getDate().getTimeInMillis() > System.currentTimeMillis() &&
        item.getWhich_list_belongs() == 0) {
      item.getNotify_interval().setTime(item.getNotify_interval().getOrg_time());
      Intent intent = new Intent(context, AlarmReceiver.class);
      byte[] ob_array = serialize(item);
      intent.putExtra(ITEM, ob_array);
      PendingIntent sender = PendingIntent.getBroadcast(
          context, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      checkNotNull(alarmManager);

      alarmManager.setAlarmClock(
          new AlarmManager.AlarmClockInfo(item.getDate().getTimeInMillis(), null), sender);
    }
  }

  public void deleteAlarm(Item item) {

    if(isAlarmSetted(item)) {
      Intent intent = new Intent(context, AlarmReceiver.class);
      PendingIntent sender = PendingIntent.getBroadcast(
          context, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      checkNotNull(alarmManager);

      alarmManager.cancel(sender);
      sender.cancel();
    }
  }

  public boolean isAlarmSetted(Item item) {

    Intent intent = new Intent(context, AlarmReceiver.class);
    PendingIntent sender = PendingIntent.getBroadcast(
        context, (int)item.getId(), intent, PendingIntent.FLAG_NO_CREATE);

    return sender != null;
  }

  public void updateDB(Item item, String table) {

    accessor.executeUpdate(item.getId(), serialize(item), table);
  }
}
