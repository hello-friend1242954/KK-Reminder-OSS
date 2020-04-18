package com.hideaki.kk_reminder;

import android.app.AlarmManager;
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
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.transition.Fade;
import androidx.transition.Transition;
import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;

import static com.google.common.base.Preconditions.checkArgument;
import static com.hideaki.kk_reminder.MyDatabaseHelper.TODO_TABLE;
import static com.hideaki.kk_reminder.StartupReceiver.getDynamicContext;
import static com.hideaki.kk_reminder.StartupReceiver.getIsDirectBootContext;
import static com.hideaki.kk_reminder.UtilClass.ACTION_IN_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.BOOLEAN_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.BOOLEAN_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.BOOT_FROM_NOTIFICATION;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_1_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_1_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_2_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_2_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_3_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_MINUS_TIME_3_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_1_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_1_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_2_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_2_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_3_HOUR;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_PLUS_TIME_3_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_TEXT_SIZE;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_URI_SOUND;
import static com.hideaki.kk_reminder.UtilClass.DONE_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.HOUR;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL_COPY;
import static com.hideaki.kk_reminder.UtilClass.IS_COPIED_FROM_OLD_VERSION;
import static com.hideaki.kk_reminder.UtilClass.IS_DARK_MODE;
import static com.hideaki.kk_reminder.UtilClass.IS_DARK_THEME_FOLLOW_SYSTEM;
import static com.hideaki.kk_reminder.UtilClass.IS_EXPANDABLE_TODO;
import static com.hideaki.kk_reminder.UtilClass.IS_PREMIUM;
import static com.hideaki.kk_reminder.UtilClass.IS_QUERIED_PURCHASE_HISTORY;
import static com.hideaki.kk_reminder.UtilClass.IS_RECREATED;
import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.MENU_POSITION;
import static com.hideaki.kk_reminder.UtilClass.MINUTE;
import static com.hideaki.kk_reminder.UtilClass.NON_SCHEDULED_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.PLAY_SLIDE_ANIMATION;
import static com.hideaki.kk_reminder.UtilClass.PRODUCT_ID_PREMIUM;
import static com.hideaki.kk_reminder.UtilClass.SCHEDULED_ITEM_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_HOUR;
import static com.hideaki.kk_reminder.UtilClass.SNOOZE_DEFAULT_MINUTE;
import static com.hideaki.kk_reminder.UtilClass.SUBMENU_POSITION;
import static com.hideaki.kk_reminder.UtilClass.copyDatabase;
import static com.hideaki.kk_reminder.UtilClass.copySharedPreferences;
import static com.hideaki.kk_reminder.UtilClass.deserialize;
import static com.hideaki.kk_reminder.UtilClass.generateUniqueId;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static com.hideaki.kk_reminder.UtilClass.readFileFromAssets;
import static com.hideaki.kk_reminder.UtilClass.serialize;
import static com.hideaki.kk_reminder.UtilClass.setCursorDrawableColor;
import static java.util.Objects.requireNonNull;

public class MainActivity extends AppCompatActivity
  implements NavigationView.OnNavigationItemSelectedListener,
  PurchasesUpdatedListener,
  AcknowledgePurchaseResponseListener {

  @SuppressWarnings("FieldNamingConvention")
  static String IS_FIRST_USE = "";

  private Timer timer;
  private Handler handler = new Handler();
  private Runnable runnable;
  private DBAccessor accessor = null;
  int minusTime1Hour;
  int minusTime1Minute;
  int minusTime2Hour;
  int minusTime2Minute;
  int minusTime3Hour;
  int minusTime3Minute;
  int plusTime1Hour;
  int plusTime1Minute;
  int plusTime2Hour;
  int plusTime2Minute;
  int plusTime3Hour;
  int plusTime3Minute;
  int snoozeDefaultHour;
  int snoozeDefaultMinute;
  int whichMenuOpen;
  int whichSubmenuOpen;
  boolean isExpandableTodo;
  boolean isPremium;
  PinnedHeaderExpandableListView expandableListView;
  MyExpandableListAdapter expandableListAdapter;
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
  GeneralSettingsAdapter generalSettings;
  ActionBarFragment actionBarFragment;
  ExpandableListViewFragment expandableListViewFragment;
  Toolbar toolbar;
  int menuItemColor;
  int menuBackgroundColor;
  int statusBarColor;
  int accentColor;
  int secondaryTextColor;
  int order;
  String detail;
  private int whichList;
  private boolean isInOnCreate;
  boolean isBootFromNotification;
  private int tryCount;
  private BillingClient billingClient;
  public AlertDialog promotionDialog;
  private BroadcastReceiver defaultSnoozeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      MyExpandableListAdapter.children = getChildren(MyDatabaseHelper.TODO_TABLE);
      expandableListAdapter.notifyDataSetChanged();
    }
  };
  int dialogStyleId;
  int textSize;
  int whichTextSize;
  boolean isPlaySlideAnimation;
  static boolean isScreenOn = false;
  boolean isDarkMode;
  boolean isDarkThemeFollowSystem;
  boolean isFirstUse;
  boolean isQueriedPurchaseHistory;
  int primaryMaterialDarkColor;
  int primaryDarkMaterialDarkColor;
  int backgroundMaterialDarkColor;
  int backgroundFloatingMaterialDarkColor;
  int primaryTextMaterialDarkColor;
  int secondaryTextMaterialDarkColor;
  private AlertDialog updateInfoMessageDialog = null;
  private boolean isRecreated;
  private List<SkuDetails> skuDetailsList = null;
  List<ItemAdapter> todoItemList;
  boolean isCopiedFromOldVersion;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    if(IS_FIRST_USE.equals("")) {
      IS_FIRST_USE = "IS_FIRST_USE_" + getString(R.string.version_value);
    }

    super.onCreate(savedInstanceState);

    accessor = new DBAccessor(this, false);

    // 共通設定と新しく追加したリストのリストア
    generalSettings = querySettingsDB();
    if(generalSettings == null) {
      initSettings();
    }

    // SharedPreferencesの内、intを読み出す
    SharedPreferences intPreferences =
      getDynamicContext(this).getSharedPreferences(
        getIsDirectBootContext(this) ? INT_GENERAL_COPY : INT_GENERAL,
        MODE_PRIVATE
      );
    minusTime1Hour = intPreferences.getInt(DEFAULT_MINUS_TIME_1_HOUR, 0);
    minusTime1Minute = intPreferences.getInt(DEFAULT_MINUS_TIME_1_MINUTE, 5);
    minusTime2Hour = intPreferences.getInt(DEFAULT_MINUS_TIME_2_HOUR, 1);
    minusTime2Minute = intPreferences.getInt(DEFAULT_MINUS_TIME_2_MINUTE, 0);
    minusTime3Hour = intPreferences.getInt(DEFAULT_MINUS_TIME_3_HOUR, 0);
    minusTime3Minute = intPreferences.getInt(DEFAULT_MINUS_TIME_3_MINUTE, 0);
    plusTime1Hour = intPreferences.getInt(DEFAULT_PLUS_TIME_1_HOUR, 0);
    plusTime1Minute = intPreferences.getInt(DEFAULT_PLUS_TIME_1_MINUTE, 5);
    plusTime2Hour = intPreferences.getInt(DEFAULT_PLUS_TIME_2_HOUR, 1);
    plusTime2Minute = intPreferences.getInt(DEFAULT_PLUS_TIME_2_MINUTE, 0);
    plusTime3Hour = intPreferences.getInt(DEFAULT_PLUS_TIME_3_HOUR, 0);
    plusTime3Minute = intPreferences.getInt(DEFAULT_PLUS_TIME_3_MINUTE, 0);
    whichTextSize = intPreferences.getInt(DEFAULT_TEXT_SIZE, 0);
    textSize = 18 + 2 * whichTextSize;
    snoozeDefaultHour = intPreferences.getInt(SNOOZE_DEFAULT_HOUR, 0);
    snoozeDefaultMinute = intPreferences.getInt(SNOOZE_DEFAULT_MINUTE, 15);
    whichMenuOpen = intPreferences.getInt(MENU_POSITION, 0);
    whichSubmenuOpen = intPreferences.getInt(SUBMENU_POSITION, 0);

    // SharedPreferencesの内、booleanを読み出す
    SharedPreferences booleanPreferences =
      getDynamicContext(this).getSharedPreferences(
        getIsDirectBootContext(this) ? BOOLEAN_GENERAL_COPY : BOOLEAN_GENERAL,
        MODE_PRIVATE
      );
    isCopiedFromOldVersion =
      booleanPreferences.getBoolean(IS_COPIED_FROM_OLD_VERSION, false);
    isQueriedPurchaseHistory =
      booleanPreferences.getBoolean(IS_QUERIED_PURCHASE_HISTORY, false);
    isRecreated = booleanPreferences.getBoolean(IS_RECREATED, false);
    isFirstUse = booleanPreferences.getBoolean(IS_FIRST_USE, true);
    isDarkMode = booleanPreferences.getBoolean(IS_DARK_MODE, false);
    isDarkThemeFollowSystem =
      booleanPreferences.getBoolean(IS_DARK_THEME_FOLLOW_SYSTEM, true);
    isPlaySlideAnimation = booleanPreferences.getBoolean(PLAY_SLIDE_ANIMATION, true);
    isExpandableTodo = booleanPreferences.getBoolean(IS_EXPANDABLE_TODO, true);
    isPremium = booleanPreferences.getBoolean(IS_PREMIUM, false);

    // 広告読み出し機能のセットアップ
    MobileAds.initialize(this, getString(R.string.app_id));

    if(!isPremium) {

      // ビリングサービスのセットアップ
      setupBillingServices();

      // プロモーション用ダイアログのセットアップ
      createPromotionDialog();
    }

    // アプリ更新後初めての起動時にアップデート情報を表示
    if(isFirstUse) {
      updateInfoMessageDialog = createUpdateInfoMessageDialog();
      if(updateInfoMessageDialog != null) {
        updateInfoMessageDialog.show();
      }
    }

    // ダークモードの設定
    int currentNightMode =
      getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    if(!isDarkThemeFollowSystem) {
      if(isDarkMode && currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
        setBooleanGeneralInSharedPreferences(IS_RECREATED, true);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
      }
      else if(!isDarkMode && currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
        setBooleanGeneralInSharedPreferences(IS_RECREATED, true);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
      }
      else {
        setBooleanGeneralInSharedPreferences(IS_RECREATED, false);
      }
    }

    // テーマの設定
    MyThemeAdapter theme = generalSettings.getTheme();
    if(theme.getColor() != 0) {
      Resources res = getResources();
      TypedArray typedArraysOfArray = res.obtainTypedArray(R.array.colorStylesArray);

      int stylesArrayId = typedArraysOfArray.getResourceId(theme.getColorGroup(), -1);
      checkArgument(stylesArrayId != -1);
      TypedArray typedArray = res.obtainTypedArray(stylesArrayId);

      int styleId = typedArray.getResourceId(theme.getColorChild(), -1);
      checkArgument(styleId != -1);

      typedArray.recycle();
      typedArraysOfArray.recycle();

      setTheme(styleId);
    }

    theme.setIsColorPrimary(false);
    if(theme.getColor() != 0) {
      Resources res = getResources();
      TypedArray typedArraysOfArray = res.obtainTypedArray(R.array.colorDialogStylesArray);

      int dialogStylesArrayId = typedArraysOfArray.getResourceId(
        theme.getColorGroup(),
        -1
      );
      checkArgument(dialogStylesArrayId != -1);
      TypedArray typedArray = res.obtainTypedArray(dialogStylesArrayId);

      dialogStyleId = typedArray.getResourceId(theme.getColorChild(), -1);
      checkArgument(dialogStyleId != -1);

      typedArray.recycle();
      typedArraysOfArray.recycle();
    }
    else {
      dialogStyleId = R.style.BaseDialog_Base;
    }
    theme.setIsColorPrimary(true);

    setContentView(R.layout.activity_main);

    // ToolbarをActionBarに互換を持たせて設定
    toolbar = findViewById(R.id.toolbar_layout);
    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    requireNonNull(actionBar);

    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);

    // NavigationDrawerの設定
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

    // 各NonScheduledListを読み込む
    for(NonScheduledListAdapter list : generalSettings.getNonScheduledLists()) {
      Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_my_list_24dp);
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
          ContextCompat.getColor(this, R.color.iconGray),
          PorterDuff.Mode.SRC_IN
        ));
      }
      menu.add(R.id.reminder_list, generateUniqueId(), 1, list.getTitle())
        .setIcon(drawable)
        .setCheckable(true);
    }

    // Adapterの初期化
    todoItemList = queryAllDB(TODO_TABLE);
    expandableListAdapter =
      new MyExpandableListAdapter(getChildren(todoItemList), this);
    listAdapter = new MyListAdapter(this);
    manageListAdapter = new ManageListAdapter(
      new ArrayList<>(generalSettings.getNonScheduledLists()),
      this
    );
    colorPickerListAdapter = new ColorPickerListAdapter(this);
    tagEditListAdapter = new TagEditListAdapter(
      new ArrayList<>(generalSettings.getTagList()),
      this
    );
    doneListAdapter = new DoneListAdapter(this);

    // Intentが送られている場合はonNewIntent()に渡す(送られていない場合は通常の初期化処理を行う)
    isInOnCreate = true;
    onNewIntent(getIntent());

    // 画面がフォアグラウンドの状態におけるDefaultManuallySnoozeReceiverからのインテントを待ち受ける
    registerReceiver(defaultSnoozeReceiver, new IntentFilter(ACTION_IN_NOTIFICATION));
  }

  @Override
  protected void onDestroy() {

    super.onDestroy();
    if(billingClient != null) {
      billingClient.endConnection();
    }
    unregisterReceiver(defaultSnoozeReceiver);
  }

  private AlertDialog createUpdateInfoMessageDialog() {

    final String updateInfoFileName =
      "update_info_" +
        getString(R.string.version_value) +
        (LOCALE.equals(Locale.JAPAN) ? "_ja.txt" : "_en.txt");

    String message = readFileFromAssets(this, updateInfoFileName);
    if(!"".equals(message)) {
      TextView customTitle = new TextView(this);
      String title =
        "ver. " + getString(R.string.version_value) + " " + getString(R.string.update_info);
      int leftPadding = getPxFromDp(this, 8);
      int topPadding = getPxFromDp(this, 20);
      int rightPadding = getPxFromDp(this, 8);
      int bottomPadding = getPxFromDp(this, 8);
      customTitle.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
      customTitle.setTextSize(getPxFromDp(this, 8));
      customTitle.setTypeface(Typeface.DEFAULT_BOLD);
      customTitle.setText(title);
      if(isDarkMode) {
        customTitle.setTextColor(
          ContextCompat.getColor(this, R.color.primaryTextMaterialDark)
        );
      }
      else {
        customTitle.setTextColor(ContextCompat.getColor(this, android.R.color.black));
      }
      customTitle.setGravity(Gravity.CENTER);

      final AlertDialog updateInfoMessageDialog = new AlertDialog.Builder(this)
        .setCancelable(false)
        .setCustomTitle(customTitle)
        .setMessage(message)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {

          }
        })
        .create();

      updateInfoMessageDialog.setOnShowListener(new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialogInterface) {

          updateInfoMessageDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accentColor);
          updateInfoMessageDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(accentColor);
        }
      });

      return updateInfoMessageDialog;
    }
    else {
      return null;
    }
  }

  private void createPromotionDialog() {

    promotionDialog = new AlertDialog.Builder(this)
      .setTitle(R.string.disable_ads)
      .setMessage(R.string.disable_ads_promotion)
      .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
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

    promotionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialogInterface) {

        promotionDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accentColor);
        promotionDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(accentColor);
      }
    });
  }

  private void setupBillingServices() {

    if(billingClient == null) {
      tryCount = 0;
      billingClient = BillingClient
        .newBuilder(this)
        .setListener(this)
        .enablePendingPurchases()
        .build();

      billingClient.startConnection(new BillingClientStateListener() {
        @Override
        public void onBillingSetupFinished(BillingResult billingResult) {

          if(billingResult.getResponseCode() == BillingResponseCode.OK) {
            // プレミアムアカウントかどうかの確認
            checkIsPremium();
          }
          else {
            tryCount++;
            if(tryCount < 3) {
              billingClient.startConnection(this);
            }
            else {
              Log.e("onBillingSetup", "Cannot start connection");
            }
          }
        }

        @Override
        public void onBillingServiceDisconnected() {

          tryCount++;
          if(tryCount < 3) {
            billingClient.startConnection(this);
          }
          else {
            Log.e("onBillingSetup", "Cannot start connection");
          }
        }
      });
    }
    else {
      checkIsPremium();
    }
  }

  private void checkIsPremium() {

    Purchase.PurchasesResult purchasesResult =
      billingClient.queryPurchases(BillingClient.SkuType.INAPP);
    int responseCode = purchasesResult.getResponseCode();
    if(responseCode == BillingResponseCode.OK) {
      Log.i("checkIsPremium", getResponseCodeString(responseCode));
      List<Purchase> purchaseList = purchasesResult.getPurchasesList();
      if(purchaseList == null || purchaseList.isEmpty()) {
        if(!isQueriedPurchaseHistory) {
          queryPurchaseHistory();
        }
      }
      else {
        for(Purchase purchase : purchaseList) {
          checkPurchaseState(purchase);
        }
      }
    }
    else {
      Log.w("checkIsPremium", getResponseCodeString(responseCode));
    }
  }

  // 購入履歴を問い合わせる(ネットワークアクセス処理)
  private void queryPurchaseHistory() {

    billingClient.queryPurchaseHistoryAsync(
      BillingClient.SkuType.INAPP,
      new PurchaseHistoryResponseListener() {
        @Override
        public void onPurchaseHistoryResponse(
          BillingResult billingResult,
          List<PurchaseHistoryRecord> purchasesList
        ) {

          int responseCode = billingResult.getResponseCode();
          if(responseCode == BillingResponseCode.OK) {
            Log.i("queryPurchaseHistory", getResponseCodeString(responseCode));
            setBooleanGeneralInSharedPreferences(IS_QUERIED_PURCHASE_HISTORY, true);
            if(purchasesList == null || purchasesList.isEmpty()) {
              Log.w("queryPurchaseHistory", "No History");
            }
            else {
              for(PurchaseHistoryRecord purchase : purchasesList) {
                if(PRODUCT_ID_PREMIUM.equals(purchase.getSku())) {
                  Log.i("queryPurchaseHistory", purchase.getSku() + ": Purchased");
                  Toast
                    .makeText(
                      MainActivity.this,
                      getString(R.string.succeed_to_upgrade),
                      Toast.LENGTH_LONG
                    )
                    .show();
                  setBooleanGeneralInSharedPreferences(IS_PREMIUM, true);
                  if(expandableListViewFragment != null) {
                    expandableListViewFragment.disableAdView();
                  }
                }
              }
            }
          }
          else {
            Log.w("queryPurchaseHistory", getResponseCodeString(responseCode));
          }
        }
      }
    );
  }

  @Override
  public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {

    int responseCode = billingResult.getResponseCode();
    if(responseCode == BillingResponseCode.OK) {
      if(purchases == null || purchases.isEmpty()) {
        Log.e("onPurchasesUpdated", "purchases is null or empty");
        Toast
          .makeText(MainActivity.this, getString(R.string.error_occurred), Toast.LENGTH_LONG)
          .show();
      }
      else {
        Log.i("onPurchasesUpdated", getResponseCodeString(responseCode));
        for(Purchase purchase : purchases) {
          checkPurchaseState(purchase);
        }
      }
    }
    else if(responseCode == BillingResponseCode.USER_CANCELED) {
      Log.w("onPurchasesUpdated", getResponseCodeString(responseCode));
      Toast
        .makeText(MainActivity.this, getString(R.string.cancel_to_buy), Toast.LENGTH_LONG)
        .show();
    }
    else {
      Log.e("onPurchasesUpdated", getResponseCodeString(responseCode));
      Toast
        .makeText(MainActivity.this, getString(R.string.error_occurred), Toast.LENGTH_LONG)
        .show();
    }
  }

  private void checkPurchaseState(Purchase purchase) {

    String purchaseState = handlePurchase(purchase);
    if("purchased".equals(purchaseState)) {
      Log.i(
        "checkPurchaseState",
        "Sku: " + purchase.getSku() + ", State: " + purchaseState
      );
      if(PRODUCT_ID_PREMIUM.equals(purchase.getSku())) {
        Toast
          .makeText(
            this,
            getString(R.string.succeed_to_upgrade),
            Toast.LENGTH_LONG
          )
          .show();
        setBooleanGeneralInSharedPreferences(IS_PREMIUM, true);
        if(expandableListViewFragment != null) {
          expandableListViewFragment.disableAdView();
        }
      }
    }
    else {
      Log.w(
        "checkPurchaseState",
        "Sku: " + purchase.getSku() + ", State: " + purchaseState
      );
    }
  }

  // 購入を承認する
  String handlePurchase(Purchase purchase) {

    String stateStr = "error";
    int purchaseState = purchase.getPurchaseState();
    if(purchaseState == Purchase.PurchaseState.PURCHASED) {
      stateStr = "purchased";
      // まだ購入が承認されていない場合は承認を行う
      if(!purchase.isAcknowledged()) {
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams
          .newBuilder()
          .setPurchaseToken(purchase.getPurchaseToken())
          .build();

        billingClient.acknowledgePurchase(
          acknowledgePurchaseParams,
          this
        );
      }
    }
    else if(purchaseState == Purchase.PurchaseState.PENDING) {
      stateStr = "pending";
    }
    else if(purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
      stateStr = "unspecified";
    }

    return stateStr;
  }

  @Override
  public void onAcknowledgePurchaseResponse(BillingResult billingResult) {

    int responseCode = billingResult.getResponseCode();
    if(responseCode != BillingResponseCode.OK) {
      Log.e("onAcknowledgePurchase", getResponseCodeString(responseCode));
    }
  }

  private void onBuyButtonClicked() {

    getSkuDetails(PRODUCT_ID_PREMIUM).onSuccess(new Continuation<SkuDetails, Void>() {
      @Override
      public Void then(Task<SkuDetails> task) {

        SkuDetails skuDetails = task.getResult();
        BillingFlowParams flowParams = BillingFlowParams
          .newBuilder()
          .setSkuDetails(skuDetails)
          .build();

        BillingResult billingResult =
          billingClient.launchBillingFlow(MainActivity.this, flowParams);

        int responseCode = billingResult.getResponseCode();
        if(responseCode == BillingResponseCode.OK) {
          Log.i("onBuyButtonClicked", getResponseCodeString(responseCode));
        }
        else {
          Log.e("onBuyButtonClicked", getResponseCodeString(responseCode));
        }

        return null;
      }
    });
  }

  @SuppressWarnings("SameParameterValue")
  private Task<SkuDetails> getSkuDetails(@NonNull final String sku) {

    // continueWith()もcontinueWithTask()もクライアントの渡したContinuationインスタンス内で定義される
    // then()は非同期で処理されるが、continueWith()と異なりcontinueWithTask()はthen()内でさらなる
    // 非同期処理が行われることを前提としている。例えば、非同期処理を行うメソッドとして、引数に文字列を受け取り、
    // 特定の時間停止した後、受け取った文字列をそのまま返す処理を別スレッドで行うasyncTask()を仮定すると、
    // asyncTask("foo").continueWith()として、then()内でreturn asyncTask(task.getResult() + "bar")
    // し、さらに.continueWith()を続けると、前のthen()内の処理であるasyncTask()内でsetResult()が呼び出され
    // ていない(処理が完了していない)限り、そのcontinueWith()内のthen()ではtask.getResult()の値が不定となる。
    // これはcontinueWith()の実装において、単にユーザの渡したContinuationインスタンスのthen()内の処理が
    // 完了することが次の.continue*()メソッドの処理を開始する条件となっているためである。一方
    // continueWithTask()では、Continuationインスタンスのthen()内に非同期処理を行うメソッドが存在する場合、
    // そのメソッドにTask APIを実装することで、そのメソッドが返すTaskが完了することが次の.continue*()メソッド
    // の処理を開始する条件となっているので、非同期で処理されるthen()内にさらに非同期処理が存在していても処理結果
    // の整合性を保つことができる。onSuccess()とonSuccessTask()も全く同じ関係であり、continueWith()は、
    // 前のTaskが完了していればそのTaskがfailed, canceled, succeededのいずれであってもthen()内の処理を行う
    // が、onSuccess()は、前のTaskがsucceededのときのみthen()内の処理を行う。

    final TaskCompletionSource<SkuDetails> taskCompletionSource =
      new TaskCompletionSource<>();
    if(skuDetailsList == null) {
      querySkuDetailsList().onSuccess(new Continuation<List<SkuDetails>, Void>() {
        @Override
        public Void then(Task<List<SkuDetails>> task) {

          skuDetailsList = task.getResult();
          getSkuDetailsFromListAndSetTaskCompletionSource(
            sku,
            skuDetailsList,
            taskCompletionSource
          );
          return null;
        }
      });
    }
    else {
      getSkuDetailsFromListAndSetTaskCompletionSource(sku, skuDetailsList, taskCompletionSource);
    }

    return taskCompletionSource.getTask();
  }

  private void getSkuDetailsFromListAndSetTaskCompletionSource(
    String sku,
    List<SkuDetails> skuDetailsList,
    TaskCompletionSource<SkuDetails> taskCompletionSource
  ) {

    for(SkuDetails skuDetails : skuDetailsList) {
      if(sku.equals(skuDetails.getSku())) {
        Log.i("getSkuDetailsFromList", sku + " found");
        taskCompletionSource.setResult(skuDetails);
        return;
      }
    }

    taskCompletionSource.setError(new IllegalStateException());
    Log.e("getSkuDetailsFromList", sku + " not found");
    Toast
      .makeText(
        this,
        getString(R.string.error_occurred),
        Toast.LENGTH_LONG
      )
      .show();
  }

  private Task<List<SkuDetails>> querySkuDetailsList() {

    final TaskCompletionSource<List<SkuDetails>> taskCompletionSource =
      new TaskCompletionSource<>();

    List<String> skuList = new ArrayList<>();
    skuList.add(PRODUCT_ID_PREMIUM);

    SkuDetailsParams params = SkuDetailsParams
      .newBuilder()
      .setSkusList(skuList)
      .setType(BillingClient.SkuType.INAPP)
      .build();

    billingClient.querySkuDetailsAsync(params, new SkuDetailsResponseListener() {

      @Override
      public void onSkuDetailsResponse(
        BillingResult billingResult, List<SkuDetails> list
      ) {

        int responseCode = billingResult.getResponseCode();
        if(responseCode == BillingResponseCode.OK) {
          if(list == null || list.isEmpty()) {
            Log.e("querySkuDetailsList", "list is null or empty");
            taskCompletionSource.setError(new IllegalStateException());
            Toast
              .makeText(
                MainActivity.this,
                getString(R.string.error_occurred),
                Toast.LENGTH_LONG
              )
              .show();
          }
          else {
            Log.i("querySkuDetailsList", getResponseCodeString(responseCode));
            taskCompletionSource.setResult(list);
          }
        }
        else if(responseCode == BillingResponseCode.USER_CANCELED) {
          Log.w("querySkuDetailsList", getResponseCodeString(responseCode));
          taskCompletionSource.setCancelled();
          Toast
            .makeText(
              MainActivity.this,
              getString(R.string.cancel_to_buy),
              Toast.LENGTH_LONG
            )
            .show();
        }
        else {
          Log.e("querySkuDetailsList", getResponseCodeString(responseCode));
          taskCompletionSource.setError(new IllegalStateException());
          Toast
            .makeText(
              MainActivity.this,
              getString(R.string.error_occurred),
              Toast.LENGTH_LONG
            )
            .show();
        }
      }
    });

    return taskCompletionSource.getTask();
  }

  private String getResponseCodeString(int responseCode) {

    switch(responseCode) {
      case BillingResponseCode.OK:
        return "OK";
      case BillingResponseCode.USER_CANCELED:
        return "USER_CANCELED";
      case BillingResponseCode.SERVICE_UNAVAILABLE:
        return "SERVICE_UNAVAILABLE";
      case BillingResponseCode.BILLING_UNAVAILABLE:
        return "BILLING_UNAVAILABLE";
      case BillingResponseCode.ITEM_UNAVAILABLE:
        return "ITEM_UNAVAILABLE";
      case BillingResponseCode.DEVELOPER_ERROR:
        return "DEVELOPER_ERROR";
      case BillingResponseCode.ERROR:
        return "ERROR";
      case BillingResponseCode.ITEM_ALREADY_OWNED:
        return "ITEM_ALREADY_OWNED";
      case BillingResponseCode.ITEM_NOT_OWNED:
        return "ITEM_NOT_OWNED";
      case BillingResponseCode.SERVICE_DISCONNECTED:
        return "SERVICE_DISCONNECTED";
      case BillingResponseCode.FEATURE_NOT_SUPPORTED:
        return "FEATURE_NOT_SUPPORTED";
      default:
        throw new IllegalArgumentException("Such a Response Code not exists!");
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {

    super.onNewIntent(intent);

    // テキストがACTION_SENDで送られてきたときに、詳細の項目にそのテキストを
    // セットした状態で編集画面を表示する
    detail = null;
    if(Intent.ACTION_SEND.equals(intent.getAction())) {
      Bundle extras = intent.getExtras();
      if(extras != null) {
        detail = extras.getString(Intent.EXTRA_TEXT);
      }
    }

    if(detail == null) {
      if(BOOT_FROM_NOTIFICATION.equals(intent.getAction())) {

        isBootFromNotification = true;
        setIntGeneralInSharedPreferences(MENU_POSITION, 0);
        setIntGeneralInSharedPreferences(SUBMENU_POSITION, 0);
        int size = menu.size();
        for(int i = 1; i < size; i++) {
          MenuItem menuItem = menu.getItem(i);
          if(menuItem.hasSubMenu()) {
            SubMenu subMenu = menuItem.getSubMenu();
            int subSize = subMenu.size();
            for(int j = 0; j < subSize; j++) {
              MenuItem subMenuItem = subMenu.getItem(j);
              subMenuItem.setChecked(false);
            }
          }
          menuItem.setChecked(false);
        }
      }

      showList();
      isInOnCreate = false;
    }
    else {

      List<NonScheduledListAdapter> nonScheduledListList =
        generalSettings.getNonScheduledLists();
      int size = nonScheduledListList.size();
      String[] items = new String[size + 1];
      items[0] = menu.findItem(R.id.scheduled_list).getTitle().toString();
      for(int i = 0; i < size; i++) {
        items[i + 1] = nonScheduledListList.get(i).getTitle();
      }

      final SingleChoiceItemsAdapter adapter = new SingleChoiceItemsAdapter(items);
      final AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(R.string.action_send_booted_dialog_title)
        .setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

          }
        })
        .setPositiveButton(R.string.determine, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            whichList = SingleChoiceItemsAdapter.checkedPosition;

            setIntGeneralInSharedPreferences(MENU_POSITION, whichList);
            setIntGeneralInSharedPreferences(SUBMENU_POSITION, 0);

            showList();
            isInOnCreate = false;
          }
        })
        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            detail = null;
            if(isInOnCreate) {
              showList();
            }
            isInOnCreate = false;
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {

            detail = null;
            if(isInOnCreate) {
              showList();
            }
            isInOnCreate = false;
          }
        })
        .create();

      dialog.setOnShowListener(new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialogInterface) {

          dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accentColor);
          dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(accentColor);
        }
      });

      dialog.show();
    }
  }

  private void showList() {

    // 前回開いていたNavigationDrawer上のメニューをリストアする
    try {
      menuItem = menu.getItem(whichMenuOpen);
    }
    catch(IndexOutOfBoundsException e) {
      menuItem = menu.getItem(0);
    }
    if(menuItem.hasSubMenu()) {
      menuItem = menuItem.getSubMenu().getItem(whichSubmenuOpen);
    }

    // 選択状態のリストア
    menuItem.setChecked(true);
    navigationView.setCheckedItem(menuItem);

    createAndSetFragmentColor();

    if(order == 0) {
      if(isExpandableTodo) {
        showExpandableListViewFragment();
      }
      else {
        showDoneListViewFragment();
      }
    }
    else if(order == 1) {
      if(generalSettings.getNonScheduledLists().get(whichMenuOpen - 1).isTodo()) {
        showListViewFragment();
      }
      else {
        showDoneListViewFragment();
      }
    }
    else if(order == 3) {
      showManageListViewFragment();
    }
    else if(order == 4) {
      showGeneralSettingsFragment();
    }
    else if(order == 5) {
      showHelpAndFeedbackFragment();
    }
  }

  @Override
  protected void onResume() {

    super.onResume();

    int currentNightMode =
      getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    if(isDarkThemeFollowSystem) {
      if(isDarkMode && currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
        setBooleanGeneralInSharedPreferences(IS_RECREATED, true);
        setBooleanGeneralInSharedPreferences(IS_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        recreate();
      }
      else if(!isDarkMode && currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
        setBooleanGeneralInSharedPreferences(IS_RECREATED, true);
        setBooleanGeneralInSharedPreferences(IS_DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        recreate();
      }
      else {
        setBooleanGeneralInSharedPreferences(IS_RECREATED, false);
      }
    }

    if(isFirstUse && !isRecreated) {
      setBooleanGeneralInSharedPreferences(IS_FIRST_USE, false);
    }


    // generalSettingsにおいて旧バージョンからの移行処理が行われた場合は、まだデータベース全体の
    // 移行処理が行われていないものとしてデータベースを更新する。
    // ダークモード切り替えによるActivityのrecreate()が発生した場合は処理が中断してしまうので
    // !isRecreatedの条件により、確実に1回だけ実行されるようにしてある。
    if(generalSettings.isCopiedFromOldVersion()) {
      setBooleanGeneralInSharedPreferences(IS_COPIED_FROM_OLD_VERSION, true);
      generalSettings.setIsCopiedFromOldVersion(false);
    }
    if(!isRecreated && isCopiedFromOldVersion) {
      CopyFromOldVersionProgressBarDialogFragment dialog =
        new CopyFromOldVersionProgressBarDialogFragment();
      dialog.show(getSupportFragmentManager(), "copy_from_old_version_progress_bar");
    }

    // すべての通知を既読し、通知チャネルを削除する
    clearAllNotification();

    if(!isPremium) {

      // ビリングサービスのセットアップ
      setupBillingServices();
    }

    Fragment mainFragment =
      getSupportFragmentManager().findFragmentByTag(ExpandableListViewFragment.TAG);
    if(mainFragment != null) {
      MyExpandableListAdapter.isLockBlockNotifyChange = true;
      MyExpandableListAdapter.isBlockNotifyChange = true;
      updateListTask(null, -1, true);
      setUpdateListTimerTask(true);
    }

    isScreenOn = true;
  }

  @Override
  protected void onPause() {

    super.onPause();

    if(updateInfoMessageDialog != null) {
      updateInfoMessageDialog.cancel();
      updateInfoMessageDialog = null;
    }

    // ExpandableListViewの自動更新を止める
    setUpdateListTimerTask(false);

    // DirectBootモードからMainActivityを起動すると何故かonResume()が呼ばれた後に一度onPause()が
    // 呼ばれ、再びonResume()が呼ばれるが、そうなっても良いようにonPause()側で端末暗号化ストレージ
    // へのコピー処理にgetIsDirectBootContext()を使って条件を設けている。
    if(!getIsDirectBootContext(this)) {
      // データベースを端末暗号化ストレージへコピーする
      copyDatabase(this, false);
      copySharedPreferences(this, false);
    }

    isScreenOn = false;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {

    return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {

    if(drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawer(GravityCompat.START);
    }
    else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {

    super.onPostCreate(savedInstanceState);
    drawerToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {

    super.onConfigurationChanged(newConfig);
    drawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

    if(menuItem.getItemId() != R.id.add_list && menuItem.getItemId() != R.id.share) {

      // 選択されたメニューアイテム以外のチェックを外す
      if(!menuItem.isChecked()) {

        int size = menu.size();
        for(int i = 0; i < size; i++) {

          if(menu.getItem(i) == menuItem) {
            setIntGeneralInSharedPreferences(MENU_POSITION, i);
          }

          if(menu.getItem(i).hasSubMenu()) {
            SubMenu subMenu = menu.getItem(i).getSubMenu();
            int subSize = subMenu.size();
            for(int j = 0; j < subSize; j++) {
              if(subMenu.getItem(j) == menuItem) {
                setIntGeneralInSharedPreferences(MENU_POSITION, i);
                setIntGeneralInSharedPreferences(SUBMENU_POSITION, j);
              }
              subMenu.getItem(j).setChecked(false);
            }
          }
          else {
            menu.getItem(i).setChecked(false);
          }
        }
        menuItem.setChecked(true);
        navigationView.setCheckedItem(menuItem);

        // 選択されたmenuItemに対応するフラグメントを表示
        this.menuItem = menuItem;
        createAndSetFragmentColor();
        switch(order) {
          case 0: {
            if(isExpandableTodo) {
              showExpandableListViewFragment();
            }
            else {
              showDoneListViewFragment();
            }
            break;
          }
          case 1: {
            if(generalSettings.getNonScheduledLists().get(whichMenuOpen - 1).isTodo()) {
              showListViewFragment();
            }
            else {
              showDoneListViewFragment();
            }
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
      else {
        drawerLayout.closeDrawer(GravityCompat.START);
      }
    }
    else if(menuItem.getItemId() == R.id.add_list) {
      // ダイアログに表示するEditTextの設定
      LinearLayout linearLayout = new LinearLayout(MainActivity.this);
      linearLayout.setOrientation(LinearLayout.VERTICAL);
      final EditText editText = new EditText(MainActivity.this);
      setCursorDrawableColor(this, editText);
      editText.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(
        accentColor,
        PorterDuff.Mode.SRC_IN
      ));
      editText.setHint(R.string.list_hint);
      editText.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ));
      linearLayout.addView(editText);
      int paddingPx = getPxFromDp(this, 20);
      linearLayout.setPadding(paddingPx, 0, paddingPx, 0);

      // 新しいリストの名前を設定するダイアログを表示
      final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
        .setTitle(R.string.add_list)
        .setView(linearLayout)
        .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            // GeneralSettingsとManageListAdapterへの反映
            String name = editText.getText().toString();
            if(name.equals("")) {
              name = getString(R.string.default_list);
            }
            generalSettings.addNonScheduledList(0, new NonScheduledListAdapter(name));
            List<NonScheduledListAdapter> nonScheduledListList =
              generalSettings.getNonScheduledLists();
            int size = nonScheduledListList.size();
            for(int i = 0; i < size; i++) {
              nonScheduledListList.get(i).setOrder(i);
            }
            ManageListAdapter.nonScheduledLists =
              new ArrayList<>(nonScheduledListList);
            manageListAdapter.notifyDataSetChanged();

            // 一旦reminder_listグループ内のアイテムをすべて消してから元に戻すことで新しく追加したリストの順番を追加した順に並び替える

            // デフォルトアイテムのリストア
            menu.removeGroup(R.id.reminder_list);
            menu.add(R.id.reminder_list, R.id.scheduled_list, 0, R.string.nav_scheduled_item)
              .setIcon(R.drawable.ic_time)
              .setCheckable(true);
            menu.add(R.id.reminder_list, R.id.add_list, 2, R.string.add_list)
              .setIcon(R.drawable.ic_add_24dp)
              .setCheckable(false);

            // 新しく追加したリストのリストア
            for(NonScheduledListAdapter list : nonScheduledListList) {
              Drawable drawable =
                ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_my_list_24dp);
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
                  ContextCompat.getColor(MainActivity.this, R.color.iconGray),
                  PorterDuff.Mode.SRC_IN
                ));
              }
              menu.add(R.id.reminder_list, generateUniqueId(), 1, list.getTitle())
                .setIcon(drawable)
                .setCheckable(true);
            }

            if(order != 0) {
              setIntGeneralInSharedPreferences(
                MENU_POSITION,
                whichMenuOpen + 1
              );
              MainActivity.this.menuItem = menu.getItem(whichMenuOpen);
            }
            navigationView.setCheckedItem(MainActivity.this.menuItem);

            // データベースへの反映
            updateSettingsDB();
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

          dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accentColor);
          dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(accentColor);
        }
      });

      dialog.show();

      // ダイアログ表示時にソフトキーボードを自動で表示
      editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {

          if(hasFocus) {
            Window dialogWindow = dialog.getWindow();
            requireNonNull(dialogWindow);

            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
          }
        }
      });
      editText.requestFocus();
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

  public void updateListTask(
    final ItemAdapter snackBarItem,
    final int groupPosition,
    boolean unlockNotify
  ) {

    boolean isUpdated = false;
    int groupsSize = MyExpandableListAdapter.groups.size();
    for(int groupCount = 0; groupCount < groupsSize; groupCount++) {

      int groupChanged = 0;
      List<ItemAdapter> itemList;
      try {
        itemList = MyExpandableListAdapter.children.get(groupCount);
      }
      catch(NullPointerException e) {
        continue;
      }

      int childrenSize = itemList.size();
      for(int childCount = 0; childCount < childrenSize; childCount++) {

        ItemAdapter item = itemList.get(childCount);
        Calendar now = Calendar.getInstance();
        Calendar tomorrow = (Calendar)now.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        int specDay = item.getDate().get(Calendar.DAY_OF_MONTH);
        long subTime = item.getDate().getTimeInMillis() - now.getTimeInMillis();
        long subDay = subTime / (24 * HOUR);

        if(subTime < 0) {
          if(groupCount != 0) {
            MyExpandableListAdapter.children.get(0).add(item);
            groupChanged |= (1 << childCount);
          }
        }
        else if(subDay < 1 && specDay == now.get(Calendar.DAY_OF_MONTH)) {
          if(groupCount != 1) {
            MyExpandableListAdapter.children.get(1).add(item);
            groupChanged |= (1 << childCount);
          }
        }
        else if(subDay < 2 && specDay == tomorrow.get(Calendar.DAY_OF_MONTH)) {
          if(groupCount != 2) {
            MyExpandableListAdapter.children.get(2).add(item);
            groupChanged |= (1 << childCount);
          }
        }
        else if(subDay < 8) {
          if(groupCount != 3) {
            MyExpandableListAdapter.children.get(3).add(item);
            groupChanged |= (1 << childCount);
          }
        }
        else {
          if(groupCount != 4) {
            MyExpandableListAdapter.children.get(4).add(item);
            groupChanged |= (1 << childCount);
          }
        }
      }

      int removeCount = 0;
      int binaryLength = Integer.toBinaryString(groupChanged).length();
      for(int i = 0; i < binaryLength; i++) {
        if((groupChanged & (1 << i)) != 0) {
          MyExpandableListAdapter.children.get(groupCount).remove(i - removeCount);
          removeCount++;
          isUpdated = true;
        }
      }
    }

    if(isUpdated) {
      updateExpandableParentGroups(true);
    }

    if(!MyExpandableListAdapter.isLockBlockNotifyChange || unlockNotify) {
      expandableListAdapter.notifyDataSetChanged();
      MyExpandableListAdapter.isBlockNotifyChange = false;
      MyExpandableListAdapter.isLockBlockNotifyChange = false;

      if(timer != null) {
        if(runnable != null) {
          handler.removeCallbacks(runnable);
        }
        else {
          runnable = new Runnable() {
            @Override
            public void run() {

              updateListTask(null, -1, false);
              runnable = null;
            }
          };
        }
        handler.postDelayed(runnable, 2 * MINUTE);
      }
    }

    if(snackBarItem != null) {
      View parentView = findViewById(android.R.id.content);
      requireNonNull(parentView);
      Snackbar.make(parentView, getResources().getString(R.string.complete), Snackbar.LENGTH_LONG)
        .addCallback(new Snackbar.Callback() {
          @Override
          public void onShown(Snackbar sb) {

            super.onShown(sb);
          }

          @Override
          public void onDismissed(Snackbar transientBottomBar, int event) {

            super.onDismissed(transientBottomBar, event);
            MyExpandableListAdapter.panelLockId = 0;
          }
        })
        .setAction(R.string.undo, new View.OnClickListener() {
          @Override
          public void onClick(View v) {

            MyExpandableListAdapter.isLockBlockNotifyChange = true;
            MyExpandableListAdapter.isBlockNotifyChange = true;

            if(MyExpandableListAdapter.isClosed) {
              MyExpandableListAdapter.hasPanel = snackBarItem.getId();
              MyExpandableListAdapter.isClosed = false;
            }
            if(
              snackBarItem.getDayRepeat().getWhichSet() != 0 ||
                snackBarItem.getMinuteRepeat().getWhichSet() != 0
            ) {
              snackBarItem.setAlarmStopped(snackBarItem.isOrgIsAlarmStopped());
              snackBarItem.setAlteredTime(snackBarItem.getOrgAlteredTime());
              if((snackBarItem.getMinuteRepeat().getWhichSet() & 1) != 0) {
                snackBarItem
                  .getMinuteRepeat()
                  .setCount(snackBarItem.getMinuteRepeat().getOrgCount2());
              }
              else if((snackBarItem.getMinuteRepeat().getWhichSet() & (1 << 1)) != 0) {
                snackBarItem
                  .getMinuteRepeat()
                  .setDuration(snackBarItem.getMinuteRepeat().getOrgDuration2());
              }

              Calendar now = Calendar.getInstance();
              if(now.get(Calendar.SECOND) >= 30) {
                now.add(Calendar.MINUTE, 1);
              }
              now.set(Calendar.SECOND, 0);
              now.set(Calendar.MILLISECOND, 0);

              if(
                snackBarItem.getOrgDate().getTimeInMillis() < now.getTimeInMillis() &&
                  snackBarItem.getAlteredTime() != 0
              ) {
                snackBarItem.getDate().setTimeInMillis(
                  now.getTimeInMillis() +
                    snackBarItem.getAlteredTime()
                );
              }
              else {
                snackBarItem.getDate().setTimeInMillis(
                  snackBarItem.getOrgDate().getTimeInMillis() +
                    snackBarItem.getAlteredTime()
                );
              }
              Collections.sort(
                MyExpandableListAdapter.children.get(groupPosition),
                SCHEDULED_ITEM_COMPARATOR
              );
              expandableListAdapter.notifyDataSetChanged();

              deleteAlarm(snackBarItem);
              if(!snackBarItem.isAlarmStopped()) {
                setAlarm(snackBarItem);
              }
              updateDB(snackBarItem, MyDatabaseHelper.TODO_TABLE);
            }
            else {
              Calendar now = Calendar.getInstance();
              if(now.get(Calendar.SECOND) >= 30) {
                now.add(Calendar.MINUTE, 1);
              }
              now.set(Calendar.SECOND, 0);
              now.set(Calendar.MILLISECOND, 0);

              if(
                snackBarItem.getOrgDate().getTimeInMillis() < now.getTimeInMillis() &&
                  snackBarItem.getAlteredTime() != 0
              ) {
                snackBarItem.getDate().setTimeInMillis(
                  now.getTimeInMillis() +
                    snackBarItem.getAlteredTime()
                );
              }
              else {
                snackBarItem.getDate().setTimeInMillis(
                  snackBarItem.getOrgDate().getTimeInMillis() +
                    snackBarItem.getAlteredTime()
                );
              }

              MyExpandableListAdapter.children.get(groupPosition).add(snackBarItem);
              for(List<ItemAdapter> itemList : MyExpandableListAdapter.children) {
                Collections.sort(itemList, SCHEDULED_ITEM_COMPARATOR);
              }
              expandableListAdapter.notifyDataSetChanged();

              if(!snackBarItem.isAlarmStopped()) {
                setAlarm(snackBarItem);
              }
              insertDB(snackBarItem, MyDatabaseHelper.TODO_TABLE);
              deleteDB(snackBarItem, MyDatabaseHelper.DONE_TABLE);
            }

            updateListTask(null, -1, true);
          }
        })
        .show();
    }
  }

  void updateExpandableParentGroups(boolean sort) {

    Calendar now = Calendar.getInstance();
    Calendar tomorrowCal = (Calendar)now.clone();
    tomorrowCal.add(Calendar.DAY_OF_MONTH, 1);
    CharSequence today;
    CharSequence tomorrow;
    if(LOCALE.equals(Locale.JAPAN)) {
      today = DateFormat.format(" - yyyy年M月d日(E)", now);
      tomorrow = DateFormat.format(" - yyyy年M月d日(E)", tomorrowCal);
    }
    else {
      today = DateFormat.format(" - yyyy/M/d (E)", now);
      tomorrow = DateFormat.format(" - yyyy/M/d (E)", tomorrowCal);
    }
    MyExpandableListAdapter.groups.set(1, getString(R.string.today) + today);
    MyExpandableListAdapter.groups.set(2, getString(R.string.tomorrow) + tomorrow);

    if(sort) {
      for(List<ItemAdapter> itemList : MyExpandableListAdapter.children) {
        Collections.sort(itemList, SCHEDULED_ITEM_COMPARATOR);
      }
    }
  }

  private class UpdateListTimerTask extends TimerTask {

    @Override
    public void run() {

      handler.post(new Runnable() {
        @Override
        public void run() {

          int second = Calendar.getInstance().get(Calendar.SECOND);
          if(!MyExpandableListAdapter.isBlockNotifyChange && second == 0) {
            updateListTask(null, -1, false);
          }
        }
      });
    }
  }

  public void setUpdateListTimerTask(boolean isSet) {

    if(timer != null && !isSet) {
      timer.cancel();
      timer = null;
    }
    else if(timer == null && isSet) {
      timer = new Timer();
      TimerTask timerTask = new UpdateListTimerTask();
      timer.schedule(timerTask, 0, 1000);
    }
  }

  public List<List<ItemAdapter>> getChildren(String table) {

    return getChildren(queryAllDB(table));
  }

  public List<List<ItemAdapter>> getChildren(List<ItemAdapter> itemList) {

    List<ItemAdapter> pastList = new ArrayList<>();
    List<ItemAdapter> todayList = new ArrayList<>();
    List<ItemAdapter> tomorrowList = new ArrayList<>();
    List<ItemAdapter> weekList = new ArrayList<>();
    List<ItemAdapter> futureList = new ArrayList<>();

    Calendar now = Calendar.getInstance();
    Calendar tomorrow = (Calendar)now.clone();
    tomorrow.add(Calendar.DAY_OF_MONTH, 1);

    for(ItemAdapter item : itemList) {

      if(item.getWhichListBelongs() == 0) {
        if(item.getNotifyInterval().getTime() == item.getNotifyInterval().getOrgTime()) {
          deleteAlarm(item);
        }
        if(!isAlarmSet(item) && !item.isAlarmStopped()) {
          setAlarm(item);
        }

        int specDay = item.getDate().get(Calendar.DAY_OF_MONTH);
        long subTime = item.getDate().getTimeInMillis() - now.getTimeInMillis();
        long subDay = subTime / (24 * HOUR);

        if(subTime < 0) {
          pastList.add(item);
        }
        else if(subDay < 1 && specDay == now.get(Calendar.DAY_OF_MONTH)) {
          todayList.add(item);
        }
        else if(subDay < 2 && specDay == tomorrow.get(Calendar.DAY_OF_MONTH)) {
          tomorrowList.add(item);
        }
        else if(subDay < 8) {
          weekList.add(item);
        }
        else {
          futureList.add(item);
        }
      }
    }

    List<List<ItemAdapter>> children = new ArrayList<>();
    children.add(pastList);
    children.add(todayList);
    children.add(tomorrowList);
    children.add(weekList);
    children.add(futureList);

    for(List<ItemAdapter> child : children) {
      Collections.sort(child, SCHEDULED_ITEM_COMPARATOR);
    }
    return children;
  }

  public void addChildren(ItemAdapter item, String table) {

    Calendar now = Calendar.getInstance();
    Calendar tomorrow = (Calendar)now.clone();
    tomorrow.add(Calendar.DAY_OF_MONTH, 1);

    int specDay = item.getDate().get(Calendar.DAY_OF_MONTH);
    long subTime = item.getDate().getTimeInMillis() - now.getTimeInMillis();
    long subDay = subTime / (24 * HOUR);

    if(subTime < 0) {
      MyExpandableListAdapter.children.get(0).add(item);
    }
    else if(subDay < 1 && specDay == now.get(Calendar.DAY_OF_MONTH)) {
      MyExpandableListAdapter.children.get(1).add(item);
    }
    else if(subDay < 2 && specDay == tomorrow.get(Calendar.DAY_OF_MONTH)) {
      MyExpandableListAdapter.children.get(2).add(item);
    }
    else if(subDay < 8) {
      MyExpandableListAdapter.children.get(3).add(item);
    }
    else {
      MyExpandableListAdapter.children.get(4).add(item);
    }

    for(List<ItemAdapter> itemList : MyExpandableListAdapter.children) {
      Collections.sort(itemList, SCHEDULED_ITEM_COMPARATOR);
    }
    expandableListAdapter.notifyDataSetChanged();
    insertDB(item, table);
  }

  public void setUpdatedItemPosition(long id) {

    List<List<ItemAdapter>> children = MyExpandableListAdapter.children;
    int count = 0;
    int iSize = MyExpandableListAdapter.groups.size();
    for(int i = 0; i < iSize && id != 0; i++) {
      if(MyExpandableListAdapter.displayGroups[i]) {
        List<ItemAdapter> itemList = children.get(i);
        int jSize = itemList.size();
        for(int j = 0; j < jSize; j++) {
          ItemAdapter item = itemList.get(j);
          if(item.getId() == id) {
            try {
              ExpandableListViewFragment.position =
                expandableListView.getFlatListPosition(ExpandableListView.getPackedPositionForChild(
                  count,
                  j
                ));
              ExpandableListViewFragment.offset = ExpandableListViewFragment.groupHeight;
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

  public List<ItemAdapter> getNonScheduledItem(String table) {

    if(whichMenuOpen > 0) {
      long listId = generalSettings.getNonScheduledLists().get(whichMenuOpen - 1).getId();
      Log.i("getNonScheduledItem", "listId: " + listId);
      List<ItemAdapter> itemList = new ArrayList<>();
      for(ItemAdapter item : queryAllDB(table)) {
        Log.i("getNonScheduledItem", "item.getWhichListBelongs(): " + item.getWhichListBelongs());
        if(item.getWhichListBelongs() == listId) {
          itemList.add(item);
        }
      }
      Collections.sort(itemList, NON_SCHEDULED_ITEM_COMPARATOR);

      return itemList;
    }
    else {
      return new ArrayList<>();
    }
  }

  public List<ItemAdapter> getDoneItem() {

    long listId = whichMenuOpen == 0 ? 0 : generalSettings.getNonScheduledLists().get(
      whichMenuOpen - 1).getId();
    List<ItemAdapter> itemList = new ArrayList<>();
    for(ItemAdapter item : queryAllDB(MyDatabaseHelper.DONE_TABLE)) {
      if(item.getWhichListBelongs() == listId) {
        itemList.add(item);
      }
    }
    Collections.sort(itemList, DONE_ITEM_COMPARATOR);

    return itemList;
  }

  private void initSettings() {

    generalSettings = new GeneralSettingsAdapter();

    // データベースを新たに作成する場合、基本的な一般設定を追加しておく

    // タグのデフォルト設定
    // タグなし
    TagAdapter tag = new TagAdapter(0);
    tag.setName(getString(R.string.none));
    tag.setOrder(0);
    generalSettings.addTag(tag);

    // 家事タグ
    tag = new TagAdapter(1);
    tag.setName(getString(R.string.home));
    tag.setPrimaryColor(Color.parseColor("#4caf50"));
    tag.setPrimaryLightColor(Color.parseColor("#80e27e"));
    tag.setPrimaryDarkColor(Color.parseColor("#087f23"));
    tag.setPrimaryTextColor(Color.parseColor("#000000"));
    tag.setColorOrderGroup(9);
    tag.setColorOrderChild(5);
    tag.setOrder(1);
    generalSettings.addTag(tag);

    // 仕事タグ
    tag = new TagAdapter(2);
    tag.setName(getString(R.string.work));
    tag.setPrimaryColor(Color.parseColor("#2196f3"));
    tag.setPrimaryLightColor(Color.parseColor("#6ec6ff"));
    tag.setPrimaryDarkColor(Color.parseColor("#0069c0"));
    tag.setPrimaryTextColor(Color.parseColor("#000000"));
    tag.setColorOrderGroup(5);
    tag.setColorOrderChild(5);
    tag.setOrder(2);
    generalSettings.addTag(tag);

    // ショッピングタグ
    tag = new TagAdapter(3);
    tag.setName(getString(R.string.shopping));
    tag.setPrimaryColor(Color.parseColor("#f44336"));
    tag.setPrimaryLightColor(Color.parseColor("#ff7961"));
    tag.setPrimaryDarkColor(Color.parseColor("#ba000d"));
    tag.setPrimaryTextColor(Color.parseColor("#000000"));
    tag.setColorOrderGroup(0);
    tag.setColorOrderChild(5);
    tag.setOrder(3);
    generalSettings.addTag(tag);


    // Itemのデフォルト設定
    ItemAdapter item = generalSettings.getItem();

    // NotifyInterval
    NotifyIntervalAdapter notifyInterval = new NotifyIntervalAdapter();
    notifyInterval.setHour(0);
    notifyInterval.setMinute(5);
    notifyInterval.setOrgTime(6);
    notifyInterval.setWhichSet(1);

    if(notifyInterval.getOrgTime() != 0) {
      String summary;
      if(LOCALE.equals(Locale.JAPAN)) {
        summary = getString(R.string.unless_complete_task);
        if(notifyInterval.getHour() != 0) {
          summary += getResources().getQuantityString(
            R.plurals.hour,
            notifyInterval.getHour(),
            notifyInterval.getHour()
          );
        }
        if(notifyInterval.getMinute() != 0) {
          summary += getResources().getQuantityString(
            R.plurals.minute,
            notifyInterval.getMinute(),
            notifyInterval.getMinute()
          );
        }
        summary += getString(R.string.per);
        if(notifyInterval.getOrgTime() == -1) {
          summary += getString(R.string.infinite_times_notify);
        }
        else {
          summary +=
            getResources().getQuantityString(R.plurals.times_notify, notifyInterval.getOrgTime(),
              notifyInterval.getOrgTime()
            );
        }
      }
      else {
        summary = "Notify every ";
        if(notifyInterval.getHour() != 0) {
          summary += getResources().getQuantityString(
            R.plurals.hour,
            notifyInterval.getHour(),
            notifyInterval.getHour()
          );
          if(!LOCALE.equals(Locale.JAPAN)) {
            summary += " ";
          }
        }
        if(notifyInterval.getMinute() != 0) {
          summary += getResources().getQuantityString(
            R.plurals.minute,
            notifyInterval.getMinute(),
            notifyInterval.getMinute()
          );
          if(!LOCALE.equals(Locale.JAPAN)) {
            summary += " ";
          }
        }
        if(notifyInterval.getOrgTime() != -1) {
          summary +=
            getResources().getQuantityString(R.plurals.times_notify, notifyInterval.getOrgTime(),
              notifyInterval.getOrgTime()
            ) + " ";
        }
        summary += getString(R.string.unless_complete_task);
      }

      notifyInterval.setLabel(summary);
    }
    else {
      notifyInterval.setLabel(getString(R.string.none));
    }
    item.setNotifyInterval(notifyInterval);

    // AlarmSound
    item.setSoundUri(DEFAULT_URI_SOUND.toString());

    insertSettingsDB();
  }

  // 受け取ったオブジェクトをシリアライズしてデータベースへ挿入
  public void insertDB(ItemAdapter item, String table) {

    accessor.executeInsert(item.getId(), serialize(item.getItem()), table);
  }

  public void updateDB(ItemAdapter item, String table) {

    accessor.executeUpdate(item.getId(), serialize(item.getItem()), table);
  }

  public void deleteDB(ItemAdapter item, String table) {

    accessor.executeDelete(item.getId(), table);
  }

  // 指定されたテーブルからオブジェクトのバイト列をすべて取り出し、デシリアライズしてオブジェクトのリストで返す。
  public List<ItemAdapter> queryAllDB(String table) {

    List<ItemAdapter> itemList = new ArrayList<>();

    List<byte[]> streamList = null;
    do {
      try {
        streamList = accessor.executeQueryAll(table);
      }
      catch(SQLiteCantOpenDatabaseException e) {
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ex) {
          Log.e("queryAllDB", Log.getStackTraceString(ex));
        }
      }
    }
    while(getIsDirectBootContext(this) && streamList == null);

    if(streamList != null) {
      for(byte[] stream : streamList) {
        itemList.add(new ItemAdapter(deserialize(stream)));
      }
    }

    return itemList;
  }

  public boolean isItemExists(ItemAdapter item, String table) {

    return accessor.executeQueryById(item.getId(), table) != null;
  }

  public void insertSettingsDB() {

    accessor.executeInsert(
      1,
      serialize(generalSettings.getGeneralSettings()),
      MyDatabaseHelper.SETTINGS_TABLE
    );
  }

  public void updateSettingsDB() {

    accessor.executeUpdate(
      1,
      serialize(generalSettings.getGeneralSettings()),
      MyDatabaseHelper.SETTINGS_TABLE
    );
  }

  public GeneralSettingsAdapter querySettingsDB() {

    Object generalSettings = null;
    do {
      try {
        generalSettings = deserialize(
          accessor.executeQueryById(1, MyDatabaseHelper.SETTINGS_TABLE)
        );
      }
      catch(SQLiteCantOpenDatabaseException e) {
        try {
          Thread.sleep(10);
        }
        catch(InterruptedException ex) {
          Log.e("querySettingsDB", Log.getStackTraceString(ex));
        }
      }
    }
    while(getIsDirectBootContext(this) && generalSettings == null);

    return generalSettings == null? null : new GeneralSettingsAdapter(generalSettings);
  }

  @SuppressWarnings("MethodParameterNamingConvention")
  public void setIntGeneralInSharedPreferences(String TAG, int value) {

    switch(TAG) {
      case DEFAULT_MINUS_TIME_1_HOUR: {
        minusTime1Hour = value;
        break;
      }
      case DEFAULT_MINUS_TIME_1_MINUTE: {
        minusTime1Minute = value;
        break;
      }
      case DEFAULT_MINUS_TIME_2_HOUR: {
        minusTime2Hour = value;
        break;
      }
      case DEFAULT_MINUS_TIME_2_MINUTE: {
        minusTime2Minute = value;
        break;
      }
      case DEFAULT_MINUS_TIME_3_HOUR: {
        minusTime3Hour = value;
        break;
      }
      case DEFAULT_MINUS_TIME_3_MINUTE: {
        minusTime3Minute = value;
        break;
      }
      case DEFAULT_PLUS_TIME_1_HOUR: {
        plusTime1Hour = value;
        break;
      }
      case DEFAULT_PLUS_TIME_1_MINUTE: {
        plusTime1Minute = value;
        break;
      }
      case DEFAULT_PLUS_TIME_2_HOUR: {
        plusTime2Hour = value;
        break;
      }
      case DEFAULT_PLUS_TIME_2_MINUTE: {
        plusTime2Minute = value;
        break;
      }
      case DEFAULT_PLUS_TIME_3_HOUR: {
        plusTime3Hour = value;
        break;
      }
      case DEFAULT_PLUS_TIME_3_MINUTE: {
        plusTime3Minute = value;
        break;
      }
      case DEFAULT_TEXT_SIZE: {
        whichTextSize = value;
        textSize = 18 + 2 * value;
        break;
      }
      case SNOOZE_DEFAULT_HOUR: {
        snoozeDefaultHour = value;
        break;
      }
      case SNOOZE_DEFAULT_MINUTE: {
        snoozeDefaultMinute = value;
        break;
      }
      case MENU_POSITION: {
        whichMenuOpen = value;
        break;
      }
      case SUBMENU_POSITION: {
        whichSubmenuOpen = value;
        break;
      }
      default: {
        throw new IllegalArgumentException(TAG);
      }
    }

    getDynamicContext(this)
      .getSharedPreferences(
        getIsDirectBootContext(this) ? INT_GENERAL_COPY : INT_GENERAL,
        Context.MODE_PRIVATE
      )
      .edit()
      .putInt(TAG, value)
      .apply();
  }

  @SuppressWarnings("MethodParameterNamingConvention")
  public void setBooleanGeneralInSharedPreferences(String TAG, boolean value) {

    switch(TAG) {
      case IS_COPIED_FROM_OLD_VERSION: {
        isCopiedFromOldVersion = value;
        break;
      }
      case IS_QUERIED_PURCHASE_HISTORY: {
        isQueriedPurchaseHistory = value;
        break;
      }
      case IS_RECREATED: {
        isRecreated = value;
        break;
      }
      case IS_DARK_MODE: {
        isDarkMode = value;
        break;
      }
      case IS_DARK_THEME_FOLLOW_SYSTEM: {
        isDarkThemeFollowSystem = value;
        break;
      }
      case PLAY_SLIDE_ANIMATION: {
        isPlaySlideAnimation = value;
        break;
      }
      case IS_EXPANDABLE_TODO: {
        isExpandableTodo = value;
        break;
      }
      case IS_PREMIUM: {
        isPremium = value;
        break;
      }
      default: {
        if(TAG.equals(IS_FIRST_USE)) {
          isFirstUse = value;
          break;
        }
        else {
          throw new IllegalArgumentException(TAG);
        }
      }
    }

    getDynamicContext(this)
      .getSharedPreferences(
        getIsDirectBootContext(this) ? BOOLEAN_GENERAL_COPY : BOOLEAN_GENERAL,
        Context.MODE_PRIVATE
      )
      .edit()
      .putBoolean(TAG, value)
      .apply();
  }

  public void setAlarm(ItemAdapter item) {

    if(
      item.getDate().getTimeInMillis() > System.currentTimeMillis() &&
        item.getWhichListBelongs() == 0
    ) {
      item.getNotifyInterval().setTime(item.getNotifyInterval().getOrgTime());
      Intent intent = new Intent(this, AlarmReceiver.class);
      byte[] obArray = serialize(item.getItem());
      intent.putExtra(ITEM, obArray);
      PendingIntent sender = PendingIntent.getBroadcast(
        this, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
      requireNonNull(alarmManager);

      alarmManager.setAlarmClock(
        new AlarmManager.AlarmClockInfo(item.getDate().getTimeInMillis(), null), sender);
    }
  }

  public void deleteAlarm(ItemAdapter item) {

    if(isAlarmSet(item)) {
      Intent intent = new Intent(this, AlarmReceiver.class);
      PendingIntent sender = PendingIntent.getBroadcast(
        this, (int)item.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

      AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
      requireNonNull(alarmManager);

      alarmManager.cancel(sender);
      sender.cancel();
    }
  }

  public boolean isAlarmSet(ItemAdapter item) {

    Intent intent = new Intent(this, AlarmReceiver.class);
    PendingIntent sender = PendingIntent.getBroadcast(
      this, (int)item.getId(), intent, PendingIntent.FLAG_NO_CREATE);

    return sender != null;
  }

  String getControlTimeText(boolean isMinus, int which) {

    String controlTimeText = "";
    int hour;
    int minute;
    switch(which) {
      case 1: {
        hour = isMinus ? minusTime1Hour : plusTime1Hour;
        minute = isMinus ? minusTime1Minute : plusTime1Minute;
        break;
      }
      case 2: {
        hour = isMinus ? minusTime2Hour : plusTime2Hour;
        minute = isMinus ? minusTime2Minute : plusTime2Minute;
        break;
      }
      case 3: {
        hour = isMinus ? minusTime3Hour : plusTime3Hour;
        minute = isMinus ? minusTime3Minute : plusTime3Minute;
        break;
      }
      default: {
        throw new IllegalStateException("Such a control num not exists! : " + which);
      }
    }
    if(hour != 0) {
      controlTimeText += hour + getString(R.string.control_time_hour);
    }
    if(minute != 0) {
      controlTimeText += minute + getString(R.string.control_time_minute);
    }
    if(hour == 0 && minute == 0) {
      controlTimeText = isMinus ? getString(R.string.minus1d) : getString(R.string.plus1d);
    }
    else {
      controlTimeText = isMinus ? "-" + controlTimeText : "+" + controlTimeText;
    }

    return controlTimeText;
  }

  void clearAllNotification() {

    NotificationManager manager =
      (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    requireNonNull(manager);
    manager.cancelAll();

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      List<NotificationChannel> channelList = manager.getNotificationChannels();
      if(channelList != null) {
        int size = channelList.size();
        for(int i = 0; i < size; i++) {
          manager.deleteNotificationChannel(channelList.get(i).getId());
        }
      }
    }
  }

  private void createAndSetFragmentColor() {

    // 開いているFragmentに応じた色を作成
    order = menuItem.getOrder();
    if(order == 1) {
      NonScheduledListAdapter list = generalSettings.getNonScheduledLists().get(whichMenuOpen - 1);
      if(list.getColor() == 0) {
        setDefaultColor();
      }
      else {
        menuItemColor = list.getTextColor();
        menuBackgroundColor = list.getColor();
        statusBarColor = list.getDarkColor();
        list.setIsColorPrimary(false);
        if(list.getColor() == 0) {
          accentColor = ContextCompat.getColor(this, R.color.colorAccent);
          secondaryTextColor = ContextCompat.getColor(this, android.R.color.black);
        }
        else {
          accentColor = list.getColor();
          secondaryTextColor = list.getTextColor();
        }
        list.setIsColorPrimary(true);
      }
    }
    else {
      setDefaultColor();
    }

    if(isDarkMode) {
      primaryMaterialDarkColor =
        ContextCompat.getColor(this, R.color.primaryMaterialDark);
      primaryDarkMaterialDarkColor =
        ContextCompat.getColor(this, R.color.primaryDarkMaterialDark);
      backgroundMaterialDarkColor =
        ContextCompat.getColor(this, R.color.backgroundMaterialDark);
      backgroundFloatingMaterialDarkColor =
        ContextCompat.getColor(this, R.color.backgroundFloatingMaterialDark);
      primaryTextMaterialDarkColor =
        ContextCompat.getColor(this, R.color.primaryTextMaterialDark);
      secondaryTextMaterialDarkColor =
        ContextCompat.getColor(this, R.color.secondaryTextMaterialDark);
      menuItemColor = primaryTextMaterialDarkColor;
      menuBackgroundColor = primaryMaterialDarkColor;
      statusBarColor = primaryDarkMaterialDarkColor;
    }

    // ハンバーガーアイコンの色を指定
    drawerToggle.getDrawerArrowDrawable().setColor(menuItemColor);
    // DisplayHomeAsUpEnabledに指定する戻るボタンの色を指定
    upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material);
    requireNonNull(upArrow);
    upArrow.setColorFilter(new PorterDuffColorFilter(
      menuItemColor,
      PorterDuff.Mode.SRC_IN
    ));
    // ツールバーとステータスバーの色を指定
    toolbar.setTitleTextColor(menuItemColor);
    toolbar.setBackgroundColor(menuBackgroundColor);
    Window window = getWindow();
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    window.setStatusBarColor(statusBarColor);
  }

  private void setDefaultColor() {

    MyThemeAdapter theme = generalSettings.getTheme();
    if(theme.getColor() == 0) {
      menuItemColor = Color.WHITE;
      menuBackgroundColor = ContextCompat.getColor(this, R.color.colorPrimary);
      statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark);
      theme.setIsColorPrimary(false);
      if(theme.getColor() == 0) {
        accentColor = ContextCompat.getColor(this, R.color.colorAccent);
        secondaryTextColor = ContextCompat.getColor(this, android.R.color.black);
      }
      else {
        accentColor = theme.getColor();
        secondaryTextColor = theme.getTextColor();
      }
      theme.setIsColorPrimary(true);
    }
    else {
      menuItemColor = theme.getTextColor();
      menuBackgroundColor = theme.getColor();
      statusBarColor = theme.getDarkColor();
      theme.setIsColorPrimary(false);
      if(theme.getColor() == 0) {
        accentColor = ContextCompat.getColor(this, R.color.colorAccent);
        secondaryTextColor = ContextCompat.getColor(this, android.R.color.black);
      }
      else {
        accentColor = theme.getColor();
        secondaryTextColor = theme.getTextColor();
      }
      theme.setIsColorPrimary(true);
    }
  }

  public void showDoneListViewFragment() {

    actionBarFragment = ActionBarFragment.newInstance();
    showFragment(DoneListViewFragment.TAG, DoneListViewFragment.newInstance(),
      ActionBarFragment.TAG, actionBarFragment, false
    );
  }

  public void showTagEditListViewFragment() {

    showFragment(TagEditListViewFragment.TAG, TagEditListViewFragment.newInstance(),
      null, null, true
    );
  }

  public void showColorPickerListViewFragment() {

    showFragment(ColorPickerListViewFragment.TAG, ColorPickerListViewFragment.newInstance(),
      null, null, true
    );
  }

  public void showManageListViewFragment() {

    actionBarFragment = ActionBarFragment.newInstance();
    showFragment(ManageListViewFragment.TAG, ManageListViewFragment.newInstance(),
      ActionBarFragment.TAG, actionBarFragment, false
    );
  }

  public void showListViewFragment() {

    actionBarFragment = ActionBarFragment.newInstance();
    showFragment(ListViewFragment.TAG, ListViewFragment.newInstance(),
      ActionBarFragment.TAG, actionBarFragment, false
    );
  }

  public void showExpandableListViewFragment() {

    actionBarFragment = ActionBarFragment.newInstance();
    expandableListViewFragment = ExpandableListViewFragment.newInstance();
    showFragment(ExpandableListViewFragment.TAG, expandableListViewFragment,
      ActionBarFragment.TAG, actionBarFragment, false
    );
  }

  // 編集画面を表示(引数にitemを渡すとそのitemの情報が入力された状態で表示)
  public void showMainEditFragment() {

    MainEditFragment.isMainPopping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstance(),
      null, null, true
    );
  }

  public void showMainEditFragment(String detail) {

    MainEditFragment.isMainPopping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstance(detail),
      null, null, true
    );
  }

  public void showMainEditFragment(ItemAdapter item) {

    MainEditFragment.isMainPopping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstance(item.clone()),
      null, null, true
    );
  }

  public void showMainEditFragmentForList() {

    MainEditFragment.isMainPopping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstanceForList(),
      null, null, true
    );
  }

  public void showMainEditFragmentForList(NonScheduledListAdapter list) {

    MainEditFragment.isMainPopping = false;
    showFragment(MainEditFragment.TAG, MainEditFragment.newInstanceForList(list.clone()),
      null, null, true
    );
  }

  public void showNotesFragment(ItemAdapter item) {

    Fragment nextFragment;
    String nextFragmentTAG;
    if(item.isChecklistMode()) {
      nextFragment = NotesChecklistModeFragment.newInstance(item);
      nextFragmentTAG = NotesChecklistModeFragment.TAG;
    }
    else {
      nextFragment = NotesEditModeFragment.newInstance(item);
      nextFragmentTAG = NotesEditModeFragment.TAG;
    }

    MainEditFragment.isNotesPopping = false;
    showFragment(nextFragmentTAG, nextFragment, null, null, true);
  }

  public void showGeneralSettingsFragment() {

    showFragment(GeneralSettingsFragment.TAG, GeneralSettingsFragment.newInstance(),
      null, null, false
    );
  }

  public void showHelpAndFeedbackFragment() {

    showFragment(HelpAndFeedbackFragment.TAG, HelpAndFeedbackFragment.newInstance(),
      null, null, false
    );
  }

  public void showAboutThisAppFragment() {

    showFragment(AboutThisAppFragment.TAG, AboutThisAppFragment.newInstance(),
      null, null, true
    );
  }

  private void commitFragment(
    Fragment remove1, Fragment remove2, String add1, Fragment addFragment1,
    String add2, Fragment addFragment2, boolean backStack
  ) {

    Transition transition = new Fade()
      .setDuration(300);
    FragmentManager manager = getSupportFragmentManager();

    FragmentTransaction transaction = manager.beginTransaction();
    if(remove1 != null) {
      remove1.setExitTransition(transition);
      transaction.remove(remove1);
    }
    if(remove2 != null) {
      remove2.setExitTransition(transition);
      transaction.remove(remove2);
    }
    if(add1 != null) {
      addFragment1.setEnterTransition(transition);
      transaction.add(R.id.content, addFragment1, add1);
    }
    if(add2 != null) {
      addFragment2.setEnterTransition(transition);
      transaction.add(R.id.content, addFragment2, add2);
    }
    if(backStack) {
      transaction.addToBackStack(null);
    }
    transaction.commit();
  }

  private void showFragment(
    String add1,
    Fragment addFragment1,
    String add2,
    Fragment addFragment2,
    boolean backStack
  ) {

    FragmentManager manager = getSupportFragmentManager();
    Fragment rmFragment = manager.findFragmentById(R.id.content);

    if(rmFragment == null) {
      commitFragment(null, null, add1, addFragment1, add2, addFragment2, backStack);
    }
    else if(rmFragment instanceof ActionBarFragment) {

      String[] mainTAGs = {
        ExpandableListViewFragment.TAG, ListViewFragment.TAG,
        ManageListViewFragment.TAG, DoneListViewFragment.TAG
      };
      boolean isFoundMatchFragment = false;
      for(String mainTAG : mainTAGs) {
        Fragment mainFragment = manager.findFragmentByTag(mainTAG);
        if(mainFragment != null && mainFragment.isVisible()) {
          isFoundMatchFragment = true;
          commitFragment(
            mainFragment,
            rmFragment,
            add1,
            addFragment1,
            add2,
            addFragment2,
            backStack
          );
          break;
        }
      }
      if(!isFoundMatchFragment) {
        commitFragment(rmFragment, null, add1, addFragment1, add2, addFragment2, backStack);
      }
    }
    else if(rmFragment instanceof ExpandableListViewFragment ||
      rmFragment instanceof ListViewFragment
      || rmFragment instanceof ManageListViewFragment ||
      rmFragment instanceof DoneListViewFragment) {

      commitFragment(rmFragment, manager.findFragmentByTag(ActionBarFragment.TAG),
        add1, addFragment1, add2, addFragment2, backStack
      );
    }
    else {
      commitFragment(rmFragment, null, add1, addFragment1, add2, addFragment2, backStack);
    }
  }
}