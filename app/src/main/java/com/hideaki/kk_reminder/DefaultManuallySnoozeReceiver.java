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
import static java.util.Objects.requireNonNull;

public class DefaultManuallySnoozeReceiver extends BroadcastReceiver {

  private DBAccessor accessor = null;
  private Context context;

  @Override
  public void onReceive(Context context, Intent intent) {

    this.context = context;
    ItemAdapter item = new ItemAdapter(deserialize(intent.getByteArrayExtra(ITEM)));
    requireNonNull(item);

    accessor = new DBAccessor(getDynamicContext(context), getIsDirectBootContext(context));

    // 通知を既読する
    SharedPreferences stringPreferences = getDynamicContext(context).getSharedPreferences(
      getIsDirectBootContext(context) ? STRING_GENERAL_COPY : STRING_GENERAL,
      MODE_PRIVATE
    );
    Set<String> idTable =
      stringPreferences.getStringSet(NOTIFICATION_ID_TABLE, new TreeSet<String>());
    requireNonNull(idTable);
    int parentId = intent.getIntExtra(PARENT_NOTIFICATION_ID, 0);
    int childId = intent.getIntExtra(CHILD_NOTIFICATION_ID, 0);
    String channelId = intent.getStringExtra(CHANNEL_ID);
    NotificationManager manager =
      (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
    requireNonNull(manager);

    for(int i = 1; i <= childId; i++) {
      manager.cancel(parentId + i);
    }
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.deleteNotificationChannel(channelId);
    }
    idTable.remove(Integer.toBinaryString(parentId));
    stringPreferences
      .edit()
      .putStringSet(NOTIFICATION_ID_TABLE, idTable)
      .apply();

    if(!getIsDirectBootContext(context)) {
      copySharedPreferences(context, false);
    }

    SharedPreferences intPreferences = getDynamicContext(context).getSharedPreferences(
      getIsDirectBootContext(context) ? INT_GENERAL_COPY : INT_GENERAL,
      MODE_PRIVATE
    );
    int snoozeDefaultHour = intPreferences.getInt(SNOOZE_DEFAULT_HOUR, 0);
    int snoozeDefaultMinute = intPreferences.getInt(SNOOZE_DEFAULT_MINUTE, 15);
    long defaultSnooze = snoozeDefaultHour * HOUR + snoozeDefaultMinute * MINUTE;

    if(item.getAlteredTime() == 0) {
      item.setOrgDate((Calendar)item.getDate().clone());
    }
    item.getDate().setTimeInMillis(currentTimeMinutes() + defaultSnooze);
    item.addAlteredTime(defaultSnooze);

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

  public void setAlarm(ItemAdapter item) {

    if(item.getDate().getTimeInMillis() > System.currentTimeMillis() &&
      item.getWhichListBelongs() == 0) {
      item.getNotifyInterval().setTime(item.getNotifyInterval().getOrgTime());
      Intent intent = new Intent(context, AlarmReceiver.class);
      byte[] obArray = serialize(item.getItem());
      intent.putExtra(ITEM, obArray);
      PendingIntent sender = PendingIntent.getBroadcast(
        context, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      requireNonNull(alarmManager);

      alarmManager.setAlarmClock(
        new AlarmManager.AlarmClockInfo(item.getDate().getTimeInMillis(), null), sender);
    }
  }

  public void deleteAlarm(ItemAdapter item) {

    if(isAlarmSet(item)) {
      Intent intent = new Intent(context, AlarmReceiver.class);
      PendingIntent sender = PendingIntent.getBroadcast(
        context, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      requireNonNull(alarmManager);

      alarmManager.cancel(sender);
      sender.cancel();
    }
  }

  public boolean isAlarmSet(ItemAdapter item) {

    Intent intent = new Intent(context, AlarmReceiver.class);
    PendingIntent sender = PendingIntent.getBroadcast(
      context, (int)item.getId(), intent, PendingIntent.FLAG_NO_CREATE);

    return sender != null;
  }

  public void updateDB(ItemAdapter item, String table) {

    accessor.executeUpdate(item.getId(), serialize(item.getItem()), table);
  }
}
