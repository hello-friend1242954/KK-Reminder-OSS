package com.hideaki.kk_reminder;

import androidx.multidex.MultiDexApplication;
import androidx.appcompat.app.AppCompatDelegate;

public class MyApplication extends MultiDexApplication {

  @Override
  public void onCreate() {

    super.onCreate();
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    registerActivityLifecycleCallbacks(new MyLifecycleHandler());
  }
}
