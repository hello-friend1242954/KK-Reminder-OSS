package com.hideaki.kk_reminder;

import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.Nullable;

import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.deserialize;
import static java.util.Objects.requireNonNull;

public class DeleteDoneListService extends IntentService {

  DBAccessor accessor;

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

    requireNonNull(intent);
    Item item = (Item)deserialize(intent.getByteArrayExtra(ITEM));
    requireNonNull(item);
    deleteDB(item, MyDatabaseHelper.DONE_TABLE);
  }

  public void deleteDB(Item item, String table) {

    accessor.executeDelete(item.getId(), table);
  }
}
