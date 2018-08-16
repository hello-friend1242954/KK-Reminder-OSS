package com.example.hideaki.reminder;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyExpandableListAdapter extends BaseExpandableListAdapter implements Filterable {

  private static String set_time;
  private static Calendar tmp;
  private static Calendar tmp2;
  private static Calendar tmp3;
  private static Calendar tmp4;
  private static boolean[] display_groups = new boolean[5];
  static final List<String> groups;
  public static List<List<Item>> children;
  private static List<List<Item>> org_children;
  private Context context;
  private OnFragmentInteractionListener mListener;
  private Pattern pattern;
  private Matcher matcher;
  private TableRow tableRow;
  private TextView panel_item;
  private boolean date_is_minus;
  private Item item;
  private MyOnClickListener listener;
  private static long has_panel; //コントロールパネルがvisibleであるItemのid値を保持する
  private MyComparator comparator = new MyComparator();
  private Calendar now;
  private int day_of_week;
  private int day_of_week_last;
  private int day_of_month;
  private int day_of_month_last;
  private int month;
  private int month_last;
  private int max_day_of_month;
  private int max_bit;
  private boolean match_to_ordinal_num;
  private boolean sunday_match;

  static {
    groups = new ArrayList<>();

    groups.add("過去");
    groups.add("今日");
    groups.add("明日");
    groups.add("一週間");
    groups.add("一週間以上");
  }

  public MyExpandableListAdapter(List<List<Item>> children, Context context) {
    this.children = children;
    this.context = context;

    if(context instanceof OnFragmentInteractionListener) {
      mListener = (OnFragmentInteractionListener)context;
    } else {
      throw new RuntimeException(context.toString()
          + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public Filter getFilter() {
    Filter filter = new Filter() {
      @Override
      protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        List<List<Item>> filteredList = new ArrayList<>();

        //入力文字列が大文字を含むかどうか調べる
        boolean is_upper = false;
        for(int i = 0; i < constraint.length(); i++) {
          if(Character.isUpperCase(constraint.charAt(i))) {
            is_upper = true;
            break;
          }
        }

        //検索処理
        if(org_children == null) org_children = children;
        else children = org_children;

        for(List<Item> itemList : children) {
          List<Item> filteredItem = new ArrayList<>();

          for(Item item : itemList) {
            if(item.getDetail() != null) {
              String detail = item.getDetail();

              if(!is_upper) {
                detail = detail.toLowerCase();
              }

              pattern = Pattern.compile(constraint.toString());
              matcher = pattern.matcher(detail);

              if(matcher.find()) {
                filteredItem.add(item);
              }
            }
          }

          filteredList.add(filteredItem);
        }

        results.count = filteredList.size();
        results.values = filteredList;

        return results;
      }

      @Override
      protected void publishResults(CharSequence constraint, FilterResults results) {
        children = (List<List<Item>>)results.values;

        //リストの表示更新
        notifyDataSetChanged();
      }
    };

    return filter;
  }

  private class MyComparator implements Comparator<Item> {

    @Override
    public int compare(Item o1, Item o2) {
      if(o1.getDate().getTimeInMillis() < o2.getDate().getTimeInMillis()) {
        return -1;
      }
      else if(o1.getDate().getTimeInMillis() == o2.getDate().getTimeInMillis()) {
        return 0;
      }
      else return 1;
    }
  }

  private static class ChildViewHolder {

    TextView time;
    TextView detail;
    TextView repeat;
    CardView child_card;
    ImageView clock_image;
    CheckBox check_item;
    TableLayout control_panel;
  }

  private class MyOnClickListener implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private int group_position;
    private int child_position;
    private Item item;
    private View convertView;
    private ChildViewHolder viewHolder;

    public MyOnClickListener(int group_position, int child_position, Item item, View convertView,
                             ChildViewHolder viewHolder) {

      this.group_position = group_position;
      this.child_position = child_position;
      this.item = item;
      this.convertView = convertView;
      this.viewHolder = viewHolder;
    }

    @Override
    public void onClick(View v) {

      switch(v.getId()) {
        case R.id.child_card:
          if(viewHolder.control_panel.getVisibility() == View.GONE) {
            has_panel = item.getId();
            viewHolder.control_panel.setVisibility(View.VISIBLE);
          }
          else viewHolder.control_panel.setVisibility(View.GONE);
          break;
        case R.id.clock_image:
          if(item.getTime_altered() == 0 && mListener.isAlarmSetted(item)) {
            item.setAlarm_stopped(true);
            mListener.deleteAlarm(item);
          }
          else if(item.getTime_altered() == 0 && !item.isAlarm_stopped()) {
            item.setAlarm_stopped(true);
          }
          else if(item.getTime_altered() == 0 && item.isAlarm_stopped()) {
            item.setAlarm_stopped(false);
            mListener.setAlarm(item);
          }
          else if(item.getTime_altered() != 0) {
            item.setDate((Calendar)item.getOrg_date().clone());
            item.setTime_altered(0);
            mListener.deleteAlarm(item);
            mListener.setAlarm(item);
          }

          try {
            mListener.updateDB(item, MyDatabaseHelper.TODO_TABLE);
          } catch(IOException e) {
            e.printStackTrace();
          }

          displayDate(viewHolder, item);
          break;
        case R.id.m5m:
          if(item.getDate().getTimeInMillis() > System.currentTimeMillis() + 5 * 60 * 1000) {
            if(item.getTime_altered() == 0) {
              item.setOrg_date((Calendar)item.getDate().clone());
            }
            item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + -5 * 60 * 1000);

            item.addTime_altered(-5 * 60 * 1000);
            if(item.isAlarm_stopped()) item.setAlarm_stopped(false);

            mListener.deleteAlarm(item);
            mListener.setAlarm(item);
            try {
              mListener.updateDB(item, MyDatabaseHelper.TODO_TABLE);
            } catch(IOException e) {
              e.printStackTrace();
            }

            displayDate(viewHolder, item);
          }
          break;
        case R.id.m1h:
          if(item.getDate().getTimeInMillis() > System.currentTimeMillis() + 1 * 60 * 60 * 1000) {
            if(item.getTime_altered() == 0) {
              item.setOrg_date((Calendar)item.getDate().clone());
            }
            item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + -1 * 60 * 60 * 1000);

            item.addTime_altered(-1 * 60 * 60 * 1000);
            if(item.isAlarm_stopped()) item.setAlarm_stopped(false);

            mListener.deleteAlarm(item);
            mListener.setAlarm(item);
            try {
              mListener.updateDB(item, MyDatabaseHelper.TODO_TABLE);
            } catch(IOException e) {
              e.printStackTrace();
            }

            displayDate(viewHolder, item);
          }
          break;
        case R.id.m1d:
          if(item.getDate().getTimeInMillis() > System.currentTimeMillis() + 24 * 60 * 60 * 1000) {
            if(item.getTime_altered() == 0) {
              item.setOrg_date((Calendar)item.getDate().clone());
            }
            item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + -24 * 60 * 60 * 1000);

            item.addTime_altered(-24 * 60 * 60 * 1000);
            if(item.isAlarm_stopped()) item.setAlarm_stopped(false);

            mListener.deleteAlarm(item);
            mListener.setAlarm(item);
            try {
              mListener.updateDB(item, MyDatabaseHelper.TODO_TABLE);
            } catch(IOException e) {
              e.printStackTrace();
            }

            displayDate(viewHolder, item);
          }
          break;
        case R.id.edit:
          mListener.showMainEditFragment(item);
          viewHolder.control_panel.setVisibility(View.GONE);
          break;
        case R.id.p5m:
          if(item.getTime_altered() == 0) {
            item.setOrg_date((Calendar)item.getDate().clone());
          }

          if(item.getDate().getTimeInMillis() < System.currentTimeMillis()) {
            item.getDate().setTimeInMillis(System.currentTimeMillis() + 5 * 60 * 1000);
          }
          else {
            item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + 5 * 60 * 1000);
          }

          item.addTime_altered(5 * 60 * 1000);
          if(item.isAlarm_stopped()) item.setAlarm_stopped(false);

          mListener.deleteAlarm(item);
          mListener.setAlarm(item);
          try {
            mListener.updateDB(item, MyDatabaseHelper.TODO_TABLE);
          } catch(IOException e) {
            e.printStackTrace();
          }

          displayDate(viewHolder, item);
          break;
        case R.id.p1h:
          if(item.getTime_altered() == 0) {
            item.setOrg_date((Calendar)item.getDate().clone());
          }

          if(item.getDate().getTimeInMillis() < System.currentTimeMillis()) {
            item.getDate().setTimeInMillis(System.currentTimeMillis() + 1 * 60 * 60 * 1000);
          }
          else {
            item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + 1 * 60 * 60 * 1000);
          }

          item.addTime_altered(1 * 60 * 60 * 1000);
          if(item.isAlarm_stopped()) item.setAlarm_stopped(false);

          mListener.deleteAlarm(item);
          mListener.setAlarm(item);
          try {
            mListener.updateDB(item, MyDatabaseHelper.TODO_TABLE);
          } catch(IOException e) {
            e.printStackTrace();
          }

          displayDate(viewHolder, item);
          break;
        case R.id.p1d:
          if(item.getTime_altered() == 0) {
            item.setOrg_date((Calendar)item.getDate().clone());
          }

          if(item.getDate().getTimeInMillis() < System.currentTimeMillis()) {
            item.getDate().setTimeInMillis(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
          }
          else {
            item.getDate().setTimeInMillis(item.getDate().getTimeInMillis() + 24 * 60 * 60 * 1000);
          }

          item.addTime_altered(24 * 60 * 60 * 1000);
          if(item.isAlarm_stopped()) item.setAlarm_stopped(false);

          mListener.deleteAlarm(item);
          mListener.setAlarm(item);
          try {
            mListener.updateDB(item, MyDatabaseHelper.TODO_TABLE);
          } catch(IOException e) {
            e.printStackTrace();
          }

          displayDate(viewHolder, item);
          break;
        case R.id.notes:
          mListener.showNotesFragment(item);
          break;
      }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

      if(isChecked) {

        if(item.getTime_altered() == 0) {
          item.setOrg_date((Calendar)item.getDate().clone());
        }
        else {
          item.setDate((Calendar)item.getOrg_date().clone());
        }
        if((item.getRepeat().getSetted() & (1 << 0)) != 0) {

          //Dayリピート設定時
          now = Calendar.getInstance();

          if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
            tmp = (Calendar)item.getDate().clone();
            tmp.add(Calendar.DAY_OF_MONTH, item.getRepeat().getInterval());
          }
          else {
            tmp = (Calendar)now.clone();
            tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
            tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
            tmp.set(Calendar.SECOND, 0);

            if(tmp.before(now)) tmp.add(Calendar.DAY_OF_MONTH, item.getRepeat().getInterval());
          }
        }
        else if((item.getRepeat().getSetted() & (1 << 1)) != 0) {

          //Weekリピート設定時
          now = Calendar.getInstance();

          if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
            tmp = (Calendar)item.getDate().clone();
            day_of_week = item.getDate().get(Calendar.DAY_OF_WEEK) < 2 ?
                item.getDate().get(Calendar.DAY_OF_WEEK) + 5 : item.getDate().get(Calendar.DAY_OF_WEEK) - 2;

            //intervalの処理
            max_bit = 0;
            for(int i = 0; i < 7; i++) {
              if((item.getRepeat().getWeek() & (1 << i)) != 0) {
                max_bit = i;
              }
            }
            day_of_week_last = max_bit;
            if(day_of_week >= day_of_week_last) {
              tmp.add(Calendar.DAY_OF_MONTH, (item.getRepeat().getInterval() - 1) * 7);
            }

            int i = 1;
            while(i < 7 - day_of_week + 1) {
              if((item.getRepeat().getWeek() & (1 << (day_of_week + i))) != 0) {
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
            day_of_week = now.get(Calendar.DAY_OF_WEEK) < 2 ?
                now.get(Calendar.DAY_OF_WEEK) + 5 : now.get(Calendar.DAY_OF_WEEK) - 2;

            //intervalの処理
            max_bit = 0;
            for(int i = 0; i < 7; i++) {
              if((item.getRepeat().getWeek() & (1 << i)) != 0) {
                max_bit = i;
              }
            }
            day_of_week_last = max_bit;
            if(day_of_week > day_of_week_last) {
              tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
              if(tmp.after(now)) {
                tmp.add(Calendar.DAY_OF_MONTH, -7);
              }
              tmp.add(Calendar.DAY_OF_MONTH, (item.getRepeat().getInterval()) * 7);
              day_of_week = 0;
            }
            else if(day_of_week == day_of_week_last) {
              if(tmp.before(now)) {
                tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                if(tmp.after(now)) {
                  tmp.add(Calendar.DAY_OF_MONTH, -7);
                }
                tmp.add(Calendar.DAY_OF_MONTH, (item.getRepeat().getInterval()) * 7);
                day_of_week = 0;
              }
            }

            int i = 0;
            while(i < 7 - day_of_week) {
              if((item.getRepeat().getWeek() & (1 << (day_of_week + i))) != 0) {
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
                tmp.add(Calendar.DAY_OF_MONTH, (item.getRepeat().getInterval()) * 7);
                day_of_week = 0;
              }
            }
          }
        }
        else if((item.getRepeat().getSetted() & (1 << 2)) != 0) {

          //Monthリピート設定時
          now = Calendar.getInstance();

          if(item.getRepeat().isDays_of_month_setted()) {

            //DaysOfMonthリピート設定時
            if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
              tmp = (Calendar)item.getDate().clone();
              day_of_month = item.getDate().get(Calendar.DAY_OF_MONTH);

              //intervalの処理
              max_bit = 0;
              for(int i = 0; i < 31; i++) {
                if((item.getRepeat().getDays_of_month() & (1 << i)) != 0) {
                  max_bit = i;
                }
              }
              day_of_month_last = max_bit + 1;
              if(day_of_month_last > tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                day_of_month_last = tmp.getActualMaximum(Calendar.DAY_OF_MONTH);
              }
              if(tmp.get(Calendar.DAY_OF_MONTH) >= day_of_month_last) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                day_of_month = 1;
              }

              int i = 0;
              while(i < 31 - day_of_month + 1) {
                if((item.getRepeat().getDays_of_month() & (1 << (day_of_month - 1 + i))) != 0) {
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
                  tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                  day_of_month = 1;
                }
              }
            }
            else {
              tmp = (Calendar)now.clone();
              tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
              tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
              tmp.set(Calendar.SECOND, 0);
              day_of_month = now.get(Calendar.DAY_OF_MONTH);

              //intervalの処理
              max_bit = 0;
              for(int i = 0; i < 31; i++) {
                if((item.getRepeat().getDays_of_month() & (1 << i)) != 0) {
                  max_bit = i;
                }
              }
              day_of_month_last = max_bit + 1;
              if(day_of_month_last > tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                day_of_month_last = tmp.getActualMaximum(Calendar.DAY_OF_MONTH);
              }
              if(tmp.get(Calendar.DAY_OF_MONTH) > day_of_month_last) {
                tmp.set(Calendar.DAY_OF_MONTH, 1);
                tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                day_of_month = 1;
              }
              else if(tmp.get(Calendar.DAY_OF_MONTH) == day_of_month_last) {
                if(tmp.before(now)) {
                  tmp.set(Calendar.DAY_OF_MONTH, 1);
                  tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                  day_of_month = 1;
                }
              }

              int i = 0;
              while(i < 31 - day_of_month + 1) {
                if((item.getRepeat().getDays_of_month() & (1 << (day_of_month - 1 + i))) != 0) {
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
                  tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                  day_of_month = 1;
                }
              }
            }
          }
          else {

            //OnTheMonthリピート設定時
            now = Calendar.getInstance();
            if(item.getRepeat().getOn_the_month().ordinal() < 6) {
              day_of_week = item.getRepeat().getOn_the_month().ordinal() + 2;
            }
            else if(item.getRepeat().getOn_the_month().ordinal() == 6) {
              day_of_week = 1;
            }
            else {
              day_of_week = item.getRepeat().getOn_the_month().ordinal() + 1;
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
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getRepeat().getOrdinal_number()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, day_of_week);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }
                  }

                  while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getRepeat().getOrdinal_number()
                      && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }

                  if(tmp.after(item.getDate())) break;
                  else {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
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
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getRepeat().getOrdinal_number()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }

                    month = tmp.get(Calendar.MONTH);
                  }

                  while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getRepeat().getOrdinal_number()
                      && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }

                  tmp.add(Calendar.DAY_OF_MONTH, item.getRepeat().getWeekday_num());
                  item.getRepeat().setWeekday_num(item.getRepeat().getWeekday_num() + 1);

                  if(tmp.after(item.getDate()) && month == tmp.get(Calendar.MONTH)) {
                    item.getRepeat().setWeekday_num(0);
                    break;
                  }
                  else if(item.getRepeat().getWeekday_num() > 4 || month != tmp.get(Calendar.MONTH)) {
                    tmp.add(Calendar.DAY_OF_MONTH, -item.getRepeat().getWeekday_num() + 1);
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }

                    month = tmp.get(Calendar.MONTH);
                    item.getRepeat().setWeekday_num(0);
                  }
                }
              }
              else if(day_of_week == 9) {
                month = tmp.get(Calendar.MONTH);
                tmp2 = (Calendar)tmp.clone();
                tmp2.add(Calendar.DAY_OF_MONTH, 1);

                match_to_ordinal_num = false;
                if(item.getRepeat().getOrdinal_number() == 5) {
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) == tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                    match_to_ordinal_num = true;
                  }
                }
                else {
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) == item.getRepeat().getOrdinal_number()) {
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
                    if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getRepeat().getOrdinal_number()) {
                      tmp.set(Calendar.DAY_OF_MONTH, 1);
                      tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                      tmp2 = (Calendar)tmp.clone();

                      tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                      if(tmp.before(tmp2)) {
                        tmp.add(Calendar.DAY_OF_MONTH, 7);
                      }
                    }

                    while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getRepeat().getOrdinal_number()
                        && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }

                    if(tmp.after(item.getDate())) break;
                    else {
                      tmp.set(Calendar.DAY_OF_MONTH, 1);
                      tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
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
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getRepeat().getOrdinal_number()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, day_of_week);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }
                  }

                  while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getRepeat().getOrdinal_number()
                      && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }

                  if(tmp.after(now)) break;
                  else {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
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
                  if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getRepeat().getOrdinal_number()) {
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }

                    month = tmp.get(Calendar.MONTH);
                  }

                  while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getRepeat().getOrdinal_number()
                      && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                    tmp.add(Calendar.DAY_OF_MONTH, 7);
                  }

                  tmp.add(Calendar.DAY_OF_MONTH, item.getRepeat().getWeekday_num());
                  item.getRepeat().setWeekday_num(item.getRepeat().getWeekday_num() + 1);

                  if(tmp.after(now) && month == tmp.get(Calendar.MONTH)) {
                    item.getRepeat().setWeekday_num(0);
                    break;
                  }
                  else if(item.getRepeat().getWeekday_num() > 4 || month != tmp.get(Calendar.MONTH)) {
                    tmp.add(Calendar.DAY_OF_MONTH, -item.getRepeat().getWeekday_num() + 1);
                    tmp.set(Calendar.DAY_OF_MONTH, 1);
                    tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                    tmp2 = (Calendar)tmp.clone();

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    if(tmp.before(tmp2)) {
                      tmp.add(Calendar.DAY_OF_MONTH, 7);
                    }

                    month = tmp.get(Calendar.MONTH);
                    item.getRepeat().setWeekday_num(0);
                  }
                }
              }
              else if(day_of_week == 9) {
                sunday_match = false;
                if(tmp.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && tmp.after(now)) {
                  tmp2 = (Calendar)tmp.clone();
                  tmp2.add(Calendar.DAY_OF_MONTH, -1);

                  if(tmp2.get(Calendar.DAY_OF_WEEK_IN_MONTH) == item.getRepeat().getOrdinal_number()) {
                    sunday_match = true;
                  }
                }

                if(!sunday_match) {

                  month = tmp.get(Calendar.MONTH);
                  tmp2 = (Calendar)tmp.clone();
                  tmp2.add(Calendar.DAY_OF_MONTH, 1);

                  match_to_ordinal_num = false;
                  if(item.getRepeat().getOrdinal_number() == 5) {
                    if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) == tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                      match_to_ordinal_num = true;
                    }
                  }
                  else {
                    if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) == item.getRepeat().getOrdinal_number()) {
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
                    } else if(tmp3.get(Calendar.MONTH) == month) {
                      tmp.add(Calendar.DAY_OF_MONTH, -7);
                    }

                    tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);

                    while(true) {

                      //intervalの処理
                      if(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) > item.getRepeat().getOrdinal_number()) {
                        tmp.set(Calendar.DAY_OF_MONTH, 1);
                        tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
                        tmp2 = (Calendar)tmp.clone();

                        tmp.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                        if(tmp.before(tmp2)) {
                          tmp.add(Calendar.DAY_OF_MONTH, 7);
                        }
                      }

                      while(tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < item.getRepeat().getOrdinal_number()
                          && tmp.get(Calendar.DAY_OF_WEEK_IN_MONTH) < tmp.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
                        tmp.add(Calendar.DAY_OF_MONTH, 7);
                      }

                      if(tmp.after(now)) break;
                      else {
                        tmp.set(Calendar.DAY_OF_MONTH, 1);
                        tmp.add(Calendar.MONTH, item.getRepeat().getInterval());
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
        else if((item.getRepeat().getSetted() & (1 << 3)) != 0) {

          //Yearリピート設定時
          now = Calendar.getInstance();

          if(item.getDate().getTimeInMillis() > now.getTimeInMillis()) {
            tmp = (Calendar)item.getDate().clone();
            month = item.getDate().get(Calendar.MONTH);

            //intervalの処理
            max_bit = 0;
            for(int i = 0; i < 12; i++) {
              if((item.getRepeat().getYear() & (1 << i)) != 0) {
                max_bit = i;
              }
            }
            month_last = max_bit;
            if(month >= month_last) {
              tmp.set(Calendar.MONTH, 0);
              tmp.add(Calendar.YEAR, item.getRepeat().getInterval());
              month = 0;
            }

            int i = 0;
            while(i < 12 - month) {
              if((item.getRepeat().getYear() & (1 << (month + i))) != 0) {
                tmp.add(Calendar.MONTH, i);
                if(tmp.get(Calendar.DAY_OF_MONTH) < item.getRepeat().getDay_of_month_of_year()
                    && item.getRepeat().getDay_of_month_of_year() <= tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                  tmp.set(Calendar.DAY_OF_MONTH, item.getRepeat().getDay_of_month_of_year());
                }

                if(tmp.after(item.getDate())) {
                  break;
                }
              }
              i++;

              if(i >= 12 - month) {
                i = 0;
                tmp.set(Calendar.MONTH, 0);
                tmp.add(Calendar.YEAR, item.getRepeat().getInterval());
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
            month = now.get(Calendar.MONTH);

            //intervalの処理
            max_bit = 0;
            for(int i = 0; i < 12; i++) {
              if((item.getRepeat().getYear() & (1 << i)) != 0) {
                max_bit = i;
              }
            }
            month_last = max_bit;
            if(month > month_last) {
              tmp.set(Calendar.MONTH, 0);
              tmp.add(Calendar.YEAR, item.getRepeat().getInterval());
              month = 0;
            }
            else if(month == month_last) {
              if(tmp.before(now)) {
                tmp.set(Calendar.MONTH, 0);
                tmp.add(Calendar.YEAR, item.getRepeat().getInterval());
                month = 0;
              }
            }

            int i = 0;
            while(i < 12 - month) {
              if((item.getRepeat().getYear() & (1 << (month + i))) != 0) {
                tmp.add(Calendar.MONTH, i);
                if(tmp.get(Calendar.DAY_OF_MONTH) < item.getRepeat().getDay_of_month_of_year()
                    && item.getRepeat().getDay_of_month_of_year() <= tmp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                  tmp.set(Calendar.DAY_OF_MONTH, item.getRepeat().getDay_of_month_of_year());
                }

                if(tmp.after(now)) {
                  break;
                }
              }
              i++;

              if(i >= 12 - month) {
                i = 0;
                tmp.set(Calendar.MONTH, 0);
                tmp.add(Calendar.YEAR, item.getRepeat().getInterval());
                month = 0;
              }
            }
          }
        }


        //tmp設定後の処理
        if(item.getRepeat().getSetted() != 0) {
          item.setOrg_alarm_stopped(item.isAlarm_stopped());
          item.setOrg_time_altered(item.getTime_altered());
          item.setDate((Calendar)tmp.clone());

          if(item.isAlarm_stopped()) item.setAlarm_stopped(false);
          if(item.getTime_altered() != 0) item.setTime_altered(0);

          mListener.deleteAlarm(item);
          mListener.setAlarm(item);
          try {
            mListener.updateDB(item, MyDatabaseHelper.TODO_TABLE);
          } catch(IOException e) {
            e.printStackTrace();
          }
        }
        else {
          children.get(group_position).remove(child_position);

          mListener.deleteAlarm(item);
          try {
            mListener.deleteDB(item, MyDatabaseHelper.TODO_TABLE);
          } catch(IOException e) {
            e.printStackTrace();
          }
        }

        Snackbar.make(convertView, context.getResources().getString(R.string.complete), Snackbar.LENGTH_LONG)
            .setAction(R.string.undo, new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                if(item.getRepeat().getSetted() != 0) {
                  item.setAlarm_stopped(item.isOrg_alarm_stopped());
                  item.setTime_altered(item.getOrg_time_altered());
                  item.getDate().setTimeInMillis(item.getOrg_date().getTimeInMillis() + item.getTime_altered());

                  mListener.deleteAlarm(item);
                  if(!item.isAlarm_stopped()) {
                    mListener.setAlarm(item);
                  }
                  try {
                    mListener.updateDB(item, MyDatabaseHelper.TODO_TABLE);
                  } catch(IOException e) {
                    e.printStackTrace();
                  }
                }
                else {
                  item.getDate().setTimeInMillis(item.getOrg_date().getTimeInMillis() + item.getTime_altered());
                  children.get(group_position).add(item);
                  for(List<Item> itemList : children) {
                    Collections.sort(itemList, comparator);
                  }
                  notifyDataSetChanged();

                  if(!item.isAlarm_stopped()) {
                    mListener.setAlarm(item);
                  }
                  try {
                    mListener.insertDB(item, MyDatabaseHelper.TODO_TABLE);
                  } catch(IOException e) {
                    e.printStackTrace();
                  }
                }
              }
            })
            .show();
      }
    }
  }

  @Override
  public int getGroupCount() {

    //表示するgroupsのサイズを返す。
    int count = 0;
    Arrays.fill(display_groups, false);
    for(int i = 0; i < groups.size(); i++) {
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
    for(int j = 0; j < groups.size(); j++) {
      if(display_groups[j]) {
        //単に return children.get(j).size() とすると、表示するgroupsの1番目だけを返し続けてしまうので、
        //if(count == i) と条件を付けることで、getChildrenCount()の呼び出された回数に応じて表示する
        //groupsの対応するgroupのみ返すようにしている。
        if(count == i) return children.get(j).size();
        count++;
      }
    }

    return children.get(i).size();
  }

  @Override
  public Object getGroup(int i) {

    //getGroupCount()によって返されるgroupsのサイズ分だけ呼ばれる。引数のiに関してもgetChildrenCount()と同じ。
    int count = 0;
    for(int j = 0; j < groups.size(); j++) {
      if(display_groups[j]) {
        if(count == i) return groups.get(j);
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
    for(int j = 0; j < groups.size(); j++) {
      if(display_groups[j]) {
        if(count == i) return children.get(j).get(i1);
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
      convertView = LayoutInflater
          .from(viewGroup.getContext())
          .inflate(R.layout.parent_layout, null);
    }

    ((TextView)convertView.findViewById(R.id.day)).setText(getGroup(i).toString());

    return convertView;
  }

  @Override
  public View getChildView(int i, int i1, boolean b, View convertView, final ViewGroup viewGroup) {

    final ChildViewHolder viewHolder;

    if(convertView == null) {
      convertView = LayoutInflater
          .from(viewGroup.getContext())
          .inflate(R.layout.child_layout, null);

      viewHolder = new ChildViewHolder();
      viewHolder.time = convertView.findViewById(R.id.date);
      viewHolder.detail = convertView.findViewById(R.id.detail);
      viewHolder.repeat = convertView.findViewById(R.id.repeat);
      viewHolder.child_card = convertView.findViewById(R.id.child_card);
      viewHolder.clock_image = convertView.findViewById(R.id.clock_image);
      viewHolder.check_item = convertView.findViewById(R.id.check_item);
      viewHolder.control_panel = convertView.findViewById(R.id.control_panel);

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ChildViewHolder)convertView.getTag();
    }

    //現在のビュー位置でのitemの取得とリスナーの初期化
    item = (Item)getChild(i, i1);
    int count = 0;
    for(int j = 0; j < groups.size(); j++) {
      if(display_groups[j]) {
        if(count == i) listener = new MyOnClickListener(j, i1, item, convertView, viewHolder);
        count++;
      }
    }

    //設定された時間、詳細、リピート通知のインターバルを表示
    displayDate(viewHolder, item);
    viewHolder.detail.setText(item.getDetail());
    if(item.getRepeat().getLabel() == null) viewHolder.repeat.setText(R.string.non_repeat);
    else viewHolder.repeat.setText(item.getRepeat().getLabel());

    //チェックが入っている場合、チェックを外す
    if(viewHolder.check_item.isChecked()) {
      viewHolder.check_item.setChecked(false);
    }

    //ある子ビューでコントロールパネルを出したとき、他の子ビューのコントロールパネルを閉じる
    if(item.getId() != has_panel && viewHolder.control_panel.getVisibility() == View.VISIBLE) {
      viewHolder.control_panel.setVisibility(View.GONE);
    }

    viewHolder.child_card.setOnClickListener(listener);
    viewHolder.clock_image.setOnClickListener(listener);
    viewHolder.check_item.setOnCheckedChangeListener(listener);

    for(int j = 0; j < viewHolder.control_panel.getChildCount(); j++) {
      tableRow = (TableRow)viewHolder.control_panel.getChildAt(j);
      for(int k = 0; k < tableRow.getChildCount(); k++) {
        panel_item = (TextView)tableRow.getChildAt(k);
        panel_item.setOnClickListener(listener);
      }
    }

    return convertView;
  }

  //時間を表示する処理
  private void displayDate(ChildViewHolder viewHolder, Item item) {

    Calendar now = Calendar.getInstance();
    if(now.get(Calendar.YEAR) == item.getDate().get(Calendar.YEAR)) {
      set_time = (String)DateFormat.format("M月d日(E)H:mm", item.getDate());
    }
    else {
      set_time = (String)DateFormat.format("yyyy年M月d日(E)H:mm", item.getDate());
    }
    long date_sub = item.getDate().getTimeInMillis() - now.getTimeInMillis();

    date_is_minus = false;
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
    if(how_far_years != 0) tmp.add(Calendar.YEAR, how_far_years);
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
    if(how_far_years != 0) tmp.add(Calendar.YEAR, how_far_years);
    if(how_far_months != 0) tmp.add(Calendar.MONTH, how_far_months);
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

    long how_far_days = date_sub / (1000 * 60 * 60 * 24);
    long how_far_hours = date_sub / (1000 * 60 * 60);
    long how_far_minutes = date_sub / (1000 * 60);


    String display_date = set_time + " (";
    if(how_far_years != 0) {
      display_date += how_far_years + "年";
      if(how_far_months != 0) display_date += how_far_months + "ヶ月";
      if(how_far_weeks != 0) display_date += how_far_weeks + "週間";
    }
    else if(how_far_months != 0) {
      display_date += how_far_months + "ヶ月";
      if(how_far_weeks != 0) display_date += how_far_weeks + "週間";
    }
    else if(how_far_weeks != 0) {
      display_date += how_far_weeks + "週間";
      how_far_days -= 7 * how_far_weeks;
      if(how_far_days != 0) display_date += how_far_days + "日";
    }
    else if(how_far_days != 0) {
      display_date += how_far_days + "日";
    }
    else if(how_far_hours != 0) {
      display_date += how_far_hours + "時間";
      how_far_minutes -= 60 * how_far_hours;
      if(how_far_minutes != 0) display_date += how_far_minutes + "分";
    }
    else if(how_far_minutes != 0) {
      display_date += how_far_minutes + "分";
    }
    else {
      display_date += "<< 1分 >>";
    }
    display_date += ")";

    viewHolder.time.setText(display_date);

    if(item.isAlarm_stopped()) viewHolder.time.setTextColor(Color.GRAY);
    else if(date_is_minus) viewHolder.time.setTextColor(Color.RED);
    else viewHolder.time.setTextColor(Color.BLACK);

    if(item.isAlarm_stopped()) viewHolder.clock_image.setColorFilter(Color.GRAY);
    else if(item.getTime_altered() != 0) viewHolder.clock_image.setColorFilter(Color.BLUE);
    else viewHolder.clock_image.setColorFilter(0xFF09C858);
  }

  @Override
  public boolean isChildSelectable(int i, int i1) {
    return true;
  }

  public interface OnFragmentInteractionListener {
    void showMainEditFragment(Item item);
    void showNotesFragment(Item item);
    void setAlarm(Item item);
    void deleteAlarm(Item item);
    boolean isAlarmSetted(Item item);
    void insertDB(Item item, String table) throws IOException;
    void updateDB(Item item, String table) throws IOException;
    void deleteDB(Item item, String table) throws IOException;
  }
}
