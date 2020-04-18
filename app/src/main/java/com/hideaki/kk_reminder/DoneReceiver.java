package com.hideaki.kk_reminder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.hideaki.kk_reminder.StartupReceiver.getDynamicContext;
import static com.hideaki.kk_reminder.StartupReceiver.getIsDirectBootContext;
import static com.hideaki.kk_reminder.UtilClass.ACTION_IN_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.CHANNEL_ID;
import static com.hideaki.kk_reminder.UtilClass.CHILD_NOTIFICATION_ID;
import static com.hideaki.kk_reminder.UtilClass.CREATED;
import static com.hideaki.kk_reminder.UtilClass.DESTROYED;
import static com.hideaki.kk_reminder.UtilClass.HOUR;
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
import static java.util.Objects.requireNonNull;

public class DoneReceiver extends BroadcastReceiver {

  private static Calendar tmp;
  private DBAccessor accessor = null;
  private Context context;

  @Override
  public void onReceive(Context context, Intent intent) {

    this.context = context;
    ItemAdapter item = new ItemAdapter(deserialize(intent.getByteArrayExtra(ITEM)));
    requireNonNull(item);

    accessor = new DBAccessor(getDynamicContext(context), getIsDirectBootContext(context));

    // 通知を既読する
    SharedPreferences stringPreferences = getDynamicContext(context).getSharedPreferences(
      getIsDirectBootContext(context) ? STRING_GENERAL_COPY : STRING_GENERAL,
      MODE_PRIVATE
    );
    Set<String> idTable =
      stringPreferences.getStringSet(NOTIFICATION_ID_TABLE, new TreeSet<String>());
    requireNonNull(idTable);
    int parentId = intent.getIntExtra(PARENT_NOTIFICATION_ID, 0);
    int childId = intent.getIntExtra(CHILD_NOTIFICATION_ID, 0);
    String channelId = intent.getStringExtra(CHANNEL_ID);
    NotificationManager manager =
      (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
    requireNonNull(manager);

    for(int i = 1; i <= childId; i++) {
      manager.cancel(parentId + i);
    }
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.deleteNotificationChannel(channelId);
    }
    idTable.remove(Integer.toBinaryString(parentId));
    stringPreferences
      .edit()
      .putStringSet(NOTIFICATION_ID_TABLE, idTable)
      .apply();

    if(!getIsDirectBootContext(context)) {
      copySharedPreferences(context, false);
    }

    // 繰り返し処理
    if(item.getAlteredTime() == 0) {
      item.setOrgDate((Calendar)item.getDate().clone());
    }
    else {
      item.setDate((Calendar)item.getOrgDate().clone());
    }

    if((item.getMinuteRepeat().getWhichSet() & 1) != 0
      && item.getMinuteRepeat().getCount() == 0) {

      item.setDate((Calendar)item.getOrgDate2().clone());
    }
    else if((item.getMinuteRepeat().getWhichSet() & (1 << 1)) != 0
      && item.getMinuteRepeat().getInterval() > item.getMinuteRepeat().getDuration()) {

      item.setDate((Calendar)item.getOrgDate2().clone());
    }

    boolean inMinuteRepeat = false;
    if((item.getMinuteRepeat().getWhichSet() & 1) != 0) {
      item.getMinuteRepeat().setOrgCount2(item.getMinuteRepeat().getCount());
    }
    else if((item.getMinuteRepeat().getWhichSet() & (1 << 1)) != 0) {
      item.getMinuteRepeat().setOrgDuration2(item.getMinuteRepeat().getDuration());
    }

    Calendar now;
    int dayOfWeek;
    int month;

    if((item.getMinuteRepeat().getWhichSet() & 1) != 0 &&
      item.getMinuteRepeat().getCount() != 0) {

      // countリピート設定時
      inMinuteRepeat = true;
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);

      if(item.getMinuteRepeat().getCount() == item.getMinuteRepeat().getOrgCount()) {
        item.setOrgDate2((Calendar)item.getOrgDate().clone());
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

      item.setOrgIsAlarmStopped(item.isAlarmStopped());
      item.setOrgAlteredTime(item.getAlteredTime());
      item.setAlteredTime(0);
      item.setAlarmStopped(false);
      item.getMinuteRepeat().setCount(item.getMinuteRepeat().getCount() - 1);
    }
    else if((item.getMinuteRepeat().getWhichSet() & (1 << 1)) != 0
      && item.getMinuteRepeat().getInterval() <= item.getMinuteRepeat().getDuration()) {

      // durationリピート設定時
      inMinuteRepeat = true;
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);

      if(item.getMinuteRepeat().getDuration() == item.getMinuteRepeat().getOrgDuration()) {
        item.setOrgDate2((Calendar)item.getOrgDate().clone());
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
      item.setOrgIsAlarmStopped(item.isAlarmStopped());
      item.setOrgAlteredTime(item.getAlteredTime());
      item.setAlteredTime(0);
      item.setAlarmStopped(false);
      item.getMinuteRepeat().setDuration(
        item.getMinuteRepeat().getDuration() - item.getMinuteRepeat().getInterval()
      );
    }
    else if((item.getDayRepeat().getWhichSet() & 1) != 0) {

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
        Calendar itemDateTmp = (Calendar)item.getDate().clone();
        itemDateTmp.set(Calendar.HOUR_OF_DAY, 0);
        itemDateTmp.set(Calendar.MINUTE, 0);
        Calendar nowDateTmp = (Calendar)now.clone();
        nowDateTmp.set(Calendar.HOUR_OF_DAY, 0);
        nowDateTmp.set(Calendar.MINUTE, 0);
        long diff = nowDateTmp.getTimeInMillis() - itemDateTmp.getTimeInMillis();
        int dayDiff = (int)(diff / (24 * HOUR));
        int interval = item.getDayRepeat().getInterval();
        int remainder = dayDiff % interval;
        tmp = (Calendar)now.clone();
        tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
        tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
        if(remainder == 0) {
          if(tmp.compareTo(now) <= 0) {
            tmp.add(Calendar.DAY_OF_MONTH, interval);
          }
        }
        else {
          tmp.add(Calendar.DAY_OF_MONTH, interval - remainder);
        }
      }
    }
    else if((item.getDayRepeat().getWhichSet() & (1 << 1)) != 0) {

      // Weekリピート設定時
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);
      int dayOfWeekLast;

      if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
        tmp = (Calendar)item.getDate().clone();
        dayOfWeek = item.getDate().get(Calendar.DAY_OF_WEEK) < 2 ?
          item.getDate().get(Calendar.DAY_OF_WEEK) + 5 :
          item.getDate().get(Calendar.DAY_OF_WEEK) - 2;

        // intervalの処理
        dayOfWeekLast = Integer.toBinaryString(item.getDayRepeat().getWeek()).length() - 1;
        if(dayOfWeek >= dayOfWeekLast) {
          tmp.add(Calendar.DAY_OF_MONTH, (item.getDayRepeat().getInterval() - 1) * 7);
        }

        int i = 1;
        while(i < 7 - dayOfWeek + 1) {
          if((item.getDayRepeat().getWeek() & (1 << (dayOfWeek + i))) != 0) {
            tmp.add(Calendar.DAY_OF_MONTH, i);

            break;
          }
          i++;

          if(i >= 7 - dayOfWeek + 1) {
            i = 0;
            tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            if(dayOfWeek != 6) {
              tmp.add(Calendar.DAY_OF_MONTH, 7);
            }
            dayOfWeek = 0;
          }
        }
      }
      else {
        tmp = (Calendar)now.clone();
        tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
        tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
        tmp.set(Calendar.SECOND, 0);
        tmp.set(Calendar.MILLISECOND, 0);
        dayOfWeek = now.get(Calendar.DAY_OF_WEEK) < 2 ?
          now.get(Calendar.DAY_OF_WEEK) + 5 : now.get(Calendar.DAY_OF_WEEK) - 2;

        // intervalの処理
        dayOfWeekLast = Integer.toBinaryString(item.getDayRepeat().getWeek()).length() - 1;
        if(dayOfWeek > dayOfWeekLast) {
          tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
          if(tmp.after(now)) {
            tmp.add(Calendar.DAY_OF_MONTH, -7);
          }
          tmp.add(Calendar.DAY_OF_MONTH, (item.getDayRepeat().getInterval()) * 7);
          dayOfWeek = 0;
        }
        else if(dayOfWeek == dayOfWeekLast) {
          if(tmp.before(now)) {
            tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            if(tmp.after(now)) {
              tmp.add(Calendar.DAY_OF_MONTH, -7);
            }
            tmp.add(Calendar.DAY_OF_MONTH, (item.getDayRepeat().getInterval()) * 7);
            dayOfWeek = 0;
          }
        }

        int i = 0;
        while(i < 7 - dayOfWeek) {
          if((item.getDayRepeat().getWeek() & (1 << (dayOfWeek + i))) != 0) {
            tmp.add(Calendar.DAY_OF_MONTH, i);

            if(tmp.after(now)) {
              break;
            }
          }
          i++;

          if(i >= 7 - dayOfWeek) {
            i = 0;
            tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            if(tmp.after(now)) {
              tmp.add(Calendar.DAY_OF_MONTH, -7);
            }
            tmp.add(Calendar.DAY_OF_MONTH, (item.getDayRepeat().getInterval()) * 7);
            dayOfWeek = 0;
          }
        }
      }
    }
    else if((item.getDayRepeat().getWhichSet() & (1 << 2)) != 0) {

      // Monthリピート設定時
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);

      if(item.getDayRepeat().isDaysOfMonthSet()) {

        // DaysOfMonthリピート設定時
        int dayOfMonth;
        int dayOfMonthLast;

        if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
          tmp = (Calendar)item.getDate().clone();
          dayOfMonth = item.getDate().get(Calendar.DAY_OF_MONTH);

          // intervalの処理
          dayOfMonthLast =
            Integer.toBinaryString(item.getDayRepeat().getDaysOfMonth()).length();
          if(dayOfMonthLast > tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            dayOfMonthLast = tmp.getActualMaximum(Calendar.DAY_OF_MONTH);
          }
          if(tmp.get(Calendar.DAY_OF_MONTH) >= dayOfMonthLast) {
            tmp.set(Calendar.DAY_OF_MONTH, 1);
            tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
            dayOfMonth = 1;
          }

          int i = 0;
          while(i < 31 - dayOfMonth + 1) {
            if((item.getDayRepeat().getDaysOfMonth() & (1 << (dayOfMonth - 1 + i))) != 0) {
              if((dayOfMonth - 1 + i) >= tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
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

            if(i >= 31 - dayOfMonth + 1) {
              i = 0;
              tmp.set(Calendar.DAY_OF_MONTH, 1);
              tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
              dayOfMonth = 1;
            }
          }
        }
        else {
          tmp = (Calendar)now.clone();
          tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
          tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
          tmp.set(Calendar.SECOND, 0);
          tmp.set(Calendar.MILLISECOND, 0);
          dayOfMonth = now.get(Calendar.DAY_OF_MONTH);

          // intervalの処理
          dayOfMonthLast =
            Integer.toBinaryString(item.getDayRepeat().getDaysOfMonth()).length();
          if(dayOfMonthLast > tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            dayOfMonthLast = tmp.getActualMaximum(Calendar.DAY_OF_MONTH);
          }
          if(tmp.get(Calendar.DAY_OF_MONTH) > dayOfMonthLast) {
            tmp.set(Calendar.DAY_OF_MONTH, 1);
            tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
            dayOfMonth = 1;
          }
          else if(tmp.get(Calendar.DAY_OF_MONTH) == dayOfMonthLast) {
            if(tmp.before(now)) {
              tmp.set(Calendar.DAY_OF_MONTH, 1);
              tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
              dayOfMonth = 1;
            }
          }

          int i = 0;
          while(i < 31 - dayOfMonth + 1) {
            if((item.getDayRepeat().getDaysOfMonth() & (1 << (dayOfMonth - 1 + i))) != 0) {
              if((dayOfMonth - 1 + i) >= tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
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

            if(i >= 31 - dayOfMonth + 1) {
              i = 0;
              tmp.set(Calendar.DAY_OF_MONTH, 1);
              tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
              dayOfMonth = 1;
            }
          }
        }
      }
      else {

        // OnTheMonthリピート設定時
        boolean matchToOrdinalNum;
        Calendar tmp2;
        Calendar tmp3;
        now = Calendar.getInstance();
        if(now.get(Calendar.SECOND) >= 30) {
          now.add(Calendar.MINUTE, 1);
        }
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        if(item.getDayRepeat().getOnTheMonth().ordinal() < 6) {
          dayOfWeek = item.getDayRepeat().getOnTheMonth().ordinal() + 2;
        }
        else if(item.getDayRepeat().getOnTheMonth().ordinal() == 6) {
          dayOfWeek = 1;
        }
        else {
          dayOfWeek = item.getDayRepeat().getOnTheMonth().ordinal() + 1;
        }

        if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
          // clone()で渡して不具合が出る場合はsetTimeInMillis()を使う
          tmp = (Calendar)item.getDate().clone();

          if(dayOfWeek < 8) {
            month = tmp.get(Calendar.MONTH);
            tmp2 = (Calendar)tmp.clone();
            tmp2.set(Calendar.DAY_OF_WEEK, dayOfWeek);
            tmp3 = (Calendar)tmp2.clone();
            tmp2.add(Calendar.MONTH, 1);
            tmp3.add(Calendar.MONTH, -1);
            if(tmp2.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, 7);
            }
            else if(tmp3.get(Calendar.MONTH) == month) {
              tmp.add(Calendar.DAY_OF_MONTH, -7);
            }

            tmp.set(Calendar.DAY_OF_WEEK, dayOfWeek);

            while(true) {

              // intervalの処理
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinalNumber()) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }
              }

              while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinalNumber()
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

                tmp.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }
              }
            }
          }
          else if(dayOfWeek == 8) {
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
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinalNumber()) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }

                month = tmp.get(Calendar.MONTH);
              }

              while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinalNumber()
                && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, 7);
              }

              tmp.add(Calendar.DAY_OF_MONTH, item.getDayRepeat().getWeekdayNum());
              item.getDayRepeat().setWeekdayNum(item.getDayRepeat().getWeekdayNum() + 1);

              if(tmp.after(item.getDate()) && month == tmp.get(Calendar.MONTH)) {
                item.getDayRepeat().setWeekdayNum(0);
                break;
              }
              else if(item.getDayRepeat().getWeekdayNum() > 4 ||
                month != tmp.get(Calendar.MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, -item.getDayRepeat().getWeekdayNum() + 1);
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }

                month = tmp.get(Calendar.MONTH);
                item.getDayRepeat().setWeekdayNum(0);
              }
            }
          }
          else if(dayOfWeek == 9) {
            month = tmp.get(Calendar.MONTH);
            tmp2 = (Calendar)tmp.clone();
            tmp2.add(Calendar.DAY_OF_MONTH, 1);

            matchToOrdinalNum = false;
            if(item.getDayRepeat().getOrdinalNumber() == 5) {
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                matchToOrdinalNum = true;
              }
            }
            else {
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                item.getDayRepeat().getOrdinalNumber()) {
                matchToOrdinalNum = true;
              }
            }

            if(matchToOrdinalNum && tmp.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
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
                  item.getDayRepeat().getOrdinalNumber()) {
                  tmp.set(Calendar.DAY_OF_MONTH, 1);
                  tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                  tmp2 = (Calendar)tmp.clone();

                  tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                  if(tmp.before(tmp2)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }
                }

                while(
                  tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinalNumber()
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

          if(dayOfWeek < 8) {
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

            tmp.set(Calendar.DAY_OF_WEEK, dayOfWeek);

            while(true) {

              // intervalの処理
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinalNumber()) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }
              }

              while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinalNumber()
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

                tmp.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }
              }
            }
          }
          else if(dayOfWeek == 8) {
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
              if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinalNumber()) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }

                month = tmp.get(Calendar.MONTH);
              }

              while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinalNumber()
                && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, 7);
              }

              tmp.add(Calendar.DAY_OF_MONTH, item.getDayRepeat().getWeekdayNum());
              item.getDayRepeat().setWeekdayNum(item.getDayRepeat().getWeekdayNum() + 1);

              if(tmp.after(now) && month == tmp.get(Calendar.MONTH)) {
                item.getDayRepeat().setWeekdayNum(0);
                break;
              }
              else if(item.getDayRepeat().getWeekdayNum() > 4 ||
                month != tmp.get(Calendar.MONTH)) {
                tmp.add(Calendar.DAY_OF_MONTH, -item.getDayRepeat().getWeekdayNum() + 1);
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                tmp2 = (Calendar)tmp.clone();

                tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                if(tmp.before(tmp2)) {
                  tmp.add(Calendar.DAY_OF_MONTH, 7);
                }

                month = tmp.get(Calendar.MONTH);
                item.getDayRepeat().setWeekdayNum(0);
              }
            }
          }
          else if(dayOfWeek == 9) {
            boolean sundayMatch = false;

            if(tmp.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && tmp.after(now)) {
              tmp2 = (Calendar)tmp.clone();
              tmp2.add(Calendar.DAY_OF_MONTH, -1);

              if(tmp2.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                item.getDayRepeat().getOrdinalNumber()) {
                sundayMatch = true;
              }
            }

            if(!sundayMatch) {

              month = tmp.get(Calendar.MONTH);
              tmp2 = (Calendar)tmp.clone();
              tmp2.add(Calendar.DAY_OF_MONTH, 1);

              matchToOrdinalNum = false;
              if(item.getDayRepeat().getOrdinalNumber() == 5) {
                if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                  tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                  matchToOrdinalNum = true;
                }
              }
              else {
                if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) ==
                  item.getDayRepeat().getOrdinalNumber()) {
                  matchToOrdinalNum = true;
                }
              }

              if(matchToOrdinalNum && tmp.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
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
                    item.getDayRepeat().getOrdinalNumber()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }
                  }

                  while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                    item.getDayRepeat().getOrdinalNumber()
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
    else if((item.getDayRepeat().getWhichSet() & (1 << 3)) != 0) {

      // Yearリピート設定時
      now = Calendar.getInstance();
      if(now.get(Calendar.SECOND) >= 30) {
        now.add(Calendar.MINUTE, 1);
      }
      now.set(Calendar.SECOND, 0);
      now.set(Calendar.MILLISECOND, 0);
      int monthLast;

      if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
        tmp = (Calendar)item.getDate().clone();
        month = item.getDate().get(Calendar.MONTH);

        // intervalの処理
        monthLast = Integer.toBinaryString(item.getDayRepeat().getYear()).length() - 1;
        if(month >= monthLast) {
          tmp.set(Calendar.MONTH, 0);
          tmp.add(Calendar.YEAR, item.getDayRepeat().getInterval());
          month = 0;
        }

        int i = 0;
        while(i < 12 - month) {
          if((item.getDayRepeat().getYear() & (1 << (month + i))) != 0) {
            tmp.add(Calendar.MONTH, i);
            if(tmp.get(Calendar.DAY_OF_MONTH) < item.getDayRepeat().getDayOfMonthOfYear()
              && item.getDayRepeat().getDayOfMonthOfYear() <=
              tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
              tmp.set(Calendar.DAY_OF_MONTH, item.getDayRepeat().getDayOfMonthOfYear());
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
        tmp.set(
          Calendar.DAY_OF_MONTH,
          Math.min(
            tmp.getActualMaximum(Calendar.DAY_OF_MONTH),
            item.getDate().get(Calendar.DAY_OF_MONTH)
          )
        );
        tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
        tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
        tmp.set(Calendar.SECOND, 0);
        tmp.set(Calendar.MILLISECOND, 0);
        month = now.get(Calendar.MONTH);

        // intervalの処理
        monthLast = Integer.toBinaryString(item.getDayRepeat().getYear()).length() - 1;
        if(month > monthLast) {
          tmp.set(Calendar.MONTH, 0);
          tmp.add(Calendar.YEAR, item.getDayRepeat().getInterval());
          month = 0;
        }
        else if(month == monthLast) {
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
            if(tmp.get(Calendar.DAY_OF_MONTH) < item.getDayRepeat().getDayOfMonthOfYear()
              && item.getDayRepeat().getDayOfMonthOfYear() <=
              tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
              tmp.set(Calendar.DAY_OF_MONTH, item.getDayRepeat().getDayOfMonthOfYear());
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

    if(!inMinuteRepeat) {
      if((item.getMinuteRepeat().getWhichSet() & 1) != 0
        && item.getMinuteRepeat().getCount() == 0) {

        if(item.getDayRepeat().getWhichSet() != 0) {
          item.getMinuteRepeat().setCount(item.getMinuteRepeat().getOrgCount());
        }
        else {
          item.getMinuteRepeat().setWhichSet(0);
        }
      }
      else if((item.getMinuteRepeat().getWhichSet() & (1 << 1)) != 0
        && item.getMinuteRepeat().getInterval() > item.getMinuteRepeat().getDuration()) {

        if(item.getDayRepeat().getWhichSet() != 0) {
          item.getMinuteRepeat().setDuration(item.getMinuteRepeat().getOrgDuration());
        }
        else {
          item.getMinuteRepeat().setWhichSet(0);
        }
      }
    }


    // tmp設定後の処理
    if(item.getDayRepeat().getWhichSet() != 0 || item.getMinuteRepeat().getWhichSet() != 0) {
      if(!inMinuteRepeat) {
        item.setOrgIsAlarmStopped(item.isAlarmStopped());
        item.setOrgAlteredTime(item.getAlteredTime());
        if(item.isAlarmStopped()) {
          item.setAlarmStopped(false);
        }
        if(item.getAlteredTime() != 0) {
          item.setAlteredTime(0);
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

  public void setAlarm(ItemAdapter item) {

    if(item.getDate().getTimeInMillis() > System.currentTimeMillis() &&
      item.getWhichListBelongs() == 0) {
      item.getNotifyInterval().setTime(item.getNotifyInterval().getOrgTime());
      Intent intent = new Intent(context, AlarmReceiver.class);
      byte[] obArray = serialize(item.getItem());
      intent.putExtra(ITEM, obArray);
      PendingIntent sender = PendingIntent.getBroadcast(
        context, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      requireNonNull(alarmManager);

      alarmManager.setAlarmClock(
        new AlarmManager.AlarmClockInfo(item.getDate().getTimeInMillis(), null), sender);
    }
  }

  public void deleteAlarm(ItemAdapter item) {

    if(isAlarmSet(item)) {
      Intent intent = new Intent(context, AlarmReceiver.class);
      PendingIntent sender = PendingIntent.getBroadcast(
        context, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      requireNonNull(alarmManager);

      alarmManager.cancel(sender);
      sender.cancel();
    }
  }

  public boolean isAlarmSet(ItemAdapter item) {

    Intent intent = new Intent(context, AlarmReceiver.class);
    PendingIntent sender = PendingIntent.getBroadcast(
      context, (int)item.getId(), intent, PendingIntent.FLAG_NO_CREATE);

    return sender != null;
  }

  public void insertDB(ItemAdapter item, String table) {

    accessor.executeInsert(item.getId(), serialize(item.getItem()), table);
  }

  public void updateDB(ItemAdapter item, String table) {

    accessor.executeUpdate(item.getId(), serialize(item.getItem()), table);
  }

  public void deleteDB(ItemAdapter item, String table) {

    accessor.executeDelete(item.getId(), table);
  }
}
