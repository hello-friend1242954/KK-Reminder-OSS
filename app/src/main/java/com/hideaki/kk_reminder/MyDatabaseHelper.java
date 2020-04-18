package com.hideaki.kk_reminder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHelper extends SQLiteOpenHelper {

  static final String TODO_TABLE = "todo";
  static final String DONE_TABLE = "done";
  static final String SETTINGS_TABLE = "settings";
  static final String DATABASE = "reminder.db";
  static final String DATABASE_COPY = "reminder_copy.db";
  private static final String CREATE_TODO_TABLE =
    "CREATE TABLE todo(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, item_id INTEGER NOT NULL, serial BLOB NOT NULL)";
  private static final String CREATE_DONE_TABLE =
    "CREATE TABLE done(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, item_id INTEGER NOT NULL, serial BLOB NOT NULL)";
  private static final String CREATE_SETTINGS_TABLE =
    "CREATE TABLE settings(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, item_id INTEGER NOT NULL, serial BLOB NOT NULL)";
  private static MyDatabaseHelper normalContextSingleton = null;
  private static MyDatabaseHelper directBootContextSingleton = null;

  public static synchronized MyDatabaseHelper getInstance(
    Context context,
    boolean isDirectBootContext
  ) {

    if(isDirectBootContext) {
      if(directBootContextSingleton == null) {
        directBootContextSingleton = new MyDatabaseHelper(context, DATABASE_COPY);
      }
      return directBootContextSingleton;
    }
    else {
      if(normalContextSingleton == null) {
        normalContextSingleton = new MyDatabaseHelper(context, DATABASE);
      }
      return normalContextSingleton;
    }
  }

  @SuppressWarnings("MethodParameterNamingConvention")
  private MyDatabaseHelper(Context context, String DATABASE_NAME) {

    super(context, DATABASE_NAME, null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {

    db.execSQL(CREATE_TODO_TABLE);
    db.execSQL(CREATE_DONE_TABLE);
    db.execSQL(CREATE_SETTINGS_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

  }
}
