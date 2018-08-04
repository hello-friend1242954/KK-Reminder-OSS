package com.example.hideaki.reminder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;

public class AlarmReceiver extends BroadcastReceiver {

  private int time;
  private byte[] ob_array;
  private long id;
  private long reset_schedule;
  private Item item;
  private Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
  private PowerManager powerManager;
  private PowerManager.WakeLock wakeLock;
  private Intent open_activity;
  private Intent recursive_alarm;
  private PendingIntent sender;
  private PendingIntent recursive_sender;
  private NotificationCompat.Builder builder;
  private NotificationManager manager;
  private AlarmManager alarmManager;

  @Override
  public void onReceive(Context context, Intent intent) {

    ob_array = intent.getByteArrayExtra(MainEditFragment.ITEM);
    if(ob_array == null) throw new NullPointerException("ob_array is null");
    try {
      item = (Item)MainActivity.deserialize(ob_array);
    } catch(IOException e) {
      e.printStackTrace();
    } catch(ClassNotFoundException e) {
      e.printStackTrace();
    }
    if(item == null) throw new NullPointerException("item is null");

    open_activity = new Intent(context, MainActivity.class);
    sender = PendingIntent.getActivity(
        context, 0, open_activity, PendingIntent.FLAG_UPDATE_CURRENT);

    builder = new NotificationCompat.Builder(context, "reminder")
        .setContentTitle("Reminder")
        .setContentText(item.getDetail())
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(sender)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setLights(Color.GREEN, 2000, 1000)
        .setVibrate(new long[]{0, 500})
        .setSound(uri);

    time = item.getNotify_interval().getTime();
    if(time > 0) id = item.getId() * (time + 2);
    else if(time < 0) id = -(item.getId() * (time - 2));
    else id = item.getId();
    manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify((int)id, builder.build());

    powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    wakeLock = powerManager.newWakeLock(
        PowerManager.FULL_WAKE_LOCK |
            PowerManager.ACQUIRE_CAUSES_WAKEUP |
            PowerManager.ON_AFTER_RELEASE, "Notification");
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
      if(!powerManager.isScreenOn()) {
        wakeLock.acquire(10000);
      }
    }
    else {
      if(!powerManager.isInteractive()) {
        wakeLock.acquire(10000);
      }
    }

    //再帰通知処理
    if(time > 0 || time < 0) {
      item.getNotify_interval().setTime(--time);
      recursive_alarm = new Intent(context, AlarmReceiver.class);
      try {
        ob_array = MainActivity.serialize(item);
      } catch(IOException e) {
        e.printStackTrace();
      }
      recursive_alarm.putExtra(MainEditFragment.ITEM, ob_array);
      recursive_sender = PendingIntent.getBroadcast(
          context, (int)item.getId(), recursive_alarm, PendingIntent.FLAG_UPDATE_CURRENT);

      reset_schedule = System.currentTimeMillis()
          + item.getNotify_interval().getHour() * 60 * 60 * 1000
          + item.getNotify_interval().getMinute() * 60 * 1000;
      alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        alarmManager.setAlarmClock(
            new AlarmManager.AlarmClockInfo(reset_schedule, null), recursive_sender);
      } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reset_schedule, recursive_sender);
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, reset_schedule, recursive_sender);
      }
    }
  }
}