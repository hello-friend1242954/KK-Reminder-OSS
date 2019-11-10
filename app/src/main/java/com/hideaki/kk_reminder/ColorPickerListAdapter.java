package com.hideaki.kk_reminder;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.cardview.widget.CardView;
import petrov.kristiyan.colorpicker.ColorPicker;

import static com.google.common.base.Preconditions.checkArgument;

public class ColorPickerListAdapter extends BaseAdapter {

  private final List<String> color_name_lists;
  private MainActivity activity;
  private static boolean manually_checked;
  static int checked_position;
  private TypedArray typedArraysOfArray;
  private TypedArray typedArray;
  private TypedArray colorVariationArray;
  private Resources res;
  static int order;
  Tag adapterTag;
  Tag orgTag;
  static boolean is_general_settings;
  static boolean from_list_tag_edit;
  static boolean is_scrolling;
  static boolean is_first;

  ColorPickerListAdapter(MainActivity activity) {

    this.activity = activity;
    color_name_lists =
      new ArrayList<>(Arrays.asList(
        activity
          .getResources()
          .getStringArray(R.array.colors_array)
      ));
  }

  private static class ViewHolder {

    CardView color_list_card;
    AnimCheckBox checkBox;
    TextView color_name;
    ImageView pallet;
  }

  private class MyOnClickListener
    implements View.OnClickListener, AnimCheckBox.OnCheckedChangeListener {

    private int position;
    private ViewHolder viewHolder;

    MyOnClickListener(int position, ViewHolder viewHolder) {

      this.position = position;
      this.viewHolder = viewHolder;
    }

    @Override
    public void onClick(View v) {

      if(!viewHolder.checkBox.isChecked()) {
        viewHolder.checkBox.setChecked(true);
      }
      else {
        viewHolder.checkBox.setChecked(false);
      }
    }

    @SuppressLint("ResourceType")
    @Override
    public void onChange(AnimCheckBox view, boolean checked) {

      if(checked && manually_checked) {

        is_first = false;
        checked_position = position;

        // チェックしたときにダイアログに表示する色を配列で指定
        res = activity.getResources();
        typedArraysOfArray = res.obtainTypedArray(R.array.colorsArray);

        int colors_array_id = typedArraysOfArray.getResourceId(position, -1);
        checkArgument(colors_array_id != -1);
        typedArray = res.obtainTypedArray(colors_array_id);

        int size = typedArray.length();
        List<Integer> colorsList = new ArrayList<>();
        for(int i = 0; i < size; i++) {
          int colors_id = typedArray.getResourceId(i, -1);
          checkArgument(colors_id != -1);
          colorVariationArray = res.obtainTypedArray(colors_id);
          int color = colorVariationArray.getColor(0, -1);
          checkArgument(color != -1);

          colorsList.add(color);
        }

        int[] colors_array = new int[size];
        for(int i = 0; i < size; i++) {
          colors_array[i] = colorsList.get(i);
        }

        // ダイアログで選択を行わない場合も考慮してデフォルトの色を設定しておく
        int colors_id = typedArray.getResourceId(5, -1);
        checkArgument(colors_id != -1);
        colorVariationArray = res.obtainTypedArray(colors_id);

        int default_color = colorVariationArray.getColor(0, -1);
        int default_light_color = colorVariationArray.getColor(1, 0);
        int default_dark_color = colorVariationArray.getColor(2, -1);
        int default_text_color = colorVariationArray.getColor(3, 1);

        checkArgument(default_color != -1);
        checkArgument(default_light_color != 0);
        checkArgument(default_dark_color != -1);
        checkArgument(default_text_color != 1);

        if(!is_general_settings) {
          if(order == 0 || order == 1 || order == 4 || from_list_tag_edit) {
            adapterTag.setPrimary_color(default_color);
            adapterTag.setPrimary_light_color(default_light_color);
            adapterTag.setPrimary_dark_color(default_dark_color);
            adapterTag.setPrimary_text_color(default_text_color);
            adapterTag.setColor_order_group(position);
            adapterTag.setColor_order_child(5);

            orgTag.setPrimary_color(default_color);
            orgTag.setPrimary_light_color(default_light_color);
            orgTag.setPrimary_dark_color(default_dark_color);
            orgTag.setPrimary_text_color(default_text_color);
            orgTag.setColor_order_group(position);
            orgTag.setColor_order_child(5);
            activity.updateSettingsDB();
          }
          else if(order == 3) {
            MainEditFragment.list.setColor(default_color);
            MainEditFragment.list.setLightColor(default_light_color);
            MainEditFragment.list.setDarkColor(default_dark_color);
            MainEditFragment.list.setTextColor(default_text_color);
            MainEditFragment.list.setColorGroup(position);
            MainEditFragment.list.setColorChild(5);
          }
        }
        else {
          MyTheme theme = activity.generalSettings.getTheme();
          theme.setColor(default_color);
          theme.setLightColor(default_light_color);
          theme.setDarkColor(default_dark_color);
          theme.setTextColor(default_text_color);
          theme.setColorGroup(position);
          theme.setColorChild(5);
          activity.updateSettingsDB();
        }
        notifyDataSetChanged();

        new ColorPicker(activity)
          .setColors(colors_array)
          .setTitle(activity.getString(R.string.pick_color_description))
          .setDefaultColorButton(default_color)
          .setRoundColorButton(true)
          .disableDefaultButtons(true)
          .setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
            @Override
            public void setOnFastChooseColorListener(int position, int color) {

              int colors_id = typedArray.getResourceId(position, -1);
              checkArgument(colors_id != -1);
              colorVariationArray = res.obtainTypedArray(colors_id);
              int light_color = colorVariationArray.getColor(1, 0);
              int dark_color = colorVariationArray.getColor(2, -1);
              int text_color = colorVariationArray.getColor(3, 1);

              checkArgument(light_color != 0);
              checkArgument(dark_color != -1);
              checkArgument(text_color != 1);

              if(!is_general_settings) {
                if(order == 0 || order == 1 || order == 4 || from_list_tag_edit) {
                  adapterTag.setPrimary_color(color);
                  adapterTag.setPrimary_light_color(light_color);
                  adapterTag.setPrimary_dark_color(dark_color);
                  adapterTag.setPrimary_text_color(text_color);
                  adapterTag.setColor_order_child(position);

                  orgTag.setPrimary_color(color);
                  orgTag.setPrimary_light_color(light_color);
                  orgTag.setPrimary_dark_color(dark_color);
                  orgTag.setPrimary_text_color(text_color);
                  orgTag.setColor_order_child(position);
                  activity.updateSettingsDB();
                }
                else if(order == 3) {
                  MainEditFragment.list.setColor(color);
                  MainEditFragment.list.setLightColor(light_color);
                  MainEditFragment.list.setDarkColor(dark_color);
                  MainEditFragment.list.setTextColor(text_color);
                  MainEditFragment.list.setColorChild(position);
                }
              }
              else {
                MyTheme theme = activity.generalSettings.getTheme();
                theme.setColor(color);
                theme.setLightColor(light_color);
                theme.setDarkColor(dark_color);
                theme.setTextColor(text_color);
                theme.setColorChild(position);
                activity.updateSettingsDB();
              }

              viewHolder.pallet.setColorFilter(color);

              colorVariationArray.recycle();
              typedArray.recycle();
              typedArraysOfArray.recycle();
            }

            @Override
            public void onCancel() {

              colorVariationArray.recycle();
              typedArray.recycle();
              typedArraysOfArray.recycle();
            }
          })
          .show();
      }
      else if(position == checked_position && manually_checked) {
        is_first = false;
        if(!is_general_settings) {
          if(order == 0 || order == 1 || order == 4 || from_list_tag_edit) {
            adapterTag.setPrimary_color(0);
            adapterTag.setColor_order_group(-1);

            orgTag.setPrimary_color(0);
            orgTag.setColor_order_group(-1);
            activity.updateSettingsDB();
          }
          else if(order == 3) {
            MainEditFragment.list.setColor(0);
            MainEditFragment.list.setColorGroup(-1);
          }
        }
        else {
          MyTheme theme = activity.generalSettings.getTheme();
          theme.setColor(0);
          theme.setColorGroup(-1);
          activity.updateSettingsDB();
        }

        checked_position = -1;
        notifyDataSetChanged();
      }
    }
  }

  @Override
  public int getCount() {

    return color_name_lists.size();
  }

  @Override
  public Object getItem(int position) {

    return color_name_lists.get(position);
  }

  @Override
  public long getItemId(int position) {

    return position;
  }

  @SuppressLint("ResourceType")
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    final ViewHolder viewHolder;

    if(convertView == null || convertView.getTag() == null) {
      convertView = View.inflate(parent.getContext(), R.layout.color_picker_list_layout, null);

      viewHolder = new ViewHolder();
      viewHolder.color_list_card = convertView.findViewById(R.id.color_list_card);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.color_name = convertView.findViewById(R.id.color_name);
      viewHolder.pallet = convertView.findViewById(R.id.tag_pallet);

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)convertView.getTag();
    }

    // 現在のビュー位置でのcolor_nameの取得とリスナーの初期化
    String color_name = (String)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(position, viewHolder);

    // 各リスナーの設定
    viewHolder.color_list_card.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    // 各種表示処理
    if(activity.isDarkMode) {
      viewHolder.color_list_card.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      viewHolder.color_name.setTextColor(activity.secondaryTextMaterialDarkColor);
    }
    viewHolder.color_name.setText(color_name);
    viewHolder.color_name.setTextSize(activity.text_size);

    // チェック状態の初期化
    if(position != checked_position) {
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

    // パレットの色を設定
    Resources res = activity.getResources();
    TypedArray typedArraysOfArray = res.obtainTypedArray(R.array.colorsArray);

    int colors_array_id = typedArraysOfArray.getResourceId(position, -1);
    checkArgument(colors_array_id != -1);
    TypedArray typedArray = res.obtainTypedArray(colors_array_id);

    int colors_id = -1;
    if(position == checked_position) {
      if(!is_general_settings) {
        if(order == 0 || order == 1 || order == 4 || from_list_tag_edit) {
          colors_id = typedArray.getResourceId(adapterTag.getColor_order_child(), -1);
        }
        else if(order == 3) {
          colors_id = typedArray.getResourceId(MainEditFragment.list.getColorChild(), -1);
        }
      }
      else {
        colors_id =
          typedArray.getResourceId(
            activity.generalSettings.getTheme().getColorChild(),
            -1
          );
      }
    }
    else {
      colors_id = typedArray.getResourceId(5, -1);
    }
    checkArgument(colors_id != -1);
    TypedArray colorVariationArray = res.obtainTypedArray(colors_id);
    int color = colorVariationArray.getColor(0, -1);
    checkArgument(color != -1);

    viewHolder.pallet.setColorFilter(color);

    colorVariationArray.recycle();
    typedArray.recycle();
    typedArraysOfArray.recycle();

    // CardViewが横から流れてくるアニメーション
    if(is_scrolling && activity.play_slide_animation) {
      Animation animation = AnimationUtils.loadAnimation(activity, R.anim.listview_motion);
      convertView.startAnimation(animation);
    }

    return convertView;
  }
}