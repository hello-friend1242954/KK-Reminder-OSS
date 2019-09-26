package com.hideaki.kk_reminder;

import android.app.IntentService;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import static com.hideaki.kk_reminder.UtilClass.copyDatabaseKernel;

public class CopyDatabaseService extends IntentService {

  public CopyDatabaseService() {

    super(CopyDatabaseService.class.getSimpleName());
  }

  public CopyDatabaseService(String name) {

    super(name);
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  @Override
  protected void onHandleIntent(@Nullable Intent intent) {

    copyDatabaseKernel(this, false);
  }
}
