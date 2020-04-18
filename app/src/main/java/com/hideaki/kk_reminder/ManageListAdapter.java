package com.hideaki.kk_reminder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManageListAdapter extends BaseAdapter implements Filterable {

  static List<NonScheduledListAdapter> nonScheduledLists;
  private static long hasPanel; // コントロールパネルがvisibleであるItemのid値を保持する
  private MainActivity activity;
  MyDragListener myDragListener;
  private int draggingPosition = -1;
  static boolean isSorting;
  private List<NonScheduledListAdapter> filteredLists;
  static boolean isScrolling;

  ManageListAdapter(List<NonScheduledListAdapter> nonScheduledLists, MainActivity activity) {

    ManageListAdapter.nonScheduledLists = nonScheduledLists;
    this.activity = activity;
    hasPanel = 0;
    myDragListener = new MyDragListener();
    isSorting = false;
  }

  private static class ViewHolder {

    LinearLayout linearLayout;
    CardView listCard;
    ImageView orderIcon;
    ImageView listIcon;
    TextView detail;
    CardView controlCard;
    TextView edit;
    ImageView tagPallet;
  }

  private class MyOnClickListener implements View.OnClickListener {

    private NonScheduledListAdapter list;
    private ViewHolder viewHolder;

    MyOnClickListener(NonScheduledListAdapter list, ViewHolder viewHolder) {

      this.list = list;
      this.viewHolder = viewHolder;
    }

    @Override
    public void onClick(View v) {

      activity.actionBarFragment.searchView.clearFocus();
      switch(v.getId()) {
        case R.id.list_card: {
          if(viewHolder.edit.getVisibility() == View.GONE) {
            hasPanel = list.getId();
            View cardView = (View)viewHolder.edit.getParent().getParent();
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
                    final TextView panel = visibleView.findViewById(R.id.edit);
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

                  viewHolder.edit.setVisibility(View.VISIBLE);
                }
              });
          }
          else {
            hasPanel = 0;
            ((View)viewHolder.edit.getParent().getParent())
              .animate()
              .translationY(-30.0f)
              .alpha(0.0f)
              .setDuration(150)
              .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                  super.onAnimationEnd(animation);
                  viewHolder.edit.setVisibility(View.GONE);
                }
              });
          }
          break;
        }
        case R.id.edit: {
          activity.listView.clearTextFilter();
          activity.showMainEditFragmentForList(list);
          hasPanel = 0;
          viewHolder.edit.setVisibility(View.GONE);
          break;
        }
      }
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

      NonScheduledListAdapter list = nonScheduledLists.get(positionFrom);
      nonScheduledLists.remove(positionFrom);
      nonScheduledLists.add(positionTo, list);

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
          nonScheduledLists = new ArrayList<>(activity.generalSettings.getNonScheduledLists());
        }
        else {
          nonScheduledLists = activity.actionBarFragment.nonScheduledLists;
        }

        filteredLists = new ArrayList<>();
        for(NonScheduledListAdapter list : nonScheduledLists) {
          if(list.getTitle() != null) {
            String detail = list.getTitle();

            if(!isUpper) {
              detail = detail.toLowerCase();
            }

            Pattern pattern = Pattern.compile(constraint.toString());
            Matcher matcher = pattern.matcher(detail);

            if(matcher.find()) {
              filteredLists.add(list);
            }
          }
        }

        FilterResults results = new FilterResults();
        results.count = filteredLists.size();
        results.values = filteredLists;

        return results;
      }

      @Override
      @SuppressWarnings("unchecked")
      protected void publishResults(CharSequence constraint, FilterResults results) {

        nonScheduledLists = (List<NonScheduledListAdapter>)results.values;
        if(nonScheduledLists == null) {
          nonScheduledLists = new ArrayList<>();
        }

        // リストの表示更新
        notifyDataSetChanged();
      }
    };
  }

  @Override
  public int getCount() {

    return nonScheduledLists.size();
  }

  @Override
  public Object getItem(int position) {

    return nonScheduledLists.get(position);
  }

  @Override
  public long getItemId(int position) {

    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    final ViewHolder viewHolder;

    if(convertView == null || convertView.getTag() == null) {
      convertView = View.inflate(parent.getContext(), R.layout.non_scheduled_list_layout, null);

      viewHolder = new ViewHolder();
      viewHolder.linearLayout = convertView.findViewById(R.id.linearLayout);
      viewHolder.listCard = convertView.findViewById(R.id.list_card);
      viewHolder.orderIcon = convertView.findViewById(R.id.order_icon);
      viewHolder.listIcon = convertView.findViewById(R.id.list_icon);
      viewHolder.detail = convertView.findViewById(R.id.detail);
      viewHolder.controlCard = convertView.findViewById(R.id.control_card);
      viewHolder.edit = convertView.findViewById(R.id.edit);
      viewHolder.tagPallet = convertView.findViewById(R.id.tag_pallet);

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)convertView.getTag();
    }

    // 現在のビュー位置でのlistの取得とリスナーの初期化
    NonScheduledListAdapter list = (NonScheduledListAdapter)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(list, viewHolder);

    // 各リスナーの設定
    viewHolder.linearLayout.setOnClickListener(null);
    viewHolder.listCard.setOnClickListener(listener);
    viewHolder.edit.setOnClickListener(listener);

    // 各種表示処理
    if(activity.isDarkMode) {
      viewHolder.listCard.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      viewHolder.controlCard.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      TextView[] textViews = {
        viewHolder.detail, viewHolder.edit
      };
      for(TextView textView : textViews) {
        textView.setTextColor(activity.secondaryTextMaterialDarkColor);
      }
    }
    viewHolder.detail.setText(list.getTitle());
    viewHolder.detail.setTextSize(activity.textSize);
    if(list.getWhichTagBelongs() == 0) {
      viewHolder.tagPallet.setVisibility(View.GONE);
    }
    else {
      viewHolder.tagPallet.setVisibility(View.VISIBLE);
      TagAdapter tag = activity.generalSettings.getTagById(list.getWhichTagBelongs());
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

    // ある子ビューでコントロールパネルを出したとき、他の子ビューのコントロールパネルを閉じる
    if(viewHolder.edit.getVisibility() == View.VISIBLE && list.getId() != hasPanel) {
      viewHolder.edit.setVisibility(View.GONE);
    }
    else if(viewHolder.edit.getVisibility() == View.GONE && list.getId() == hasPanel) {
      viewHolder.edit.setVisibility(View.VISIBLE);
    }

    // パレットの色を設定
    if(list.getColor() != 0) {
      viewHolder.listIcon.setColorFilter(list.getColor());
    }
    else {
      viewHolder.listIcon.setColorFilter(ContextCompat.getColor(activity, R.color.iconGray));
    }

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
