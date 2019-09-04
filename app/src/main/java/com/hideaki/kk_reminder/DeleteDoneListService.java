package com.hideaki.kk_reminder;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.deserialize;
import static com.google.common.base.Preconditions.checkNotNull;

public class DeleteDoneListService extends IntentService {

  DBAccessor accessor = null;

  public DeleteDoneListService() {

    super(DeleteDoneListService.class.getSimpleName());
    accessor = new DBAccessor(this, false);
  }

  public DeleteDoneListService(String name) {

    super(name);
    accessor = new DBAccessor(this, false);
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {

    checkNotNull(intent);
    Item item = (Item)deserialize(intent.getByteArrayExtra(ITEM));
    deleteDB(item, MyDatabaseHelper.DONE_TABLE);
  }

  public void deleteDB(Item item, String table) {

    accessor.executeDelete(item.getId(), table);
  }
}
