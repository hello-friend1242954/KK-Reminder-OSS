package com.hideaki.kk_reminder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.CHANGE_GRADE;
import static com.hideaki.kk_reminder.UtilClass.COLLAPSE_GROUP;
import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.IS_PREMIUM;
import static com.hideaki.kk_reminder.UtilClass.LINE_SEPARATOR;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.MINUTE;
import static com.hideaki.kk_reminder.UtilClass.NON_SCHEDULED_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.SCHEDULED_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.currentTimeMinutes;

public class MyExpandableListAdapter extends BaseExpandableListAdapter implements Filterable {

  private static Calendar tmp;
  static boolean[] display_groups = new boolean[5];
  static List<String> groups = new ArrayList<>();
  static List<List<Item>> children;
  static long has_panel; //コントロールパネルがvisibleであるItemのid値を保持する
  static long panel_lock_id;
  private MainActivity activity;
  ActionMode actionMode = null;
  static int checked_item_num;
  private static boolean manually_checked;
  private List<List<Item>> filteredList;
  private ColorStateList defaultColorStateList;
  static boolean block_notify_change = false;
  static boolean lock_block_notify_change = false;
  private final Handler handler = new Handler();
  private Runnable runnable;
  private int collapse_group;
  private boolean is_manual_expand_or_collapse = true;
  static boolean is_scrolling;
  static boolean isClosed = false; //完了したタスクのコントロールパネルが閉じられたときに立てるフラグ

  MyExpandableListAdapter(List<List<Item>> children, MainActivity activity) {

    MyExpandableListAdapter.children = children;

    //groupsの初期化
    groups.clear();
    Calendar now = Calendar.getInstance();
    Calendar tomorrow_cal = (Calendar)now.clone();
    tomorrow_cal.add(Calendar.DAY_OF_MONTH, 1);
    CharSequence today;
    CharSequence tomorrow;
    if(LOCALE.equals(Locale.JAPAN)) {
      today = DateFormat.format(" - yyyy年M月d日(E)", now);
      tomorrow = DateFormat.format(" - yyyy年M月d日(E)", tomorrow_cal);
    }
    else {
      today = DateFormat.format(" - yyyy/M/d (E)", now);
      tomorrow = DateFormat.format(" - yyyy/M/d (E)", tomorrow_cal);
    }
    groups.add(activity.getString(R.string.past));
    groups.add(activity.getString(R.string.today) + today);
    groups.add(activity.getString(R.string.tomorrow) + tomorrow);
    groups.add(activity.getString(R.string.this_week));
    groups.add(activity.getString(R.string.future));

    this.activity = activity;
    SharedPreferences intPreferences = activity.getSharedPreferences(INT_GENERAL, MODE_PRIVATE);
    collapse_group = intPreferences.getInt(COLLAPSE_GROUP, 0);
  }

  private static class ChildViewHolder {

    CardView child_card;
    ImageView clock_image;
    TextView time;
    TextView detail;
    TextView repeat;
    AnimCheckBox checkBox;
    ImageView tagPallet;
    TableLayout control_panel;
    TextView minusTime1;
    TextView minusTime2;
    TextView minusTime3;
    TextView plusTime1;
    TextView plusTime2;
    TextView plusTime3;
    TextView notes;
  }

  private class MyOnClickListener implements View.OnClickListener, View.OnLongClickListener,
      ActionMode.Callback, AnimCheckBox.OnCheckedChangeListener {

    private int group_position;
    private int child_position;
    private Item item;
    private ChildViewHolder viewHolder;
    private int which_list;
    private List<Item> itemListToMove;

    private MyOnClickListener(int group_position, int child_position, Item item,
        ChildViewHolder viewHolder) {

      this.group_position = group_position;
      this.child_position = child_position;
      this.item = item;
      this.viewHolder = viewHolder;
    }

    private void setTimeStep(boolean isMinus, int which) {

      int hour;
      int minute;
      switch(which) {
        case 1: {
          hour = isMinus ? activity.minusTime1Hour : activity.plusTime1Hour;
          minute = isMinus ? activity.minusTime1Minute : activity.plusTime1Minute;
          break;
        }
        case 2: {
          hour = isMinus ? activity.minusTime2Hour : activity.plusTime2Hour;
          minute = isMinus ? activity.minusTime2Minute : activity.plusTime2Minute;
          break;
        }
        case 3: {
          hour = isMinus ? activity.minusTime3Hour : activity.plusTime3Hour;
          minute = isMinus ? activity.minusTime3Minute : activity.plusTime3Minute;
          break;
        }
        default: {
          throw new IllegalStateException("Such a control num not exists! : " + which);
        }
      }

      long timeStepInMillis;
      if(hour == 0 && minute == 0) {
        timeStepInMillis = 24 * HOUR;
      }
      else {
        timeStepInMillis = hour * HOUR + minute * MINUTE;
      }

      if(isMinus) {
        if(item.getDate().getTimeInMillis() > System.currentTimeMillis() + timeStepInMillis) {

          lock_block_notify_change = true;
          block_notify_change = true;

          if(item.getTime_altered() == 0) {
            item.setOrg_date((Calendar)item.getDate().clone());
          }
          item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + -timeStepInMillis);

          item.addTime_altered(-timeStepInMillis);
          if(item.isAlarm_stopped()) {
            item.setAlarm_stopped(false);
          }

          activity.deleteAlarm(item);
          activity.setAlarm(item);
          activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);

          sortItemInGroup();
          displayDate(viewHolder, item);
        }
      }
      else {
        lock_block_notify_change = true;
        block_notify_change = true;

        if(item.getTime_altered() == 0) {
          item.setOrg_date((Calendar)item.getDate().clone());
        }

        if(item.getDate().getTimeInMillis() < System.currentTimeMillis()) {
          item.getDate().setTimeInMillis(currentTimeMinutes() + timeStepInMillis);
        }
        else {
          item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + timeStepInMillis);
        }

        item.addTime_altered(timeStepInMillis);
        if(item.isAlarm_stopped()) {
          item.setAlarm_stopped(false);
        }

        activity.deleteAlarm(item);
        activity.setAlarm(item);
        activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);

        sortItemInGroup();
        displayDate(viewHolder, item);
      }
    }

    @Override
    public void onClick(View v) {

      //すべての通知を既読する
      NotificationManager manager = (NotificationManager)activity.getSystemService(NOTIFICATION_SERVICE);
      checkNotNull(manager);
      manager.cancelAll();

      activity.actionBarFragment.searchView.clearFocus();
      switch(v.getId()) {
        case R.id.child_card: {

          if(actionMode == null) {
            if(viewHolder.control_panel.getVisibility() == View.GONE) {
              if(item.getId() != panel_lock_id) {
                has_panel = item.getId();
                View cardView = (View)viewHolder.control_panel.getParent().getParent();
                cardView.setTranslationY(-30.0f);
                cardView.setAlpha(0.0f);
                cardView
                    .animate()
                    .translationY(0.0f)
                    .alpha(1.0f)
                    .setDuration(150)
                    .setListener(new AnimatorListenerAdapter() {

                      @Override
                      public void onAnimationStart(Animator animation) {

                        super.onAnimationStart(animation);

                        //他タスクのコントロールパネルを閉じる
                        int visible_count = activity.expandableListView.getChildCount();
                        for(int i = 0; i < visible_count; i++) {
                          View visibleView = activity.expandableListView.getChildAt(i);
                          final TableLayout panel = visibleView.findViewById(R.id.control_panel);
                          if(panel != null && panel.getVisibility() == View.VISIBLE) {
                            ((View)panel.getParent().getParent())
                                .animate()
                                .translationY(-30.0f)
                                .alpha(0.0f)
                                .setDuration(150)
                                .setListener(new AnimatorListenerAdapter() {
                                  @Override
                                  public void onAnimationEnd(Animator animation) {

                                    super.onAnimationEnd(animation);
                                    panel.setVisibility(View.GONE);
                                  }
                                });
                            break;
                          }
                        }

                        viewHolder.control_panel.setVisibility(View.VISIBLE);
                      }
                    });
              }
            }
            else {
              has_panel = 0;
              ((View)viewHolder.control_panel.getParent().getParent())
                  .animate()
                  .translationY(-30.0f)
                  .alpha(0.0f)
                  .setDuration(150)
                  .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {

                      super.onAnimationEnd(animation);
                      viewHolder.control_panel.setVisibility(View.GONE);
                    }
                  });
            }
          }
          else if(viewHolder.checkBox.isChecked()) {
            viewHolder.checkBox.setChecked(false);
          }
          else {
            viewHolder.checkBox.setChecked(true);
          }
          break;
        }
        case R.id.clock_image: {

          if(actionMode == null) {
            if(item.getTime_altered() == 0 && activity.isAlarmSetted(item)) {
              item.setAlarm_stopped(true);
              activity.deleteAlarm(item);
            }
            else if(item.getTime_altered() == 0 && !item.isAlarm_stopped()) {
              item.setAlarm_stopped(true);
            }
            else if(item.getTime_altered() == 0 && item.isAlarm_stopped()) {
              item.setAlarm_stopped(false);
              activity.setAlarm(item);
            }
            else if(item.getTime_altered() != 0) {

              lock_block_notify_change = true;
              block_notify_change = true;

              item.setDate((Calendar)item.getOrg_date().clone());
              item.setTime_altered(0);
              Collections.sort(children.get(group_position), SCHEDULED_ITEM_COMPARATOR);

              activity.updateListTask(null, -1, true);

              activity.deleteAlarm(item);
              activity.setAlarm(item);
            }

            activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);

            displayDate(viewHolder, item);
          }
          else if(viewHolder.checkBox.isChecked()) {
            viewHolder.checkBox.setChecked(false);
          }
          else {
            viewHolder.checkBox.setChecked(true);
          }
          break;
        }
        case R.id.minus_time1: {
          setTimeStep(true, 1);
          break;
        }
        case R.id.minus_time2: {
          setTimeStep(true, 2);
          break;
        }
        case R.id.minus_time3: {
          setTimeStep(true, 3);
          break;
        }
        case R.id.edit: {
          activity.expandableListView.clearTextFilter();
          activity.showMainEditFragment(item);
          has_panel = 0;
          viewHolder.control_panel.setVisibility(View.GONE);
          break;
        }
        case R.id.plus_time1: {
          setTimeStep(false, 1);
          break;
        }
        case R.id.plus_time2: {
          setTimeStep(false, 2);
          break;
        }
        case R.id.plus_time3: {
          setTimeStep(false, 3);
          break;
        }
        case R.id.notes: {
          activity.expandableListView.clearTextFilter();
          activity.showNotesFragment(item);
          break;
        }
      }
    }

    @Override
    public void onChange(AnimCheckBox animCheckBox, boolean checked) {

      if(checked && actionMode == null && manually_checked) {

        lock_block_notify_change = true;
        block_notify_change = true;

        //すべての通知を既読する
        NotificationManager manager = (NotificationManager)activity.getSystemService(NOTIFICATION_SERVICE);
        checkNotNull(manager);
        manager.cancelAll();

        if(has_panel == item.getId()) {
          isClosed = true;
          has_panel = 0;
        }
        else {
          isClosed = false;
        }
        panel_lock_id = item.getId();
        if(viewHolder.control_panel.getVisibility() == View.VISIBLE) {
          ((View)viewHolder.control_panel.getParent().getParent())
              .animate()
              .translationY(-30.0f)
              .alpha(0.0f)
              .setDuration(150)
              .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                  super.onAnimationEnd(animation);
                  viewHolder.control_panel.setVisibility(View.GONE);
                }
              });
        }

        activity.actionBarFragment.searchView.clearFocus();
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

        if((item.getMinuteRepeat().getWhich_setted() & 1) != 0 && item.getMinuteRepeat().getCount() != 0) {

          //countリピート設定時
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
          tmp.set(Calendar.HOUR_OF_DAY, tmp.get(Calendar.HOUR_OF_DAY) + item.getMinuteRepeat().getHour());
          tmp.add(Calendar.MINUTE, item.getMinuteRepeat().getMinute());

          item.setOrg_alarm_stopped(item.isAlarm_stopped());
          item.setOrg_time_altered(item.getTime_altered());
          item.setTime_altered(0);
          item.setAlarm_stopped(false);
          item.getMinuteRepeat().setCount(item.getMinuteRepeat().getCount() - 1);
        }
        else if((item.getMinuteRepeat().getWhich_setted() & (1 << 1)) != 0
            && item.getMinuteRepeat().getInterval() <= item.getMinuteRepeat().getDuration()) {

          //durationリピート設定時
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
          tmp.set(Calendar.HOUR_OF_DAY, tmp.get(Calendar.HOUR_OF_DAY) + item.getMinuteRepeat().getHour());
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

          //Dayリピート設定時
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

          //Weekリピート設定時
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
                item.getDate().get(Calendar.DAY_OF_WEEK) + 5 : item.getDate().get(Calendar.DAY_OF_WEEK) - 2;

            //intervalの処理
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

            //intervalの処理
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

          //Monthリピート設定時
          now = Calendar.getInstance();
          if(now.get(Calendar.SECOND) >= 30) {
            now.add(Calendar.MINUTE, 1);
          }
          now.set(Calendar.SECOND, 0);
          now.set(Calendar.MILLISECOND, 0);

          if(item.getDayRepeat().isDays_of_month_setted()) {

            //DaysOfMonthリピート設定時
            int day_of_month;
            int day_of_month_last;

            if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
              tmp = (Calendar)item.getDate().clone();
              day_of_month = item.getDate().get(Calendar.DAY_OF_MONTH);

              //intervalの処理
              day_of_month_last = Integer.toBinaryString(item.getDayRepeat().getDays_of_month()).length();
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

              //intervalの処理
              day_of_month_last = Integer.toBinaryString(item.getDayRepeat().getDays_of_month()).length();
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

            //OnTheMonthリピート設定時
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
              //clone()で渡して不具合が出る場合はsetTimeInMillis()を使う
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

                  //intervalの処理
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
                      && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
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

                  //intervalの処理
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
                      && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }

                  tmp.add(Calendar.DAY_OF_MONTH, item.getDayRepeat().getWeekday_num());
                  item.getDayRepeat().setWeekday_num(item.getDayRepeat().getWeekday_num() + 1);

                  if(tmp.after(item.getDate()) && month == tmp.get(Calendar.MONTH)) {
                    item.getDayRepeat().setWeekday_num(0);
                    break;
                  }
                  else if(item.getDayRepeat().getWeekday_num() > 4 || month != tmp.get(Calendar.MONTH)) {
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
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) == tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                    match_to_ordinal_num = true;
                  }
                }
                else {
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) == item.getDayRepeat().getOrdinal_number()) {
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

                    //intervalの処理
                    if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinal_number()) {
                      tmp.set(Calendar.DAY_OF_MONTH, 1);
                      tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                      tmp2 = (Calendar)tmp.clone();

                      tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                      if(tmp.before(tmp2)) {
                        tmp.add(Calendar.DAY_OF_MONTH, 7);
                      }
                    }

                    while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinal_number()
                        && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
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

                  //intervalの処理
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
                      && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
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

                  //intervalの処理
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
                      && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }

                  tmp.add(Calendar.DAY_OF_MONTH, item.getDayRepeat().getWeekday_num());
                  item.getDayRepeat().setWeekday_num(item.getDayRepeat().getWeekday_num() + 1);

                  if(tmp.after(now) && month == tmp.get(Calendar.MONTH)) {
                    item.getDayRepeat().setWeekday_num(0);
                    break;
                  }
                  else if(item.getDayRepeat().getWeekday_num() > 4 || month != tmp.get(Calendar.MONTH)) {
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

                  if(tmp2.get(Calendar.DAY_OF_WEEK_IN_MONTH) == item.getDayRepeat().getOrdinal_number()) {
                    sunday_match = true;
                  }
                }

                if(!sunday_match) {

                  month = tmp.get(Calendar.MONTH);
                  tmp2 = (Calendar)tmp.clone();
                  tmp2.add(Calendar.DAY_OF_MONTH, 1);

                  match_to_ordinal_num = false;
                  if(item.getDayRepeat().getOrdinal_number() == 5) {
                    if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) == tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                      match_to_ordinal_num = true;
                    }
                  }
                  else {
                    if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) == item.getDayRepeat().getOrdinal_number()) {
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

                      //intervalの処理
                      if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getDayRepeat().getOrdinal_number()) {
                        tmp.set(Calendar.DAY_OF_MONTH, 1);
                        tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                        tmp2 = (Calendar)tmp.clone();

                        tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                        if(tmp.before(tmp2)) {
                          tmp.add(Calendar.DAY_OF_MONTH, 7);
                        }
                      }

                      while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getDayRepeat().getOrdinal_number()
                          && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
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

          //Yearリピート設定時
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

            //intervalの処理
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
                    && item.getDayRepeat().getDay_of_month_of_year() <= tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
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
            //itemに登録されている日にちが今月の日にちの最大値を超えている場合、今月の日にちの最大値を設定する
            if(tmp.getActualMaximum(Calendar.DAY_OF_MONTH) < item.getDate().get(Calendar.DAY_OF_MONTH)) {
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

            //intervalの処理
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
                    && item.getDayRepeat().getDay_of_month_of_year() <= tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
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


        //tmp設定後の処理
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
          Collections.sort(children.get(group_position), SCHEDULED_ITEM_COMPARATOR);

          activity.deleteAlarm(item);
          activity.setAlarm(item);
          activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);
        }
        else {
          children.get(group_position).remove(child_position);
          item.setDoneDate(Calendar.getInstance());

          activity.deleteAlarm(item);
          activity.deleteDB(item, MyDatabaseHelper.TODO_TABLE);
          activity.insertDB(item, MyDatabaseHelper.DONE_TABLE);
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {

            activity.updateListTask(item, group_position, true);
          }
        }, 400);
      }
      else if(checked && manually_checked) {
        item.setSelected(true);
        notifyDataSetChanged();
        checked_item_num++;
        actionMode.setTitle(Integer.toString(checked_item_num));
      }
      else if(actionMode != null && manually_checked) {
        item.setSelected(false);
        notifyDataSetChanged();
        checked_item_num--;
        actionMode.setTitle(Integer.toString(checked_item_num));
        if(checked_item_num == 0) {
          actionMode.finish();
        }
      }
    }

    @Override
    public boolean onLongClick(View v) {

      //すべての通知を既読する
      NotificationManager manager = (NotificationManager)activity.getSystemService(NOTIFICATION_SERVICE);
      checkNotNull(manager);
      manager.cancelAll();

      if(actionMode != null) {

        if(viewHolder.checkBox.isChecked()) {
          viewHolder.checkBox.setChecked(false);
        }
        else {
          viewHolder.checkBox.setChecked(true);
        }

        return true;
      }
      else {
        actionMode = activity.startSupportActionMode(this);
        viewHolder.checkBox.setChecked(true);
        return true;
      }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {

      actionMode.getMenuInflater().inflate(R.menu.action_mode_menu, menu);

      //ActionMode時のみツールバーとステータスバーの色を設定
      Window window = activity.getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(ContextCompat.getColor(activity, R.color.darker_grey));

      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {

      MenuItem moveTaskItem = menu.findItem(R.id.move_task_between_list);
      if(ManageListAdapter.nonScheduledLists.size() > 0) {
        moveTaskItem.setVisible(true);
      }
      else {
        moveTaskItem.setVisible(false);
      }
      return true;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {

      switch(menuItem.getItemId()) {
        case R.id.delete: {

          itemListToMove = new ArrayList<>();
          for(List<Item> itemList : children) {
            for(Item item : itemList) {
              if(item.isSelected()) {
                itemListToMove.add(0, item);
              }
            }
          }

          String message = activity.getResources().getQuantityString(R.plurals.cab_delete_message,
              itemListToMove.size(), itemListToMove.size()) + " (" + activity.getString(R.string.delete_dialog_message) + ")";
          final AlertDialog dialog = new AlertDialog.Builder(activity)
              .setTitle(R.string.cab_delete)
              .setMessage(message)
              .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                  for(List<Item> itemList : children) {
                    for(Item item : itemList) {
                      if(item.isSelected()) {
                        activity.deleteDB(item, MyDatabaseHelper.TODO_TABLE);
                        MyExpandableListAdapter.children = activity.getChildren(MyDatabaseHelper.TODO_TABLE);
                        activity.deleteAlarm(item);
                      }
                    }
                  }

                  actionMode.finish();
                }
              })
              .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
              })
              .create();

          dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accent_color);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accent_color);
            }
          });

          dialog.show();

          return true;
        }
        case R.id.move_task_between_list: {

          itemListToMove = new ArrayList<>();
          for(List<Item> itemList : children) {
            for(Item item : itemList) {
              if(item.isSelected()) {
                itemListToMove.add(0, item);
              }
            }
          }

          int size = ManageListAdapter.nonScheduledLists.size();
          String[] items = new String[size];
          for(int i = 0; i < size; i++) {
            items[i] = ManageListAdapter.nonScheduledLists.get(i).getTitle();
          }

          String title = activity.getResources().getQuantityString(R.plurals.cab_selected_task_num,
              itemListToMove.size(), itemListToMove.size()) + activity.getString(R.string.cab_move_task_message);
          final SingleChoiceItemsAdapter adapter = new SingleChoiceItemsAdapter(items);
          final AlertDialog dialog = new AlertDialog.Builder(activity)
              .setTitle(title)
              .setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
              })
              .setPositiveButton(R.string.determine, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                  which_list = SingleChoiceItemsAdapter.checked_position;

                  long list_id = activity.generalSettings.getNonScheduledLists().get(which_list).getId();
                  MyListAdapter.itemList = new ArrayList<>();
                  for(Item item : activity.queryAllDB(MyDatabaseHelper.TODO_TABLE)) {
                    if(item.getWhich_list_belongs() == list_id) {
                      MyListAdapter.itemList.add(item);
                    }
                  }
                  Collections.sort(MyListAdapter.itemList, NON_SCHEDULED_ITEM_COMPARATOR);

                  for(Item item : itemListToMove) {

                    item.setSelected(false);

                    //リストのIDをitemに登録する
                    item.setWhich_list_belongs(list_id);

                    MyListAdapter.itemList.add(0, item);
                    activity.deleteAlarm(item);
                  }

                  int size = MyListAdapter.itemList.size();
                  for(int i = 0; i < size; i++) {
                    Item item = MyListAdapter.itemList.get(i);
                    item.setOrder(i);
                    activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);
                  }

                  children = activity.getChildren(MyDatabaseHelper.TODO_TABLE);

                  actionMode.finish();
                }
              })
              .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
              })
              .create();

          dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accent_color);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accent_color);
            }
          });

          dialog.show();

          return true;
        }
        case R.id.clone: {

          itemListToMove = new ArrayList<>();
          for(List<Item> itemList : children) {
            for(Item item : itemList) {
              if(item.isSelected()) {
                itemListToMove.add(0, item);
              }
            }
          }

          String message = activity.getResources().getQuantityString(R.plurals.cab_clone_message,
              itemListToMove.size(), itemListToMove.size());
          final AlertDialog dialog = new AlertDialog.Builder(activity)
              .setTitle(R.string.cab_clone)
              .setMessage(message)
              .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                  MainEditFragment.checked_item_num = checked_item_num;
                  MainEditFragment.itemListToMove = new ArrayList<>(itemListToMove);
                  MainEditFragment.is_cloning_task = true;
                  itemListToMove.get(itemListToMove.size() - 1).setSelected(false);
                  activity.showMainEditFragment(
                      itemListToMove.get(itemListToMove.size() - 1).copy()
                  );

                  actionMode.finish();
                }
              })
              .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
              })
              .create();

          dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accent_color);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accent_color);
            }
          });

          dialog.show();

          return true;
        }
        case R.id.share: {

          itemListToMove = new ArrayList<>();
          for(List<Item> itemList : children) {
            for(Item item : itemList) {
              if(item.isSelected()) {
                itemListToMove.add(0, item);
              }
            }
          }

          String message = activity.getResources().getQuantityString(R.plurals.cab_share_message,
              itemListToMove.size(), itemListToMove.size());
          final AlertDialog dialog = new AlertDialog.Builder(activity)
              .setTitle(R.string.cab_share)
              .setMessage(message)
              .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                  for(Item item : itemListToMove) {
                    String send_content = activity.getString(R.string.due_date) + ": "
                        + DateFormat.format("yyyy/M/d k:mm", item.getDate())
                        + LINE_SEPARATOR
                        + activity.getString(R.string.detail) + ": " + item.getDetail()
                        + LINE_SEPARATOR
                        + activity.getString(R.string.memo) + ": " + item.getNotesString();

                    Intent intent = new Intent()
                        .setAction(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, send_content);
                    activity.startActivity(intent);
                  }

                  actionMode.finish();
                }
              })
              .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
              })
              .create();

          dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accent_color);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accent_color);
            }
          });

          dialog.show();

          return true;
        }
        default: {
          actionMode.finish();
          return true;
        }
      }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {

      Window window = activity.getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(activity.status_bar_color);

      MyExpandableListAdapter.this.actionMode = null;
      for(List<Item> itemList : children) {
        for(Item item : itemList) {
          if(item.isSelected()) {
            item.setSelected(false);
          }
        }
      }

      checked_item_num = 0;
      notifyDataSetChanged();
    }
  }

  @Override
  public Filter getFilter() {

    return new Filter() {
      @Override
      protected FilterResults performFiltering(CharSequence constraint) {

        //アカウントテスト用
        if(CHANGE_GRADE.equals(constraint.toString())) {
          if(activity.is_premium) {
            activity.setBooleanGeneralInSharedPreferences(IS_PREMIUM, false);
          }
          else {
            activity.setBooleanGeneralInSharedPreferences(IS_PREMIUM, true);
          }
        }

        //入力文字列が大文字を含むかどうか調べる
        boolean is_upper = false;
        for(int i = 0; i < constraint.length(); i++) {
          if(Character.isUpperCase(constraint.charAt(i))) {
            is_upper = true;
            break;
          }
        }

        //検索処理
        if(activity.actionBarFragment.checkedTag == -1) {
          children = activity.getChildren(MyDatabaseHelper.TODO_TABLE);
        }
        else {
          children = activity.actionBarFragment.filteredLists;
        }

        filteredList = new ArrayList<>();
        for(List<Item> itemList : children) {
          List<Item> filteredItem = new ArrayList<>();

          for(Item item : itemList) {
            if(item.getDetail() != null) {
              String detail = item.getDetail();

              if(!is_upper) {
                detail = detail.toLowerCase();
              }

              Pattern pattern = Pattern.compile(constraint.toString());
              Matcher matcher = pattern.matcher(detail);

              if(matcher.find()) {
                filteredItem.add(item);
              }
            }
          }

          filteredList.add(filteredItem);
        }

        FilterResults results = new FilterResults();
        results.count = filteredList.size();
        results.values = filteredList;

        return results;
      }

      @Override
      @SuppressWarnings("unchecked")
      protected void publishResults(CharSequence constraint, FilterResults results) {

        children = (List<List<Item>>)results.values;

        //リストの表示更新
        notifyDataSetChanged();
      }
    };
  }

  @Override
  public int getGroupCount() {

    //表示するgroupsのサイズを返す。
    int count = 0;
    Arrays.fill(display_groups, false);
    int size = groups.size();
    for(int i = 0; i < size; i++) {
      if(children.get(i).size() != 0) {
        display_groups[i] = true;
        count++;
      }
    }

    return count;
//    return groups.size();
  }

  @Override
  public int getChildrenCount(int i) {

    //getChildrenCount()はgetGroupCount()によって返されるgroupsのサイズ分だけ(サイズが3なら3回)呼ばれる。
    //iはgetChildrenCount()の呼ばれた回数を表す。すなわちiは呼び出しを3回とすると1回目の呼び出しにおいて、
    //表示するgroupsの0番目を表す。2回目では1番目、3回目では2番目である。
    int count = 0;
    int size = groups.size();
    for(int j = 0; j < size; j++) {
      if(display_groups[j]) {
        //単に return children.get(j).size() とすると、表示するgroupsの1番目だけを返し続けてしまうので、
        //if(count == i) と条件を付けることで、getChildrenCount()の呼び出された回数に応じて表示する
        //groupsの対応するgroupのみ返すようにしている。
        if(count == i) {
          return children.get(j).size();
        }
        count++;
      }
    }

    return children.get(i).size();
  }

  @Override
  public Object getGroup(int i) {

    //getGroupCount()によって返されるgroupsのサイズ分だけ呼ばれる。引数のiに関してもgetChildrenCount()と同じ。
    int count = 0;
    int size = groups.size();
    for(int j = 0; j < size; j++) {
      if(display_groups[j]) {
        if(count == i) {
          return groups.get(j);
        }
        count++;
      }
    }

    return groups.get(i);
  }

  @Override
  public Object getChild(int i, int i1) {

    //getChildrenCount()によって返されるchildrenのサイズ分×getGroupCount()によって返されるgroupsのサイズ分
    //だけ呼ばれる。あとは他のメソッドと同じ。
    int count = 0;
    int size = groups.size();
    for(int j = 0; j < size; j++) {
      if(display_groups[j]) {
        if(count == i) {
          return children.get(j).get(i1);
        }
        count++;
      }
    }

    return children.get(i).get(i1);
  }

  @Override
  public long getGroupId(int i) {

    return i;
  }

  @Override
  public long getChildId(int i, int i1) {

    return i1;
  }

  @Override
  public boolean hasStableIds() {

    return true;
  }

  @Override
  public View getGroupView(int i, boolean b, View convertView, ViewGroup viewGroup) {

    if(convertView == null) {
      convertView = View.inflate(viewGroup.getContext(), R.layout.parent_layout, null);
    }

    //グループの開閉状態の保持
    ((ExpandableListView)viewGroup).setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
      @Override
      public void onGroupCollapse(int groupPosition) {

        if(is_manual_expand_or_collapse) {
          int count = 0;
          int size = groups.size();
          for(int j = 0; j < size; j++) {
            if(display_groups[j]) {
              if(count == groupPosition) {
                collapse_group |= 1 << j;

                activity.getSharedPreferences(INT_GENERAL, MODE_PRIVATE)
                    .edit()
                    .putInt(COLLAPSE_GROUP, collapse_group)
                    .apply();

                break;
              }
              count++;
            }
          }
        }

        is_manual_expand_or_collapse = true;
      }
    });
    ((ExpandableListView)viewGroup).setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
      @Override
      public void onGroupExpand(int groupPosition) {

        if(is_manual_expand_or_collapse) {
          int count = 0;
          int size = groups.size();
          for(int j = 0; j < size; j++) {
            if(display_groups[j]) {
              if(count == groupPosition) {
                collapse_group &= ~(1 << j);

                activity.getSharedPreferences(INT_GENERAL, MODE_PRIVATE)
                    .edit()
                    .putInt(COLLAPSE_GROUP, collapse_group)
                    .apply();

                break;
              }
              count++;
            }
          }
        }

        is_manual_expand_or_collapse = true;
      }
    });

    int size = groups.size();
    int count = 0;
    for(int j = 0; j < size; j++) {
      if(display_groups[j]) {
        is_manual_expand_or_collapse = false;
        if((collapse_group & 1 << j) != 0) {
          ((ExpandableListView)viewGroup).collapseGroup(count);
        }
        else {
          ((ExpandableListView)viewGroup).expandGroup(count);
        }
        count++;
      }
    }

    ((TextView)convertView.findViewById(R.id.day)).setText(getGroup(i).toString());

    return convertView;
  }

  @Override
  public View getChildView(int i, int i1, boolean b, View convertView, final ViewGroup viewGroup) {

    final ChildViewHolder viewHolder;

    if(convertView == null || convertView.getTag() == null) {

      // convertView != null && convertView.getTag() == nullのときは描画を最初から行うため、
      // 描画の完了に時間がかかり、ExpandableListViewFragment内でアイテムのポジションを決定し
      // た後にgetChildViewの描画が完了する。そのため、アイテムのポジションが最初の設定からず
      // れてしまうので、ここでアイテムのポジションの決定を行う。
      if(convertView != null && !ExpandableListViewFragment.isGetTagNull) {
        ExpandableListViewFragment.isGetTagNull = true;
        activity.expandableListViewFragment.setPosition();
      }

      convertView = View.inflate(viewGroup.getContext(), R.layout.child_layout, null);

      viewHolder = new ChildViewHolder();
      viewHolder.child_card = convertView.findViewById(R.id.child_card);
      viewHolder.clock_image = convertView.findViewById(R.id.clock_image);
      viewHolder.time = convertView.findViewById(R.id.date);
      viewHolder.detail = convertView.findViewById(R.id.detail);
      viewHolder.repeat = convertView.findViewById(R.id.repeat);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.tagPallet = convertView.findViewById(R.id.tag_pallet);
      viewHolder.control_panel = convertView.findViewById(R.id.control_panel);
      viewHolder.minusTime1 = convertView.findViewById(R.id.minus_time1);
      viewHolder.minusTime2 = convertView.findViewById(R.id.minus_time2);
      viewHolder.minusTime3 = convertView.findViewById(R.id.minus_time3);
      viewHolder.plusTime1 = convertView.findViewById(R.id.plus_time1);
      viewHolder.plusTime2 = convertView.findViewById(R.id.plus_time2);
      viewHolder.plusTime3 = convertView.findViewById(R.id.plus_time3);
      viewHolder.notes = convertView.findViewById(R.id.notes);
      defaultColorStateList = viewHolder.notes.getTextColors();

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ChildViewHolder)convertView.getTag();
    }

    //現在のビュー位置でのitemの取得とリスナーの初期化
    Item item = (Item)getChild(i, i1);
    int count = 0;
    MyOnClickListener listener = null;
    int size = groups.size();
    for(int j = 0; j < size; j++) {
      if(display_groups[j]) {
        if(count == i) {
          listener = new MyOnClickListener(j, i1, item, viewHolder);
          break;
        }
        count++;
      }
    }

    //各リスナーの設定
    viewHolder.child_card.setOnClickListener(listener);
    viewHolder.clock_image.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    viewHolder.child_card.setOnLongClickListener(listener);
    viewHolder.clock_image.setOnLongClickListener(listener);
    viewHolder.checkBox.setOnLongClickListener(listener);

    int control_panel_size = viewHolder.control_panel.getChildCount();
    for(int j = 0; j < control_panel_size; j++) {
      TableRow tableRow = (TableRow)viewHolder.control_panel.getChildAt(j);
      int table_row_size = tableRow.getChildCount();
      for(int k = 0; k < table_row_size; k++) {
        TextView panel_item = (TextView)tableRow.getChildAt(k);
        panel_item.setOnClickListener(listener);
      }
    }

    //各種表示処理
    displayDate(viewHolder, item);
    viewHolder.detail.setText(item.getDetail());
    viewHolder.detail.setTextSize(activity.text_size);
    displayRepeat(viewHolder, item);
    if(item.getWhich_tag_belongs() == 0) {
      viewHolder.tagPallet.setVisibility(View.GONE);
    }
    else {
      viewHolder.tagPallet.setVisibility(View.VISIBLE);
      int color = activity.generalSettings.getTagById(item.getWhich_tag_belongs()).getPrimary_color();
      if(color != 0) {
        viewHolder.tagPallet.setColorFilter(color);
      }
      else {
        viewHolder.tagPallet.setColorFilter(ContextCompat.getColor(activity, R.color.icon_gray));
      }
    }
    if(item.getNotesList().size() == 0) {
      viewHolder.notes.setTextColor(defaultColorStateList);
    }
    else {
      viewHolder.notes.setTextColor(activity.accent_color);
    }
    viewHolder.minusTime1.setText(activity.getControlTimeText(true, 1));
    viewHolder.minusTime2.setText(activity.getControlTimeText(true, 2));
    viewHolder.minusTime3.setText(activity.getControlTimeText(true, 3));
    viewHolder.plusTime1.setText(activity.getControlTimeText(false, 1));
    viewHolder.plusTime2.setText(activity.getControlTimeText(false, 2));
    viewHolder.plusTime3.setText(activity.getControlTimeText(false, 3));

    //ある子ビューでコントロールパネルを出したとき、他の子ビューのコントロールパネルを閉じる
    if(viewHolder.control_panel.getVisibility() == View.VISIBLE &&
        (item.getId() != has_panel || actionMode != null)) {
      ((View)viewHolder.control_panel.getParent().getParent())
          .animate()
          .translationY(-30.0f)
          .alpha(0.0f)
          .setDuration(150)
          .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

              super.onAnimationEnd(animation);
              viewHolder.control_panel.setVisibility(View.GONE);
            }
          });
    }
    else if(viewHolder.control_panel.getVisibility() == View.GONE && item.getId() == has_panel && actionMode == null) {
      View cardView = (View)viewHolder.control_panel.getParent().getParent();
      cardView.setTranslationY(-30.0f);
      cardView.setAlpha(0.0f);
      cardView
          .animate()
          .translationY(0.0f)
          .alpha(1.0f)
          .setDuration(150)
          .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

              super.onAnimationEnd(animation);
              viewHolder.control_panel.setVisibility(View.VISIBLE);
            }
          });
    }

    //チェックが入っている場合、チェックを外す
    if(viewHolder.checkBox.isChecked() && !item.isSelected()) {
      manually_checked = false;
      viewHolder.checkBox.setChecked(false);
    }
    else if(!viewHolder.checkBox.isChecked() && item.isSelected()) {
      manually_checked = false;
      viewHolder.checkBox.setChecked(true);
    }
    manually_checked = true;

    //CardViewが横から流れてくるアニメーション
    if(is_scrolling && activity.play_slide_animation) {
      Animation animation = AnimationUtils.loadAnimation(activity, R.anim.listview_motion);
      convertView.startAnimation(animation);
    }

    return convertView;
  }

  private void sortItemInGroup() {

    if(runnable != null) {
      handler.removeCallbacks(runnable);
    }
    else {
      runnable = new Runnable() {
        @Override
        public void run() {

          for(List<Item> itemList : children) {
            Collections.sort(itemList, SCHEDULED_ITEM_COMPARATOR);
          }
          activity.updateListTask(null, -1, true);
          runnable = null;
        }
      };
    }
    handler.postDelayed(runnable, 2000);
  }

  //時間を表示する処理
  private void displayDate(ChildViewHolder viewHolder, Item item) {

    Calendar now = Calendar.getInstance();
    String set_time;
    if(now.get(Calendar.YEAR) == item.getDate().get(Calendar.YEAR)) {
      if(LOCALE.equals(Locale.JAPAN)) {
        set_time = (String)DateFormat.format("M月d日(E) k:mm", item.getDate());
      }
      else {
        set_time = (String)DateFormat.format("M/d (E) k:mm", item.getDate());
      }
    }
    else {
      if(LOCALE.equals(Locale.JAPAN)) {
        set_time = (String)DateFormat.format("yyyy年M月d日(E) k:mm", item.getDate());
      }
      else {
        set_time = (String)DateFormat.format("yyyy/M/d (E) k:mm", item.getDate());
      }
    }
    long date_sub = item.getDate().getTimeInMillis() - now.getTimeInMillis();

    boolean date_is_minus = false;
    if(date_sub < 0) {
      date_sub = -date_sub;
      date_is_minus = true;
    }

    int how_far_years = 0;
    tmp = (Calendar)now.clone();
    if(date_is_minus) {
      tmp.add(Calendar.YEAR, -1);
      while(tmp.after(item.getDate())) {
        tmp.add(Calendar.YEAR, -1);
        how_far_years++;
      }
    }
    else {
      tmp.add(Calendar.YEAR, 1);
      while(tmp.before(item.getDate())) {
        tmp.add(Calendar.YEAR, 1);
        how_far_years++;
      }
    }

    int how_far_months = 0;
    tmp = (Calendar)now.clone();
    if(how_far_years != 0) {
      tmp.add(Calendar.YEAR, how_far_years);
    }
    if(date_is_minus) {
      tmp.add(Calendar.MONTH, -1);
      while(tmp.after(item.getDate())) {
        tmp.add(Calendar.MONTH, -1);
        how_far_months++;
      }
    }
    else {
      tmp.add(Calendar.MONTH, 1);
      while(tmp.before(item.getDate())) {
        tmp.add(Calendar.MONTH, 1);
        how_far_months++;
      }
    }

    int how_far_weeks = 0;
    tmp = (Calendar)now.clone();
    if(how_far_years != 0) {
      tmp.add(Calendar.YEAR, how_far_years);
    }
    if(how_far_months != 0) {
      tmp.add(Calendar.MONTH, how_far_months);
    }
    if(date_is_minus) {
      tmp.add(Calendar.DAY_OF_WEEK_IN_MONTH, -1);
      while(tmp.after(item.getDate())) {
        tmp.add(Calendar.DAY_OF_WEEK_IN_MONTH, -1);
        how_far_weeks++;
      }
    }
    else {
      tmp.add(Calendar.DAY_OF_WEEK_IN_MONTH, 1);
      while(tmp.before(item.getDate())) {
        tmp.add(Calendar.DAY_OF_WEEK_IN_MONTH, 1);
        how_far_weeks++;
      }
    }

    int how_far_days = (int)(date_sub / (24 * HOUR));
    int how_far_hours = (int)(date_sub / HOUR);
    int how_far_minutes = (int)(date_sub / MINUTE);


    Resources res = activity.getResources();
    String display_date = set_time + " (";
    if(!LOCALE.equals(Locale.JAPAN)) {
      display_date += " ";
    }
    if(how_far_years != 0) {
      display_date += res.getQuantityString(R.plurals.year, how_far_years, how_far_years);
      if(!LOCALE.equals(Locale.JAPAN)) {
        display_date += " ";
      }
      if(how_far_months != 0) {
        display_date += res.getQuantityString(R.plurals.month, how_far_months, how_far_months);
        if(!LOCALE.equals(Locale.JAPAN)) {
          display_date += " ";
        }
      }
      if(how_far_weeks != 0) {
        display_date += res.getQuantityString(R.plurals.week, how_far_weeks, how_far_weeks);
        if(!LOCALE.equals(Locale.JAPAN)) {
          display_date += " ";
        }
      }
    }
    else if(how_far_months != 0) {
      display_date += res.getQuantityString(R.plurals.month, how_far_months, how_far_months);
      if(!LOCALE.equals(Locale.JAPAN)) {
        display_date += " ";
      }
      if(how_far_weeks != 0) {
        display_date += res.getQuantityString(R.plurals.week, how_far_weeks, how_far_weeks);
        if(!LOCALE.equals(Locale.JAPAN)) {
          display_date += " ";
        }
      }
    }
    else if(how_far_weeks != 0) {
      display_date += res.getQuantityString(R.plurals.week, how_far_weeks, how_far_weeks);
      if(!LOCALE.equals(Locale.JAPAN)) {
        display_date += " ";
      }
      how_far_days -= 7 * how_far_weeks;
      if(how_far_days != 0) {
        display_date += res.getQuantityString(R.plurals.day, how_far_days, how_far_days);
        if(!LOCALE.equals(Locale.JAPAN)) {
          display_date += " ";
        }
      }
    }
    else if(how_far_days != 0) {
      display_date += res.getQuantityString(R.plurals.day, how_far_days, how_far_days);
      if(!LOCALE.equals(Locale.JAPAN)) {
        display_date += " ";
      }
    }
    else if(how_far_hours != 0) {
      display_date += res.getQuantityString(R.plurals.hour, how_far_hours, how_far_hours);
      if(!LOCALE.equals(Locale.JAPAN)) {
        display_date += " ";
      }
      how_far_minutes -= 60 * how_far_hours;
      if(how_far_minutes != 0) {
        display_date += res.getQuantityString(R.plurals.minute, how_far_minutes, how_far_minutes);
        if(!LOCALE.equals(Locale.JAPAN)) {
          display_date += " ";
        }
      }
    }
    else if(how_far_minutes != 0) {
      display_date += res.getQuantityString(R.plurals.minute, how_far_minutes, how_far_minutes);
      if(!LOCALE.equals(Locale.JAPAN)) {
        display_date += " ";
      }
    }
    else {
      display_date += activity.getString(R.string.within_one_minute);
      if(!LOCALE.equals(Locale.JAPAN)) {
        display_date += " ";
      }
    }
    display_date += ")";

    viewHolder.time.setText(display_date);

    if(item.isAlarm_stopped()) {
      viewHolder.time.setTextColor(Color.GRAY);
    }
    else if(date_is_minus) {
      viewHolder.time.setTextColor(Color.RED);
    }
    else {
      viewHolder.time.setTextColor(Color.BLACK);
    }

    if(item.isAlarm_stopped()) {
      viewHolder.clock_image.setColorFilter(Color.GRAY);
    }
    else if(item.getTime_altered() != 0) {
      viewHolder.clock_image.setColorFilter(Color.BLUE);
    }
    else {
      viewHolder.clock_image.setColorFilter(0xFF09C858);
    }
  }

  //リピートを表示する処理
  private void displayRepeat(ChildViewHolder viewHolder, Item item) {

    String repeat_str = "";
    String tmp = item.getDayRepeat().getLabel();
    if(tmp != null && !"".equals(tmp) && !activity.getString(R.string.none).equals(tmp)) {
      if(!LOCALE.equals(Locale.JAPAN)) {
        repeat_str += "Repeat ";
      }
      repeat_str += tmp;
      int scale = item.getDayRepeat().getScale();
      int template = item.getDayRepeat().getWhich_template();
      if(LOCALE.equals(Locale.JAPAN)) {
        if(template > 0 && template < 1 << 5) {
          if(template > 1) {
            repeat_str += "に";
          }
        }
        else if(scale > 1) {
          repeat_str += "に";
        }
      }
    }

    tmp = item.getMinuteRepeat().getLabel();
    if(tmp != null && !"".equals(tmp) && !activity.getString(R.string.none).equals(tmp)) {
      if(!LOCALE.equals(Locale.JAPAN) && !"".equals(repeat_str)) {
        repeat_str += " and ";
      }
      repeat_str += tmp;
    }

    String day = item.getDayRepeat().getLabel();
    String minute = item.getMinuteRepeat().getLabel();
    if(LOCALE.equals(Locale.JAPAN) && day != null && !"".equals(day) && !activity.getString(R.string.none).equals(day)
        && (minute == null || "".equals(minute) || activity.getString(R.string.none).equals(minute))) {
      repeat_str += "繰り返す";
    }

    if("".equals(repeat_str)) {
      viewHolder.repeat.setText(R.string.non_repeat);
    }
    else {
      viewHolder.repeat.setText(repeat_str);
    }
  }

  @Override
  public boolean isChildSelectable(int i, int i1) {

    return true;
  }
}
