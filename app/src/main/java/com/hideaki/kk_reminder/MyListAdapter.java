package com.hideaki.kk_reminder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.LINE_SEPARATOR;
import static com.hideaki.kk_reminder.UtilClass.NON_SCHEDULED_ITEM_COMPARATOR;

public class MyListAdapter extends BaseAdapter implements Filterable {

  static List<Item> itemList;
  static long has_panel; //コントロールパネルがvisibleであるItemのid値を保持する
  private static long panel_lock_id;
  private MainActivity activity;
  ActionMode actionMode = null;
  static int checked_item_num;
  private static boolean manually_checked;
  DragListener dragListener;
  private int draggingPosition = -1;
  static boolean is_sorting;
  private List<Item> filteredItem;
  private ColorStateList defaultColorStateList;
  static boolean is_scrolling;
  private static boolean isClosed = false; //完了したタスクのコントロールパネルが閉じられたときに立てるフラグ

  MyListAdapter(MainActivity activity) {

    this.activity = activity;
    dragListener = new DragListener();
  }

  private static class ViewHolder {

    LinearLayout linearLayout;
    CardView item_card;
    ImageView order_icon;
    TextView detail;
    AnimCheckBox checkBox;
    ImageView tagPallet;
    TableLayout control_panel;
    TextView notes;
  }

  private class MyOnClickListener implements View.OnClickListener, View.OnLongClickListener,
      ActionMode.Callback, AnimCheckBox.OnCheckedChangeListener {

    private int position;
    private Item item;
    private ViewHolder viewHolder;
    private int which_list;
    private List<Item> itemListToMove;

    MyOnClickListener(int position, Item item, ViewHolder viewHolder) {

      this.position = position;
      this.item = item;
      this.viewHolder = viewHolder;
    }

    @Override
    public void onClick(View v) {

      activity.actionBarFragment.searchView.clearFocus();
      switch(v.getId()) {
        case R.id.item_card: {

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
                        int visible_count = activity.listView.getChildCount();
                        for(int i = 0; i < visible_count; i++) {
                          View visibleView = activity.listView.getChildAt(i);
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
          else viewHolder.checkBox.setChecked(true);
          break;
        }
        case R.id.edit: {
          activity.listView.clearTextFilter();
          activity.showMainEditFragment(item);
          has_panel = 0;
          viewHolder.control_panel.setVisibility(View.GONE);
          break;
        }
        case R.id.notes: {
          activity.listView.clearTextFilter();
          activity.showNotesFragment(item);
          break;
        }
      }
    }

    @Override
    public void onChange(AnimCheckBox view, boolean checked) {

      if(checked && actionMode == null && manually_checked) {

        if(has_panel == item.getId()) {
          isClosed = true;
          has_panel = 0;
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
        itemList.remove(position);
        item.setDoneDate(Calendar.getInstance());
        activity.deleteDB(item, MyDatabaseHelper.TODO_TABLE);
        activity.insertDB(item, MyDatabaseHelper.DONE_TABLE);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {

            notifyDataSetChanged();

            View parentView = activity.findViewById(android.R.id.content);
            checkNotNull(parentView);
            Snackbar.make(parentView, activity.getResources().getString(R.string.complete), Snackbar.LENGTH_LONG)
                .addCallback(new Snackbar.Callback() {
                  @Override
                  public void onShown(Snackbar sb) {

                    super.onShown(sb);
                  }

                  @Override
                  public void onDismissed(Snackbar transientBottomBar, int event) {

                    super.onDismissed(transientBottomBar, event);
                    MyListAdapter.panel_lock_id = 0;
                  }
                })
                .setAction(R.string.undo, new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {

                    if(isClosed) {
                      has_panel = item.getId();
                      isClosed = false;
                    }
                    itemList.add(item);
                    Collections.sort(itemList, NON_SCHEDULED_ITEM_COMPARATOR);
                    notifyDataSetChanged();

                    activity.insertDB(item, MyDatabaseHelper.TODO_TABLE);
                    activity.deleteDB(item, MyDatabaseHelper.DONE_TABLE);
                  }
                })
                .show();
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
      Window window = activity.getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(ContextCompat.getColor(activity, R.color.darker_grey));

      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
      return false;
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

          String message = activity.getResources().getQuantityString(R.plurals.cab_delete_message,
              itemListToMove.size(), itemListToMove.size()) + " (" + activity.getString(R.string.delete_dialog_message) + ")";
          final AlertDialog dialog = new AlertDialog.Builder(activity)
              .setTitle(R.string.cab_delete)
              .setMessage(message)
              .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                  for(Item item : itemListToMove) {
                    activity.deleteDB(item, MyDatabaseHelper.TODO_TABLE);
                  }
                  itemList = activity.getNonScheduledItem(MyDatabaseHelper.TODO_TABLE);

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
          for(Item item : itemList) {
            if(item.isSelected()) {
              itemListToMove.add(0, item);
            }
          }

          int j = 1;
          int size = ManageListAdapter.nonScheduledLists.size();
          String[] items = new String[size];
          items[0] = activity.menu.findItem(R.id.scheduled_list).getTitle().toString();
          for(int i = 0; i < size; i++) {
            if(activity.whichMenuOpen - 1 != i) {
              items[i + j] = ManageListAdapter.nonScheduledLists.get(i).getTitle();
            }
            else j = 0;
          }

          String title = activity.getResources().getQuantityString(R.plurals.cab_selected_task_num,
              itemListToMove.size(), itemListToMove.size()) + activity.getString(R.string.cab_move_task_message);
          final SingleChoiceItemsAdapter adapter = new SingleChoiceItemsAdapter(items);
          final AlertDialog dialog = new AlertDialog.Builder(activity)
              .setTitle(title)
              .setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
              })
              .setPositiveButton(R.string.determine, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                  int position = SingleChoiceItemsAdapter.checked_position;
                  if(position >= activity.whichMenuOpen) {
                    which_list = position + 1;
                  }
                  else which_list = position;

                  if(which_list == 0) {
                    for(Item item : itemListToMove) item.setSelected(false);
                    MainEditFragment.checked_item_num = checked_item_num;
                    MainEditFragment.itemListToMove = new ArrayList<>(itemListToMove);
                    activity.showMainEditFragment(
                        itemListToMove.get(itemListToMove.size() - 1)
                    );
                  }
                  else {
                    long list_id = activity.generalSettings.getNonScheduledLists().get(which_list - 1).getId();
                    itemList = new ArrayList<>();
                    for(Item item : activity.queryAllDB(MyDatabaseHelper.TODO_TABLE)) {
                      if(item.getWhich_list_belongs() == list_id) {
                        itemList.add(item);
                      }
                    }
                    Collections.sort(itemList, NON_SCHEDULED_ITEM_COMPARATOR);

                    for(Item item : itemListToMove) {

                      item.setSelected(false);

                      //リストのIDをitemに登録する
                      item.setWhich_list_belongs(list_id);

                      itemList.add(0, item);
                    }

                    int size = itemList.size();
                    for(int i = 0; i < size; i++) {
                      Item item = itemList.get(i);
                      item.setOrder(i);
                      activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);
                    }

                    itemList = activity.getNonScheduledItem(MyDatabaseHelper.TODO_TABLE);
                    notifyDataSetChanged();
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
        case R.id.clone: {

          itemListToMove = new ArrayList<>();
          for(Item item : itemList) {
            if(item.isSelected()) {
              itemListToMove.add(0, item);
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
          for(Item item : itemList) {
            if(item.isSelected()) {
              itemListToMove.add(0, item);
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
                    String send_content = activity.getString(R.string.detail) + ": " + item.getDetail()
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

      MyListAdapter.this.actionMode = null;
      for(Item item : itemList) {
        if(item.isSelected()) {
          item.setSelected(false);
        }
      }

      checked_item_num = 0;
      notifyDataSetChanged();
    }
  }

  class DragListener extends SortableListView.SimpleDragListener {

    @Override
    public int onStartDrag(int position) {

      draggingPosition = position;
      notifyDataSetChanged();

      return position;
    }

    @Override
    public int onDuringDrag(int positionFrom, int positionTo) {

      if(positionFrom < 0 || positionTo < 0 || positionFrom == positionTo) {
        return positionFrom;
      }

      Item item = itemList.get(positionFrom);
      itemList.remove(positionFrom);
      itemList.add(positionTo, item);

      draggingPosition = positionTo;
      notifyDataSetChanged();

      return positionTo;
    }

    @Override
    public boolean onStopDrag(int positionFrom, int positionTo) {

      draggingPosition = -1;
      notifyDataSetChanged();

      return super.onStopDrag(positionFrom, positionTo);
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
        if(activity.actionBarFragment.checkedTag == -1) {
          itemList = activity.getNonScheduledItem(MyDatabaseHelper.TODO_TABLE);
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

    if(convertView == null || convertView.getTag() == null) {
      convertView = View.inflate(parent.getContext(), R.layout.non_sheduled_item_layout, null);

      viewHolder = new ViewHolder();
      viewHolder.linearLayout = convertView.findViewById(R.id.linearLayout);
      viewHolder.item_card = convertView.findViewById(R.id.item_card);
      viewHolder.order_icon = convertView.findViewById(R.id.order_icon);
      viewHolder.detail = convertView.findViewById(R.id.detail);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.tagPallet = convertView.findViewById(R.id.tag_pallet);
      viewHolder.control_panel = convertView.findViewById(R.id.control_panel);
      viewHolder.notes = convertView.findViewById(R.id.notes);
      defaultColorStateList = viewHolder.notes.getTextColors();

      convertView.setTag(viewHolder);
    }
    else viewHolder = (ViewHolder)convertView.getTag();

    //現在のビュー位置でのitemの取得とリスナーの初期化
    Item item = (Item)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(position, item, viewHolder);

    //各リスナーの設定
    viewHolder.linearLayout.setOnClickListener(null);
    viewHolder.item_card.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    viewHolder.item_card.setOnLongClickListener(listener);
    viewHolder.checkBox.setOnLongClickListener(listener);

    int control_panel_size = viewHolder.control_panel.getChildCount();
    for(int i = 0; i < control_panel_size; i++) {
      TableRow tableRow = (TableRow)viewHolder.control_panel.getChildAt(i);
      int table_row_size = tableRow.getChildCount();
      for(int j = 0; j < table_row_size; j++) {
        TextView panel_item = (TextView)tableRow.getChildAt(j);
        panel_item.setOnClickListener(listener);
      }
    }

    //各種表示処理
    viewHolder.detail.setText(item.getDetail());
    viewHolder.detail.setTextSize(activity.text_size);
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
    else viewHolder.notes.setTextColor(activity.accent_color);

    //ある子ビューでコントロールパネルを出したとき、他の子ビューのコントロールパネルを閉じる
    if(viewHolder.control_panel.getVisibility() == View.VISIBLE
        && (item.getId() != has_panel || actionMode != null)) {
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

    if(is_sorting) viewHolder.order_icon.setVisibility(View.VISIBLE);
    else viewHolder.order_icon.setVisibility(View.GONE);

    //並び替え中にドラッグしているアイテムが二重に表示されないようにする
    convertView.setVisibility(position == draggingPosition ? View.INVISIBLE : View.VISIBLE);

    //CardViewが横から流れてくるアニメーション
    if(is_scrolling && activity.play_slide_animation) {
      Animation animation = AnimationUtils.loadAnimation(activity, R.anim.listview_motion);
      convertView.startAnimation(animation);
    }

    return convertView;
  }
}
