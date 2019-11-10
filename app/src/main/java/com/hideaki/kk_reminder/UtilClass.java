package com.hideaki.kk_reminder;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

class UtilClass {

  private UtilClass() {

  }

  private static final AtomicInteger uniqueId = new AtomicInteger(1);
  static final ScheduledItemComparator SCHEDULED_ITEM_COMPARATOR = new ScheduledItemComparator();
  static final NonScheduledItemComparator NON_SCHEDULED_ITEM_COMPARATOR =
    new NonScheduledItemComparator();
  static final DoneItemComparator DONE_ITEM_COMPARATOR = new DoneItemComparator();
  static final NotesComparator NOTES_COMPARATOR = new NotesComparator();
  static final Uri DEFAULT_URI_SOUND =
    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
  static final String ACTION_IN_NOTIFICATION = "ACTION_IN_NOTIFICATION";
  static final String BOOT_FROM_NOTIFICATION = "BOOT_FROM_NOTIFICATION";
  static final int REQUEST_CODE_RINGTONE_PICKER = 0;
  static final String ITEM = "ITEM";
  static final String LIST = "LIST";
  static final String INT_GENERAL = "INT_GENERAL";
  static final String INT_GENERAL_COPY = "INT_GENERAL_COPY";
  static final String DEFAULT_MINUS_TIME_1_HOUR = "DEFAULT_MINUS_TIME_1_HOUR";
  static final String DEFAULT_MINUS_TIME_1_MINUTE = "DEFAULT_MINUS_TIME_1_MINUTE";
  static final String DEFAULT_MINUS_TIME_2_HOUR = "DEFAULT_MINUS_TIME_2_HOUR";
  static final String DEFAULT_MINUS_TIME_2_MINUTE = "DEFAULT_MINUS_TIME_2_MINUTE";
  static final String DEFAULT_MINUS_TIME_3_HOUR = "DEFAULT_MINUS_TIME_3_HOUR";
  static final String DEFAULT_MINUS_TIME_3_MINUTE = "DEFAULT_MINUS_TIME_3_MINUTE";
  static final String DEFAULT_PLUS_TIME_1_HOUR = "DEFAULT_PLUS_TIME_1_HOUR";
  static final String DEFAULT_PLUS_TIME_1_MINUTE = "DEFAULT_PLUS_TIME_1_MINUTE";
  static final String DEFAULT_PLUS_TIME_2_HOUR = "DEFAULT_PLUS_TIME_2_HOUR";
  static final String DEFAULT_PLUS_TIME_2_MINUTE = "DEFAULT_PLUS_TIME_2_MINUTE";
  static final String DEFAULT_PLUS_TIME_3_HOUR = "DEFAULT_PLUS_TIME_3_HOUR";
  static final String DEFAULT_PLUS_TIME_3_MINUTE = "DEFAULT_PLUS_TIME_3_MINUTE";
  static final String DEFAULT_TEXT_SIZE = "DEFAULT_TEXT_SIZE";
  static final String SNOOZE_DEFAULT_HOUR = "SNOOZE_DEFAULT_HOUR";
  static final String SNOOZE_DEFAULT_MINUTE = "SNOOZE_DEFAULT_MINUTE";
  static final String MENU_POSITION = "MENU_POSITION";
  static final String SUBMENU_POSITION = "SUBMENU_POSITION";
  static final String CREATED = "CREATED";
  static final String DESTROYED = "DESTROYED";
  //  static final String ATTACHED = "ATTACHED";
//  static final String DETACHED = "DETACHED";
  static final String CHANGE_GRADE = "I am sceoppa100 developer";
  static final String COLLAPSE_GROUP = "COLLAPSE_GROUP";
  //  static final String RESUMED = "RESUMED";
//  static final String PAUSED = "PAUSED";
//  static final String STARTED = "STARTED";
//  static final String STOPPED = "STOPPED";
  static final String BOOLEAN_GENERAL = "BOOLEAN_GENERAL";
  static final String BOOLEAN_GENERAL_COPY = "BOOLEAN_GENERAL_COPY";
  static final String IS_RECREATED = "IS_RECREATED";
  static final String IS_ID_TABLE_FLOOD = "IS_ID_TABLE_FLOOD";
  static final String IS_DARK_MODE = "IS_DARK_MODE";
  static final String IS_DARK_THEME_FOLLOW_SYSTEM = "IS_DARK_THEME_FOLLOW_SYSTEM";
  static final String PLAY_SLIDE_ANIMATION = "PLAY_SLIDE_ANIMATION";
  static final String IS_EXPANDABLE_TODO = "IS_EXPANDABLE_TODO";
  static final String IS_PREMIUM = "IS_PREMIUM";
  static final String STRING_GENERAL = "STRING_GENERAL";
  static final String STRING_GENERAL_COPY = "STRING_GENERAL_COPY";
  static final String VIBRATION_PATTERN = "VIBRATION_PATTERN";
  static final String DEFAULT_VIBRATION_PATTERN = "0, 250, 250, 250";
  static final String CHANNEL_ID = "CHANNEL_ID";
  static final String NOTIFICATION_ID_TABLE = "NOTIFICATION_ID_TABLE";
  static final String PARENT_NOTIFICATION_ID = "PARENT_NOTIFICATION_ID";
  static final String CHILD_NOTIFICATION_ID = "CHILD_NOTIFICATION_ID";
  static final String LINE_SEPARATOR = System.getProperty("line.separator");
  static final String PRODUCT_ID_PREMIUM = "com.hideaki.premium";
  static final int REQUEST_CODE_SIGN_IN = 1;
  static long MINUTE = 60 * 1000;
  static long HOUR = 60 * 60 * 1000;
  static Locale LOCALE = Locale.getDefault();

  static String readFileFromAssets(Context context, String fileName) {

    StringBuilder text = new StringBuilder();
    try(
      InputStream is = context.getAssets().open(fileName);
      BufferedReader br = new BufferedReader(new InputStreamReader(is))
    ) {

      String str;
      while((str = br.readLine()) != null) {
        text
          .append(str)
          .append("\n");
      }
    }
    catch(IOException e) {
      e.printStackTrace();
    }

    return text.toString();
  }

  static String getRegularizedVibrationStr(String vibrationStr) {

    if(vibrationStr.equals("")) {
      return DEFAULT_VIBRATION_PATTERN;
    }

    String[] vibrationsStr = vibrationStr.split(",");
    int size = vibrationsStr.length;
    for(int i = 0; i < size; i++) {
      vibrationsStr[i] = vibrationsStr[i].trim();
    }
    StringBuilder vibrationStrBuilder = new StringBuilder();
    boolean isAllZero = true;
    for(String s : vibrationsStr) {
      try {
        long val = Long.parseLong(s);
        if(isAllZero && val != 0) {
          isAllZero = false;
        }
        vibrationStrBuilder
          .append(s)
          .append(", ");
      }
      catch(NumberFormatException e) {
        return DEFAULT_VIBRATION_PATTERN;
      }
    }

    return isAllZero ?
      DEFAULT_VIBRATION_PATTERN :
      vibrationStrBuilder.substring(0, vibrationStrBuilder.length() - 2);
  }

  static long[] getVibrationPattern(String regularizedVibrationStr) {

    String[] vibrationsStr = regularizedVibrationStr.split(",");
    int size = vibrationsStr.length;
    for(int i = 0; i < size; i++) {
      vibrationsStr[i] = vibrationsStr[i].trim();
    }
    long[] vibrationPattern = new long[size];
    for(int i = 0; i < size; i++) {
      vibrationPattern[i] = Long.parseLong(vibrationsStr[i]);
    }

    return vibrationPattern;
  }

  static void setCursorDrawableColor(MainActivity activity, EditText editText) {

    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
      try {
        Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
        f.setAccessible(true);
        if(activity.isDarkMode) {
          f.set(editText, R.drawable.cursor_dark);
        }
        else {
          f.set(editText, R.drawable.cursor);
        }
      }
      catch(Exception ignored) {
      }
    }
    else {
      // mCursorDrawableResへのアクセスは非SDKインターフェースなのでAndroid10以降は無効となる
      // 代わりにAPI29で追加されたEditText#setTextCursorDrawable()を使う
      if(activity.isDarkMode) {
        editText.setTextCursorDrawable(R.drawable.cursor_dark);
      }
      else {
        editText.setTextCursorDrawable(R.drawable.cursor);
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private static Bitmap getBitmap(VectorDrawable vectorDrawable) {

    Bitmap bitmap = Bitmap.createBitmap(
      vectorDrawable.getIntrinsicWidth(),
      vectorDrawable.getIntrinsicHeight(),
      Bitmap.Config.ARGB_8888
    );
    Canvas canvas = new Canvas(bitmap);
    vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    vectorDrawable.draw(canvas);

    return bitmap;
  }

  private static Bitmap getBitmap(VectorDrawableCompat vectorDrawable) {

    Bitmap bitmap = Bitmap.createBitmap(
      vectorDrawable.getIntrinsicWidth(),
      vectorDrawable.getIntrinsicHeight(),
      Bitmap.Config.ARGB_8888
    );
    Canvas canvas = new Canvas(bitmap);
    vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    vectorDrawable.draw(canvas);

    return bitmap;
  }

  @SuppressLint("NewApi")
  public static Bitmap getBitmap(
    Context context,
    @DrawableRes int drawableResId
  ) {

    Drawable drawable = ContextCompat.getDrawable(context, drawableResId);
    if(drawable instanceof BitmapDrawable) {
      return ((BitmapDrawable)drawable).getBitmap();
    }
    else if(drawable instanceof VectorDrawableCompat) {
      return getBitmap((VectorDrawableCompat)drawable);
    }
    else if(drawable instanceof VectorDrawable) {
      return getBitmap((VectorDrawable)drawable);
    }
    else {
      throw new IllegalArgumentException("Unsupported drawable type");
    }
  }

  static long currentTimeMinutes() {

    Calendar now = Calendar.getInstance();
    if(now.get(Calendar.SECOND) >= 30) {
      now.add(Calendar.MINUTE, 1);
    }
    now.set(Calendar.SECOND, 0);
    now.set(Calendar.MILLISECOND, 0);

    return now.getTimeInMillis();
  }

  static int getPxFromDp(Context context, int dp) {

    float scale = context.getResources().getDisplayMetrics().density; // 画面のdensityを指定
    return (int)(dp * scale + 0.5f);
  }

  // int型のユニークIDを取得するメソッド
  static int generateUniqueId() {

    for(; ; ) {
      final int result = uniqueId.get();
      int new_value = result + 1;
      if(new_value == Integer.MAX_VALUE) {
        new_value = 1;
      }
      if(uniqueId.compareAndSet(result, new_value)) {
        return result;
      }
    }
  }

  // シリアライズメソッド
  static byte[] serialize(Object data) {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(data);
      oos.flush();
      oos.close();
    }
    catch(IOException e) {
      e.printStackTrace();
    }

    return baos.toByteArray();
  }

  // デシリアライズメソッド
  static Object deserialize(byte[] stream) {

    if(stream == null) {
      return null;
    }
    else {
      ByteArrayInputStream bais = new ByteArrayInputStream(stream);
      Object data = null;
      try {
        ObjectInputStream ois = new ObjectInputStream(bais);
        data = ois.readObject();
        ois.close();
      }
      catch(IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }

      return data;
    }
  }

  // 指定されたContext上でデータベースの複製を作り、それを他方のContextへ移動する
  static void copyDatabase(Context context, boolean isFromDirectBootContext) {

    // 引数のcontextは必ず通常の(directBootContextでない)Contextを指定すること
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      // NormalContext上でコピーする場合はアプリに負荷をかけないようにするため非同期の
      // IntentServiceを利用し、DirectBootContext上でコピーする場合は同期で実行させて
      // sendBroadcast()を発行し、アプリ起動時のタスクの情報を最も直近に行われた
      // Transactionに同期させる。(実際には仕様上、アプリのMainActivityがフォアグラウンド
      // にあるときのみ非同期でコピーを行っている。)
      if(!isFromDirectBootContext && MainActivity.isScreenOn) {
        Intent intent = new Intent(context, CopyDatabaseService.class);
        try {
          context.startService(intent);
        }
        catch(IllegalStateException e) {
          e.printStackTrace();
          copyDatabaseKernel(context, false);
        }
      }
      else {
        copyDatabaseKernel(context, isFromDirectBootContext);
      }
    }
  }

  // 指定されたContext上でSharedPreferencesの複製を作り、それを他方のContextへ移動する
  static void copySharedPreferences(Context context, boolean isFromDirectBootContext) {

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      if(!isFromDirectBootContext && MainActivity.isScreenOn) {
        Intent intent = new Intent(context, CopySharedPreferencesService.class);
        try {
          context.startService(intent);
        }
        catch(IllegalStateException e) {
          e.printStackTrace();
          copySharedPreferencesKernel(context, false);
        }
      }
      else {
        copySharedPreferencesKernel(context, isFromDirectBootContext);
      }
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  static void copyDatabaseKernel(Context context, boolean isFromDirectBootContext) {

    // データベースの複製を作る
    // 引数のcontextは必ず通常の(directBootContextでない)Contextを指定すること
    Context direct_boot_context = context.createDeviceProtectedStorageContext();
    String databasePath;
    if(isFromDirectBootContext) {
      databasePath =
        direct_boot_context.getDatabasePath(MyDatabaseHelper.DATABASE_COPY).getAbsolutePath();
    }
    else {
      databasePath = context.getDatabasePath(MyDatabaseHelper.DATABASE).getAbsolutePath();
    }
    File file = new File(databasePath);
    OutputStream os = null;
    InputStream is = null;

    if(file.exists()) {
      try {
        String BASE_NAME = isFromDirectBootContext ?
          MyDatabaseHelper.DATABASE : MyDatabaseHelper.DATABASE_COPY;
        os = new FileOutputStream(file.getParent() + "/" + BASE_NAME);
        is = new FileInputStream(databasePath);

        byte[] buffer = new byte[1024];
        int length;
        while((length = is.read(buffer)) > 0) {
          os.write(buffer, 0, length);
        }

        os.flush();
      }
      catch(Exception e) {
        e.printStackTrace();
      }
      finally {
        try {
          if(os != null) {
            os.close();
          }
          if(is != null) {
            is.close();
          }
        }
        catch(Exception e) {
          e.printStackTrace();
        }
      }
    }

    // 指定されたContext上で複製したデータベースを他方のContextへ移動する
    if(isFromDirectBootContext) {
      context.moveDatabaseFrom(direct_boot_context, MyDatabaseHelper.DATABASE);
    }
    else {
      direct_boot_context.moveDatabaseFrom(context, MyDatabaseHelper.DATABASE_COPY);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  static void copySharedPreferencesKernel(Context context, boolean isFromDirectBootContext) {

    // SharedPreferencesの複製を作る
    // 引数のcontextは必ず通常の(directBootContextでない)Contextを指定すること
    Context direct_boot_context = context.createDeviceProtectedStorageContext();
    final String sharedPreferencesDirectory =
      context.getFilesDir().getParent() + "/shared_prefs/";
    final String[] sharedPreferencesFiles = {INT_GENERAL, BOOLEAN_GENERAL, STRING_GENERAL};
    for(String sharedPreferencesFile : sharedPreferencesFiles) {
      String sharedPreferencesPath = sharedPreferencesDirectory;
      if(isFromDirectBootContext) {
        sharedPreferencesPath += sharedPreferencesFile + "_COPY.xml";
      }
      else {
        sharedPreferencesPath += sharedPreferencesFile + ".xml";
      }
      File file = new File(sharedPreferencesPath);
      OutputStream os = null;
      InputStream is = null;

      if(file.exists()) {
        try {
          String BASE_NAME = isFromDirectBootContext ?
            sharedPreferencesFile + ".xml" : sharedPreferencesFile + "_COPY.xml";
          os = new FileOutputStream(file.getParent() + "/" + BASE_NAME);
          is = new FileInputStream(sharedPreferencesPath);

          byte[] buffer = new byte[1024];
          int length;
          while((length = is.read(buffer)) > 0) {
            os.write(buffer, 0, length);
          }

          os.flush();
        }
        catch(Exception e) {
          e.printStackTrace();
        }
        finally {
          try {
            if(os != null) {
              os.close();
            }
            if(is != null) {
              is.close();
            }
          }
          catch(Exception e) {
            e.printStackTrace();
          }
        }
      }

      // 指定されたContext上で複製したSharedPreferencesを他方のContextへ移動する
      if(isFromDirectBootContext) {
        context.moveSharedPreferencesFrom(direct_boot_context, sharedPreferencesFile);
      }
      else {
        direct_boot_context.moveSharedPreferencesFrom(
          context,
          sharedPreferencesFile + "_COPY"
        );
      }
    }
  }
}
