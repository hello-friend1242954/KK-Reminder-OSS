package com.hideaki.kk_reminder;

import android.content.DialogInterface;
import android.graphics.PorterDuff;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import android.graphics.PorterDuffColorFilter;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static com.hideaki.kk_reminder.UtilClass.setCursorDrawableColor;

public class TagEditListAdapter extends BaseAdapter {

  static List<Tag> tagList;
  private MainActivity activity;
  DragListener dragListener;
  private int draggingPosition = -1;
  static boolean is_sorting;
  static int order;
  private static boolean manually_checked;
  static long checked_item_id; // チェックの入っているItemのid値を保持する
  static boolean is_editing;
  static boolean is_first;

  TagEditListAdapter(List<Tag> tagList, MainActivity activity) {

    TagEditListAdapter.tagList = tagList;
    this.activity = activity;
    dragListener = new DragListener();
    is_sorting = false;
    is_editing = false;
  }

  private static class ViewHolder {

    ConstraintLayout tagItem;
    ImageView orderIcon;
    ImageView delete;
    AnimCheckBox checkBox;
    TextView tagName;
    ImageView pallet;
  }

  private class MyOnClickListener
      implements View.OnClickListener, AnimCheckBox.OnCheckedChangeListener {

    private int position;
    private Tag tag;
    private ViewHolder viewHolder;

    MyOnClickListener(int position, Tag tag, ViewHolder viewHolder) {

      this.position = position;
      this.tag = tag;
      this.viewHolder = viewHolder;
    }

    @Override
    public void onClick(View v) {

      if(!is_editing && !is_sorting) {
        if(!viewHolder.checkBox.isChecked()) {
          viewHolder.checkBox.setChecked(true);
        }
        else {
          viewHolder.checkBox.setChecked(false);
        }
      }
      else if(position != 0) {
        switch(v.getId()) {

          case R.id.tag_item: {

            // ダイアログに表示するEditTextの設定
            LinearLayout linearLayout = new LinearLayout(activity);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            final EditText editText = new EditText(activity);
            setCursorDrawableColor(editText);
            editText.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(
                activity.accent_color,
                PorterDuff.Mode.SRC_IN
            ));
            editText.setText(tag.getName());
            editText.setHint(R.string.tag_hint);
            editText.setSelection(tag.getName().length());
            editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            linearLayout.addView(editText);
            int paddingPx = getPxFromDp(activity, 20);
            linearLayout.setPadding(paddingPx, 0, paddingPx, 0);

            final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.name_tag)
                .setView(linearLayout)
                .setPositiveButton(R.string.determine, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {

                    String name = editText.getText().toString();
                    if(!name.equals("")) {
                      tag.setName(name);
                      activity.generalSettings.getTagList().get(position).setName(name);
                      activity.updateSettingsDB();
                    }
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

            // ダイアログ表示時にソフトキーボードを自動で表示
            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
              @Override
              public void onFocusChange(View v, boolean hasFocus) {

                if(hasFocus) {
                  Window dialogWindow = dialog.getWindow();
                  checkNotNull(dialogWindow);

                  dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
              }
            });
            editText.requestFocus();

            break;
          }
          case R.id.tag_pallet: {

            ColorPickerListViewFragment.tag_position = position;
            activity.showColorPickerListViewFragment();
            break;
          }
          case R.id.delete: {

            final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.delete_tag_title)
                .setMessage(R.string.delete_tag_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {

                    if(tag.getId() == checked_item_id) {
                      if(order == 0 || order == 1 || order == 4) {
                        MainEditFragment.item.setWhich_tag_belongs(0);
                      }
                      else if(order == 3) {
                        MainEditFragment.list.setWhich_tag_belongs(0);
                      }
                      checked_item_id = 0;
                    }

                    activity.generalSettings.getTagList().remove(position);
                    int size = activity.generalSettings.getTagList().size();
                    for(int i = 0; i < size; i++) {
                      activity.generalSettings.getTagList().get(i).setOrder(i);
                    }
                    tagList = new ArrayList<>(activity.generalSettings.getTagList());
                    notifyDataSetChanged();

                    activity.updateSettingsDB();
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

            break;
          }
        }
      }
    }

    @Override
    public void onChange(AnimCheckBox view, boolean checked) {

      if(checked && manually_checked) {
        is_first = false;
        if(order == 0 || order == 1 || order == 4) {
          MainEditFragment.item.setWhich_tag_belongs(tag.getId());
        }
        else if(order == 3) {
          MainEditFragment.list.setWhich_tag_belongs(tag.getId());
        }
        checked_item_id = tag.getId();
        notifyDataSetChanged();
      }
      else if(!checked && manually_checked) {
        is_first = false;
        notifyDataSetChanged();
      }
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

      Tag tag = tagList.get(positionFrom);
      tagList.remove(positionFrom);
      if(positionTo == 0) {
        positionTo = 1;
      }
      tagList.add(positionTo, tag);

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
  public int getCount() {

    return tagList.size();
  }

  @Override
  public Object getItem(int position) {

    return tagList.get(position);
  }

  @Override
  public long getItemId(int position) {

    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    final ViewHolder viewHolder;

    if(convertView == null || convertView.getTag() == null) {
      convertView = View.inflate(parent.getContext(), R.layout.tag_edit_list_layout, null);

      viewHolder = new ViewHolder();
      viewHolder.tagItem = convertView.findViewById(R.id.tag_item);
      viewHolder.orderIcon = convertView.findViewById(R.id.order_icon);
      viewHolder.delete = convertView.findViewById(R.id.delete);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.tagName = convertView.findViewById(R.id.tag_name);
      viewHolder.pallet = convertView.findViewById(R.id.tag_pallet);

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)convertView.getTag();
    }

    // 現在のビュー位置でのtagの取得とリスナーの初期化
    Tag tag = (Tag)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(position, tag, viewHolder);

    // リスナーの設定
    viewHolder.tagItem.setOnClickListener(listener);
    viewHolder.pallet.setOnClickListener(listener);
    viewHolder.delete.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    // チェック状態の初期化
    if(tag.getId() != checked_item_id) {
      manually_checked = false;
      if(is_first) {
        viewHolder.checkBox.setChecked(false, false);
      }
      else {
        viewHolder.checkBox.setChecked(false);
      }
    }
    else {
      manually_checked = false;
      if(is_first) {
        viewHolder.checkBox.setChecked(true, false);
      }
      else {
        viewHolder.checkBox.setChecked(true);
      }
    }
    manually_checked = true;

    // 各種表示処理
    viewHolder.tagName.setText(tag.getName());

    // パレットの色を設定
    if(tag.getId() == 0) {
      viewHolder.pallet.setVisibility(View.GONE);
    }
    else if(tag.getPrimary_color() == 0) {
      viewHolder.pallet.setVisibility(View.VISIBLE);
      viewHolder.pallet.setColorFilter(ContextCompat.getColor(activity, R.color.icon_gray));
    }
    else {
      viewHolder.pallet.setVisibility(View.VISIBLE);
      viewHolder.pallet.setColorFilter(tag.getPrimary_color());
    }

    // タグの左にあるアイコンの表示設定
    if(is_editing || is_sorting) {
      viewHolder.checkBox.setVisibility(View.GONE);
      if(tag.getId() != 0) {
        if(is_editing) {
          viewHolder.delete.setVisibility(View.VISIBLE);
        }
        else if(is_sorting) {
          viewHolder.orderIcon.setVisibility(View.VISIBLE);
        }
      }
    }
    else {
      viewHolder.checkBox.setVisibility(View.VISIBLE);
      viewHolder.delete.setVisibility(View.GONE);
      viewHolder.orderIcon.setVisibility(View.GONE);
    }

    // 並び替え中にドラッグしているアイテムが二重に表示されないようにする
    convertView.setVisibility(position == draggingPosition ? View.INVISIBLE : View.VISIBLE);

    return convertView;
  }
}
