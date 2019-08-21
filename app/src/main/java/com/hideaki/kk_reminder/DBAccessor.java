package com.hideaki.kk_reminder;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;
import java.util.List;

class DBAccessor {

  private SQLiteDatabase sdb;
  private MyDatabaseHelper helper;
  private String state_str;
  private SQLiteStatement statement;

  DBAccessor(Context context) {

    this.helper = MyDatabaseHelper.getInstance(context);
  }

  private void closeDB() {

    if(sdb != null) {
      sdb.close();
      sdb = null;
    }
  }

  void executeInsert(long id, byte[] stream, String table) {

    sdb = helper.getWritableDatabase();
    state_str = "INSERT INTO " + table + "(item_id, serial) VALUES(?, ?)";

    sdb.beginTransaction();
    try {
      statement = sdb.compileStatement(state_str);
      statement.bindLong(1, id);
      statement.bindBlob(2, stream);

      statement.executeInsert();

      sdb.setTransactionSuccessful();
    }
    catch(SQLException e) {
      e.printStackTrace();
    }
    finally {
      sdb.endTransaction();
      closeDB();
    }
  }

  void executeUpdate(long id, byte[] stream, String table) {

    sdb = helper.getWritableDatabase();
    state_str = "UPDATE " + table + " SET serial = ? WHERE item_id = ?";

    sdb.beginTransaction();
    try {
      statement = sdb.compileStatement(state_str);
      statement.bindBlob(1, stream);
      statement.bindLong(2, id);

      statement.executeUpdateDelete();

      sdb.setTransactionSuccessful();
    }
    catch(SQLException e) {
      e.printStackTrace();
    }
    finally {
      sdb.endTransaction();
      closeDB();
    }
  }

  void executeDelete(long id, String table) {

    sdb = helper.getWritableDatabase();
    state_str = "DELETE FROM " + table + " WHERE item_id = ?";

    sdb.beginTransaction();
    try {
      statement = sdb.compileStatement(state_str);
      statement.bindLong(1, id);

      statement.executeUpdateDelete();

      sdb.setTransactionSuccessful();
    }
    catch(SQLException e) {
      e.printStackTrace();
    }
    finally {
      sdb.endTransaction();
      closeDB();
    }
  }

  List<byte[]> executeQueryAll(String table) {

    sdb = helper.getReadableDatabase();
    Cursor cursor = null;
    state_str = "SELECT * FROM " + table;

    try {
      cursor = sdb.rawQuery(state_str, null);
      return readCursorBySerial(cursor);
    }
    finally {
      if(cursor != null) cursor.close();
      closeDB();
    }
  }

  byte[] executeQueryById(long id, String table) {

    sdb = helper.getReadableDatabase();
    Cursor cursor = null;
    state_str = "SELECT * FROM " + table + " WHERE item_id = ?";

    try {
      cursor = sdb.rawQuery(state_str, new String[]{Long.toString(id)});
      return readCursorById(cursor);
    }
    finally {
      if(cursor != null) cursor.close();
      closeDB();
    }
  }

  private byte[] readCursorById(Cursor cursor) {

    int indexSerial;

    indexSerial = cursor.getColumnIndex("serial");

    if(cursor.moveToNext()) {
      return cursor.getBlob(indexSerial);
    }
    else {
      return null;
    }
  }

  private List<byte[]> readCursorBySerial(Cursor cursor) {

    List<byte[]> list = new ArrayList<>();
    int indexSerial;

    indexSerial = cursor.getColumnIndex("serial");

    while(cursor.moveToNext()) {
      list.add(cursor.getBlob(indexSerial));
    }

    return list;
  }
}
