package com.hideaki.kk_reminder;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

import static android.content.Context.MODE_PRIVATE;
import static com.hideaki.kk_reminder.StartupReceiver.getDynamicContext;
import static com.hideaki.kk_reminder.StartupReceiver.getIsDirectBootContext;
import static com.hideaki.kk_reminder.UtilClass.CREATED;
import static com.hideaki.kk_reminder.UtilClass.DESTROYED;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.copySharedPreferences;

public class MyLifecycleHandler implements Application.ActivityLifecycleCallbacks {

  private int created;
  private int destroyed;
//  private int resumed;
//  private int paused;
//  private int started;
//  private int stopped;

  @Override
  public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {

    if(activity instanceof MainActivity) {
      created++;
      saveCountInSharedPreferences(activity);
    }
  }

  @Override
  public void onActivityStarted(@NonNull Activity activity) {

//    started++;
//    saveCountInSharedPreferences(activity);
  }

  @Override
  public void onActivityResumed(@NonNull Activity activity) {

//    resumed++;
//    saveCountInSharedPreferences(activity);
  }

  @Override
  public void onActivityPaused(@NonNull Activity activity) {

//    paused++;
//    if(resumed == paused) {
//      resumed = 0;
//      paused = 0;
//    }
//    saveCountInSharedPreferences(activity);
  }

  @Override
  public void onActivityStopped(@NonNull Activity activity) {

//    stopped++;
//    if(started == stopped) {
//      started = 0;
//      stopped = 0;
//    }
//    saveCountInSharedPreferences(activity);
  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

  }

  @Override
  public void onActivityDestroyed(@NonNull Activity activity) {

    if(activity instanceof MainActivity) {
      destroyed++;
      if(created == destroyed) {
        created = 0;
        destroyed = 0;
      }
      saveCountInSharedPreferences(activity);

      copySharedPreferences(activity, false);
    }
  }

  private void saveCountInSharedPreferences(Activity activity) {

    getDynamicContext(activity)
        .getSharedPreferences(
            getIsDirectBootContext(activity) ? INT_GENERAL_COPY : INT_GENERAL,
            MODE_PRIVATE
        )
        .edit()
        .putInt(CREATED, created)
        .putInt(DESTROYED, destroyed)
//        .putInt(RESUMED, resumed)
//        .putInt(PAUSED, paused)
//        .putInt(STARTED, started)
//        .putInt(STOPPED, stopped)
        .apply();
  }
}
