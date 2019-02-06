package com.hideaki.kk_reminder;

import android.app.AlarmManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.diegocarloslima.fgelv.lib.FloatingGroupExpandableListView;
import com.diegocarloslima.fgelv.lib.WrapperExpandableListAdapter;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hideaki.kk_reminder.UtilClass.ACTION_IN_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.BOOLEAN_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.BOOT_FROM_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_URI_SOUND;
import static com.hideaki.kk_reminder.UtilClass.DONE_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.IS_EXPANDABLE_TODO;
import static com.hideaki.kk_reminder.UtilClass.IS_PREMIUM;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.MENU_POSITION;
import static com.hideaki.kk_reminder.UtilClass.NON_SCHEDULED_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.PRODUCT_ID_PREMIUM;
import static com.hideaki.kk_reminder.UtilClass.SCHEDULED_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_HOUR;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.SUBMENU_POSITION;
import static com.hideaki.kk_reminder.UtilClass.deserialize;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static com.hideaki.kk_reminder.UtilClass.serialize;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, PurchasesUpdatedListener {

  private Timer timer;
  private Handler handler = new Handler();
  private DBAccessor accessor = null;
  int snooze_default_hour;
  int snooze_default_minute;
  int which_menu_open;
  int which_submenu_open;
  boolean is_expandable_todo;
  boolean is_premium;
  FloatingGroupExpandableListView expandableListView;
  MyExpandableListAdapter expandableListAdapter;
  WrapperExpandableListAdapter wrapperAdapter;
  SortableListView listView;
  MyListAdapter listAdapter;
  ManageListAdapter manageListAdapter;
  ColorPickerListAdapter colorPickerListAdapter;
  TagEditListAdapter tagEditListAdapter;
  DoneListAdapter doneListAdapter;
  DrawerLayout drawerLayout;
  NavigationView navigationView;
  ActionBarDrawerToggle drawerToggle;
  Drawable upArrow;
  Menu menu;
  MenuItem menuItem;
  GeneralSettings generalSettings;
  ActionBarFragment actionBarFragment;
  Toolbar toolbar;
  int menu_item_color;
  int menu_background_color;
  int status_bar_color;
  int accent_color;
  int secondary_text_color;
  int order;
  String detail;
  private int which_list;
  private boolean is_in_on_create;
  boolean is_boot_from_notification;
  private int try_count;
  private BillingClient billingClient;
  public AlertDialog promotionDialog;
  private GeneralSettingsFragment generalSettingsFragment;
  private BroadcastReceiver defaultSnoozeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      MyExpandableListAdapter.children = getChildren(MyDatabaseHelper.TODO_TABLE);
      expandableListAdapter.notifyDataSetChanged();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    accessor = new DBAccessor(this);

    //共通設定と新しく追加したリストのリストア
    generalSettings = querySettingsDB();
    if(generalSettings == null) {
      initSettings();
    }

    //SharedPreferencesの内、intを読み出す
    SharedPreferences intPreferences = getSharedPreferences(INT_GENERAL, MODE_PRIVATE);
    snooze_default_hour = intPreferences.getInt(SNOOZE_DEFAULT_HOUR, 0);
    snooze_default_minute = intPreferences.getInt(SNOOZE_DEFAULT_MINUTE, 15);
    which_menu_open = intPreferences.getInt(MENU_POSITION, 0);
    which_submenu_open = intPreferences.getInt(SUBMENU_POSITION, 0);

    //SharedPreferencesの内、booleanを読み出す
    SharedPreferences booleanPreferences = getSharedPreferences(BOOLEAN_GENERAL, MODE_PRIVATE);
    is_expandable_todo = booleanPreferences.getBoolean(IS_EXPANDABLE_TODO, true);
    is_premium = booleanPreferences.getBoolean(IS_PREMIUM, false);

    //広告読み出し機能のセットアップ
    MobileAds.initialize(this, getString(R.string.app_id));

    if(!is_premium) {

      //ビリングサービスのセットアップ
      setupBillingServices();

      //プロモーション用ダイアログのセットアップ
      createPromotionDialog();
    }

    //テーマの設定
    MyTheme theme = generalSettings.getTheme();
    if(theme.getColor() != 0) {
      Resources res = getResources();
      TypedArray typedArraysOfArray = res.obtainTypedArray(R.array.colorStylesArray);

      int styles_array_id = typedArraysOfArray.getResourceId(theme.getColorGroup(), -1);
      checkArgument(styles_array_id != -1);
      TypedArray typedArray = res.obtainTypedArray(styles_array_id);

      int style_id = typedArray.getResourceId(theme.getColorChild(), -1);
      checkArgument(style_id != -1);

      typedArray.recycle();
      typedArraysOfArray.recycle();

      setTheme(style_id);
    }
    setContentView(R.layout.activity_main);

    //ToolbarをActionBarに互換を持たせて設定
    toolbar = findViewById(R.id.toolbar_layout);
    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    checkNotNull(actionBar);

    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);

    //NavigationDrawerの設定
    drawerLayout = findViewById(R.id.drawer_layout);
    drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
        R.string.drawer_open, R.string.drawer_close
    );
    drawerToggle.setDrawerIndicatorEnabled(true);
    drawerLayout.addDrawerListener(drawerToggle);

    navigationView = findViewById(R.id.nav_view);
    menu = navigationView.getMenu();
    navigationView.setItemIconTintList(null);
    navigationView.setNavigationItemSelectedListener(this);

    //各NonScheduledListを読み込む
    for(NonScheduledList list : generalSettings.getNonScheduledLists()) {
      Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_my_list_24dp);
      checkNotNull(drawable);
      drawable = drawable.mutate();
      if(list.getColor() != 0) {
        drawable.setColorFilter(list.getColor(), PorterDuff.Mode.SRC_IN);
      }
      else {
        drawable.setColorFilter(ContextCompat.getColor(this, R.color.icon_gray), PorterDuff.Mode.SRC_IN);
      }
      menu.add(R.id.reminder_list, Menu.NONE, 1, list.getTitle())
          .setIcon(drawable)
          .setCheckable(true);
    }

    //Adapterの初期化
    expandableListAdapter = new MyExpandableListAdapter(getChildren(MyDatabaseHelper.TODO_TABLE), this);
    wrapperAdapter = new WrapperExpandableListAdapter(expandableListAdapter);
    listAdapter = new MyListAdapter(this);
    manageListAdapter = new ManageListAdapter(new ArrayList<>(generalSettings.getNonScheduledLists()), this);
    colorPickerListAdapter = new ColorPickerListAdapter(this);
    tagEditListAdapter = new TagEditListAdapter(new ArrayList<>(generalSettings.getTagList()), this);
    doneListAdapter = new DoneListAdapter(this);

    //Intentが送られている場合はonNewIntent()に渡す(送られていない場合は通常の初期化処理を行う)
    is_in_on_create = true;
    onNewIntent(getIntent());

    //Notificationチャネルの作成
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
      checkNotNull(notificationManager);
      NotificationChannel notificationChannel = new NotificationChannel("kk_reminder_01",
          getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);

      notificationManager.createNotificationChannel(notificationChannel);
    }

    //画面がフォアグラウンドの状態におけるDefaultManuallySnoozeReceiverからのインテントを待ち受ける
    registerReceiver(defaultSnoozeReceiver, new IntentFilter(ACTION_IN_NOTIFICATION));
  }

  @Override
  protected void onDestroy() {

    super.onDestroy();
    unregisterReceiver(defaultSnoozeReceiver);
  }

  private void setupBillingServices() {

    if(billingClient == null) {
      try_count = 0;
      billingClient = BillingClient
          .newBuilder(this)
          .setListener(this)
          .build();
      billingClient.startConnection(new BillingClientStateListener() {
        @Override
        public void onBillingSetupFinished(int responseCode) {

          if(responseCode == BillingClient.BillingResponse.OK) {

            //プレミアムアカウントかどうかの確認
            checkIsPremium();
          }
          else {
            try_count++;
            if(try_count == 3) {
              Toast.makeText(MainActivity.this, getString(R.string.fail_to_setup_billing), Toast.LENGTH_LONG).show();
            }
            if(try_count < 3) billingClient.startConnection(this);
          }
//          Toast.makeText(MainActivity.this, "セットアップ完了", Toast.LENGTH_LONG).show();
//
//          List<String> skuList = new ArrayList<>();
//          skuList.add(PRODUCT_ID_PREMIUM);
//          SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
//              .setSkusList(skuList)
//              .setType(BillingClient.SkuType.INAPP);
//
//          billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
//            @Override
//            public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
//
//              if(responseCode == BillingClient.BillingResponse.OK && skuDetailsList != null) {
//                for(SkuDetails skuDetails : skuDetailsList) {
//                  String sku = skuDetails.getSku();
//                  String price = skuDetails.getPrice();
//                  if(PRODUCT_ID_PREMIUM.equals(sku)) {
//                    Toast.makeText(MainActivity.this, "商品ID: " + PRODUCT_ID_PREMIUM, Toast.LENGTH_LONG).show();
//                    Toast.makeText(MainActivity.this, "値段: " + price, Toast.LENGTH_LONG).show();
//                  }
//                }
//              }
//            }
//          });
//        }
        }

        @Override
        public void onBillingServiceDisconnected() {

          try_count++;
          if(try_count == 3) {
            Toast.makeText(MainActivity.this, getString(R.string.fail_to_setup_billing), Toast.LENGTH_LONG).show();
          }
          if(try_count < 3) billingClient.startConnection(this);
        }
      });
    }
    else checkIsPremium();
  }

  private void checkIsPremium() {

    Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
    List<Purchase> purchaseList = purchasesResult.getPurchasesList();
    if(purchaseList != null) {
      for(Purchase purchase : purchaseList) {
        if(PRODUCT_ID_PREMIUM.equals(purchase.getSku())) {
          Toast.makeText(this, getString(R.string.succeed_to_upgrade), Toast.LENGTH_LONG).show();
          setBooleanGeneralInSharedPreferences(IS_PREMIUM, true);
          finish();
          startActivity(new Intent(this, MainActivity.class));
        }
      }
    }
  }

  @Override
  public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

    if(responseCode == BillingClient.BillingResponse.OK && purchases != null) {
      for(Purchase purchase : purchases) {
        if(PRODUCT_ID_PREMIUM.equals(purchase.getSku())) {
          Toast.makeText(this, getString(R.string.succeed_to_upgrade), Toast.LENGTH_LONG).show();
          setBooleanGeneralInSharedPreferences(IS_PREMIUM, true);
          finish();
          startActivity(new Intent(this, MainActivity.class));
        }
      }
    }
    else if(responseCode == BillingClient.BillingResponse.USER_CANCELED) {
      Toast.makeText(MainActivity.this, getString(R.string.cancel_to_buy), Toast.LENGTH_LONG).show();
    }
    else {
      Toast.makeText(MainActivity.this, getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
    }
  }

  private void createPromotionDialog() {

    String[] items = getResources().getStringArray(R.array.feature_list);

    promotionDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.upgrade_to_premium_account)
        .setItems(items, null)
        .setPositiveButton(R.string.upgrade, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            onBuyButtonClicked();
          }
        })
        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
          }
        })
        .create();
  }

  private void onBuyButtonClicked() {

    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
        .setSku(PRODUCT_ID_PREMIUM)
        .setType(BillingClient.SkuType.INAPP)
        .build();
    billingClient.launchBillingFlow(this, flowParams);
  }

  @Override
  protected void onNewIntent(Intent intent) {

    super.onNewIntent(intent);

    //テキストがACTION_SENDで送られてきたときに、詳細の項目にそのテキストをセットした状態で編集画面を表示する
    detail = null;
    if(Intent.ACTION_SEND.equals(intent.getAction())) {
      Bundle extras = intent.getExtras();
      if(extras != null) {
        detail = extras.getString(Intent.EXTRA_TEXT);
      }
    }

    if(detail == null) {
      if(BOOT_FROM_NOTIFICATION.equals(intent.getAction())) {

        is_boot_from_notification = true;
        setIntGeneralInSharedPreferences(MENU_POSITION, 0);
        setIntGeneralInSharedPreferences(SUBMENU_POSITION, 0);
        int size = menu.size();
        for(int i = 1; i < size; i++) {
          MenuItem menuItem = menu.getItem(i);
          if(menuItem.hasSubMenu()) {
            SubMenu subMenu = menuItem.getSubMenu();
            int sub_size = subMenu.size();
            for(int j = 0; j < sub_size; j++) {
              MenuItem subMenuItem = subMenu.getItem(j);
              subMenuItem.setChecked(false);
            }
          }
          menuItem.setChecked(false);
        }
      }

      showList();
      is_in_on_create = false;
    }
    else {

      int size = generalSettings.getNonScheduledLists().size();
      String[] items = new String[size + 1];
      items[0] = menu.findItem(R.id.scheduled_list).getTitle().toString();
      for(int i = 0; i < size; i++) {
        items[i + 1] = generalSettings.getNonScheduledLists().get(i).getTitle();
      }

      new AlertDialog.Builder(this)
          .setTitle(R.string.action_send_booted_dialog_title)
          .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              which_list = which;
            }
          })
          .setPositiveButton(R.string.determine, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

              setIntGeneralInSharedPreferences(MENU_POSITION, which_list);
              setIntGeneralInSharedPreferences(SUBMENU_POSITION, 0);

              showList();
              is_in_on_create = false;
            }
          })
          .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

              detail = null;
              if(is_in_on_create) showList();
              is_in_on_create = false;
            }
          })
          .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

              detail = null;
              if(is_in_on_create) showList();
              is_in_on_create = false;
            }
          })
          .show();
    }
  }

  private void showList() {

    //前回開いていたNavigationDrawer上のメニューをリストアする
    menuItem = menu.getItem(which_menu_open);
    if(menuItem.hasSubMenu()) {
      menuItem = menuItem.getSubMenu().getItem(which_submenu_open);
    }

    //選択状態のリストア
    menuItem.setChecked(true);

    createAndSetFragmentColor();

    if(order == 0) {
      if(is_expandable_todo) {
        showExpandableListViewFragment();
      }
      else showDoneListViewFragment();
    }
    else if(order == 1) {
      if(generalSettings.getNonScheduledLists().get(which_menu_open - 1).isTodo()) {
        showListViewFragment();
      }
      else showDoneListViewFragment();
    }
    else if(order == 3) showManageListViewFragment();
    else if(order == 4) showGeneralSettingsFragment();
    else if(order == 5) showHelpAndFeedbackFragment();
  }

  @Override
  protected void onResume() {

    super.onResume();

    //すべての通知を既読する
    NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    checkNotNull(manager);
    manager.cancelAll();

    if(!is_premium) {

      //ビリングサービスのセットアップ
      setupBillingServices();
    }

    //データベースを端末暗号化ストレージへコピーする
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Context direct_boot_context = createDeviceProtectedStorageContext();
      direct_boot_context.moveDatabaseFrom(this, MyDatabaseHelper.TODO_TABLE);
      direct_boot_context.moveDatabaseFrom(this, MyDatabaseHelper.DONE_TABLE);
      direct_boot_context.moveDatabaseFrom(this, MyDatabaseHelper.SETTINGS_TABLE);
    }

    Fragment mainFragment = getSupportFragmentManager().findFragmentByTag(ExpandableListViewFragment.TAG);
    if(mainFragment != null && mainFragment.isVisible()) {
      setUpdateListTimerTask(true);
    }
  }

  @Override
  protected void onStop() {

    super.onStop();

    //バックアップアカウントにログインしている場合はここでログアウトする
    if(is_premium && generalSettingsFragment != null) {
      if(generalSettingsFragment.backupAndRestoreFragment != null) {
        generalSettingsFragment.backupAndRestoreFragment.signOut();
      }
    }
  }

  @Override
  protected void onPause() {

    super.onPause();

    //データベースを端末暗号化ストレージへコピーする
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Context direct_boot_context = createDeviceProtectedStorageContext();
      direct_boot_context.moveDatabaseFrom(this, MyDatabaseHelper.TODO_TABLE);
      direct_boot_context.moveDatabaseFrom(this, MyDatabaseHelper.DONE_TABLE);
      direct_boot_context.moveDatabaseFrom(this, MyDatabaseHelper.SETTINGS_TABLE);
    }

    //ExpandableListViewの自動更新を止める
    setUpdateListTimerTask(false);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {

    if(drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawer(GravityCompat.START);
    }
    else super.onBackPressed();
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {

    super.onPostCreate(savedInstanceState);
    drawerToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {

    super.onConfigurationChanged(newConfig);
    drawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

    if(menuItem.getItemId() != R.id.add_list && menuItem.getItemId() != R.id.share) {

      //選択されたメニューアイテム以外のチェックを外す
      if(!menuItem.isChecked()) {

        int size = menu.size();
        for(int i = 0; i < size; i++) {

          if(menu.getItem(i) == menuItem) {
            setIntGeneralInSharedPreferences(MENU_POSITION, i);
          }

          if(menu.getItem(i).hasSubMenu()) {
            SubMenu subMenu = menu.getItem(i).getSubMenu();
            int sub_size = subMenu.size();
            for(int j = 0; j < sub_size; j++) {
              if(subMenu.getItem(j) == menuItem) {
                setIntGeneralInSharedPreferences(MENU_POSITION, i);
                setIntGeneralInSharedPreferences(SUBMENU_POSITION, j);
              }
              subMenu.getItem(j).setChecked(false);
            }
          }
          else menu.getItem(i).setChecked(false);
        }
        menuItem.setChecked(true);

        //選択されたmenuItemに対応するフラグメントを表示
        this.menuItem = menuItem;
        createAndSetFragmentColor();
        switch(order) {
          case 0: {
            if(is_expandable_todo) {
              showExpandableListViewFragment();
            }
            else showDoneListViewFragment();
            break;
          }
          case 1: {
            if(generalSettings.getNonScheduledLists().get(which_menu_open - 1).isTodo()) {
              showListViewFragment();
            }
            else showDoneListViewFragment();
            break;
          }
          case 3: {
            showManageListViewFragment();
            break;
          }
          case 4: {
            showGeneralSettingsFragment();
            break;
          }
          case 5: {
            showHelpAndFeedbackFragment();
            break;
          }
        }
      }
      else drawerLayout.closeDrawer(GravityCompat.START);
    }
    else if(menuItem.getItemId() == R.id.add_list) {
      //ダイアログに表示するEditTextの設定
      LinearLayout linearLayout = new LinearLayout(MainActivity.this);
      linearLayout.setOrientation(LinearLayout.VERTICAL);
      final EditText editText = new EditText(MainActivity.this);
      editText.setHint(R.string.list_hint);
      editText.setLayoutParams(new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
      ));
      linearLayout.addView(editText);
      int paddingPx = getPxFromDp(this, 20);
      linearLayout.setPadding(paddingPx, 0, paddingPx, 0);

      //新しいリストの名前を設定するダイアログを表示
      final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
          .setTitle(R.string.add_list)
          .setView(linearLayout)
          .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

              //GeneralSettingsとManageListAdapterへの反映
              String name = editText.getText().toString();
              if(name.equals("")) {
                name = getString(R.string.default_list);
              }
              generalSettings.getNonScheduledLists().add(0, new NonScheduledList(name));
              int size = generalSettings.getNonScheduledLists().size();
              for(int i = 0; i < size; i++) {
                generalSettings.getNonScheduledLists().get(i).setOrder(i);
              }
              ManageListAdapter.nonScheduledLists = new ArrayList<>(generalSettings.getNonScheduledLists());
              manageListAdapter.notifyDataSetChanged();

              //一旦reminder_listグループ内のアイテムをすべて消してから元に戻すことで新しく追加したリストの順番を追加した順に並び替える

              //デフォルトアイテムのリストア
              menu.removeGroup(R.id.reminder_list);
              menu.add(R.id.reminder_list, R.id.scheduled_list, 0, R.string.nav_scheduled_item)
                  .setIcon(R.drawable.ic_time)
                  .setCheckable(true);
              menu.add(R.id.reminder_list, R.id.add_list, 2, R.string.add_list)
                  .setIcon(R.drawable.ic_add_24dp)
                  .setCheckable(false);

              //新しく追加したリストのリストア
              for(NonScheduledList list : generalSettings.getNonScheduledLists()) {
                Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_my_list_24dp);
                checkNotNull(drawable);
                drawable = drawable.mutate();
                if(list.getColor() != 0) {
                  drawable.setColorFilter(list.getColor(), PorterDuff.Mode.SRC_IN);
                }
                else {
                  drawable.setColorFilter(
                      ContextCompat.getColor(MainActivity.this, R.color.icon_gray), PorterDuff.Mode.SRC_IN
                  );
                }
                menu.add(R.id.reminder_list, Menu.NONE, 1, list.getTitle())
                    .setIcon(drawable)
                    .setCheckable(true);
              }

              //データベースへの反映
              updateSettingsDB();
            }
          })
          .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
          })
          .show();

      //ダイアログ表示時にソフトキーボードを自動で表示
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
    }
    else if(menuItem.getItemId() == R.id.share) {
      String title = getString(R.string.app_promotion);
      String content = getString(R.string.app_url);

      Intent intent = new Intent()
          .setAction(Intent.ACTION_SEND)
          .setType("text/plain")
          .putExtra(Intent.EXTRA_SUBJECT, title)
          .putExtra(Intent.EXTRA_TEXT, content);

      startActivity(intent);
    }

    return false;
  }

  public void updateListTask(final Item snackBarItem, final int group_position, boolean unlock_notify) {

    boolean is_updated = false;
    int groups_size = MyExpandableListAdapter.groups.size();
    for(int group_count = 0; group_count < groups_size; group_count++) {

      int group_changed = 0;
      List<Item> itemList = MyExpandableListAdapter.children.get(group_count);

      int children_size = itemList.size();
      for(int child_count = 0; child_count < children_size; child_count++) {

        Item item = itemList.get(child_count);
        Calendar now = Calendar.getInstance();
        Calendar tomorrow = (Calendar)now.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        int spec_day = item.getDate().get(Calendar.DAY_OF_MONTH);
        long sub_time = item.getDate().getTimeInMillis() - now.getTimeInMillis();
        long sub_day = sub_time / (24 * HOUR);

        if(sub_time < 0) {
          if(group_count != 0) {
            MyExpandableListAdapter.children.get(0).add(item);
            group_changed |= (1 << child_count);
          }
        }
        else if(sub_day < 1 && spec_day == now.get(Calendar.DAY_OF_MONTH)) {
          if(group_count != 1) {
            MyExpandableListAdapter.children.get(1).add(item);
            group_changed |= (1 << child_count);
          }
        }
        else if(sub_day < 2 && spec_day == tomorrow.get(Calendar.DAY_OF_MONTH)) {
          if(group_count != 2) {
            MyExpandableListAdapter.children.get(2).add(item);
            group_changed |= (1 << child_count);
          }
        }
        else if(sub_day < 8) {
          if(group_count != 3) {
            MyExpandableListAdapter.children.get(3).add(item);
            group_changed |= (1 << child_count);
          }
        }
        else {
          if(group_count != 4) {
            MyExpandableListAdapter.children.get(4).add(item);
            group_changed |= (1 << child_count);
          }
        }
      }

      int remove_count = 0;
      int binary_length = Integer.toBinaryString(group_changed).length();
      for(int i = 0; i < binary_length; i++) {
        if((group_changed & (1 << i)) != 0) {
          MyExpandableListAdapter.children.get(group_count).remove(i - remove_count);
          remove_count++;
          is_updated = true;
        }
      }
    }

    if(is_updated) {
      Calendar now = Calendar.getInstance();
      Calendar tomorrow_cal = (Calendar)now.clone();
      tomorrow_cal.add(Calendar.DAY_OF_MONTH, 1);
      CharSequence today;
      CharSequence tomorrow;
      if(LOCALE.equals(Locale.JAPAN)) {
        today = DateFormat.format(" - yyyy年M月d日(E)", now);
        tomorrow = DateFormat.format(" - yyyy年M月d日(E)", tomorrow_cal);
      }
      else {
        today = DateFormat.format(" - yyyy/M/d (E)", now);
        tomorrow = DateFormat.format(" - yyyy/M/d (E)", tomorrow_cal);
      }
      MyExpandableListAdapter.groups.set(1, getString(R.string.today) + today);
      MyExpandableListAdapter.groups.set(2, getString(R.string.tomorrow) + tomorrow);

      for(List<Item> itemList : MyExpandableListAdapter.children) {
        Collections.sort(itemList, SCHEDULED_ITEM_COMPARATOR);
      }
    }

    if(!MyExpandableListAdapter.lock_block_notify_change || unlock_notify) {
      expandableListAdapter.notifyDataSetChanged();
      MyExpandableListAdapter.block_notify_change = false;
      MyExpandableListAdapter.lock_block_notify_change = false;
    }

    if(snackBarItem != null) {
      Snackbar.make(findViewById(R.id.content), getResources().getString(R.string.complete), Snackbar.LENGTH_LONG)
          .addCallback(new Snackbar.Callback() {
            @Override
            public void onShown(Snackbar sb) {

              super.onShown(sb);
            }

            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {

              super.onDismissed(transientBottomBar, event);
              MyExpandableListAdapter.panel_lock_id = 0;
            }
          })
          .setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

              MyExpandableListAdapter.lock_block_notify_change = true;
              MyExpandableListAdapter.block_notify_change = true;

              if(snackBarItem.getDayRepeat().getSetted() != 0 || snackBarItem.getMinuteRepeat().getWhich_setted() != 0) {
                snackBarItem.setAlarm_stopped(snackBarItem.isOrg_alarm_stopped());
                snackBarItem.setTime_altered(snackBarItem.getOrg_time_altered());
                if((snackBarItem.getMinuteRepeat().getWhich_setted() & 1) != 0) {
                  snackBarItem.getMinuteRepeat().setCount(snackBarItem.getMinuteRepeat().getOrg_count2());
                }
                else if((snackBarItem.getMinuteRepeat().getWhich_setted() & (1 << 1)) != 0) {
                  snackBarItem.getMinuteRepeat().setDuration(snackBarItem.getMinuteRepeat().getOrg_duration2());
                }
                snackBarItem.getDate().setTimeInMillis(snackBarItem.getOrg_date().getTimeInMillis() + snackBarItem.getTime_altered());
                Collections.sort(MyExpandableListAdapter.children.get(group_position), SCHEDULED_ITEM_COMPARATOR);
                expandableListAdapter.notifyDataSetChanged();

                deleteAlarm(snackBarItem);
                if(!snackBarItem.isAlarm_stopped()) setAlarm(snackBarItem);
                updateDB(snackBarItem, MyDatabaseHelper.TODO_TABLE);
              }
              else {
                snackBarItem.getDate().setTimeInMillis(snackBarItem.getOrg_date().getTimeInMillis() + snackBarItem.getTime_altered());
                MyExpandableListAdapter.children.get(group_position).add(snackBarItem);
                for(List<Item> itemList : MyExpandableListAdapter.children) {
                  Collections.sort(itemList, SCHEDULED_ITEM_COMPARATOR);
                }
                expandableListAdapter.notifyDataSetChanged();

                if(!snackBarItem.isAlarm_stopped()) setAlarm(snackBarItem);
                insertDB(snackBarItem, MyDatabaseHelper.TODO_TABLE);
                deleteDB(snackBarItem, MyDatabaseHelper.DONE_TABLE);
              }

              updateListTask(null, -1, true);
            }
          })
          .show();
    }
  }

  private class UpdateListTimerTask extends TimerTask {

    @Override
    public void run() {
      handler.post(new Runnable() {
        @Override
        public void run() {

          if(!MyExpandableListAdapter.block_notify_change) {
            updateListTask(null, -1, false);
          }
        }
      });
    }
  }

  public void setUpdateListTimerTask(boolean is_set) {

    if(timer != null && !is_set) {
      timer.cancel();
      timer = null;
    }
    else if(timer == null && is_set) {
      timer = new Timer();
      TimerTask timerTask = new UpdateListTimerTask();
      timer.schedule(timerTask, 0, 1000);
    }
  }

  public List<List<Item>> getChildren(String table) {

    List<Item> past_list = new ArrayList<>();
    List<Item> today_list = new ArrayList<>();
    List<Item> tomorrow_list = new ArrayList<>();
    List<Item> week_list = new ArrayList<>();
    List<Item> future_list = new ArrayList<>();

    Calendar now = Calendar.getInstance();
    Calendar tomorrow = (Calendar)now.clone();
    tomorrow.add(Calendar.DAY_OF_MONTH, 1);

    for(Item item : queryAllDB(table)) {

      if(item.getWhich_list_belongs() == 0) {
        if(item.getNotify_interval().getTime() == item.getNotify_interval().getOrg_time()) {
          deleteAlarm(item);
        }
        if(!isAlarmSetted(item) && !item.isAlarm_stopped()) {
          setAlarm(item);
        }

        int spec_day = item.getDate().get(Calendar.DAY_OF_MONTH);
        long sub_time = item.getDate().getTimeInMillis() - now.getTimeInMillis();
        long sub_day = sub_time / (24 * HOUR);

        if(sub_time < 0) {
          past_list.add(item);
        }
        else if(sub_day < 1 && spec_day == now.get(Calendar.DAY_OF_MONTH)) {
          today_list.add(item);
        }
        else if(sub_day < 2 && spec_day == tomorrow.get(Calendar.DAY_OF_MONTH)) {
          tomorrow_list.add(item);
        }
        else if(sub_day < 8) {
          week_list.add(item);
        }
        else {
          future_list.add(item);
        }
      }
    }

    List<List<Item>> children = new ArrayList<>();
    children.add(past_list);
    children.add(today_list);
    children.add(tomorrow_list);
    children.add(week_list);
    children.add(future_list);

    for(List<Item> itemList : children) {
      Collections.sort(itemList, SCHEDULED_ITEM_COMPARATOR);
    }
    return children;
  }

  public void addChildren(Item item, String table) {

    Calendar now = Calendar.getInstance();
    Calendar tomorrow = (Calendar)now.clone();
    tomorrow.add(Calendar.DAY_OF_MONTH, 1);

    int spec_day = item.getDate().get(Calendar.DAY_OF_MONTH);
    long sub_time = item.getDate().getTimeInMillis() - now.getTimeInMillis();
    long sub_day = sub_time / (24 * HOUR);

    if(sub_time < 0) {
      MyExpandableListAdapter.children.get(0).add(item);
    }
    else if(sub_day < 1 && spec_day == now.get(Calendar.DAY_OF_MONTH)) {
      MyExpandableListAdapter.children.get(1).add(item);
    }
    else if(sub_day < 2 && spec_day == tomorrow.get(Calendar.DAY_OF_MONTH)) {
      MyExpandableListAdapter.children.get(2).add(item);
    }
    else if(sub_day < 8) {
      MyExpandableListAdapter.children.get(3).add(item);
    }
    else {
      MyExpandableListAdapter.children.get(4).add(item);
    }

    for(List<Item> itemList : MyExpandableListAdapter.children) {
      Collections.sort(itemList, SCHEDULED_ITEM_COMPARATOR);
    }
    expandableListAdapter.notifyDataSetChanged();
    insertDB(item, table);
  }

  public void setUpdatedItemPosition(long id) {

    List<List<Item>> children = getChildren(MyDatabaseHelper.TODO_TABLE);
    int count = 0;
    int i_size = MyExpandableListAdapter.groups.size();
    for(int i = 0; i < i_size && id != 0; i++) {
      if(MyExpandableListAdapter.display_groups[i]) {
        List<Item> itemList = children.get(i);
        int j_size = itemList.size();
        for(int j = 0; j < j_size; j++) {
          Item item = itemList.get(j);
          if(item.getId() == id) {
            try {
              ExpandableListViewFragment.position =
                  expandableListView.getFlatListPosition(ExpandableListView.getPackedPositionForChild(count, j));
              ExpandableListViewFragment.offset = ExpandableListViewFragment.group_height;
            }
            catch(NullPointerException e) {
              ExpandableListViewFragment.position = 0;
              ExpandableListViewFragment.offset = 0;
            }
            id = 0;
            break;
          }
        }

        count++;
      }
    }
  }

  public List<Item> getNonScheduledItem(String table) {

    if(which_menu_open > 0) {
      long list_id = generalSettings.getNonScheduledLists().get(which_menu_open - 1).getId();
      List<Item> itemList = new ArrayList<>();
      for(Item item : queryAllDB(table)) {
        if(item.getWhich_list_belongs() == list_id) {
          itemList.add(item);
        }
      }
      Collections.sort(itemList, NON_SCHEDULED_ITEM_COMPARATOR);

      return itemList;
    }
    else return new ArrayList<>();
  }

  public List<Item> getDoneItem() {

    long list_id = which_menu_open == 0 ? 0 : generalSettings.getNonScheduledLists().get(which_menu_open - 1).getId();
    List<Item> itemList = new ArrayList<>();
    for(Item item : queryAllDB(MyDatabaseHelper.DONE_TABLE)) {
      if(item.getWhich_list_belongs() == list_id) {
        itemList.add(item);
      }
    }
    Collections.sort(itemList, DONE_ITEM_COMPARATOR);

    return itemList;
  }

  private void initSettings() {

    generalSettings = new GeneralSettings();

    //データベースを新たに作成する場合、基本的な一般設定を追加しておく

    //タグのデフォルト設定
    //タグなし
    Tag tag = new Tag(0);
    tag.setName(getString(R.string.none));
    tag.setOrder(0);
    generalSettings.getTagList().add(tag);

    //家事タグ
    tag = new Tag(1);
    tag.setName(getString(R.string.home));
    tag.setPrimary_color(Color.parseColor("#4caf50"));
    tag.setPrimary_light_color(Color.parseColor("#80e27e"));
    tag.setPrimary_dark_color(Color.parseColor("#087f23"));
    tag.setPrimary_text_color(Color.parseColor("#000000"));
    tag.setColor_order_group(9);
    tag.setColor_order_child(5);
    tag.setOrder(1);
    generalSettings.getTagList().add(tag);

    //仕事タグ
    tag = new Tag(2);
    tag.setName(getString(R.string.work));
    tag.setPrimary_color(Color.parseColor("#2196f3"));
    tag.setPrimary_light_color(Color.parseColor("#6ec6ff"));
    tag.setPrimary_dark_color(Color.parseColor("#0069c0"));
    tag.setPrimary_text_color(Color.parseColor("#000000"));
    tag.setColor_order_group(5);
    tag.setColor_order_child(5);
    tag.setOrder(2);
    generalSettings.getTagList().add(tag);

    //ショッピングタグ
    tag = new Tag(3);
    tag.setName(getString(R.string.shopping));
    tag.setPrimary_color(Color.parseColor("#f44336"));
    tag.setPrimary_light_color(Color.parseColor("#ff7961"));
    tag.setPrimary_dark_color(Color.parseColor("#ba000d"));
    tag.setPrimary_text_color(Color.parseColor("#000000"));
    tag.setColor_order_group(0);
    tag.setColor_order_child(5);
    tag.setOrder(3);
    generalSettings.getTagList().add(tag);


    //Itemのデフォルト設定
    Item item = generalSettings.getItem();

    //NotifyInterval
    NotifyInterval notifyInterval = new NotifyInterval();
    notifyInterval.setHour(0);
    notifyInterval.setMinute(5);
    notifyInterval.setOrg_time(6);
    notifyInterval.setWhich_setted(1);

    if(notifyInterval.getOrg_time() != 0) {
      String summary;
      if(LOCALE.equals(Locale.JAPAN)) {
        summary = getString(R.string.unless_complete_task);
        if(notifyInterval.getHour() != 0) {
          summary += getResources().getQuantityString(R.plurals.hour, notifyInterval.getHour(), notifyInterval.getHour());
        }
        if(notifyInterval.getMinute() != 0) {
          summary += getResources().getQuantityString(R.plurals.minute, notifyInterval.getMinute(), notifyInterval.getMinute());
        }
        summary += getString(R.string.per);
        if(notifyInterval.getOrg_time() == -1) {
          summary += getString(R.string.infinite_times_notify);
        }
        else {
          summary += getResources().getQuantityString(R.plurals.times_notify, notifyInterval.getOrg_time(),
              notifyInterval.getOrg_time());
        }
      }
      else {
        summary = "Notify every ";
        if(notifyInterval.getHour() != 0) {
          summary += getResources().getQuantityString(R.plurals.hour, notifyInterval.getHour(), notifyInterval.getHour());
          if(!LOCALE.equals(Locale.JAPAN)) summary += " ";
        }
        if(notifyInterval.getMinute() != 0) {
          summary += getResources().getQuantityString(R.plurals.minute, notifyInterval.getMinute(), notifyInterval.getMinute());
          if(!LOCALE.equals(Locale.JAPAN)) summary += " ";
        }
        if(notifyInterval.getOrg_time() != -1) {
          summary += getResources().getQuantityString(R.plurals.times_notify, notifyInterval.getOrg_time(),
              notifyInterval.getOrg_time()) + " ";
        }
        summary += getString(R.string.unless_complete_task);
      }

      notifyInterval.setLabel(summary);
    }
    else {
      notifyInterval.setLabel(getString(R.string.none));
    }
    item.setNotify_interval(notifyInterval);

    //AlarmSound
    item.setSoundUri(DEFAULT_URI_SOUND.toString());

    //手動スヌーズ時間のデフォルト設定
    setIntGeneralInSharedPreferences(SNOOZE_DEFAULT_HOUR, 0);
    setIntGeneralInSharedPreferences(SNOOZE_DEFAULT_MINUTE, 15);

    insertSettingsDB();
  }

  //受け取ったオブジェクトをシリアライズしてデータベースへ挿入
  public void insertDB(Item item, String table) {

    accessor.executeInsert(item.getId(), serialize(item), table);
  }

  public void updateDB(Item item, String table) {

    accessor.executeUpdate(item.getId(), serialize(item), table);
  }

  public void deleteDB(Item item, String table) {

    accessor.executeDelete(item.getId(), table);
  }

  //指定されたテーブルからオブジェクトのバイト列をすべて取り出し、デシリアライズしてオブジェクトのリストで返す。
  public List<Item> queryAllDB(String table) {

    List<Item> itemList = new ArrayList<>();

    for(byte[] stream : accessor.executeQueryAll(table)) {
      itemList.add((Item)deserialize(stream));
    }

    return itemList;
  }

  public boolean isItemExists(Item item, String table) {

    return accessor.executeQueryById(item.getId(), table) != null;
  }

  public void insertSettingsDB() {

    accessor.executeInsert(1, serialize(generalSettings), MyDatabaseHelper.SETTINGS_TABLE);
  }

  public void updateSettingsDB() {

    accessor.executeUpdate(1, serialize(generalSettings), MyDatabaseHelper.SETTINGS_TABLE);
  }

  public GeneralSettings querySettingsDB() {

    return (GeneralSettings)deserialize(accessor.executeQueryById(1, MyDatabaseHelper.SETTINGS_TABLE));
  }

  public void setIntGeneralInSharedPreferences(String TAG, int value) {

    switch(TAG) {
      case SNOOZE_DEFAULT_HOUR:
        snooze_default_hour = value;
        break;
      case SNOOZE_DEFAULT_MINUTE:
        snooze_default_minute = value;
        break;
      case MENU_POSITION:
        which_menu_open = value;
        break;
      case SUBMENU_POSITION:
        which_submenu_open = value;
        break;
      default:
        throw new IllegalArgumentException(TAG);
    }

    getSharedPreferences(INT_GENERAL, Context.MODE_PRIVATE)
        .edit()
        .putInt(TAG, value)
        .apply();
  }

  public void setBooleanGeneralInSharedPreferences(String TAG, boolean value) {

    switch(TAG) {
      case IS_EXPANDABLE_TODO:
        is_expandable_todo = value;
        break;
      case IS_PREMIUM:
        is_premium = value;
        break;
      default:
        throw new IllegalArgumentException(TAG);
    }

    getSharedPreferences(BOOLEAN_GENERAL, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(TAG, value)
        .apply();
  }

  public void setAlarm(Item item) {

    if(item.getDate().getTimeInMillis() > System.currentTimeMillis() && item.getWhich_list_belongs() == 0) {
      item.getNotify_interval().setTime(item.getNotify_interval().getOrg_time());
      Intent intent = new Intent(this, AlarmReceiver.class);
      byte[] ob_array = serialize(item);
      intent.putExtra(ITEM, ob_array);
      PendingIntent sender = PendingIntent.getBroadcast(
          this, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
      checkNotNull(alarmManager);

      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        alarmManager.setAlarmClock(
            new AlarmManager.AlarmClockInfo(item.getDate().getTimeInMillis(), null), sender);
      }
      else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, item.getDate().getTimeInMillis(), sender);
      }
      else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, item.getDate().getTimeInMillis(), sender);
      }
    }
  }

  public void deleteAlarm(Item item) {

    if(isAlarmSetted(item)) {
      Intent intent = new Intent(this, AlarmReceiver.class);
      PendingIntent sender = PendingIntent.getBroadcast(
          this, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
      checkNotNull(alarmManager);

      alarmManager.cancel(sender);
      sender.cancel();
    }
  }

  public boolean isAlarmSetted(Item item) {

    Intent intent = new Intent(this, AlarmReceiver.class);
    PendingIntent sender = PendingIntent.getBroadcast(
        this, (int)item.getId(), intent, PendingIntent.FLAG_NO_CREATE);

    return sender != null;
  }

  private void createAndSetFragmentColor() {

    //開いているFragmentに応じた色を作成
    order = menuItem.getOrder();
    if(order == 1) {
      NonScheduledList list = generalSettings.getNonScheduledLists().get(which_menu_open - 1);
      if(list.getColor() == 0) setDefaultColor();
      else {
        menu_item_color = list.getTextColor();
        menu_background_color = list.getColor();
        status_bar_color = list.getDarkColor();
        list.setColor_primary(false);
        if(list.getColor() == 0) {
          accent_color = ContextCompat.getColor(this, R.color.colorAccent);
          secondary_text_color = ContextCompat.getColor(this, android.R.color.black);
        }
        else {
          accent_color = list.getColor();
          secondary_text_color = list.getTextColor();
        }
        list.setColor_primary(true);
      }
    }
    else setDefaultColor();

    //ハンバーガーアイコンの色を指定
    drawerToggle.getDrawerArrowDrawable().setColor(menu_item_color);
    //DisplayHomeAsUpEnabledに指定する戻るボタンの色を指定
    upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material);
    checkNotNull(upArrow);
    upArrow.setColorFilter(menu_item_color, PorterDuff.Mode.SRC_IN);
    //ツールバーとステータスバーの色を指定
    toolbar.setTitleTextColor(menu_item_color);
    toolbar.setBackgroundColor(menu_background_color);
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Window window = getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(status_bar_color);
    }
  }

  private void setDefaultColor() {

    MyTheme theme = generalSettings.getTheme();
    if(theme.getColor() == 0) {
      menu_item_color = Color.WHITE;
      menu_background_color = ContextCompat.getColor(this, R.color.colorPrimary);
      status_bar_color = ContextCompat.getColor(this, R.color.colorPrimaryDark);
      theme.setColor_primary(false);
      if(theme.getColor() == 0) {
        accent_color = ContextCompat.getColor(this, R.color.colorAccent);
        secondary_text_color = ContextCompat.getColor(this, android.R.color.black);
      }
      else {
        accent_color = theme.getColor();
        secondary_text_color = theme.getTextColor();
      }
      theme.setColor_primary(true);
    }
    else {
      menu_item_color = theme.getTextColor();
      menu_background_color = theme.getColor();
      status_bar_color = theme.getDarkColor();
      theme.setColor_primary(false);
      if(theme.getColor() == 0) {
        accent_color = ContextCompat.getColor(this, R.color.colorAccent);
        secondary_text_color = ContextCompat.getColor(this, android.R.color.black);
      }
      else {
        accent_color = theme.getColor();
        secondary_text_color = theme.getTextColor();
      }
      theme.setColor_primary(true);
    }
  }

  public void showDoneListViewFragment() {

    actionBarFragment = ActionBarFragment.newInstance();
    showFragment(DoneListViewFragment.TAG, DoneListViewFragment.newInstance(),
        ActionBarFragment.TAG, actionBarFragment, false);
  }

  public void showTagEditListViewFragment() {

    showFragment(TagEditListViewFragment.TAG, TagEditListViewFragment.newInstance(),
        null, null, true);
  }

  public void showColorPickerListViewFragment() {

    showFragment(ColorPickerListViewFragment.TAG, ColorPickerListViewFragment.newInstance(),
        null, null, true);
  }

  public void showManageListViewFragment() {

    actionBarFragment = ActionBarFragment.newInstance();
    showFragment(ManageListViewFragment.TAG, ManageListViewFragment.newInstance(),
        ActionBarFragment.TAG, actionBarFragment, false);
  }

  public void showListViewFragment() {

    actionBarFragment = ActionBarFragment.newInstance();
    showFragment(ListViewFragment.TAG, ListViewFragment.newInstance(),
        ActionBarFragment.TAG, actionBarFragment, false);
  }

  public void showExpandableListViewFragment() {

    actionBarFragment = ActionBarFragment.newInstance();
    showFragment(ExpandableListViewFragment.TAG, ExpandableListViewFragment.newInstance(),
        ActionBarFragment.TAG, actionBarFragment, false);
  }

  //編集画面を表示(引数にitemを渡すとそのitemの情報が入力された状態で表示)
  public void showMainEditFragment() {

    MainEditFragment.is_main_popping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstance(),
        null, null, true);
  }

  public void showMainEditFragment(String detail) {

    MainEditFragment.is_main_popping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstance(detail),
        null, null, true);
  }

  public void showMainEditFragment(Item item) {

    MainEditFragment.is_main_popping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstance(item.clone()),
        null, null, true);
  }

  public void showMainEditFragmentForList() {

    MainEditFragment.is_main_popping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstanceForList(),
        null, null, true);
  }

  public void showMainEditFragmentForList(NonScheduledList list) {

    MainEditFragment.is_main_popping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstanceForList(list.clone()),
        null, null, true);
  }

  public void showNotesFragment(Item item) {

    Fragment nextFragment;
    String nextFragmentTAG;
    if(item.isChecklist_mode()) {
      nextFragment = NotesChecklistModeFragment.newInstance(item);
      nextFragmentTAG = NotesChecklistModeFragment.TAG;
    }
    else {
      nextFragment = NotesEditModeFragment.newInstance(item);
      nextFragmentTAG = NotesEditModeFragment.TAG;
    }

    MainEditFragment.is_notes_popping = false;
    showFragment(nextFragmentTAG, nextFragment, null, null, true);
  }

  public void showGeneralSettingsFragment() {

    generalSettingsFragment = GeneralSettingsFragment.newInstance();
    showFragment(GeneralSettingsFragment.TAG, generalSettingsFragment,
        null, null, false);
  }

  public void showHelpAndFeedbackFragment() {

    showFragment(HelpAndFeedbackFragment.TAG, HelpAndFeedbackFragment.newInstance(),
        null, null, false);
  }

  public void showAboutThisAppFragment() {

    showFragment(AboutThisAppFragment.TAG, AboutThisAppFragment.newInstance(),
        null, null, true);
  }

  private void commitFragment(Fragment remove1, Fragment remove2, String add1, Fragment addFragment1,
      String add2, Fragment addFragment2, boolean back_stack) {

    FragmentManager manager = getSupportFragmentManager();

    FragmentTransaction transaction = manager.beginTransaction();
    if(remove1 != null) {
      transaction.remove(remove1);
    }
    if(remove2 != null) {
      transaction.remove(remove2);
    }
    if(add1 != null) {
      transaction.add(R.id.content, addFragment1, add1);
    }
    if(add2 != null) {
      transaction.add(R.id.content, addFragment2, add2);
    }
    if(back_stack) {
      transaction.addToBackStack(null);
    }
    transaction.commit();
  }

  private void showFragment(String add1, Fragment addFragment1, String add2, Fragment addFragment2, boolean back_stack) {

    FragmentManager manager = getSupportFragmentManager();
    Fragment rmFragment = manager.findFragmentById(R.id.content);

    if(rmFragment == null) {
      commitFragment(null, null, add1, addFragment1, add2, addFragment2, back_stack);
    }
    else if(rmFragment instanceof ActionBarFragment) {

      String[] mainTAGs = {ExpandableListViewFragment.TAG, ListViewFragment.TAG,
          ManageListViewFragment.TAG, DoneListViewFragment.TAG};
      boolean is_found_match_fragment = false;
      for(String mainTAG : mainTAGs) {
        Fragment mainFragment = manager.findFragmentByTag(mainTAG);
        if(mainFragment != null && mainFragment.isVisible()) {
          is_found_match_fragment = true;
          commitFragment(mainFragment, rmFragment, add1, addFragment1, add2, addFragment2, back_stack);
          break;
        }
      }
      if(!is_found_match_fragment) {
        commitFragment(rmFragment, null, add1, addFragment1, add2, addFragment2, back_stack);
      }
    }
    else if(rmFragment instanceof ExpandableListViewFragment || rmFragment instanceof ListViewFragment
        || rmFragment instanceof ManageListViewFragment || rmFragment instanceof DoneListViewFragment) {

      commitFragment(rmFragment, manager.findFragmentByTag(ActionBarFragment.TAG),
          add1, addFragment1, add2, addFragment2, back_stack);
    }
    else {
      commitFragment(rmFragment, null, add1, addFragment1, add2, addFragment2, back_stack);
    }
  }
}