package com.hideaki.kk_reminder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Handler;
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

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import static com.hideaki.kk_reminder.UtilClass.LINE_SEPARATOR;
import static com.hideaki.kk_reminder.UtilClass.NON_SCHEDULED_ITEM_COMPARATOR;
import static java.util.Objects.requireNonNull;

public class MyListAdapter extends BaseAdapter implements Filterable {

  static List<ItemAdapter> itemList;
  static long hasPanel; // コントロールパネルがvisibleであるItemのid値を保持する
  private static long panelLockId;
  private MainActivity activity;
  ActionMode actionMode = null;
  static int checkedItemNum;
  private static boolean isManuallyChecked;
  MyDragListener myDragListener;
  private int draggingPosition = -1;
  static boolean isSorting;
  private List<ItemAdapter> filteredItem;
  private ColorStateList defaultColorStateList;
  static boolean isScrolling;
  private static boolean isClosed = false; // 完了したタスクのコントロールパネルが閉じられたときに立てるフラグ

  MyListAdapter(MainActivity activity) {

    this.activity = activity;
    myDragListener = new MyDragListener();
  }

  private static class ViewHolder {

    LinearLayout linearLayout;
    CardView itemCard;
    ImageView orderIcon;
    TextView detail;
    AnimCheckBox checkBox;
    ImageView tagPallet;
    CardView controlCard;
    TableLayout controlPanel;
    TextView notes;
  }

  private class MyOnClickListener implements View.OnClickListener, View.OnLongClickListener,
    ActionMode.Callback, AnimCheckBox.OnCheckedChangeListener {

    private int position;
    private ItemAdapter item;
    private ViewHolder viewHolder;
    private int whichList;
    private List<ItemAdapter> itemListToMove;

    MyOnClickListener(int position, ItemAdapter item, ViewHolder viewHolder) {

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
                      int visibleCount = activity.listView.getChildCount();
                      for(int i = 0; i < visibleCount; i++) {
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
        case R.id.edit: {
          activity.listView.clearTextFilter();
          activity.showMainEditFragment(item);
          hasPanel = 0;
          viewHolder.controlPanel.setVisibility(View.GONE);
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

      if(checked && actionMode == null && isManuallyChecked) {

        if(hasPanel == item.getId()) {
          isClosed = true;
          hasPanel = 0;
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
            requireNonNull(parentView);
            Snackbar
              .make(
                parentView,
                activity.getResources().getString(R.string.complete),
                Snackbar.LENGTH_LONG
              )
              .addCallback(new Snackbar.Callback() {
                @Override
                public void onShown(Snackbar sb) {

                  super.onShown(sb);
                }

                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {

                  super.onDismissed(transientBottomBar, event);
                  MyListAdapter.panelLockId = 0;
                }
              })
              .setAction(R.string.undo, new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                  if(isClosed) {
                    hasPanel = item.getId();
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

      return false;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {

      switch(menuItem.getItemId()) {
        case R.id.delete: {

          itemListToMove = new ArrayList<>();
          for(ItemAdapter item : itemList) {
            if(item.isSelected()) {
              itemListToMove.add(0, item);
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

                for(ItemAdapter item : itemListToMove) {
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

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
            }
          });

          dialog.show();

          return true;
        }
        case R.id.move_task_between_list: {

          itemListToMove = new ArrayList<>();
          for(ItemAdapter item : itemList) {
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
            else {
              j = 0;
            }
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

                int position = SingleChoiceItemsAdapter.checkedPosition;
                if(position >= activity.whichMenuOpen) {
                  whichList = position + 1;
                }
                else {
                  whichList = position;
                }

                if(whichList == 0) {
                  for(ItemAdapter item : itemListToMove) {
                    item.setSelected(false);
                  }
                  MainEditFragment.checkedItemNum = checkedItemNum;
                  MainEditFragment.itemListToMove = new ArrayList<>(itemListToMove);
                  activity.showMainEditFragment(
                    itemListToMove.get(itemListToMove.size() - 1)
                  );
                }
                else {
                  long listId =
                    activity.generalSettings.getNonScheduledLists().get(whichList - 1).getId();
                  itemList = new ArrayList<>();
                  for(ItemAdapter item : activity.queryAllDB(MyDatabaseHelper.TODO_TABLE)) {
                    if(item.getWhichListBelongs() == listId) {
                      itemList.add(item);
                    }
                  }
                  Collections.sort(itemList, NON_SCHEDULED_ITEM_COMPARATOR);

                  for(ItemAdapter item : itemListToMove) {

                    item.setSelected(false);

                    // リストのIDをitemに登録する
                    item.setWhichListBelongs(listId);

                    itemList.add(0, item);
                  }

                  int size = itemList.size();
                  for(int i = 0; i < size; i++) {
                    ItemAdapter item = itemList.get(i);
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

              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
            }
          });

          dialog.show();

          return true;
        }
        case R.id.clone: {

          itemListToMove = new ArrayList<>();
          for(ItemAdapter item : itemList) {
            if(item.isSelected()) {
              itemListToMove.add(0, item);
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
          for(ItemAdapter item : itemList) {
            if(item.isSelected()) {
              itemListToMove.add(0, item);
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
                  String sendContent =
                    activity.getString(R.string.detail) + ": " + item.getDetail()
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

      MyListAdapter.this.actionMode = null;
      for(ItemAdapter item : itemList) {
        if(item.isSelected()) {
          item.setSelected(false);
        }
      }

      checkedItemNum = 0;
      notifyDataSetChanged();
    }
  }

  class MyDragListener extends SortableListView.SimpleDragListener {

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

      ItemAdapter item = itemList.get(positionFrom);
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
          itemList = activity.getNonScheduledItem(MyDatabaseHelper.TODO_TABLE);
        }
        else {
          itemList = activity.actionBarFragment.filteredList;
        }

        filteredItem = new ArrayList<>();
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

        FilterResults results = new FilterResults();
        results.count = filteredItem.size();
        results.values = filteredItem;

        return results;
      }

      @Override
      @SuppressWarnings("unchecked")
      protected void publishResults(CharSequence constraint, FilterResults results) {

        itemList = (List<ItemAdapter>)results.values;
        if(itemList == null) {
          itemList = new ArrayList<>();
        }

        // リストの表示更新
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
      viewHolder.itemCard = convertView.findViewById(R.id.item_card);
      viewHolder.orderIcon = convertView.findViewById(R.id.order_icon);
      viewHolder.detail = convertView.findViewById(R.id.detail);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.tagPallet = convertView.findViewById(R.id.tag_pallet);
      viewHolder.controlCard = convertView.findViewById(R.id.control_card);
      viewHolder.controlPanel = convertView.findViewById(R.id.control_panel);
      viewHolder.notes = convertView.findViewById(R.id.notes);
      defaultColorStateList = viewHolder.notes.getTextColors();

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)convertView.getTag();
    }

    // 現在のビュー位置でのitemの取得とリスナーの初期化
    ItemAdapter item = (ItemAdapter)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(position, item, viewHolder);

    // 各リスナーの設定
    viewHolder.linearLayout.setOnClickListener(null);
    viewHolder.itemCard.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    viewHolder.itemCard.setOnLongClickListener(listener);
    viewHolder.checkBox.setOnLongClickListener(listener);

    int controlPanelSize = viewHolder.controlPanel.getChildCount();
    for(int i = 0; i < controlPanelSize; i++) {
      TableRow tableRow = (TableRow)viewHolder.controlPanel.getChildAt(i);
      int tableRowSize = tableRow.getChildCount();
      for(int j = 0; j < tableRowSize; j++) {
        TextView panelItem = (TextView)tableRow.getChildAt(j);
        if(activity.isDarkMode) {
          panelItem.setTextColor(activity.secondaryTextMaterialDarkColor);
        }
        panelItem.setOnClickListener(listener);
      }
    }

    // 各種表示処理
    if(activity.isDarkMode) {
      viewHolder.itemCard.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      viewHolder.controlCard.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      viewHolder.detail.setTextColor(activity.secondaryTextMaterialDarkColor);
    }
    viewHolder.detail.setText(item.getDetail());
    viewHolder.detail.setTextSize(activity.textSize);
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

    // ある子ビューでコントロールパネルを出したとき、他の子ビューのコントロールパネルを閉じる
    if(viewHolder.controlPanel.getVisibility() == View.VISIBLE
      && (item.getId() != hasPanel || actionMode != null)) {
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

    if(isSorting) {
      viewHolder.orderIcon.setVisibility(View.VISIBLE);
    }
    else {
      viewHolder.orderIcon.setVisibility(View.GONE);
    }

    // 並び替え中にドラッグしているアイテムが二重に表示されないようにする
    convertView.setVisibility(position == draggingPosition ? View.INVISIBLE : View.VISIBLE);

    // CardViewが横から流れてくるアニメーション
    if(isScrolling && activity.isPlaySlideAnimation) {
      Animation animation = AnimationUtils.loadAnimation(activity, R.anim.listview_motion);
      convertView.startAnimation(animation);
    }

    return convertView;
  }
}
