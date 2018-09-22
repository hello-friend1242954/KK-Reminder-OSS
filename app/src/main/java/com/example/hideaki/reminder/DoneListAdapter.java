package com.example.hideaki.reminder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
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
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class DoneListAdapter extends BaseAdapter implements Filterable {

  static List<Item> itemList;
  private MainActivity activity;
  ActionMode actionMode = null;
  static int checked_item_num;
  private static boolean manually_checked;
  private List<Item> filteredItem;
  static int order;

  DoneListAdapter(MainActivity activity) {
    this.activity = activity;
  }

  private static class ViewHolder {

    CardView item_card;
    ImageView order_icon;
    ImageView clock_image;
    TextView time;
    TextView detail;
    TextView repeat;
    CheckBox checkBox;
    ImageView tagPallet;
  }

  private class MyOnClickListener implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,
      View.OnLongClickListener, ActionMode.Callback {

    private int position;
    private Item item;
    private ViewHolder viewHolder;
    private int which_list;
    private List<Item> itemListToMove;
    Calendar tmp;

    MyOnClickListener(int position, Item item, ViewHolder viewHolder) {

      this.position = position;
      this.item = item;
      this.viewHolder = viewHolder;
    }

    @Override
    public void onClick(View v) {

      activity.actionBarFragment.searchView.clearFocus();
      switch(v.getId()) {
        case R.id.child_card:
        case R.id.item_card: {

          if(actionMode == null) {
            String[] items = new String[3];
            if(order == 0) {
              Calendar now = Calendar.getInstance();
              tmp = (Calendar)now.clone();
              tmp.set(Calendar.HOUR_OF_DAY, item.getDate().get(Calendar.HOUR_OF_DAY));
              tmp.set(Calendar.MINUTE, item.getDate().get(Calendar.MINUTE));
              if(tmp.before(now)) {
                tmp.add(Calendar.DAY_OF_MONTH, 1);
              }
              items[0] = DateFormat.format("yyyy年M月d日(E) H時m分", tmp).toString() +
                  activity.getString(R.string.at) + activity.getString(R.string.recycle_task);
            }
            else if(order == 1) {
              items[0] = activity.getString(R.string.recycle_task);
            }
            items[1] = activity.getString(R.string.recycle_and_edit_task);
            items[2] = activity.getString(R.string.delete_task);

            new AlertDialog.Builder(activity)
                .setTitle(R.string.done_task_click_title)
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    which_list = which;
                  }
                })
                .setPositiveButton(R.string.determine, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {

                    itemList.remove(position);
                    notifyDataSetChanged();
                    activity.deleteDB(item, MyDatabaseHelper.DONE_TABLE);

                    if(which_list == 0) {
                      if(order == 0) {
                        item.setDate((Calendar)tmp.clone());
                        item.setAlarm_stopped(false);
                        activity.setAlarm(item);
                      }
                      activity.insertDB(item, MyDatabaseHelper.TODO_TABLE);
                    }
                    else if(which_list == 1) {
                      activity.insertDB(item, MyDatabaseHelper.TODO_TABLE);
                      activity.showMainEditFragment(item, DoneListViewFragment.TAG);
                    }

                    //データベースを更新したら、そのデータベースを端末暗号化ストレージへコピーする
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                      Context direct_boot_context = activity.createDeviceProtectedStorageContext();
                      direct_boot_context.moveDatabaseFrom(activity, MyDatabaseHelper.TODO_TABLE);
                      direct_boot_context.moveDatabaseFrom(activity, MyDatabaseHelper.DONE_TABLE);
                    }
                  }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                  }
                })
                .show();
          }
          else if(viewHolder.checkBox.isChecked()) {
            viewHolder.checkBox.setChecked(false);
          }
          else viewHolder.checkBox.setChecked(true);
          break;
        }
      }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

      if(isChecked && actionMode != null && manually_checked) {
        viewHolder.checkBox.jumpDrawablesToCurrentState();
        item.setSelected(true);
        notifyDataSetChanged();
        checked_item_num++;
        actionMode.setTitle(Integer.toString(checked_item_num));
      }
      else if(actionMode != null && manually_checked) {
        viewHolder.checkBox.jumpDrawablesToCurrentState();
        item.setSelected(false);
        notifyDataSetChanged();
        checked_item_num--;
        actionMode.setTitle(Integer.toString(checked_item_num));
        if(checked_item_num == 0) actionMode.finish();
      }
    }

    @Override
    public boolean onLongClick(View v) {

      if(actionMode != null) {

        if(viewHolder.checkBox.isChecked()) {
          viewHolder.checkBox.setChecked(false);
        }
        else viewHolder.checkBox.setChecked(true);

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
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(activity, R.color.darker_grey));
      }

      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {

      MenuItem cloneItem = menu.findItem(R.id.clone);
      cloneItem.setVisible(false);

      return true;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {

      switch(menuItem.getItemId()) {
        case R.id.delete: {

          itemListToMove = new ArrayList<>();
          for(Item item : itemList) {
            if(item.isSelected()) {
              itemListToMove.add(0, item);
            }
          }

          String message = itemListToMove.size() + activity.getString(R.string.cab_delete_message)
              + "(" + activity.getString(R.string.delete_dialog_message) + ")";
          new AlertDialog.Builder(activity)
              .setTitle(R.string.cab_delete)
              .setMessage(message)
              .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                  for(Item item : itemListToMove) {
                    activity.deleteDB(item, MyDatabaseHelper.DONE_TABLE);
                  }
                  itemList = activity.getDoneItem();
                  notifyDataSetChanged();

                  //データベースを更新したら、そのデータベースを端末暗号化ストレージへコピーする
                  if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Context direct_boot_context = activity.createDeviceProtectedStorageContext();
                    direct_boot_context.moveDatabaseFrom(activity, MyDatabaseHelper.DONE_TABLE);
                  }

                  actionMode.finish();
                }
              })
              .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
              })
              .show();

          return true;
        }
        case R.id.move_task_between_list: {

          itemListToMove = new ArrayList<>();
          for(Item item : itemList) {
            if(item.isSelected()) {
              itemListToMove.add(0, item);
            }
          }

          String message = itemListToMove.size() + activity.getString(R.string.cab_recycle_task_message);
          new AlertDialog.Builder(activity)
              .setTitle(R.string.cab_recycle_task_title)
              .setMessage(message)
              .setPositiveButton(R.string.determine, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                  if(order == 0) {
                    for(Item item : itemListToMove) {

                      item.setSelected(false);

                      activity.insertDB(item, MyDatabaseHelper.TODO_TABLE);
                      activity.deleteDB(item, MyDatabaseHelper.DONE_TABLE);
                    }
                  }
                  else if(order == 1) {
                    for(Item item : itemListToMove) {

                      item.setSelected(false);

                      MyListAdapter.itemList.add(0, item);
                      activity.insertDB(item, MyDatabaseHelper.TODO_TABLE);
                      activity.deleteDB(item, MyDatabaseHelper.DONE_TABLE);
                    }

                    int size = MyListAdapter.itemList.size();
                    for(int i = 0; i < size; i++) {
                      Item item = MyListAdapter.itemList.get(i);
                      item.setOrder(i);
                      activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);
                    }
                  }

                  itemList = activity.getDoneItem();
                  notifyDataSetChanged();

                  //データベースを更新したら、そのデータベースを端末暗号化ストレージへコピーする
                  if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Context direct_boot_context = activity.createDeviceProtectedStorageContext();
                    direct_boot_context.moveDatabaseFrom(activity, MyDatabaseHelper.TODO_TABLE);
                  }

                  actionMode.finish();
                }
              })
              .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
              })
              .show();

          return true;
        }
        case R.id.share: {

          itemListToMove = new ArrayList<>();
          for(Item item : itemList) {
            if(item.isSelected()) {
              itemListToMove.add(0, item);
            }
          }

          String message = itemListToMove.size() + activity.getString(R.string.cab_share_message);
          new AlertDialog.Builder(activity)
              .setTitle(R.string.cab_share)
              .setMessage(message)
              .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                  String LINE_SEPARATOR = System.getProperty("line.separator");
                  for(Item item : itemListToMove) {
                    String send_content = "";
                    if(order == 0) {
                      send_content = activity.getString(R.string.days_of_month) + ": "
                          + DateFormat.format("yyyy/MM/dd HH:mm", item.getDate())
                          + LINE_SEPARATOR
                          + activity.getString(R.string.detail) + ": " + item.getDetail()
                          + LINE_SEPARATOR
                          + activity.getString(R.string.memo) + ": " + item.getNotes();
                    }
                    else if(order == 1) {
                      send_content = activity.getString(R.string.detail) + ": " + item.getDetail()
                          + LINE_SEPARATOR
                          + activity.getString(R.string.memo) + ": " + item.getNotes();
                    }

                    Intent intent = new Intent()
                        .setAction(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, send_content);
                    activity.startActivity(intent);
                  }

                  actionMode.finish();
                }
              })
              .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
              })
              .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
              })
              .show();

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

      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(activity.status_bar_color);
      }

      DoneListAdapter.this.actionMode = null;
      for(Item item : itemList) {
        if(item.isSelected()) {
          item.setSelected(false);
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

        //入力文字列が大文字を含むかどうか調べる
        boolean is_upper = false;
        for(int i = 0; i < constraint.length(); i++) {
          if(Character.isUpperCase(constraint.charAt(i))) {
            is_upper = true;
            break;
          }
        }

        //検索処理
        if(activity.actionBarFragment.checked_tag == -1) {
          itemList = activity.getDoneItem();
        }
        else {
          itemList = activity.actionBarFragment.filteredList;
        }

        filteredItem = new ArrayList<>();
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

        FilterResults results = new FilterResults();
        results.count = filteredItem.size();
        results.values = filteredItem;

        return results;
      }

      @Override
      @SuppressWarnings("unchecked")
      protected void publishResults(CharSequence constraint, FilterResults results) {

        itemList = (List<Item>)results.values;

        //リストの表示更新
        notifyDataSetChanged();
      }
    };
  }

  @Override
  public int getCount() {
    return itemList.size();
  }

  @Override
  public Object getItem(int position) {
    return itemList.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    final ViewHolder viewHolder;

    if(convertView == null) {
      if(order == 0) {
        convertView = View.inflate(parent.getContext(), R.layout.child_layout, null);
      }
      else if(order == 1) {
        convertView = View.inflate(parent.getContext(), R.layout.non_sheduled_item_layout, null);
      }

      viewHolder = new ViewHolder();
      checkNotNull(convertView);
      if(order == 0) {
        viewHolder.clock_image = convertView.findViewById(R.id.clock_image);
        viewHolder.time = convertView.findViewById(R.id.date);
        viewHolder.repeat = convertView.findViewById(R.id.repeat);
        viewHolder.item_card = convertView.findViewById(R.id.child_card);
      }
      else if(order == 1) {
        viewHolder.item_card = convertView.findViewById(R.id.item_card);
        viewHolder.order_icon = convertView.findViewById(R.id.order_icon);
      }
      viewHolder.detail = convertView.findViewById(R.id.detail);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.tagPallet = convertView.findViewById(R.id.tag_pallet);

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)convertView.getTag();
    }

    //現在のビュー位置でのitemの取得とリスナーの初期化
    Item item = (Item)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(position, item, viewHolder);

    //各リスナーの設定
    viewHolder.item_card.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    viewHolder.item_card.setOnLongClickListener(listener);
    viewHolder.checkBox.setOnLongClickListener(listener);

    //各種表示処理

    //共通レイアウト
    viewHolder.detail.setText(item.getDetail());
    viewHolder.detail.setTextColor(Color.GRAY);
    if(item.getWhich_tag_belongs() == 0) {
      viewHolder.tagPallet.setVisibility(View.GONE);
    }
    else {
      viewHolder.tagPallet.setVisibility(View.VISIBLE);
      viewHolder.tagPallet.setColorFilter(
          activity.generalSettings.getTagById(item.getWhich_tag_belongs()).getPrimary_color()
      );
    }

    //チェックが入っている場合、チェックを外す
    if(viewHolder.checkBox.isChecked() && !item.isSelected()) {
      manually_checked = false;
      viewHolder.checkBox.setChecked(false);
      viewHolder.checkBox.jumpDrawablesToCurrentState();
    }
    else if(!viewHolder.checkBox.isChecked() && item.isSelected()) {
      manually_checked = false;
      viewHolder.checkBox.setChecked(true);
      viewHolder.checkBox.jumpDrawablesToCurrentState();
    }
    manually_checked = true;

    //選択モードでない場合、チェックボックスを無効にする
    if(actionMode == null) viewHolder.checkBox.setEnabled(false);
    else viewHolder.checkBox.setEnabled(true);

    //個別レイアウト
    if(order == 0) {
      viewHolder.clock_image.setColorFilter(Color.GRAY);

      Calendar now = Calendar.getInstance();
      String set_time;
      if(now.get(Calendar.YEAR) == item.getDate().get(Calendar.YEAR)) {
        set_time = (String)DateFormat.format("M月d日(E)H:mm", item.getDate());
      }
      else {
        set_time = (String)DateFormat.format("yyyy年M月d日(E)H:mm", item.getDate());
      }
      viewHolder.time.setText(set_time);
      viewHolder.time.setTextColor(Color.GRAY);

      viewHolder.repeat.setText(R.string.non_repeat);
      viewHolder.repeat.setTextColor(Color.GRAY);
    }
    else if(order == 1) {
      viewHolder.order_icon.setVisibility(View.GONE);
    }

    return convertView;
  }
}