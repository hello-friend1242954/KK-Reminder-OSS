package com.hideaki.kk_reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.deserialize;
import static com.hideaki.kk_reminder.UtilClass.serialize;

public class StartupReceiver extends BroadcastReceiver {

  private DBAccessor accessor;

  @Override
  public void onReceive(Context context, Intent intent) {

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      if(Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {
        Context direct_boot_context = context.createDeviceProtectedStorageContext();
        accessor = new DBAccessor(direct_boot_context, true);
        resetAlarm(context);
      }
    }
    else {
      if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
        accessor = new DBAccessor(context, false);
        resetAlarm(context);
      }
    }
  }

  private void resetAlarm(Context context) {

    List<byte[]> streamList = accessor.executeQueryAll(MyDatabaseHelper.TODO_TABLE);
    for(byte[] stream : streamList) {

      Item item = (Item)deserialize(stream);
      checkNotNull(item);

      if(item.getDate().getTimeInMillis() > System.currentTimeMillis() && item.getWhich_list_belongs() == 0) {
        Intent set_alarm = new Intent(context, AlarmReceiver.class);
        byte[] ob_array = serialize(item);
        set_alarm.putExtra(ITEM, ob_array);
        PendingIntent sender = PendingIntent.getBroadcast(
            context, (int)item.getId(), set_alarm, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        checkNotNull(alarmManager);

        alarmManager.setAlarmClock(
            new AlarmManager.AlarmClockInfo(item.getDate().getTimeInMillis(), null), sender);
      }
    }
  }
}
