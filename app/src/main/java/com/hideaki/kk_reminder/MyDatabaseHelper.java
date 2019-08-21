package com.hideaki.kk_reminder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHelper extends SQLiteOpenHelper {

  static final String TODO_TABLE = "todo";
  static final String DONE_TABLE = "done";
  static final String SETTINGS_TABLE = "settings";
  static final String DATABASE = "reminder.db";
  private static final String CREATE_TODO_TABLE =
      "CREATE TABLE todo(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, item_id INTEGER NOT NULL, serial BLOB NOT NULL)";
  private static final String CREATE_DONE_TABLE =
      "CREATE TABLE done(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, item_id INTEGER NOT NULL, serial BLOB NOT NULL)";
  private static final String CREATE_SETTINGS_TABLE =
      "CREATE TABLE settings(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, item_id INTEGER NOT NULL, serial BLOB NOT NULL)";
  private static MyDatabaseHelper singleton = null;

  public static synchronized MyDatabaseHelper getInstance(Context context) {

    if(singleton == null) {
      singleton = new MyDatabaseHelper(context);
    }
    return singleton;
  }

  private MyDatabaseHelper(Context context) {

    super(context, DATABASE, null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {

    db.execSQL(CREATE_TODO_TABLE);
    db.execSQL(CREATE_DONE_TABLE);
    db.execSQL(CREATE_SETTINGS_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {}
}
