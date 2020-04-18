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

  private final List<String> colorNameLists;
  private MainActivity activity;
  private static boolean isManuallyChecked;
  static int checkedPosition;
  private TypedArray typedArraysOfArray;
  private TypedArray typedArray;
  private TypedArray colorVariationArray;
  private Resources res;
  static int order;
  TagAdapter adapterTag;
  TagAdapter orgTag;
  static boolean isGeneralSettings;
  static boolean isFromListTagEdit;
  static boolean isScrolling;
  static boolean isFirst;

  ColorPickerListAdapter(MainActivity activity) {

    this.activity = activity;
    colorNameLists =
      new ArrayList<>(Arrays.asList(
        activity
          .getResources()
          .getStringArray(R.array.colors_array)
      ));
  }

  private static class ViewHolder {

    CardView colorListCard;
    AnimCheckBox checkBox;
    TextView colorName;
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

      if(checked && isManuallyChecked) {

        isFirst = false;
        checkedPosition = position;

        // チェックしたときにダイアログに表示する色を配列で指定
        res = activity.getResources();
        typedArraysOfArray = res.obtainTypedArray(R.array.colorsArray);

        int colorsArrayId = typedArraysOfArray.getResourceId(position, -1);
        checkArgument(colorsArrayId != -1);
        typedArray = res.obtainTypedArray(colorsArrayId);

        int size = typedArray.length();
        List<Integer> colorsList = new ArrayList<>();
        for(int i = 0; i < size; i++) {
          int colorsId = typedArray.getResourceId(i, -1);
          checkArgument(colorsId != -1);
          colorVariationArray = res.obtainTypedArray(colorsId);
          int color = colorVariationArray.getColor(0, -1);
          checkArgument(color != -1);

          colorsList.add(color);
        }

        int[] colorsArray = new int[size];
        for(int i = 0; i < size; i++) {
          colorsArray[i] = colorsList.get(i);
        }

        // ダイアログで選択を行わない場合も考慮してデフォルトの色を設定しておく
        int colorsId = typedArray.getResourceId(5, -1);
        checkArgument(colorsId != -1);
        colorVariationArray = res.obtainTypedArray(colorsId);

        int defaultColor = colorVariationArray.getColor(0, -1);
        int defaultLightColor = colorVariationArray.getColor(1, 0);
        int defaultDarkColor = colorVariationArray.getColor(2, -1);
        int defaultTextColor = colorVariationArray.getColor(3, 1);

        checkArgument(defaultColor != -1);
        checkArgument(defaultLightColor != 0);
        checkArgument(defaultDarkColor != -1);
        checkArgument(defaultTextColor != 1);

        if(!isGeneralSettings) {
          if(order == 0 || order == 1 || order == 4 || isFromListTagEdit) {
            adapterTag.setPrimaryColor(defaultColor);
            adapterTag.setPrimaryLightColor(defaultLightColor);
            adapterTag.setPrimaryDarkColor(defaultDarkColor);
            adapterTag.setPrimaryTextColor(defaultTextColor);
            adapterTag.setColorOrderGroup(position);
            adapterTag.setColorOrderChild(5);

            orgTag.setPrimaryColor(defaultColor);
            orgTag.setPrimaryLightColor(defaultLightColor);
            orgTag.setPrimaryDarkColor(defaultDarkColor);
            orgTag.setPrimaryTextColor(defaultTextColor);
            orgTag.setColorOrderGroup(position);
            orgTag.setColorOrderChild(5);
            activity.updateSettingsDB();
          }
          else if(order == 3) {
            MainEditFragment.list.setColor(defaultColor);
            MainEditFragment.list.setLightColor(defaultLightColor);
            MainEditFragment.list.setDarkColor(defaultDarkColor);
            MainEditFragment.list.setTextColor(defaultTextColor);
            MainEditFragment.list.setColorGroup(position);
            MainEditFragment.list.setColorChild(5);
          }
        }
        else {
          MyThemeAdapter theme = activity.generalSettings.getTheme();
          theme.setColor(defaultColor);
          theme.setLightColor(defaultLightColor);
          theme.setDarkColor(defaultDarkColor);
          theme.setTextColor(defaultTextColor);
          theme.setColorGroup(position);
          theme.setColorChild(5);
          activity.updateSettingsDB();
        }
        notifyDataSetChanged();

        new ColorPicker(activity)
          .setColors(colorsArray)
          .setTitle(activity.getString(R.string.pick_color_description))
          .setDefaultColorButton(defaultColor)
          .setRoundColorButton(true)
          .disableDefaultButtons(true)
          .setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
            @Override
            public void setOnFastChooseColorListener(int position, int color) {

              int colorsId = typedArray.getResourceId(position, -1);
              checkArgument(colorsId != -1);
              colorVariationArray = res.obtainTypedArray(colorsId);
              int lightColor = colorVariationArray.getColor(1, 0);
              int darkColor = colorVariationArray.getColor(2, -1);
              int textColor = colorVariationArray.getColor(3, 1);

              checkArgument(lightColor != 0);
              checkArgument(darkColor != -1);
              checkArgument(textColor != 1);

              if(!isGeneralSettings) {
                if(order == 0 || order == 1 || order == 4 || isFromListTagEdit) {
                  adapterTag.setPrimaryColor(color);
                  adapterTag.setPrimaryLightColor(lightColor);
                  adapterTag.setPrimaryDarkColor(darkColor);
                  adapterTag.setPrimaryTextColor(textColor);
                  adapterTag.setColorOrderChild(position);

                  orgTag.setPrimaryColor(color);
                  orgTag.setPrimaryLightColor(lightColor);
                  orgTag.setPrimaryDarkColor(darkColor);
                  orgTag.setPrimaryTextColor(textColor);
                  orgTag.setColorOrderChild(position);
                  activity.updateSettingsDB();
                }
                else if(order == 3) {
                  MainEditFragment.list.setColor(color);
                  MainEditFragment.list.setLightColor(lightColor);
                  MainEditFragment.list.setDarkColor(darkColor);
                  MainEditFragment.list.setTextColor(textColor);
                  MainEditFragment.list.setColorChild(position);
                }
              }
              else {
                MyThemeAdapter theme = activity.generalSettings.getTheme();
                theme.setColor(color);
                theme.setLightColor(lightColor);
                theme.setDarkColor(darkColor);
                theme.setTextColor(textColor);
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
      else if(position == checkedPosition && isManuallyChecked) {
        isFirst = false;
        if(!isGeneralSettings) {
          if(order == 0 || order == 1 || order == 4 || isFromListTagEdit) {
            adapterTag.setPrimaryColor(0);
            adapterTag.setColorOrderGroup(-1);

            orgTag.setPrimaryColor(0);
            orgTag.setColorOrderGroup(-1);
            activity.updateSettingsDB();
          }
          else if(order == 3) {
            MainEditFragment.list.setColor(0);
            MainEditFragment.list.setColorGroup(-1);
          }
        }
        else {
          MyThemeAdapter theme = activity.generalSettings.getTheme();
          theme.setColor(0);
          theme.setColorGroup(-1);
          activity.updateSettingsDB();
        }

        checkedPosition = -1;
        notifyDataSetChanged();
      }
    }
  }

  @Override
  public int getCount() {

    return colorNameLists.size();
  }

  @Override
  public Object getItem(int position) {

    return colorNameLists.get(position);
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
      viewHolder.colorListCard = convertView.findViewById(R.id.color_list_card);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.colorName = convertView.findViewById(R.id.color_name);
      viewHolder.pallet = convertView.findViewById(R.id.tag_pallet);

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)convertView.getTag();
    }

    // 現在のビュー位置でのcolor_nameの取得とリスナーの初期化
    String colorName = (String)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(position, viewHolder);

    // 各リスナーの設定
    viewHolder.colorListCard.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    // 各種表示処理
    if(activity.isDarkMode) {
      viewHolder.colorListCard.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      viewHolder.colorName.setTextColor(activity.secondaryTextMaterialDarkColor);
    }
    viewHolder.colorName.setText(colorName);
    viewHolder.colorName.setTextSize(activity.textSize);

    // チェック状態の初期化
    if(position != checkedPosition) {
      isManuallyChecked = false;
      if(isFirst) {
        viewHolder.checkBox.setChecked(false, false);
      }
      else {
        viewHolder.checkBox.setChecked(false);
      }
    }
    else {
      isManuallyChecked = false;
      if(isFirst) {
        viewHolder.checkBox.setChecked(true, false);
      }
      else {
        viewHolder.checkBox.setChecked(true);
      }
    }
    isManuallyChecked = true;

    // パレットの色を設定
    Resources res = activity.getResources();
    TypedArray typedArraysOfArray = res.obtainTypedArray(R.array.colorsArray);

    int colorsArrayId = typedArraysOfArray.getResourceId(position, -1);
    checkArgument(colorsArrayId != -1);
    TypedArray typedArray = res.obtainTypedArray(colorsArrayId);

    int colorsId = -1;
    if(position == checkedPosition) {
      if(!isGeneralSettings) {
        if(order == 0 || order == 1 || order == 4 || isFromListTagEdit) {
          colorsId = typedArray.getResourceId(adapterTag.getColorOrderChild(), -1);
        }
        else if(order == 3) {
          colorsId = typedArray.getResourceId(MainEditFragment.list.getColorChild(), -1);
        }
      }
      else {
        colorsId =
          typedArray.getResourceId(
            activity.generalSettings.getTheme().getColorChild(),
            -1
          );
      }
    }
    else {
      colorsId = typedArray.getResourceId(5, -1);
    }
    checkArgument(colorsId != -1);
    TypedArray colorVariationArray = res.obtainTypedArray(colorsId);
    int color = colorVariationArray.getColor(0, -1);
    checkArgument(color != -1);

    viewHolder.pallet.setColorFilter(color);

    colorVariationArray.recycle();
    typedArray.recycle();
    typedArraysOfArray.recycle();

    // CardViewが横から流れてくるアニメーション
    if(isScrolling && activity.isPlaySlideAnimation) {
      Animation animation = AnimationUtils.loadAnimation(activity, R.anim.listview_motion);
      convertView.startAnimation(animation);
    }

    return convertView;
  }
}