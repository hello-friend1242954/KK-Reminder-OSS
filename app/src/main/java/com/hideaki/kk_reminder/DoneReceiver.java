package com.hideaki.kk_reminder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.StartupReceiver.getDynamicContext;
import static com.hideaki.kk_reminder.StartupReceiver.getIsDirectBootContext;
import static com.hideaki.kk_reminder.UtilClass.ACTION_IN_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.CHILD_NOTIFICATION_ID;
import static com.hideaki.kk_reminder.UtilClass.CREATED;
import static com.hideaki.kk_reminder.UtilClass.DESTROYED;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.NOTIFICATION_ID_TABLE;
import static com.hideaki.kk_reminder.UtilClass.PARENT_NOTIFICATION_ID;
import static com.hideaki.kk_reminder.UtilClass.STRING_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.STRING_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.copyDatabase;
import static com.hideaki.kk_reminder.UtilClass.copySharedPreferences;
import static com.hideaki.kk_reminder.UtilClass.deserialize;
import static com.hideaki.kk_reminder.UtilClass.serialize;

public class DoneReceiver extends BroadcastReceiver {

  private static Calendar tmp;
  private DBAccessor accessor = null;
  private Context context;

  @Override
  public void onReceive(Context context, Intent intent) {

    this.context = context;
    Item item = (Item)deserialize(intent.getByteArrayExtra(ITEM));
    checkNotNull(item);

    accessor = new DBAccessor(getDynamicContext(context), getIsDirectBootContext(context));

    // 通知を既読する
    SharedPreferences stringPreferences = getDynamicContext(context).getSharedPreferences(
        getIsDirectBootContext(context) ? STRING_GENERAL_COPY : STRING_GENERAL,
        MODE_PRIVATE
    );
    Set<String> id_table =
        stringPreferences.getStringSet(NOTIFICATION_ID_TABLE, new TreeSet<String>());
    checkNotNull(id_table);
    int parent_id = intent.getIntExtra(PARENT_NOTIFICATION_ID, 0);
    int child_id = intent.getIntExtra(CHILD_NOTIFICATION_ID, 0);
    NotificationManager manager =
        (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
    checkNotNull(manager);

    for(int i = 1; i <= child_id; i++) {
      manager.cancel(parent_id + i);
    }
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.deleteNotificationChannel(String.valueOf(item.getId()));
    }
    id_table.remove(Integer.toBinaryString(parent_id));
    stringPreferences
        .edit()
        .putStringSet(NOTIFICATION_ID_TABLE, id_table)
        .apply();

    if(!getIsDirectBootContext(context)) {
      copySharedPreferences(context, false);
    }

    // 繰り返し処理
    if(item.getTime_altered() == 0) {
      item.setOrg_date((Calendar)item.getDate().clone());
    }
    else {
      item.setDate((Calendar)item.getOrg_date().clone());
    }

    if((item.getMinuteRepeat().getWhich_setted() & 1) != 0
        && item.getMinuteRepeat().getCount() == 0) {

      item.setDate((Calendar)item.getOrg_date2().clone());
    }
    else if((item.getMinuteRepeat().getWhich_setted() & (1 << 1)) != 0
        && item.getMinuteRepeat().getInterval() > item.getMinuteRepeat().getDuration()) {

      item.setDate((Calendar)item.getOrg_date2().clone());
    }

    boolean in_minute_repeat = false;
    if((item.getMinuteRepeat().getWhich_setted() & 1) != 0) {
      item.getMinuteRepeat().setOrg_count2(item.getMinuteRepeat().getCount());
    }
    else if((item.getMinuteRepeat().getWhich_setted() & (1 << 1)) != 0) {
      item.getMinuteRepeat().setOrg_duration2(item.getMinuteRepeat().getDuration());
    }

    Calendar now;
    int day_of_week;
    int month;

    if((item.getMinuteRepeat().getWhich_setted() & 1) != 0 &&
        item.getMinuteRepeat().getCount() != 0) {

      // countリピート設定時
      in_minute_repeat = true;
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);

      if(item.getMinuteRepeat().getCount() == item.getMinuteRepeat().getOrg_count()) {
        item.setOrg_date2((Calendar)item.getOrg_date().clone());
      }

      if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
        tmp = (Calendar)item.getDate().clone();
      }
      else {
        tmp = (Calendar)now.clone();
      }
      tmp.set(
          Calendar.HOUR_OF_DAY,
          tmp.get(Calendar.HOUR_OF_DAY) + item.getMinuteRepeat().getHour()
      );
      tmp.add(Calendar.MINUTE, item.getMinuteRepeat().getMinute());

      item.setOrg_alarm_stopped(item.isAlarm_stopped());
      item.setOrg_time_altered(item.getTime_altered());
      item.setTime_altered(0);
      item.setAlarm_stopped(false);
      item.getMinuteRepeat().setCount(item.getMinuteRepeat().getCount() - 1);
    }
    else if((item.getMinuteRepeat().getWhich_setted() & (1 << 1)) != 0
        && item.getMinuteRepeat().getInterval() <= item.getMinuteRepeat().getDuration()) {

      // durationリピート設定時
      in_minute_repeat = true;
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);

      if(item.getMinuteRepeat().getDuration() == item.getMinuteRepeat().getOrgDuration()) {
        item.setOrg_date2((Calendar)item.getOrg_date().clone());
      }

      if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
        tmp = (Calendar)item.getDate().clone();
      }
      else {
        tmp = (Calendar)now.clone();
      }
      tmp.set(
          Calendar.HOUR_OF_DAY,
          tmp.get(Calendar.HOUR_OF_DAY) + item.getMinuteRepeat().getHour()
      );
      tmp.add(Calendar.MINUTE, item.getMinuteRepeat().getMinute());
      item.setOrg_alarm_stopped(item.isAlarm_stopped());
      item.setOrg_time_altered(item.getTime_altered());
      item.setTime_altered(0);
      item.setAlarm_stopped(false);
      item.getMinuteRepeat().setDuration(
          item.getMinuteRepeat().getDuration() - item.getMinuteRepeat().getInterval()
      );
    }
    else if((item.getDayRepeat().getSetted() & 1) != 0) {

      // Dayリピート設定時
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);

      if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
        tmp = (Calendar)item.getDate().clone();
        tmp.add(Calendar.DAY_OF_MONTH, item.getDayRepeat().getInterval());
      }
      else {
        tmp = (Calendar)now.clone();
        tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
        tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));

        if(tmp.compareTo(now) <= 0) {
          tmp.add(Calendar.DAY_OF_MONTH, item.getDayRepeat().getInterval());
        }
      }
    }
    else if((item.getDayRepeat().getSetted() & (1 << 1)) != 0) {

      // Weekリピート設定時
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);
      int day_of_week_last;

      if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
        tmp = (Calendar)item.getDate().clone();
        day_of_week = item.getDate().get(Calendar.DAY_OF_WEEK) < 2 ?
            item.getDate().get(Calendar.DAY_OF_WEEK) + 5 :
            item.getDate().get(Calendar.DAY_OF_WEEK) - 2;

        // intervalの処理
        day_of_week_last = Integer.toBinaryString(item.getDayRepeat().getWeek()).length() - 1;
        if(day_of_week >= day_of_week_last) {
          tmp.add(Calendar.DAY_OF_MONTH, (item.getDayRepeat().getInterval() - 1) * 7);
        }

        int i = 1;
        while(i < 7 - day_of_week + 1) {
          if((item.getDayRepeat().getWeek() & (1 << (day_of_week + i))) != 0) {
            tmp.add(Calendar.DAY_OF_MONTH, i);

            break;
          }
          i++;

          if(i >= 7 - day_of_week + 1) {
            i = 0;
            tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            if(day_of_week != 6) {
              tmp.add(Calendar.DAY_OF_MONTH, 7);
            }
            day_of_week = 0;
          }
        }
      }
      else {
        tmp = (Calendar)now.clone();
        tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
        tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
        tmp.set(Calendar.SECOND, 0);
        tmp.set(Calendar.MILLISECOND, 0);
        day_of_week = now.get(Calendar.DAY_OF_WEEK) < 2 ?
            now.get(Calendar.DAY_OF_WEEK) + 5 : now.get(Calendar.DAY_OF_WEEK) - 2;

        // intervalの処理
        day_of_week_last = Integer.toBinaryString(item.getDayRepeat().getWeek()).length() - 1;
        if(day_of_week > day_of_week_last) {
          tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
          if(tmp.after(now)) {
            tmp.add(Calendar.DAY_OF_MONTH, -7);
          }
          tmp.add(Calendar.DAY_OF_MONTH, (item.getDayRepeat().getInterval()) * 7);
          day_of_week = 0;
        }
        else if(day_of_week == day_of_week_last) {
          if(tmp.before(now)) {
            tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            if(tmp.after(now)) {
              tmp.add(Calendar.DAY_OF_MONTH, -7);
            }
            tmp.add(Calendar.DAY_OF_MONTH, (item.getDayRepeat().getInterval()) * 7);
            day_of_week = 0;
          }
        }

        int i = 0;
        while(i < 7 - day_of_week) {
          if((item.getDayRepeat().getWeek() & (1 << (day_of_week + i))) != 0) {
            tmp.add(Calendar.DAY_OF_MONTH, i);

            if(tmp.after(now)) {
              break;
            }
          }
          i++;

          if(i >= 7 - day_of_week) {
            i = 0;
            tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            if(tmp.after(now)) {
              tmp.add(Calendar.DAY_OF_MONTH, -7);
            }
            tmp.add(Calendar.DAY_OF_MONTH, (item.getDayRepeat().getInterval()) * 7);
            day_of_week = 0;
          }
        }
      }
    }
    else if((item.getDayRepeat().getSetted() & (1 << 2)) != 0) {

      // Monthリピート設定時
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);

      if(item.getDayRepeat().isDays_of_month_setted()) {

        // DaysOfMonthリピート設定時
        int day_of_month;
        int day_of_month_last;

        if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
          tmp = (Calendar)item.getDate().clone();
          day_of_month = item.getDate().get(Calendar.DAY_OF_MONTH);

          // intervalの処理
          day_of_month_last =
              Integer.toBinaryString(item.getDayRepeat().getDays_of_month()).length();
          if(day_of_month_last > tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            day_of_month_last = tmp.getActualMaximum(Calendar.DAY_OF_MONTH);
          }
          if(tmp.get(Calendar.DAY_OF_MONTH) >= day_of_month_last) {
            tmp.set(Calendar.DAY_OF_MONTH, 1);
            tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
            day_of_month = 1;
          }

          int i = 0;
          while(i < 31 - day_of_month + 1) {
            if((item.getDayRepeat().getDays_of_month() & (1 << (day_of_month - 1 + i))) != 0) {
              if((day_of_month - 1 + i) >= tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                tmp.set(Calendar.DAY_OF_MONTH, tmp.getActualMaximum(Calendar.DAY_OF_MONTH));
              }
              else {
                tmp.add(Calendar.DAY_OF_MONTH, i);
              }

              if(tmp.after(item.getDate())) {
                break;
              }
            }
            i++;

            if(i >= 31 - day_of_month + 1) {
              i = 0;
              tmp.set(Calendar.DAY_OF_MONTH, 1);
              tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
              day_of_month = 1;
            }
          }
        }
        else {
          tmp = (Calendar)now.clone();
          tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
          tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
          tmp.set(Calendar.SECOND, 0);
          tmp.set(Calendar.MILLISECOND, 0);
          day_of_month = now.get(Calendar.DAY_OF_MONTH);

          // intervalの処理
          day_of_month_last =
              Integer.toBinaryString(item.getDayRepeat().getDays_of_month()).length();
          if(day_of_month_last > tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            day_of_month_last = tmp.getActualMaximum(Calendar.DAY_OF_MONTH);
          }
          if(tmp.get(Calendar.DAY_OF_MONTH) > day_of_month_last) {
            tmp.set(Calendar.DAY_OF_MONTH, 1);
            tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
            day_of_month = 1;
          }
          else if(tmp.get(Calendar.DAY_OF_MONTH) == day_of_month_last) {
            if(tmp.before(now)) {
              tmp.set(Calendar.DAY_OF_MONTH, 1);
              tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
              day_of_month = 1;
            }
          }

          int i = 0;
          while(i < 31 - day_of_month + 1) {
            if((item.getDayRepeat().getDays_of_month() & (1 << (day_of_month - 1 + i))) != 0) {
              if((day_of_month - 1 + i) >= tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                tmp.set(Calendar.DAY_OF_MONTH, tmp.getActualMaximum(Calendar.DAY_OF_MONTH));
              }
              else {
                tmp.add(Calendar.DAY_OF_MONTH, i);
              }

              if(tmp.after(now)) {
                break;
              }
            }
            i++;

            if(i >= 31 - day_of_month + 1) {
              i = 0;
              tmp.set(Calendar.DAY_OF_MONTH, 1);
              tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
              day_of_month = 1;
            }
          }
        }
      }
      else {

        // OnTheMonthリピート設定時
        boolean match_to_ordinal_num;
        Calendar tmp2;
        Calendar tmp3;
        now = Calendar.getInstance();
        if(now.get(Calendar.SECOND) >= 30) {
          now.add(Calendar.MINUTE, 1);
        }
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        if(item.getDayRepeat().getOn_the_month().ordinal() < 6) {
          day_of_week = item.getDayRepeat().getOn_the_month().ordinal() + 2;
        }
        else if(item.getDayRepeat().getOn_the_month().ordinal() == 6) {
          day_of_week = 1;
        }
        else {
          day_of_week = item.getDayRepeat().getOn_the_month().ordinal() + 1;
        }

        if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
          // clone()で渡して不具合が出る場合はsetTimeInMillis()を使う
          tmp = (Calendar)item.getDate().clone();

          if(day_of_week < 8) {
            month = tmp.get(Calendar.MONTH);
            tmp2 = (Calendar)tmp.clone();
            tmp2.set(Calendar.DAY_OF_WEEK, day_of_week);
            tmp3 = (Calendar)tmp2.clone();
            tmp2.add(Calendar.MONTH, 1);
            tmp3.add(Calendar.MONTH, -1);
            if(tmp2.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, 7);
            }
            else if(tmp3.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, -7);
            }

            tmp.set(Calendar.DAY_OF_WEEK, day_of_week);

            while(true) {

              // intervalの処理
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinal_number()) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, day_of_week);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }
              }

              while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinal_number()
                  && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                  tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, 7);
              }

              if(tmp.after(item.getDate())) {
                break;
              }
              else {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, day_of_week);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }
              }
            }
          }
          else if(day_of_week == 8) {
            month = tmp.get(Calendar.MONTH);
            tmp2 = (Calendar)tmp.clone();
            tmp2.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            tmp3 = (Calendar)tmp2.clone();
            tmp2.add(Calendar.MONTH, 1);
            tmp3.add(Calendar.MONTH, -1);
            if(tmp2.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, 7);
            }
            else if(tmp3.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, -7);
            }

            while(true) {
              tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

              // intervalの処理
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinal_number()) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }

                month = tmp.get(Calendar.MONTH);
              }

              while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinal_number()
                  && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                  tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, 7);
              }

              tmp.add(Calendar.DAY_OF_MONTH, item.getDayRepeat().getWeekday_num());
              item.getDayRepeat().setWeekday_num(item.getDayRepeat().getWeekday_num() + 1);

              if(tmp.after(item.getDate()) && month == tmp.get(Calendar.MONTH)) {
                item.getDayRepeat().setWeekday_num(0);
                break;
              }
              else if(item.getDayRepeat().getWeekday_num() > 4 ||
                  month != tmp.get(Calendar.MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, -item.getDayRepeat().getWeekday_num() + 1);
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }

                month = tmp.get(Calendar.MONTH);
                item.getDayRepeat().setWeekday_num(0);
              }
            }
          }
          else if(day_of_week == 9) {
            month = tmp.get(Calendar.MONTH);
            tmp2 = (Calendar)tmp.clone();
            tmp2.add(Calendar.DAY_OF_MONTH, 1);

            match_to_ordinal_num = false;
            if(item.getDayRepeat().getOrdinal_number() == 5) {
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                  tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                match_to_ordinal_num = true;
              }
            }
            else {
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                  item.getDayRepeat().getOrdinal_number()) {
                match_to_ordinal_num = true;
              }
            }

            if(match_to_ordinal_num && tmp.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                && tmp2.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, 1);
            }
            else {
              month = tmp.get(Calendar.MONTH);
              tmp2 = (Calendar)tmp.clone();
              tmp2.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
              tmp3 = (Calendar)tmp2.clone();
              tmp2.add(Calendar.MONTH, 1);
              tmp3.add(Calendar.MONTH, -1);
              if(tmp2.get(Calendar.MONTH) == month) {
                tmp.add(Calendar.DAY_OF_MONTH, 7);
              }
              else if(tmp3.get(Calendar.MONTH) == month) {
                tmp.add(Calendar.DAY_OF_MONTH, -7);
              }

              tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);

              while(true) {

                // intervalの処理
                if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) >
                    item.getDayRepeat().getOrdinal_number()) {
                  tmp.set(Calendar.DAY_OF_MONTH, 1);
                  tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                  tmp2 = (Calendar)tmp.clone();

                  tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                  if(tmp.before(tmp2)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }
                }

                while(
                    tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinal_number()
                        && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                        tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }

                if(tmp.after(item.getDate())) {
                  break;
                }
                else {
                  tmp.set(Calendar.DAY_OF_MONTH, 1);
                  tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                  tmp2 = (Calendar)tmp.clone();

                  tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                  if(tmp.before(tmp2)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }
                }
              }
            }
          }
        }
        else {
          tmp = (Calendar)now.clone();
          tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
          tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
          tmp.set(Calendar.SECOND, 0);
          tmp.set(Calendar.MILLISECOND, 0);

          if(day_of_week < 8) {
            month = tmp.get(Calendar.MONTH);
            tmp2 = (Calendar)tmp.clone();
            tmp2.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
            tmp3 = (Calendar)tmp2.clone();
            tmp2.add(Calendar.MONTH, 1);
            tmp3.add(Calendar.MONTH, -1);
            if(tmp2.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, 7);
            }
            else if(tmp3.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, -7);
            }

            tmp.set(Calendar.DAY_OF_WEEK, day_of_week);

            while(true) {

              // intervalの処理
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinal_number()) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, day_of_week);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }
              }

              while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinal_number()
                  && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                  tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, 7);
              }

              if(tmp.after(now)) {
                break;
              }
              else {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, day_of_week);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }
              }
            }
          }
          else if(day_of_week == 8) {
            month = tmp.get(Calendar.MONTH);
            tmp2 = (Calendar)tmp.clone();
            tmp2.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
            tmp3 = (Calendar)tmp2.clone();
            tmp2.add(Calendar.MONTH, 1);
            tmp3.add(Calendar.MONTH, -1);
            if(tmp2.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, 7);
            }
            else if(tmp3.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, -7);
            }

            while(true) {
              tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

              // intervalの処理
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinal_number()) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }

                month = tmp.get(Calendar.MONTH);
              }

              while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinal_number()
                  && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                  tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, 7);
              }

              tmp.add(Calendar.DAY_OF_MONTH, item.getDayRepeat().getWeekday_num());
              item.getDayRepeat().setWeekday_num(item.getDayRepeat().getWeekday_num() + 1);

              if(tmp.after(now) && month == tmp.get(Calendar.MONTH)) {
                item.getDayRepeat().setWeekday_num(0);
                break;
              }
              else if(item.getDayRepeat().getWeekday_num() > 4 ||
                  month != tmp.get(Calendar.MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, -item.getDayRepeat().getWeekday_num() + 1);
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }

                month = tmp.get(Calendar.MONTH);
                item.getDayRepeat().setWeekday_num(0);
              }
            }
          }
          else if(day_of_week == 9) {
            boolean sunday_match = false;

            if(tmp.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && tmp.after(now)) {
              tmp2 = (Calendar)tmp.clone();
              tmp2.add(Calendar.DAY_OF_MONTH, -1);

              if(tmp2.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                  item.getDayRepeat().getOrdinal_number()) {
                sunday_match = true;
              }
            }

            if(!sunday_match) {

              month = tmp.get(Calendar.MONTH);
              tmp2 = (Calendar)tmp.clone();
              tmp2.add(Calendar.DAY_OF_MONTH, 1);

              match_to_ordinal_num = false;
              if(item.getDayRepeat().getOrdinal_number() == 5) {
                if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                    tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                  match_to_ordinal_num = true;
                }
              }
              else {
                if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                    item.getDayRepeat().getOrdinal_number()) {
                  match_to_ordinal_num = true;
                }
              }

              if(match_to_ordinal_num && tmp.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                  && tmp2.get(Calendar.MONTH) == month && tmp.before(now)) {
                tmp.add(Calendar.DAY_OF_MONTH, 1);
              }
              else {
                month = tmp.get(Calendar.MONTH);
                tmp2 = (Calendar)tmp.clone();
                tmp2.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                tmp3 = (Calendar)tmp2.clone();
                tmp2.add(Calendar.MONTH, 1);
                tmp3.add(Calendar.MONTH, -1);
                if(tmp2.get(Calendar.MONTH) == month) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }
                else if(tmp3.get(Calendar.MONTH) == month) {
                  tmp.add(Calendar.DAY_OF_MONTH, -7);
                }

                tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);

                while(true) {

                  // intervalの処理
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) >
                      item.getDayRepeat().getOrdinal_number()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }
                  }

                  while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                      item.getDayRepeat().getOrdinal_number()
                      && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                      tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }

                  if(tmp.after(now)) {
                    break;
                  }
                  else {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    else if((item.getDayRepeat().getSetted() & (1 << 3)) != 0) {

      // Yearリピート設定時
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);
      int month_last;

      if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
        tmp = (Calendar)item.getDate().clone();
        month = item.getDate().get(Calendar.MONTH);

        // intervalの処理
        month_last = Integer.toBinaryString(item.getDayRepeat().getYear()).length() - 1;
        if(month >= month_last) {
          tmp.set(Calendar.MONTH, 0);
          tmp.add(Calendar.YEAR, item.getDayRepeat().getInterval());
          month = 0;
        }

        int i = 0;
        while(i < 12 - month) {
          if((item.getDayRepeat().getYear() & (1 << (month + i))) != 0) {
            tmp.add(Calendar.MONTH, i);
            if(tmp.get(Calendar.DAY_OF_MONTH) < item.getDayRepeat().getDay_of_month_of_year()
                && item.getDayRepeat().getDay_of_month_of_year() <=
                tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
              tmp.set(Calendar.DAY_OF_MONTH, item.getDayRepeat().getDay_of_month_of_year());
            }

            if(tmp.after(item.getDate())) {
              break;
            }
          }
          i++;

          if(i >= 12 - month) {
            i = 0;
            tmp.set(Calendar.MONTH, 0);
            tmp.add(Calendar.YEAR, item.getDayRepeat().getInterval());
            month = 0;
          }
        }
      }
      else {
        tmp = (Calendar)now.clone();
        // itemに登録されている日にちが今月の日にちの最大値を超えている場合、今月の日にちの最大値を設定する
        if(tmp.getActualMaximum(Calendar.DAY_OF_MONTH) <
            item.getDate().get(Calendar.DAY_OF_MONTH)) {
          tmp.set(Calendar.DAY_OF_MONTH, tmp.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        else {
          tmp.set(Calendar.DAY_OF_MONTH, item.getDate().get(Calendar.DAY_OF_MONTH));
        }
        tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
        tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
        tmp.set(Calendar.SECOND, 0);
        tmp.set(Calendar.MILLISECOND, 0);
        month = now.get(Calendar.MONTH);

        // intervalの処理
        month_last = Integer.toBinaryString(item.getDayRepeat().getYear()).length() - 1;
        if(month > month_last) {
          tmp.set(Calendar.MONTH, 0);
          tmp.add(Calendar.YEAR, item.getDayRepeat().getInterval());
          month = 0;
        }
        else if(month == month_last) {
          if(tmp.before(now)) {
            tmp.set(Calendar.MONTH, 0);
            tmp.add(Calendar.YEAR, item.getDayRepeat().getInterval());
            month = 0;
          }
        }

        int i = 0;
        while(i < 12 - month) {
          if((item.getDayRepeat().getYear() & (1 << (month + i))) != 0) {
            tmp.add(Calendar.MONTH, i);
            if(tmp.get(Calendar.DAY_OF_MONTH) < item.getDayRepeat().getDay_of_month_of_year()
                && item.getDayRepeat().getDay_of_month_of_year() <=
                tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
              tmp.set(Calendar.DAY_OF_MONTH, item.getDayRepeat().getDay_of_month_of_year());
            }

            if(tmp.after(now)) {
              break;
            }
          }
          i++;

          if(i >= 12 - month) {
            i = 0;
            tmp.set(Calendar.MONTH, 0);
            tmp.add(Calendar.YEAR, item.getDayRepeat().getInterval());
            month = 0;
          }
        }
      }
    }

    if(!in_minute_repeat) {
      if((item.getMinuteRepeat().getWhich_setted() & 1) != 0
          && item.getMinuteRepeat().getCount() == 0) {

        if(item.getDayRepeat().getSetted() != 0) {
          item.getMinuteRepeat().setCount(item.getMinuteRepeat().getOrg_count());
        }
        else {
          item.getMinuteRepeat().setWhich_setted(0);
        }
      }
      else if((item.getMinuteRepeat().getWhich_setted() & (1 << 1)) != 0
          && item.getMinuteRepeat().getInterval() > item.getMinuteRepeat().getDuration()) {

        if(item.getDayRepeat().getSetted() != 0) {
          item.getMinuteRepeat().setDuration(item.getMinuteRepeat().getOrgDuration());
        }
        else {
          item.getMinuteRepeat().setWhich_setted(0);
        }
      }
    }


    // tmp設定後の処理
    if(item.getDayRepeat().getSetted() != 0 || item.getMinuteRepeat().getWhich_setted() != 0) {
      if(!in_minute_repeat) {
        item.setOrg_alarm_stopped(item.isAlarm_stopped());
        item.setOrg_time_altered(item.getTime_altered());
        if(item.isAlarm_stopped()) {
          item.setAlarm_stopped(false);
        }
        if(item.getTime_altered() != 0) {
          item.setTime_altered(0);
        }
      }
      item.setDate((Calendar)tmp.clone());

      deleteAlarm(item);
      setAlarm(item);
      updateDB(item, MyDatabaseHelper.TODO_TABLE);
    }
    else {
      item.setDoneDate(Calendar.getInstance());

      deleteAlarm(item);
      deleteDB(item, MyDatabaseHelper.TODO_TABLE);
      insertDB(item, MyDatabaseHelper.DONE_TABLE);
    }

    // データベースを端末暗号化ストレージへコピーする
    if(!getIsDirectBootContext(context)) {
      copyDatabase(context, false);
    }

    SharedPreferences intPreferences = getDynamicContext(context).getSharedPreferences(
        getIsDirectBootContext(context) ? INT_GENERAL_COPY : INT_GENERAL,
        MODE_PRIVATE
    );
    int created = intPreferences.getInt(CREATED, -1);
    int destroyed = intPreferences.getInt(DESTROYED, -1);
    if(created > destroyed) {
      context.sendBroadcast(new Intent(ACTION_IN_NOTIFICATION));
    }
  }

  public void setAlarm(Item item) {

    if(item.getDate().getTimeInMillis() > System.currentTimeMillis() &&
        item.getWhich_list_belongs() == 0) {
      item.getNotify_interval().setTime(item.getNotify_interval().getOrg_time());
      Intent intent = new Intent(context, AlarmReceiver.class);
      byte[] ob_array = serialize(item);
      intent.putExtra(ITEM, ob_array);
      PendingIntent sender = PendingIntent.getBroadcast(
          context, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      checkNotNull(alarmManager);

      alarmManager.setAlarmClock(
          new AlarmManager.AlarmClockInfo(item.getDate().getTimeInMillis(), null), sender);
    }
  }

  public void deleteAlarm(Item item) {

    if(isAlarmSetted(item)) {
      Intent intent = new Intent(context, AlarmReceiver.class);
      PendingIntent sender = PendingIntent.getBroadcast(
          context, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      checkNotNull(alarmManager);

      alarmManager.cancel(sender);
      sender.cancel();
    }
  }

  public boolean isAlarmSetted(Item item) {

    Intent intent = new Intent(context, AlarmReceiver.class);
    PendingIntent sender = PendingIntent.getBroadcast(
        context, (int)item.getId(), intent, PendingIntent.FLAG_NO_CREATE);

    return sender != null;
  }

  public void insertDB(Item item, String table) {

    accessor.executeInsert(item.getId(), serialize(item), table);
  }

  public void updateDB(Item item, String table) {

    accessor.executeUpdate(item.getId(), serialize(item), table);
  }

  public void deleteDB(Item item, String table) {

    accessor.executeDelete(item.getId(), table);
  }
}
