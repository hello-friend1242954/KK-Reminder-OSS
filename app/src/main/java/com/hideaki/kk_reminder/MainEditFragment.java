package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.takisoft.fix.support.v7.preference.EditTextPreference;
import com.takisoft.fix.support.v7.preference.PreferenceCategory;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.transition.Fade;
import androidx.transition.Transition;

import static android.app.Activity.RESULT_OK;
import static com.google.common.base.Preconditions.checkArgument;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.LIST;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.MENU_POSITION;
import static com.hideaki.kk_reminder.UtilClass.REQUEST_CODE_RINGTONE_PICKER;
import static com.hideaki.kk_reminder.UtilClass.generateUniqueId;
import static java.util.Objects.requireNonNull;

public class MainEditFragment extends BasePreferenceFragmentCompat
  implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

  static final String TAG = MainEditFragment.class.getSimpleName();

  private EditTextPreference detail;
  PreferenceScreen datePicker;
  PreferenceScreen timePicker;
  private PreferenceScreen tag;
  private PreferenceScreen intervalItem;
  private PreferenceScreen dayRepeatItem;
  private PreferenceScreen minuteRepeatItem;
  private Preference pickAlarm;
  private PreferenceScreen vibration;
  private PreferenceScreen notes;
  static ItemAdapter item;
  private static String detailStr;
  static Calendar finalCal;
  static NotifyIntervalAdapter notifyInterval;
  static DayRepeatAdapter dayRepeat;
  static MinuteRepeatAdapter minuteRepeat;
  private static boolean isEdit;
  private static int order;
  static NonScheduledListAdapter list;
  private MainActivity activity;
  static List<ItemAdapter> itemListToMove;
  static int checkedItemNum;
  private static boolean isMovingTask;
  static boolean isCloningTask;
  private ItemAdapter nextItem;
  static boolean isNotesPopping; // Notesフラグメントで戻るボタンを押したときに一気にもとの画面に戻るために使う
  static boolean isMainPopping;
  private boolean isNextEditExists;
  private boolean isDestroyed;

  public static MainEditFragment newInstance() {

    MainEditFragment fragment = new MainEditFragment();

    ItemAdapter item = new ItemAdapter();
    isEdit = false;
    detailStr = "";
    notifyInterval = new NotifyIntervalAdapter();
    dayRepeat = new DayRepeatAdapter();
    minuteRepeat = new MinuteRepeatAdapter();
    finalCal = Calendar.getInstance();
    finalCal.add(Calendar.MINUTE, 1);
    finalCal.set(Calendar.SECOND, 0);
    finalCal.set(Calendar.MILLISECOND, 0);
    Bundle args = new Bundle();
    args.putSerializable(ITEM, item.getItem());
    fragment.setArguments(args);

    return fragment;
  }

  public static MainEditFragment newInstance(String detail) {

    MainEditFragment fragment = new MainEditFragment();

    ItemAdapter item = new ItemAdapter();
    isEdit = false;
    detailStr = detail;
    notifyInterval = new NotifyIntervalAdapter();
    dayRepeat = new DayRepeatAdapter();
    minuteRepeat = new MinuteRepeatAdapter();
    finalCal = Calendar.getInstance();
    finalCal.add(Calendar.MINUTE, 1);
    finalCal.set(Calendar.SECOND, 0);
    finalCal.set(Calendar.MILLISECOND, 0);
    Bundle args = new Bundle();
    args.putSerializable(ITEM, item.getItem());
    fragment.setArguments(args);

    return fragment;
  }

  static MainEditFragment newInstance(ItemAdapter item) {

    MainEditFragment fragment = new MainEditFragment();

    isEdit = true;
    detailStr = item.getDetail();
    notifyInterval = item.getNotifyInterval().clone();
    dayRepeat = item.getDayRepeat().clone();
    minuteRepeat = item.getMinuteRepeat().clone();
    finalCal = (Calendar)item.getDate().clone();
    Bundle args = new Bundle();
    args.putSerializable(ITEM, item.getItem());
    fragment.setArguments(args);

    return fragment;
  }

  static MainEditFragment newInstanceForList() {

    MainEditFragment fragment = new MainEditFragment();

    NonScheduledListAdapter list = new NonScheduledListAdapter();
    isEdit = false;
    detailStr = "";
    Bundle args = new Bundle();
    args.putSerializable(LIST, list.getNonScheduledList());
    fragment.setArguments(args);

    return fragment;
  }

  static MainEditFragment newInstanceForList(NonScheduledListAdapter list) {

    MainEditFragment fragment = new MainEditFragment();

    isEdit = true;
    detailStr = list.getTitle();
    Bundle args = new Bundle();
    args.putSerializable(LIST, list.getNonScheduledList());
    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;

    checkArgument(checkedItemNum >= 0);
    isMovingTask = false;

    if(checkedItemNum == 0) {
      order = activity.order;
    }
    else {
      checkedItemNum--;

      if(isCloningTask) {
        order = activity.order;
        if(checkedItemNum > 0) {
          nextItem = itemListToMove.get(checkedItemNum - 1).clone();
          nextItem.setId(Calendar.getInstance().getTimeInMillis());
        }
      }
      else {
        order = -1;
        isMovingTask = true;
        if(checkedItemNum > 0) {
          nextItem = itemListToMove.get(checkedItemNum - 1);
        }
      }
    }
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    if(activity.generalSettings != null) {
      isDestroyed = false;
      addPreferencesFromResource(R.xml.main_edit);
      setHasOptionsMenu(true);

      Bundle args = getArguments();
      requireNonNull(args);
      if(order == 3) {
        list = new NonScheduledListAdapter(args.getSerializable(LIST));
        requireNonNull(list);
      }
      else {
        item = new ItemAdapter(args.getSerializable(ITEM));
        requireNonNull(item);
      }

      // GeneralSettingsに影響を受ける部分の設定の初期化
      if(!isEdit && order == 0) {
        ItemAdapter defaultItem = activity.generalSettings.getItem();
        item.setWhichTagBelongs(defaultItem.getWhichTagBelongs());
        notifyInterval = defaultItem.getNotifyInterval().clone();
        dayRepeat = defaultItem.getDayRepeat().clone();
        minuteRepeat = defaultItem.getMinuteRepeat().clone();
        item.setSoundUri(defaultItem.getSoundUri());
        item.setVibrationPattern(defaultItem.getVibrationPattern());
      }
      else if(!isEdit && order == 1) {
        ItemAdapter defaultItem = activity.generalSettings.getItem();
        item.setWhichTagBelongs(defaultItem.getWhichTagBelongs());
      }

      // 各プリファレンスの初期化
      PreferenceScreen rootPreferenceScreen = getPreferenceScreen();

      // titleセクション
      PreferenceCategory title = (PreferenceCategory)findPreference("title");

      // detailセクション
      detail = (EditTextPreference)findPreference("detail");
      detail.setText(detailStr);
      detail.setTitle(detailStr);
      detail.setOnPreferenceClickListener(this);
      detail.setOnPreferenceChangeListener(this);

      // scheduleセクション
      PreferenceCategory schedule = (PreferenceCategory)findPreference("schedule");
      datePicker = (PreferenceScreen)findPreference("date_picker");
      datePicker.setOnPreferenceClickListener(this);
      timePicker = (PreferenceScreen)findPreference("time_picker");
      timePicker.setOnPreferenceClickListener(this);

      // colorセクション
      PreferenceCategory colorCategory = (PreferenceCategory)findPreference("color");
      PreferenceScreen primaryColor = (PreferenceScreen)findPreference("primary_color");
      primaryColor.setOnPreferenceClickListener(this);
      PreferenceScreen secondaryColor = (PreferenceScreen)findPreference("secondary_color");
      secondaryColor.setOnPreferenceClickListener(this);

      // tagセクション
      PreferenceCategory tagCategory = (PreferenceCategory)findPreference("tag_category");
      tag = (PreferenceScreen)findPreference("tag");
      tag.setOnPreferenceClickListener(this);

      // notificationセクション
      intervalItem = (PreferenceScreen)findPreference("interval");
      intervalItem.setOnPreferenceClickListener(this);
      pickAlarm = findPreference("pick_alarm");
      pickAlarm.setOnPreferenceClickListener(this);
      vibration = (PreferenceScreen)findPreference("vibration");
      vibration.setOnPreferenceClickListener(this);

      // repeatセクション
      dayRepeatItem = (PreferenceScreen)findPreference("repeat_day_unit");
      dayRepeatItem.setOnPreferenceClickListener(this);
      minuteRepeatItem = (PreferenceScreen)findPreference("repeat_minute_unit");
      minuteRepeatItem.setOnPreferenceClickListener(this);

      // notesセクション
      PreferenceCategory notesCategory = (PreferenceCategory)findPreference("notes_category");
      notes = (PreferenceScreen)findPreference("notes");
      notes.setOnPreferenceClickListener(this);

      if(order == 0 || isMovingTask) {
        rootPreferenceScreen.removePreference(colorCategory);
      }
      else if(order == 1) {
        rootPreferenceScreen.removeAll();
        rootPreferenceScreen.addPreference(title);
        rootPreferenceScreen.addPreference(tagCategory);
        rootPreferenceScreen.addPreference(notesCategory);
      }
      else if(order == 3) {
        rootPreferenceScreen.removeAll();
        rootPreferenceScreen.addPreference(title);
        rootPreferenceScreen.addPreference(tagCategory);
        rootPreferenceScreen.addPreference(colorCategory);
      }
      else if(order == 4) {
        rootPreferenceScreen.removePreference(title);
        rootPreferenceScreen.removePreference(schedule);
        rootPreferenceScreen.removePreference(colorCategory);
        rootPreferenceScreen.removePreference(notesCategory);
      }
    }
    else {
      isDestroyed = true;
      FragmentManager manager = getFragmentManager();
      requireNonNull(manager);
      manager
        .beginTransaction()
        .remove(this)
        .commit();
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

    super.onViewCreated(view, savedInstanceState);

    // 設定項目間の区切り線の非表示
    setDivider(new ColorDrawable(Color.TRANSPARENT));
    setDividerHeight(0);
  }

  @Override
  public View onCreateView(
    LayoutInflater inflater,
    @Nullable ViewGroup container,
    @Nullable Bundle savedInstanceState
  ) {

    if(!isDestroyed) {
      if(MainEditFragment.isMainPopping) {
        FragmentManager manager = getFragmentManager();
        requireNonNull(manager);
        manager.popBackStack();
      }
      View view = super.onCreateView(inflater, container, savedInstanceState);
      requireNonNull(view);

      if(activity.isDarkMode) {
        view.setBackgroundColor(activity.backgroundMaterialDarkColor);
      }
      else {
        view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
      }
      view.setFocusableInTouchMode(true);
      view.requestFocus();
      view.setOnKeyListener(new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

          if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {

            isNextEditExists = false;
            if((isMovingTask || isCloningTask) && checkedItemNum > 0) {
              activity.showMainEditFragment(nextItem.clone());
              isNextEditExists = true;
            }
            else if(isCloningTask) {
              isCloningTask = false;
            }

            if(!isNextEditExists) {
              isMainPopping = true;
            }
          }
          return false;
        }
      });

      Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
      activity.setSupportActionBar(toolbar);
      ActionBar actionBar = activity.getSupportActionBar();
      requireNonNull(actionBar);

      activity.drawerToggle.setDrawerIndicatorEnabled(false);
      actionBar.setHomeAsUpIndicator(activity.upArrow);
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setTitle(R.string.edit);

      // タイトルが未入力の場合、ヒントを表示
      if(detailStr == null || detailStr.equals("")) {
        detail.setSummary(R.string.detail_hint);
      }

      // タスクの期限のラベルの初期化
      if(order == 0 || isMovingTask) {
        if(LOCALE.equals(Locale.JAPAN)) {
          datePicker.setTitle(DateFormat.format("yyyy年M月d日(E)", finalCal));
        }
        else {
          datePicker.setTitle(DateFormat.format("yyyy/M/d (E)", finalCal));
        }
        timePicker.setTitle(DateFormat.format("kk:mm", finalCal));
      }

      // メモのラベルの初期化
      if(order == 0 || order == 1 || isMovingTask) {
        if(item.getNotesList().size() == 0) {
          notes.setSummary(R.string.none);
        }
        else {
          notes.setSummary(item.getNotesString());
        }
      }

      // Tagのラベルの初期化
      if(order == 0 || order == 1 || order == 4 || isMovingTask) {
        if(item.getWhichTagBelongs() == 0) {
          tag.setSummary(getString(R.string.none));
        }
        else {
          tag.setSummary(activity.generalSettings
            .getTagById(item.getWhichTagBelongs())
            .getName());
        }
      }
      else if(order == 3) {
        if(list.getWhichTagBelongs() == 0) {
          tag.setSummary(getString(R.string.none));
        }
        else {
          tag.setSummary(activity.generalSettings
            .getTagById(list.getWhichTagBelongs())
            .getName());
        }
      }

      // Repeat、NotifyInterval、AlarmSoundのラベルの初期化
      if(order == 0 || order == 4 || isMovingTask) {
        if(notifyInterval.getLabel() == null) {
          intervalItem.setSummary(R.string.none);
        }
        else {
          intervalItem.setSummary(notifyInterval.getLabel());
        }

        if(dayRepeat.getLabel() == null) {
          dayRepeatItem.setSummary(R.string.none);
        }
        else {
          dayRepeatItem.setSummary(dayRepeat.getLabel());
        }

        if(minuteRepeat.getLabel() == null) {
          minuteRepeatItem.setSummary(R.string.none);
        }
        else {
          minuteRepeatItem.setSummary(minuteRepeat.getLabel());
        }

        String uriString = item.getSoundUri();
        if(uriString == null) {
          pickAlarm.setSummary(R.string.none);
        }
        else {
          Ringtone ringtone = RingtoneManager.getRingtone(activity, Uri.parse(uriString));
          pickAlarm.setSummary(ringtone.getTitle(activity));
        }

        vibration.setSummary(item.getVibrationPattern());
      }

      return view;
    }
    else {
      return null;
    }
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.main_edit_menu, menu);

    // 完了メニューの実装
    MenuItem doneItem = menu.findItem(R.id.done);
    Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.ic_check_circle_24dp);
    requireNonNull(drawable);
    drawable = drawable.mutate();
    drawable.setColorFilter(new PorterDuffColorFilter(
      activity.menuItemColor,
      PorterDuff.Mode.SRC_IN
    ));
    doneItem.setIcon(drawable);

    // 削除メニューの実装
    MenuItem deleteItem = menu.findItem(R.id.delete);
    if(isEdit && !isMovingTask && !isCloningTask && order != 4) {
      drawable = ContextCompat.getDrawable(activity, R.drawable.ic_delete_24dp);
      requireNonNull(drawable);
      drawable = drawable.mutate();
      drawable.setColorFilter(new PorterDuffColorFilter(
        activity.menuItemColor,
        PorterDuff.Mode.SRC_IN
      ));
      deleteItem.setIcon(drawable);
      deleteItem.setVisible(true);
    }
    else {
      deleteItem.setVisible(false);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch(item.getItemId()) {
      case R.id.done: {
        if((order == 0 || isMovingTask) && isEdit
          && MainEditFragment.item.getDate().getTimeInMillis() != finalCal.getTimeInMillis()
          && (
          MainEditFragment.dayRepeat.getWhichSet() != 0 ||
            MainEditFragment.minuteRepeat.getWhichSet() != 0
        )) {

          final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setMessage(R.string.repeat_conflict_dialog_message)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                if(MainEditFragment.item.getAlteredTime() == 0) {
                  MainEditFragment.item.setOrgDate((Calendar)MainEditFragment.item
                    .getDate()
                    .clone());
                }
                long alteredTime = finalCal.getTimeInMillis() -
                  MainEditFragment.item.getDate().getTimeInMillis();
                MainEditFragment.item.addAlteredTime(alteredTime);

                registerItem();
              }
            })
            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                MainEditFragment.item.setOrgDate((Calendar)finalCal.clone());
                MainEditFragment.item.setAlteredTime(0);

                registerItem();
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
              dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(activity.accentColor);
              dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
            }
          });

          dialog.show();
        }
        else {
          registerItem();
        }
        return true;
      }
      case R.id.delete: {

        final AlertDialog dialog = new AlertDialog.Builder(activity)
          .setTitle(R.string.delete_dialog_title)
          .setMessage(R.string.delete_dialog_message)
          .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

              if(order == 0) {

                activity.deleteDB(MainEditFragment.item, MyDatabaseHelper.TODO_TABLE);
                MyExpandableListAdapter.children =
                  activity.getChildren(MyDatabaseHelper.TODO_TABLE);
                activity.deleteAlarm(MainEditFragment.item);
                activity.expandableListAdapter.notifyDataSetChanged();
              }
              else if(order == 1) {
                activity.deleteDB(MainEditFragment.item, MyDatabaseHelper.TODO_TABLE);
                MyListAdapter.itemList =
                  activity.getNonScheduledItem(MyDatabaseHelper.TODO_TABLE);
                activity.listAdapter.notifyDataSetChanged();
              }
              else if(order == 3) {
                // GeneralSettingsとManageListAdapterへの反映
                activity.generalSettings.removeNonScheduledList(list.getOrder());
                List<NonScheduledListAdapter> nonScheduledListList =
                  activity.generalSettings.getNonScheduledLists();
                int size = nonScheduledListList.size();
                for(int i = 0; i < size; i++) {
                  nonScheduledListList.get(i).setOrder(i);
                }
                ManageListAdapter.nonScheduledLists =
                  new ArrayList<>(nonScheduledListList);
                activity.manageListAdapter.notifyDataSetChanged();

                long id = list.getId();
                for(ItemAdapter itemInList : activity.queryAllDB(MyDatabaseHelper.TODO_TABLE)) {
                  if(itemInList.getWhichListBelongs() == id) {
                    activity.deleteDB(itemInList, MyDatabaseHelper.TODO_TABLE);
                  }
                }

                // 一旦reminder_listグループ内のアイテムをすべて消してから元に戻すことで新しく追加したリストの順番を追加した順に並び替える

                // デフォルトアイテムのリストア
                activity.menu.removeGroup(R.id.reminder_list);
                activity.menu
                  .add(R.id.reminder_list, R.id.scheduled_list, 0, R.string.nav_scheduled_item)
                  .setIcon(R.drawable.ic_time)
                  .setCheckable(true);
                activity.menu.add(R.id.reminder_list, R.id.add_list, 2, R.string.add_list)
                  .setIcon(R.drawable.ic_add_24dp)
                  .setCheckable(false);

                // 新しく追加したリストのリストア
                for(NonScheduledListAdapter list : nonScheduledListList) {
                  Drawable drawable =
                    ContextCompat.getDrawable(activity, R.drawable.ic_my_list_24dp);
                  requireNonNull(drawable);
                  drawable = drawable.mutate();
                  if(list.getColor() != 0) {
                    drawable.setColorFilter(new PorterDuffColorFilter(
                      list.getColor(),
                      PorterDuff.Mode.SRC_IN
                    ));
                  }
                  else {
                    drawable.setColorFilter(new PorterDuffColorFilter(
                      ContextCompat.getColor(activity, R.color.iconGray),
                      PorterDuff.Mode.SRC_IN
                    ));
                  }
                  activity.menu.add(R.id.reminder_list, generateUniqueId(), 1, list.getTitle())
                    .setIcon(drawable)
                    .setCheckable(true);
                }

                activity.setIntGeneralInSharedPreferences(
                  MENU_POSITION,
                  activity.whichMenuOpen - 1
                );
                activity.menuItem = activity.menu.getItem(activity.whichMenuOpen);
                activity.navigationView.setCheckedItem(activity.menuItem);

                // データベースへの反映
                activity.updateSettingsDB();
              }

              FragmentManager manager = getFragmentManager();
              requireNonNull(manager);
              manager.popBackStack();
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
      case android.R.id.home: {

        isNextEditExists = false;
        if((isMovingTask || isCloningTask) && checkedItemNum > 0) {
          activity.showMainEditFragment(nextItem.clone());
          isNextEditExists = true;
        }
        else if(isCloningTask) {
          isCloningTask = false;
        }

        if(!isNextEditExists) {
          isMainPopping = true;
          FragmentManager manager = getFragmentManager();
          requireNonNull(manager);
          manager.popBackStack();
        }

        return true;
      }
      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    if(requestCode == REQUEST_CODE_RINGTONE_PICKER) {
      if(resultCode == RESULT_OK) {

        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if(uri == null) {
          item.setSoundUri(null);
          pickAlarm.setSummary(getString(R.string.none));
        }
        else {
          item.setSoundUri(uri.toString());
          Ringtone ringtone = RingtoneManager.getRingtone(activity, uri);
          pickAlarm.setSummary(ringtone.getTitle(activity));
        }
      }
    }
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {

    switch(preference.getKey()) {

      case "detail": {

        EditText editText = detail.getEditText();
        if(editText != null) {
          editText.requestFocus();
          editText.setSelection(editText.getText().length());
          editText.setHint(R.string.detail_hint);
        }
        return true;
      }
      case "date_picker": {

        DatePickerDialogFragment dialog = new DatePickerDialogFragment(this);
        dialog.show(activity.getSupportFragmentManager(), "date_picker");
        return true;
      }
      case "time_picker": {

        TimePickerDialogFragment dialog = new TimePickerDialogFragment(this);
        dialog.show(activity.getSupportFragmentManager(), "time_picker");
        return true;
      }
      case "primary_color": {

        activity.showColorPickerListViewFragment();
        return true;
      }
      case "secondary_color": {

        list.setIsColorPrimary(false);
        activity.showColorPickerListViewFragment();
        return true;
      }
      case "tag": {

        if(order == 3) {
          ColorPickerListAdapter.isFromListTagEdit = true;
        }
        activity.showTagEditListViewFragment();
        return true;
      }
      case "interval": {

        transitionFragment(NotifyIntervalEditFragment.newInstance());
        return true;
      }
      case "repeat_day_unit": {

        transitionFragment(DayRepeatEditFragment.newInstance());
        return true;
      }
      case "repeat_minute_unit": {

        transitionFragment(MinuteRepeatEditFragment.newInstance());
        return true;
      }
      case "pick_alarm": {

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.pick_alarm));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(
          RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
          Uri.parse(activity.generalSettings.getItem().getSoundUri())
        );
        String uriString = item.getSoundUri();
        if(uriString != null) {
          intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(uriString));
        }
        startActivityForResult(intent, REQUEST_CODE_RINGTONE_PICKER);

        return true;
      }
      case "vibration": {
        transitionFragment(VibrationEditFragment.newInstance());
        return true;
      }
      case "notes": {

        activity.showNotesFragment(item);
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {

    if("detail".equals(preference.getKey())) {
      detailStr = (String)newValue;

      // タイトルが未入力の場合、ヒントを表示
      if(detailStr == null || detailStr.equals("")) {
        detail.setSummary(R.string.detail_hint);
      }
      else {
        detail.setSummary(null);
      }
      detail.setTitle(detailStr);

      return true;
    }

    return false;
  }

  private void transitionFragment(PreferenceFragmentCompat next) {

    Transition transition = new Fade()
      .setDuration(300);
    this.setExitTransition(transition);
    next.setEnterTransition(transition);
    FragmentManager manager = getFragmentManager();
    requireNonNull(manager);
    manager
      .beginTransaction()
      .remove(this)
      .add(R.id.content, next)
      .addToBackStack(null)
      .commit();
  }

  private void registerItem() {

    if(order == 0 || order == 1 || order == 4 || isMovingTask) {

      if(detailStr == null || detailStr.equals("")) {
        detailStr = getString(R.string.default_detail);
      }
      item.setDetail(detailStr);

      if(order == 0 || order == 4 || isMovingTask) {

        item.setDate((Calendar)finalCal.clone());

        // notifyIntervalの登録
        notifyInterval.setTime(notifyInterval.getOrgTime());
        item.setNotifyInterval(notifyInterval.clone());

        // dayRepeatの登録
        if(dayRepeat.getWhichSet() != 0) {
          if(dayRepeat.getWhichSet() == 1) {
            dayRepeat.dayClear();
          }
          else if(dayRepeat.getWhichSet() == (1 << 1)) {
            dayRepeat.weekClear();
          }
          else if(dayRepeat.getWhichSet() == (1 << 2)) {
            if(dayRepeat.isDaysOfMonthSet()) {
              dayRepeat.daysOfMonthClear();
            }
            else {
              dayRepeat.onTheMonthClear();
            }
          }
          else if(dayRepeat.getWhichSet() == (1 << 3)) {
            dayRepeat.yearClear();
          }
        }
        else {
          dayRepeat.clear();
        }
        item.setDayRepeat(dayRepeat.clone());

        // minuteRepeatの登録
        if(minuteRepeat.getWhichSet() != 0) {
          if(minuteRepeat.getWhichSet() == 1) {
            minuteRepeat.setCount(minuteRepeat.getOrgCount());
            minuteRepeat.countClear();
          }
          else if(minuteRepeat.getWhichSet() == (1 << 1)) {
            minuteRepeat.setDuration(minuteRepeat.getOrgDuration());
            minuteRepeat.durationClear();
          }
        }
        else {
          minuteRepeat.clear();
        }
        item.setMinuteRepeat(minuteRepeat.clone());

        item.setAlarmStopped(false);

        if(isMovingTask) {
          item.setWhichListBelongs(0);
        }

        if(order != 4) {
          if(isEdit && !isCloningTask) {

            activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);
            if(isMovingTask) {
              MyExpandableListAdapter.children = activity.getChildren(MyDatabaseHelper.TODO_TABLE);
              MyListAdapter.itemList = activity.getNonScheduledItem(MyDatabaseHelper.TODO_TABLE);
              activity.listAdapter.notifyDataSetChanged();
            }
            activity.expandableListAdapter.notifyDataSetChanged();
          }
          else {
            activity.addChildren(item, MyDatabaseHelper.TODO_TABLE);
          }

          ExpandableListViewFragment.updatedItemId = item.getId();

          activity.deleteAlarm(item);
          activity.setAlarm(item);
        }
        else {
          ItemAdapter cloneItem = item.clone();
          cloneItem.setId(Calendar.getInstance().getTimeInMillis());
          activity.generalSettings.setItem(cloneItem);
          activity.updateSettingsDB();
        }
      }
      else if(order == 1) {

        // リストのIDをitemに登録する
        item.setWhichListBelongs(
          activity.generalSettings.getNonScheduledLists().get(activity.whichMenuOpen - 1).getId()
        );

        if(isEdit && !isCloningTask) {

          activity.listAdapter.notifyDataSetChanged();
          activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);
        }
        else {

          MyListAdapter.itemList.add(0, item);
          int size = MyListAdapter.itemList.size();
          for(int i = 0; i < size; i++) {
            ItemAdapter item = MyListAdapter.itemList.get(i);
            item.setOrder(i);
            if(i == 0) {
              activity.insertDB(item, MyDatabaseHelper.TODO_TABLE);
            }
            else {
              activity.updateDB(item, MyDatabaseHelper.TODO_TABLE);
            }
          }

          activity.listAdapter.notifyDataSetChanged();
        }
      }
    }
    else if(order == 3) {

      if(detailStr == null || detailStr.equals("")) {
        detailStr = getString(R.string.default_list);
      }
      list.setTitle(detailStr);

      if(!isEdit) {
        // GeneralSettingsとManageListAdapterへの反映
        activity.generalSettings.addNonScheduledList(0, list);
        List<NonScheduledListAdapter> nonScheduledListList =
          activity.generalSettings.getNonScheduledLists();
        int size = nonScheduledListList.size();
        for(int i = 0; i < size; i++) {
          nonScheduledListList.get(i).setOrder(i);
        }
      }
      else {
        activity.generalSettings.setNonScheduledList(list);
      }
      List<NonScheduledListAdapter> nonScheduledListList =
        activity.generalSettings.getNonScheduledLists();
      ManageListAdapter.nonScheduledLists = new ArrayList<>(nonScheduledListList);
      activity.manageListAdapter.notifyDataSetChanged();

      // 一旦reminder_listグループ内のアイテムをすべて消してから元に戻すことで
      // 新しく追加したリストの順番を追加した順に並び替える

      // デフォルトアイテムのリストア
      activity.menu.removeGroup(R.id.reminder_list);
      activity.menu.add(R.id.reminder_list, R.id.scheduled_list, 0, R.string.nav_scheduled_item)
        .setIcon(R.drawable.ic_time)
        .setCheckable(true);
      activity.menu.add(R.id.reminder_list, R.id.add_list, 2, R.string.add_list)
        .setIcon(R.drawable.ic_add_24dp)
        .setCheckable(false);

      // 新しく追加したリストのリストア
      for(NonScheduledListAdapter list : nonScheduledListList) {
        Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.ic_my_list_24dp);
        requireNonNull(drawable);
        drawable = drawable.mutate();
        if(list.getColor() != 0) {
          drawable.setColorFilter(new PorterDuffColorFilter(
            list.getColor(),
            PorterDuff.Mode.SRC_IN
          ));
        }
        else {
          drawable.setColorFilter(new PorterDuffColorFilter(
            ContextCompat.getColor(activity, R.color.iconGray),
            PorterDuff.Mode.SRC_IN
          ));
        }
        activity.menu.add(R.id.reminder_list, generateUniqueId(), 1, list.getTitle())
          .setIcon(drawable)
          .setCheckable(true);
      }

      if(!isEdit) {
        activity.setIntGeneralInSharedPreferences(
          MENU_POSITION,
          activity.whichMenuOpen + 1
        );
        activity.menuItem = activity.menu.getItem(activity.whichMenuOpen);
      }
      activity.navigationView.setCheckedItem(activity.menuItem);

      // データベースへの反映
      activity.updateSettingsDB();
    }

    isNextEditExists = false;
    if((isMovingTask || isCloningTask) && checkedItemNum > 0) {
      activity.showMainEditFragment(nextItem.clone());
      isNextEditExists = true;
    }
    else if(isCloningTask) {
      isCloningTask = false;
    }

    if(!isNextEditExists) {
      isMainPopping = true;
      FragmentManager manager = getFragmentManager();
      requireNonNull(manager);
      manager.popBackStack();
    }
  }
}