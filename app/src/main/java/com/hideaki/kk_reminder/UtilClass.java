package com.hideaki.kk_reminder;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

class UtilClass {

  private UtilClass() {}

  private static final AtomicInteger uniqueId = new AtomicInteger(1);
  static final ScheduledItemComparator SCHEDULED_ITEM_COMPARATOR = new ScheduledItemComparator();
  static final NonScheduledItemComparator NON_SCHEDULED_ITEM_COMPARATOR = new NonScheduledItemComparator();
  static final DoneItemComparator DONE_ITEM_COMPARATOR = new DoneItemComparator();
  static final NotesComparator NOTES_COMPARATOR = new NotesComparator();
  static final Uri DEFAULT_URI_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
  static final String ACTION_IN_NOTIFICATION = "ACTION_IN_NOTIFICATION";
  static final String BOOT_FROM_NOTIFICATION = "BOOT_FROM_NOTIFICATION";
  static final int REQUEST_CODE_RINGTONE_PICKER = 0;
  static final String ITEM = "ITEM";
  static final String LIST = "LIST";
  static final String INT_GENERAL = "INT_GENERAL";
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
  static final String PLAY_SLIDE_ANIMATION = "PLAY_SLIDE_ANIMATION";
  static final String IS_EXPANDABLE_TODO = "IS_EXPANDABLE_TODO";
  static final String IS_PREMIUM = "IS_PREMIUM";
  static final String STRING_GENERAL = "STRING_GENERAL";
  static final String NOTIFICATION_ID_TABLE = "NOTIFICATION_ID_TABLE";
  static final String PARENT_NOTIFICATION_ID = "PARENT_NOTIFICATION_ID";
  static final String CHILD_NOTIFICATION_ID = "CHILD_NOTIFICATION_ID";
  static final String LINE_SEPARATOR = System.getProperty("line.separator");
  static final String PRODUCT_ID_PREMIUM = "com.hideaki.premium";
  static final int REQUEST_CODE_SIGN_IN = 1;
  static long MINUTE = 60 * 1000;
  static long HOUR = 60 * 60 * 1000;
  static Locale LOCALE = Locale.getDefault();

  static void setCursorDrawableColor(EditText editText) {

    try {
      Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
      f.setAccessible(true);
      f.set(editText, R.drawable.cursor);
    } catch (Exception ignored) {}
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private static Bitmap getBitmap(VectorDrawable vectorDrawable) {

    Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
        vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    vectorDrawable.draw(canvas);

    return bitmap;
  }

  private static Bitmap getBitmap(VectorDrawableCompat vectorDrawable) {

    Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
        vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    vectorDrawable.draw(canvas);

    return bitmap;
  }

  @SuppressLint("NewApi")
  public static Bitmap getBitmap(Context context, @DrawableRes int drawableResId) {

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
    else throw new IllegalArgumentException("Unsupported drawable type");
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

    float scale = context.getResources().getDisplayMetrics().density; //画面のdensityを指定
    return (int)(dp * scale + 0.5f);
  }

  //int型のユニークIDを取得するメソッド
  static int generateUniqueId() {

    for(;;) {
      final int result = uniqueId.get();
      int new_value = result + 1;
      if(new_value == Integer.MAX_VALUE) new_value = 1;
      if(uniqueId.compareAndSet(result, new_value)) {
        return result;
      }
    }
  }

  //シリアライズメソッド
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

  //デシリアライズメソッド
  static Object deserialize(byte[] stream) {

    if(stream == null) return null;
    else {
      ByteArrayInputStream bais = new ByteArrayInputStream(stream);
      Object data = null;
      try {
        ObjectInputStream ois = new ObjectInputStream(bais);
        data = ois.readObject();
        ois.close();
      } catch(IOException e) {
        e.printStackTrace();
      } catch(ClassNotFoundException e) {
        e.printStackTrace();
      }

      return data;
    }
  }
}
