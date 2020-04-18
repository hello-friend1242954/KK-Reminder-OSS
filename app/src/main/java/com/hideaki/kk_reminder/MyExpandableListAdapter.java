package com.hideaki.kk_reminder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import static android.content.Context.MODE_PRIVATE;
import static com.hideaki.kk_reminder.StartupReceiver.getDynamicContext;
import static com.hideaki.kk_reminder.StartupReceiver.getIsDirectBootContext;
import static com.hideaki.kk_reminder.UtilClass.CHANGE_GRADE;
import static com.hideaki.kk_reminder.UtilClass.COLLAPSE_GROUP;
import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.IS_PREMIUM;
import static com.hideaki.kk_reminder.UtilClass.LINE_SEPARATOR;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.MINUTE;
import static com.hideaki.kk_reminder.UtilClass.NON_SCHEDULED_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.SCHEDULED_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.currentTimeMinutes;

public class MyExpandableListAdapter extends BaseExpandableListAdapter implements Filterable {

  private static Calendar tmp;
  static boolean[] displayGroups = new boolean[5];
  static List<String> groups = new ArrayList<>();
  static List<List<ItemAdapter>> children;
  static long hasPanel; // コントロールパネルがvisibleであるItemのid値を保持する
  static long panelLockId;
  private MainActivity activity;
  ActionMode actionMode = null;
  static int checkedItemNum;
  private static boolean isManuallyChecked;
  private List<List<ItemAdapter>> filteredList;
  private ColorStateList defaultColorStateList;
  static boolean isBlockNotifyChange = false;
  static boolean isLockBlockNotifyChange = false;
  private final Handler handler = new Handler();
  private Runnable runnable;
  private int collapseGroup;
  private boolean isManualExpandOrCollapse = true;
  static boolean isScrolling;
  static boolean isClosed = false; // 完了したタスクのコントロールパネルが閉じられたときに立てるフラグ

  MyExpandableListAdapter(List<List<ItemAdapter>> children, MainActivity activity) {

    MyExpandableListAdapter.children = children;

    // groupsの初期化
    groups.clear();
    Calendar now = Calendar.getInstance();
    Calendar tomorrowCal = (Calendar)now.clone();
    tomorrowCal.add(Calendar.DAY_OF_MONTH, 1);
    CharSequence today;
    CharSequence tomorrow;
    if(LOCALE.equals(Locale.JAPAN)) {
      today = DateFormat.format(" - yyyy年M月d日(E)", now);
      tomorrow = DateFormat.format(" - yyyy年M月d日(E)", tomorrowCal);
    }
    else {
      today = DateFormat.format(" - yyyy/M/d (E)", now);
      tomorrow = DateFormat.format(" - yyyy/M/d (E)", tomorrowCal);
    }
    groups.add(activity.getString(R.string.past));
    groups.add(activity.getString(R.string.today) + today);
    groups.add(activity.getString(R.string.tomorrow) + tomorrow);
    groups.add(activity.getString(R.string.within_a_week));
    groups.add(activity.getString(R.string.in_a_week_or_later));

    this.activity = activity;
    SharedPreferences intPreferences =
      getDynamicContext(activity).getSharedPreferences(
        getIsDirectBootContext(activity) ? INT_GENERAL_COPY : INT_GENERAL,
        MODE_PRIVATE
      );
    collapseGroup = intPreferences.getInt(COLLAPSE_GROUP, 0);
  }

  private static class GroupViewHolder {

    ImageView indicator;
  }

  private static class ChildViewHolder {

    CardView childCard;
    ImageView clockImage;
    TextView time;
    TextView detail;
    TextView repeat;
    AnimCheckBox checkBox;
    ImageView tagPallet;
    CardView controlCard;
    TableLayout controlPanel;
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

    private int groupPosition;
    private int childPosition;
    private ItemAdapter item;
    private ChildViewHolder viewHolder;
    private int whichList;
    private List<ItemAdapter> itemListToMove;

    private MyOnClickListener(
      int groupPosition, int childPosition, ItemAdapter item,
      ChildViewHolder viewHolder
    ) {

      this.groupPosition = groupPosition;
      this.childPosition = childPosition;
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

          isLockBlockNotifyChange = true;
          isBlockNotifyChange = true;

          if(item.getAlteredTime() == 0) {
            item.setOrgDate((Calendar)item.getDate().clone());
          }
          item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + -timeStepInMillis);

          item.addAlteredTime(-timeStepInMillis);
          if(item.isAlarmStopped()) {
            item.setAlarmStopped(false);
          }

          activity.deleteAlarm(item);
          activity.setAlarm(item);
          activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);

          sortItemInGroup();
          displayDate(viewHolder, item);
        }
      }
      else {
        isLockBlockNotifyChange = true;
        isBlockNotifyChange = true;

        if(item.getAlteredTime() == 0) {
          item.setOrgDate((Calendar)item.getDate().clone());
        }

        if(item.getDate().getTimeInMillis() < System.currentTimeMillis()) {
          item.getDate().setTimeInMillis(currentTimeMinutes() + timeStepInMillis);
        }
        else {
          item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + timeStepInMillis);
        }

        item.addAlteredTime(timeStepInMillis);
        if(item.isAlarmStopped()) {
          item.setAlarmStopped(false);
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

      // すべての通知を既読し、通知チャネルを削除する
      activity.clearAllNotification();

      activity.actionBarFragment.searchView.clearFocus();
      switch(v.getId()) {
        case R.id.child_card: {

          if(actionMode == null) {
            if(viewHolder.controlPanel.getVisibility() == View.GONE) {
              if(item.getId() != panelLockId) {
                hasPanel = item.getId();
                View cardView = (View)viewHolder.controlPanel.getParent().getParent();
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

                      // 他タスクのコントロールパネルを閉じる
                      int visibleCount = activity.expandableListView.getChildCount();
                      for(int i = 0; i < visibleCount; i++) {
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

                      viewHolder.controlPanel.setVisibility(View.VISIBLE);
                    }
                  });
              }
            }
            else {
              hasPanel = 0;
              ((View)viewHolder.controlPanel.getParent().getParent())
                .animate()
                .translationY(-30.0f)
                .alpha(0.0f)
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationEnd(Animator animation) {

                    super.onAnimationEnd(animation);
                    viewHolder.controlPanel.setVisibility(View.GONE);
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
            if(item.getAlteredTime() == 0 && activity.isAlarmSet(item)) {
              item.setAlarmStopped(true);
              activity.deleteAlarm(item);
            }
            else if(item.getAlteredTime() == 0 && !item.isAlarmStopped()) {
              item.setAlarmStopped(true);
            }
            else if(item.getAlteredTime() == 0 && item.isAlarmStopped()) {
              item.setAlarmStopped(false);
              activity.setAlarm(item);
            }
            else if(item.getAlteredTime() != 0) {

              isLockBlockNotifyChange = true;
              isBlockNotifyChange = true;

              item.setDate((Calendar)item.getOrgDate().clone());
              item.setAlteredTime(0);
              Collections.sort(children.get(groupPosition), SCHEDULED_ITEM_COMPARATOR);

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
          hasPanel = 0;
          viewHolder.controlPanel.setVisibility(View.GONE);
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

      if(checked && actionMode == null && isManuallyChecked) {

        isLockBlockNotifyChange = true;
        isBlockNotifyChange = true;

        // すべての通知を既読し、通知チャネルを削除する
        activity.clearAllNotification();

        if(hasPanel == item.getId()) {
          isClosed = true;
          hasPanel = 0;
        }
        else {
          isClosed = false;
        }
        panelLockId = item.getId();
        if(viewHolder.controlPanel.getVisibility() == View.VISIBLE) {
          ((View)viewHolder.controlPanel.getParent().getParent())
            .animate()
            .translationY(-30.0f)
            .alpha(0.0f)
            .setDuration(150)
            .setListener(new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {

                super.onAnimationEnd(animation);
                viewHolder.controlPanel.setVisibility(View.GONE);
              }
            });
        }

        activity.actionBarFragment.searchView.clearFocus();
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
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) >
                    item.getDayRepeat().getOrdinalNumber()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, dayOfWeek);
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
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) >
                    item.getDayRepeat().getOrdinalNumber()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }

                    month = tmp.get(Calendar.MONTH);
                  }

                  while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                    item.getDayRepeat().getOrdinalNumber()
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

                    while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                      item.getDayRepeat().getOrdinalNumber()
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
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) >
                    item.getDayRepeat().getOrdinalNumber()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, dayOfWeek);
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
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) >
                    item.getDayRepeat().getOrdinalNumber()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getDayRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }

                    month = tmp.get(Calendar.MONTH);
                  }

                  while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) <
                    item.getDayRepeat().getOrdinalNumber()
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
          Collections.sort(children.get(groupPosition), SCHEDULED_ITEM_COMPARATOR);

          activity.deleteAlarm(item);
          activity.setAlarm(item);
          activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);
        }
        else {
          children.get(groupPosition).remove(childPosition);
          item.setDoneDate(Calendar.getInstance());

          activity.deleteAlarm(item);
          activity.deleteDB(item, MyDatabaseHelper.TODO_TABLE);
          activity.insertDB(item, MyDatabaseHelper.DONE_TABLE);
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {

            activity.updateListTask(item, groupPosition, true);
          }
        }, 400);
      }
      else if(checked && isManuallyChecked) {
        item.setSelected(true);
        notifyDataSetChanged();
        checkedItemNum++;
        actionMode.setTitle(Integer.toString(checkedItemNum));
      }
      else if(actionMode != null && isManuallyChecked) {
        item.setSelected(false);
        notifyDataSetChanged();
        checkedItemNum--;
        actionMode.setTitle(Integer.toString(checkedItemNum));
        if(checkedItemNum == 0) {
          actionMode.finish();
        }
      }
    }

    @Override
    public boolean onLongClick(View v) {

      // すべての通知を既読し、通知チャネルを削除する
      activity.clearAllNotification();

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

      // ActionMode時のみツールバーとステータスバーの色を設定
      Window window = activity.getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(ContextCompat.getColor(activity, R.color.darkerGrey));

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
          for(List<ItemAdapter> itemList : children) {
            for(ItemAdapter item : itemList) {
              if(item.isSelected()) {
                itemListToMove.add(0, item);
              }
            }
          }

          String message = activity.getResources().getQuantityString(R.plurals.cab_delete_message,
            itemListToMove.size(), itemListToMove.size()
          ) + " (" + activity.getString(R.string.delete_dialog_message) + ")";
          final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle(R.string.cab_delete)
            .setMessage(message)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                for(List<ItemAdapter> itemList : children) {
                  for(ItemAdapter item : itemList) {
                    if(item.isSelected()) {
                      activity.deleteDB(item, MyDatabaseHelper.TODO_TABLE);
                      MyExpandableListAdapter.children =
                        activity.getChildren(MyDatabaseHelper.TODO_TABLE);
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

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
            }
          });

          dialog.show();

          return true;
        }
        case R.id.move_task_between_list: {

          itemListToMove = new ArrayList<>();
          for(List<ItemAdapter> itemList : children) {
            for(ItemAdapter item : itemList) {
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
            itemListToMove.size(), itemListToMove.size()
          ) + activity.getString(R.string.cab_move_task_message);
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

                whichList = SingleChoiceItemsAdapter.checkedPosition;

                long listId =
                  activity.generalSettings.getNonScheduledLists().get(whichList).getId();
                MyListAdapter.itemList = new ArrayList<>();
                for(ItemAdapter item : activity.queryAllDB(MyDatabaseHelper.TODO_TABLE)) {
                  if(item.getWhichListBelongs() == listId) {
                    MyListAdapter.itemList.add(item);
                  }
                }
                Collections.sort(MyListAdapter.itemList, NON_SCHEDULED_ITEM_COMPARATOR);

                for(ItemAdapter item : itemListToMove) {

                  item.setSelected(false);

                  // リストのIDをitemに登録する
                  item.setWhichListBelongs(listId);

                  MyListAdapter.itemList.add(0, item);
                  activity.deleteAlarm(item);
                }

                int size = MyListAdapter.itemList.size();
                for(int i = 0; i < size; i++) {
                  ItemAdapter item = MyListAdapter.itemList.get(i);
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

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
            }
          });

          dialog.show();

          return true;
        }
        case R.id.clone: {

          itemListToMove = new ArrayList<>();
          for(List<ItemAdapter> itemList : children) {
            for(ItemAdapter item : itemList) {
              if(item.isSelected()) {
                itemListToMove.add(0, item);
              }
            }
          }

          String message = activity.getResources().getQuantityString(R.plurals.cab_clone_message,
            itemListToMove.size(), itemListToMove.size()
          );
          final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle(R.string.cab_clone)
            .setMessage(message)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                MainEditFragment.checkedItemNum = checkedItemNum;
                MainEditFragment.itemListToMove = new ArrayList<>(itemListToMove);
                MainEditFragment.isCloningTask = true;
                itemListToMove.get(itemListToMove.size() - 1).setSelected(false);
                ItemAdapter cloneItem = itemListToMove.get(itemListToMove.size() - 1).clone();
                cloneItem.setId(Calendar.getInstance().getTimeInMillis());
                activity.showMainEditFragment(cloneItem);

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

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
            }
          });

          dialog.show();

          return true;
        }
        case R.id.share: {

          itemListToMove = new ArrayList<>();
          for(List<ItemAdapter> itemList : children) {
            for(ItemAdapter item : itemList) {
              if(item.isSelected()) {
                itemListToMove.add(0, item);
              }
            }
          }

          String message = activity.getResources().getQuantityString(R.plurals.cab_share_message,
            itemListToMove.size(), itemListToMove.size()
          );
          final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle(R.string.cab_share)
            .setMessage(message)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                for(ItemAdapter item : itemListToMove) {
                  String sendContent = activity.getString(R.string.due_date) + ": "
                    + DateFormat.format("yyyy/M/d k:mm", item.getDate())
                    + LINE_SEPARATOR
                    + activity.getString(R.string.detail) + ": " + item.getDetail()
                    + LINE_SEPARATOR
                    + activity.getString(R.string.memo) + ": " + item.getNotesString();

                  Intent intent = new Intent()
                    .setAction(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, sendContent);
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

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
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
      window.setStatusBarColor(activity.statusBarColor);

      MyExpandableListAdapter.this.actionMode = null;
      for(List<ItemAdapter> itemList : children) {
        for(ItemAdapter item : itemList) {
          if(item.isSelected()) {
            item.setSelected(false);
          }
        }
      }

      checkedItemNum = 0;
      notifyDataSetChanged();
    }
  }

  @Override
  public Filter getFilter() {

    return new Filter() {
      @Override
      protected FilterResults performFiltering(CharSequence constraint) {

        // アカウントテスト用
        if(CHANGE_GRADE.equals(constraint.toString())) {
          if(activity.isPremium) {
            activity.setBooleanGeneralInSharedPreferences(
              IS_PREMIUM,
              false
            );
          }
          else {
            activity.setBooleanGeneralInSharedPreferences(
              IS_PREMIUM,
              true
            );
          }
        }

        // 入力文字列が大文字を含むかどうか調べる
        boolean isUpper = false;
        for(int i = 0; i < constraint.length(); i++) {
          if(Character.isUpperCase(constraint.charAt(i))) {
            isUpper = true;
            break;
          }
        }

        // 検索処理
        if(activity.actionBarFragment.checkedTag == -1) {
          children = activity.getChildren(MyDatabaseHelper.TODO_TABLE);
        }
        else {
          children = activity.actionBarFragment.filteredLists;
        }

        filteredList = new ArrayList<>();
        for(List<ItemAdapter> itemList : children) {
          List<ItemAdapter> filteredItem = new ArrayList<>();

          for(ItemAdapter item : itemList) {
            if(item.getDetail() != null) {
              String detail = item.getDetail();

              if(!isUpper) {
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

        children = (List<List<ItemAdapter>>)results.values;
        if(children == null) {
          children = new ArrayList<>();
          int size = groups.size();
          for(int i = 0; i < size; i++) {
            children.add(new ArrayList<ItemAdapter>());
          }
        }

        // リストの表示更新
        notifyDataSetChanged();
      }
    };
  }

  @Override
  public int getGroupCount() {

    // 表示するgroupsのサイズを返す。
    int count = 0;
    Arrays.fill(displayGroups, false);
    int size = groups.size();
    for(int i = 0; i < size; i++) {
      if(children.get(i).size() != 0) {
        displayGroups[i] = true;
        count++;
      }
    }

    return count;
//    return groups.size();
  }

  @Override
  public int getChildrenCount(int i) {

    // getChildrenCount()はgetGroupCount()によって返されるgroupsのサイズ分だけ(サイズが3なら3回)呼ばれる。
    // iはgetChildrenCount()の呼ばれた回数を表す。すなわちiは呼び出しを3回とすると1回目の呼び出しにおいて、
    // 表示するgroupsの0番目を表す。2回目では1番目、3回目では2番目である。
    int count = 0;
    int size = groups.size();
    for(int j = 0; j < size; j++) {
      if(displayGroups[j]) {
        // 単に return children.get(j).size() とすると、表示するgroupsの1番目だけを返し続けてしまうので、
        // if(count == i) と条件を付けることで、getChildrenCount()の呼び出された回数に応じて表示する
        // groupsの対応するgroupのみ返すようにしている。
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

    // getGroupCount()によって返されるgroupsのサイズ分だけ呼ばれる。引数のiに関してもgetChildrenCount()と同じ。
    if(i == -1) {
      return null;
    }
    int count = 0;
    int size = groups.size();
    for(int j = 0; j < size; j++) {
      if(displayGroups[j]) {
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

    // getChildrenCount()によって返されるchildrenのサイズ分×getGroupCount()によって返されるgroupsのサイズ分
    // だけ呼ばれる。あとは他のメソッドと同じ。
    int count = 0;
    int size = groups.size();
    for(int j = 0; j < size; j++) {
      if(displayGroups[j]) {
        if(count == i) {
          try {
            return children.get(j).get(i1);
          }
          catch(IndexOutOfBoundsException e) {
            Log.e("MyExpList#getChild", Log.getStackTraceString(e));
            return null;
          }
        }
        count++;
      }
    }

    try {
      return children.get(i).get(i1);
    }
    catch(IndexOutOfBoundsException e) {
      Log.e("MyExpList#getChild", Log.getStackTraceString(e));
      return null;
    }
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
  public View getGroupView(int i, boolean isExpanded, View convertView, ViewGroup viewGroup) {

    final GroupViewHolder groupViewHolder;

    if(convertView == null || convertView.getTag() == null) {
      convertView = View.inflate(viewGroup.getContext(), R.layout.parent_layout, null);

      groupViewHolder = new GroupViewHolder();
      groupViewHolder.indicator = convertView.findViewById(R.id.indicator);

      convertView.setTag(groupViewHolder);
    }
    else {
      groupViewHolder = (GroupViewHolder)convertView.getTag();
    }

    if(isExpanded) {
      groupViewHolder.indicator.setImageResource(R.drawable.ic_expand_more_grey_24dp);
    }
    else {
      groupViewHolder.indicator.setImageResource(R.drawable.ic_expand_less_right_grey_24dp);
    }

    // グループの開閉状態の保持
    ((ExpandableListView)viewGroup).setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
      @Override
      public void onGroupCollapse(int groupPosition) {

        if(isManualExpandOrCollapse) {
          int count = 0;
          int size = groups.size();
          for(int j = 0; j < size; j++) {
            if(displayGroups[j]) {
              if(count == groupPosition) {
                collapseGroup |= 1 << j;

                activity.getSharedPreferences(INT_GENERAL, MODE_PRIVATE)
                  .edit()
                  .putInt(COLLAPSE_GROUP, collapseGroup)
                  .apply();

                break;
              }
              count++;
            }
          }
        }

        isManualExpandOrCollapse = true;
      }
    });
    ((ExpandableListView)viewGroup).setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
      @Override
      public void onGroupExpand(int groupPosition) {

        if(isManualExpandOrCollapse) {
          int count = 0;
          int size = groups.size();
          for(int j = 0; j < size; j++) {
            if(displayGroups[j]) {
              if(count == groupPosition) {
                collapseGroup &= ~(1 << j);

                activity.getSharedPreferences(INT_GENERAL, MODE_PRIVATE)
                  .edit()
                  .putInt(COLLAPSE_GROUP, collapseGroup)
                  .apply();

                break;
              }
              count++;
            }
          }
        }

        isManualExpandOrCollapse = true;
      }
    });

    int size = groups.size();
    int count = 0;
    for(int j = 0; j < size; j++) {
      if(displayGroups[j]) {
        isManualExpandOrCollapse = false;
        if((collapseGroup & 1 << j) != 0) {
          ((ExpandableListView)viewGroup).collapseGroup(count);
        }
        else {
          ((ExpandableListView)viewGroup).expandGroup(count);
        }
        count++;
      }
    }

    TextView day = convertView.findViewById(R.id.day);
    if(activity.isDarkMode) {
      ConstraintLayout groupView = convertView.findViewById(R.id.group_view);
      if(groupView == null) {
        notifyDataSetChanged();
        return convertView;
      }
      groupView.setBackground(ContextCompat.getDrawable(
        activity,
        R.drawable.expandable_group_view_dark
      ));
      day.setTextColor(activity.secondaryTextMaterialDarkColor);
    }
    try {
      day.setText((String)getGroup(i));
    }
    catch(NullPointerException e) {
      Log.e("MyExpList#getGroupView", Log.getStackTraceString(e));
      notifyDataSetChanged();
    }

    return convertView;
  }

  @Override
  public View getChildView(int i, int i1, boolean b, View convertView, final ViewGroup viewGroup) {

    // タスクが空のときにExpandableListViewFragment#onCreateView()が呼ばれると、タスクが存在しないので
    // Groupも表示されず、group_heightに0が代入されてしまう。よって、MainEditFragmentからタスクの登録処理が
    // 行われるときは必ずnotifyDataSetChanged()が呼ばれるので、その呼び出しに伴い必ず呼ばれるこの
    // getChildView()内でgroup_heightが0のときに限りgroup_heightの更新処理を行う。
    if(ExpandableListViewFragment.groupHeight == 0) {
      activity.expandableListView.post(new Runnable() {
        @Override
        public void run() {

          View child = activity.expandableListView.getChildAt(0);
          if(child != null) {
            ExpandableListViewFragment.groupHeight = child.getHeight();
          }
        }
      });
    }

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
      viewHolder.childCard = convertView.findViewById(R.id.child_card);
      viewHolder.clockImage = convertView.findViewById(R.id.clock_image);
      viewHolder.time = convertView.findViewById(R.id.date);
      viewHolder.detail = convertView.findViewById(R.id.detail);
      viewHolder.repeat = convertView.findViewById(R.id.repeat);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.tagPallet = convertView.findViewById(R.id.tag_pallet);
      viewHolder.controlCard = convertView.findViewById(R.id.control_card);
      viewHolder.controlPanel = convertView.findViewById(R.id.control_panel);
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

    // 現在のビュー位置でのitemの取得とリスナーの初期化
    ItemAdapter item = (ItemAdapter)getChild(i, i1);
    if(item == null) {
      notifyDataSetChanged();
      return convertView;
    }

    int count = 0;
    MyOnClickListener listener = null;
    int size = groups.size();
    for(int j = 0; j < size; j++) {
      if(displayGroups[j]) {
        if(count == i) {
          listener = new MyOnClickListener(j, i1, item, viewHolder);
          break;
        }
        count++;
      }
    }

    // 各リスナーの設定
    viewHolder.childCard.setOnClickListener(listener);
    viewHolder.clockImage.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    viewHolder.childCard.setOnLongClickListener(listener);
    viewHolder.clockImage.setOnLongClickListener(listener);
    viewHolder.checkBox.setOnLongClickListener(listener);

    int controlPanelSize = viewHolder.controlPanel.getChildCount();
    for(int j = 0; j < controlPanelSize; j++) {
      TableRow tableRow = (TableRow)viewHolder.controlPanel.getChildAt(j);
      int tableRowSize = tableRow.getChildCount();
      for(int k = 0; k < tableRowSize; k++) {
        TextView panelItem = (TextView)tableRow.getChildAt(k);
        if(activity.isDarkMode) {
          panelItem.setTextColor(activity.secondaryTextMaterialDarkColor);
        }
        panelItem.setOnClickListener(listener);
      }
    }

    // 各種表示処理
    if(activity.isDarkMode) {
      viewHolder.childCard.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      viewHolder.controlCard.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      TextView[] textViews = {
        viewHolder.detail, viewHolder.repeat
      };
      for(TextView textView : textViews) {
        textView.setTextColor(activity.secondaryTextMaterialDarkColor);
      }
    }
    displayDate(viewHolder, item);
    viewHolder.detail.setText(item.getDetail());
    viewHolder.detail.setTextSize(activity.textSize);
    displayRepeat(viewHolder, item);
    if(item.getWhichTagBelongs() == 0) {
      viewHolder.tagPallet.setVisibility(View.GONE);
    }
    else {
      viewHolder.tagPallet.setVisibility(View.VISIBLE);
      TagAdapter tag = activity.generalSettings.getTagById(item.getWhichTagBelongs());
      int color = 0;
      if(tag != null) {
        color = tag.getPrimaryColor();
      }
      if(color != 0) {
        viewHolder.tagPallet.setColorFilter(color);
      }
      else {
        viewHolder.tagPallet.setColorFilter(ContextCompat.getColor(activity, R.color.iconGray));
      }
    }
    if(item.getNotesList().size() == 0) {
      if(!activity.isDarkMode) {
        viewHolder.notes.setTextColor(defaultColorStateList);
      }
    }
    else {
      viewHolder.notes.setTextColor(activity.accentColor);
    }
    viewHolder.minusTime1.setText(activity.getControlTimeText(true, 1));
    viewHolder.minusTime2.setText(activity.getControlTimeText(true, 2));
    viewHolder.minusTime3.setText(activity.getControlTimeText(true, 3));
    viewHolder.plusTime1.setText(activity.getControlTimeText(false, 1));
    viewHolder.plusTime2.setText(activity.getControlTimeText(false, 2));
    viewHolder.plusTime3.setText(activity.getControlTimeText(false, 3));

    // ある子ビューでコントロールパネルを出したとき、他の子ビューのコントロールパネルを閉じる
    if(viewHolder.controlPanel.getVisibility() == View.VISIBLE &&
      (item.getId() != hasPanel || actionMode != null)) {
      ((View)viewHolder.controlPanel.getParent().getParent())
        .animate()
        .translationY(-30.0f)
        .alpha(0.0f)
        .setDuration(150)
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {

            super.onAnimationEnd(animation);
            viewHolder.controlPanel.setVisibility(View.GONE);
          }
        });
    }
    else if(viewHolder.controlPanel.getVisibility() == View.GONE && item.getId() == hasPanel &&
      actionMode == null) {
      View cardView = (View)viewHolder.controlPanel.getParent().getParent();
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
            viewHolder.controlPanel.setVisibility(View.VISIBLE);
          }
        });
    }

    // チェックが入っている場合、チェックを外す
    if(viewHolder.checkBox.isChecked() && !item.isSelected()) {
      isManuallyChecked = false;
      viewHolder.checkBox.setChecked(false);
    }
    else if(!viewHolder.checkBox.isChecked() && item.isSelected()) {
      isManuallyChecked = false;
      viewHolder.checkBox.setChecked(true);
    }
    isManuallyChecked = true;

    // CardViewが横から流れてくるアニメーション
    if(isScrolling && activity.isPlaySlideAnimation) {
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

          for(List<ItemAdapter> itemList : children) {
            Collections.sort(itemList, SCHEDULED_ITEM_COMPARATOR);
          }
          activity.updateListTask(null, -1, true);
          runnable = null;
        }
      };
    }
    handler.postDelayed(runnable, 3500);
  }

  // 時間を表示する処理
  private void displayDate(ChildViewHolder viewHolder, ItemAdapter item) {

    Calendar now = Calendar.getInstance();
    String setTime;
    if(now.get(Calendar.YEAR) == item.getDate().get(Calendar.YEAR)) {
      if(LOCALE.equals(Locale.JAPAN)) {
        setTime = (String)DateFormat.format("M月d日(E) k:mm", item.getDate());
      }
      else {
        setTime = (String)DateFormat.format("M/d (E) k:mm", item.getDate());
      }
    }
    else {
      if(LOCALE.equals(Locale.JAPAN)) {
        setTime = (String)DateFormat.format("yyyy年M月d日(E) k:mm", item.getDate());
      }
      else {
        setTime = (String)DateFormat.format("yyyy/M/d (E) k:mm", item.getDate());
      }
    }
    long dateSub = item.getDate().getTimeInMillis() - now.getTimeInMillis();

    boolean dateIsMinus = false;
    if(dateSub < 0) {
      dateSub = -dateSub;
      dateIsMinus = true;
    }

    int howFarYears = 0;
    tmp = (Calendar)now.clone();
    if(dateIsMinus) {
      tmp.add(Calendar.YEAR, -1);
      while(tmp.after(item.getDate())) {
        tmp.add(Calendar.YEAR, -1);
        howFarYears++;
      }
    }
    else {
      tmp.add(Calendar.YEAR, 1);
      while(tmp.before(item.getDate())) {
        tmp.add(Calendar.YEAR, 1);
        howFarYears++;
      }
    }

    int howFarMonths = 0;
    tmp = (Calendar)now.clone();
    if(howFarYears != 0) {
      tmp.add(Calendar.YEAR, howFarYears);
    }
    if(dateIsMinus) {
      tmp.add(Calendar.MONTH, -1);
      while(tmp.after(item.getDate())) {
        tmp.add(Calendar.MONTH, -1);
        howFarMonths++;
      }
    }
    else {
      tmp.add(Calendar.MONTH, 1);
      while(tmp.before(item.getDate())) {
        tmp.add(Calendar.MONTH, 1);
        howFarMonths++;
      }
    }

    int howFarWeeks = 0;
    tmp = (Calendar)now.clone();
    if(howFarYears != 0) {
      tmp.add(Calendar.YEAR, howFarYears);
    }
    if(howFarMonths != 0) {
      tmp.add(Calendar.MONTH, howFarMonths);
    }
    if(dateIsMinus) {
      tmp.add(Calendar.DAY_OF_WEEK_IN_MONTH, -1);
      while(tmp.after(item.getDate())) {
        tmp.add(Calendar.DAY_OF_WEEK_IN_MONTH, -1);
        howFarWeeks++;
      }
    }
    else {
      tmp.add(Calendar.DAY_OF_WEEK_IN_MONTH, 1);
      while(tmp.before(item.getDate())) {
        tmp.add(Calendar.DAY_OF_WEEK_IN_MONTH, 1);
        howFarWeeks++;
      }
    }

    int howFarDays = (int)(dateSub / (24 * HOUR));
    int howFarHours = (int)(dateSub / HOUR);
    int howFarMinutes = (int)(dateSub / MINUTE);


    Resources res = activity.getResources();
    String displayDate = setTime + " (";
    if(!LOCALE.equals(Locale.JAPAN)) {
      displayDate += " ";
    }
    if(howFarYears != 0) {
      displayDate += res.getQuantityString(R.plurals.year, howFarYears, howFarYears);
      if(!LOCALE.equals(Locale.JAPAN)) {
        displayDate += " ";
      }
      if(howFarMonths != 0) {
        displayDate += res.getQuantityString(R.plurals.month, howFarMonths, howFarMonths);
        if(!LOCALE.equals(Locale.JAPAN)) {
          displayDate += " ";
        }
      }
      if(howFarWeeks != 0) {
        displayDate += res.getQuantityString(R.plurals.week, howFarWeeks, howFarWeeks);
        if(!LOCALE.equals(Locale.JAPAN)) {
          displayDate += " ";
        }
      }
    }
    else if(howFarMonths != 0) {
      displayDate += res.getQuantityString(R.plurals.month, howFarMonths, howFarMonths);
      if(!LOCALE.equals(Locale.JAPAN)) {
        displayDate += " ";
      }
      if(howFarWeeks != 0) {
        displayDate += res.getQuantityString(R.plurals.week, howFarWeeks, howFarWeeks);
        if(!LOCALE.equals(Locale.JAPAN)) {
          displayDate += " ";
        }
      }
    }
    else if(howFarWeeks != 0) {
      displayDate += res.getQuantityString(R.plurals.week, howFarWeeks, howFarWeeks);
      if(!LOCALE.equals(Locale.JAPAN)) {
        displayDate += " ";
      }
      howFarDays -= 7 * howFarWeeks;
      if(howFarDays != 0) {
        displayDate += res.getQuantityString(R.plurals.day, howFarDays, howFarDays);
        if(!LOCALE.equals(Locale.JAPAN)) {
          displayDate += " ";
        }
      }
    }
    else if(howFarDays != 0) {
      displayDate += res.getQuantityString(R.plurals.day, howFarDays, howFarDays);
      if(!LOCALE.equals(Locale.JAPAN)) {
        displayDate += " ";
      }
    }
    else if(howFarHours != 0) {
      displayDate += res.getQuantityString(R.plurals.hour, howFarHours, howFarHours);
      if(!LOCALE.equals(Locale.JAPAN)) {
        displayDate += " ";
      }
      howFarMinutes -= 60 * howFarHours;
      if(howFarMinutes != 0) {
        displayDate += res.getQuantityString(R.plurals.minute, howFarMinutes, howFarMinutes);
        if(!LOCALE.equals(Locale.JAPAN)) {
          displayDate += " ";
        }
      }
    }
    else if(howFarMinutes != 0) {
      displayDate += res.getQuantityString(R.plurals.minute, howFarMinutes, howFarMinutes);
      if(!LOCALE.equals(Locale.JAPAN)) {
        displayDate += " ";
      }
    }
    else {
      displayDate += activity.getString(R.string.within_one_minute);
      if(!LOCALE.equals(Locale.JAPAN)) {
        displayDate += " ";
      }
    }
    displayDate += ")";

    viewHolder.time.setText(displayDate);

    if(item.isAlarmStopped()) {
      viewHolder.time.setTextColor(Color.GRAY);
    }
    else if(dateIsMinus) {
      if(activity.isDarkMode) {
        viewHolder.time.setTextColor(ContextCompat.getColor(
          activity,
          R.color.red6PrimaryColor
        ));
      }
      else {
        viewHolder.time.setTextColor(Color.RED);
      }
    }
    else {
      if(activity.isDarkMode) {
        viewHolder.time.setTextColor(activity.secondaryTextMaterialDarkColor);
      }
      else {
        viewHolder.time.setTextColor(Color.BLACK);
      }
    }

    if(item.isAlarmStopped()) {
      viewHolder.clockImage.setColorFilter(Color.GRAY);
    }
    else if(item.getAlteredTime() != 0) {
      if(activity.isDarkMode) {
        viewHolder.clockImage.setColorFilter(ContextCompat.getColor(
          activity,
          R.color.blue6PrimaryColor
        ));
      }
      else {
        viewHolder.clockImage.setColorFilter(Color.BLUE);
      }
    }
    else {
      if(activity.isDarkMode) {
        viewHolder.clockImage.setColorFilter(ContextCompat.getColor(
          activity,
          R.color.green5PrimaryColor
        ));
      }
      else {
        viewHolder.clockImage.setColorFilter(0xFF09C858);
      }
    }
  }

  // リピートを表示する処理
  private void displayRepeat(ChildViewHolder viewHolder, ItemAdapter item) {

    String repeatStr = "";
    String tmp = item.getDayRepeat().getLabel();
    if(tmp != null && !"".equals(tmp) && !activity.getString(R.string.none).equals(tmp)) {
      if(!LOCALE.equals(Locale.JAPAN)) {
        repeatStr += "Repeat ";
      }
      repeatStr += tmp;
      int scale = item.getDayRepeat().getScale();
      int template = item.getDayRepeat().getWhichTemplate();
      if(LOCALE.equals(Locale.JAPAN)) {
        if(template > 0 && template < 1 << 5) {
          if(template > 1) {
            repeatStr += "に";
          }
        }
        else if(scale > 1) {
          repeatStr += "に";
        }
      }
    }

    tmp = item.getMinuteRepeat().getLabel();
    if(tmp != null && !"".equals(tmp) && !activity.getString(R.string.none).equals(tmp)) {
      if(!LOCALE.equals(Locale.JAPAN) && !"".equals(repeatStr)) {
        repeatStr += " and ";
      }
      repeatStr += tmp;
    }

    String day = item.getDayRepeat().getLabel();
    String minute = item.getMinuteRepeat().getLabel();
    if(LOCALE.equals(Locale.JAPAN) && day != null && !"".equals(day) &&
      !activity.getString(R.string.none).equals(day)
      &&
      (minute == null || "".equals(minute) || activity.getString(R.string.none).equals(minute))) {
      repeatStr += "繰り返す";
    }

    if("".equals(repeatStr)) {
      viewHolder.repeat.setText(R.string.non_repeat);
    }
    else {
      viewHolder.repeat.setText(repeatStr);
    }
  }

  @Override
  public boolean isChildSelectable(int i, int i1) {

    return true;
  }
}
