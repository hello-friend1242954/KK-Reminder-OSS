package com.hideaki.kk_reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

import androidx.annotation.RequiresApi;

import static com.hideaki.kk_reminder.UtilClass.ACTION_IN_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.copyDatabase;
import static com.hideaki.kk_reminder.UtilClass.copySharedPreferences;
import static com.hideaki.kk_reminder.UtilClass.deserialize;
import static com.hideaki.kk_reminder.UtilClass.serialize;
import static java.util.Objects.requireNonNull;

public class StartupReceiver extends BroadcastReceiver {

  private static final String IN_DIRECT_BOOT_CONTEXT_MODE = "IN_DIRECT_BOOT_CONTEXT_MODE";
  private DBAccessor accessor;

  @Override
  public void onReceive(Context context, Intent intent) {

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      if(Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {

        setIsDirectBootContext(context, true);

        Context directBootContext = context.createDeviceProtectedStorageContext();
        accessor = new DBAccessor(directBootContext, true);
        resetAlarm(context);
      }
      else if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

        setIsDirectBootContext(context, false);

        // DirectBoot終了時にDirectBootContextでの変更点をNormalContextに反映する
        copyDatabase(context, true);
        copySharedPreferences(context, true);
        context.sendBroadcast(new Intent(ACTION_IN_NOTIFICATION));
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

      ItemAdapter item = new ItemAdapter(deserialize(stream));
      requireNonNull(item);

      if(
        item.getDate().getTimeInMillis() > System.currentTimeMillis() &&
          item.getWhichListBelongs() == 0
      ) {
        Intent setAlarm = new Intent(context, AlarmReceiver.class);
        byte[] obArray = serialize(item.getItem());
        setAlarm.putExtra(ITEM, obArray);
        PendingIntent sender = PendingIntent.getBroadcast(
          context, (int)item.getId(), setAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        requireNonNull(alarmManager);

        alarmManager.setAlarmClock(
          new AlarmManager.AlarmClockInfo(
            item.getDate().getTimeInMillis(),
            null
          ),
          sender
        );
      }
    }
  }

  private static void setIsDirectBootContext(Context context, boolean isDirectBootContext) {

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      File file = getSyncFile(context, IN_DIRECT_BOOT_CONTEXT_MODE);
      writeOrDeleteFile(file, isDirectBootContext);
    }
  }

  @SuppressWarnings("SameParameterValue")
  @RequiresApi(api = Build.VERSION_CODES.N)
  private static File getSyncFile(Context context, String fileName) {

    Context directBootContext = context.createDeviceProtectedStorageContext();
    File file = directBootContext
      .getDatabasePath(MyDatabaseHelper.DATABASE_COPY)
      .getParentFile();
    requireNonNull(file);
    String parentPath = file.getAbsolutePath();

    return new File(parentPath + "/" + fileName);
  }

  private static void writeOrDeleteFile(File file, boolean isWrite) {

    if(isWrite) {
      try {
        file.createNewFile();
      }
      catch(IOException e) {
        Log.e("writeOrDeleteFile", Log.getStackTraceString(e));
      }
    }
    else {
      file.delete();
    }
  }

  static boolean getIsDirectBootContext(Context context) {

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return getSyncFile(context, IN_DIRECT_BOOT_CONTEXT_MODE).exists();
    }
    else {
      return false;
    }
  }

  static Context getDynamicContext(Context normalContext) {

    // 引数のnormalContextはDirectBootContextではなく必ず通常のContextを渡すこと
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return getIsDirectBootContext(normalContext) ?
        normalContext.createDeviceProtectedStorageContext() : normalContext;
    }
    else {
      return normalContext;
    }
  }
}
