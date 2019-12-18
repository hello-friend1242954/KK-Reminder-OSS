package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static com.hideaki.kk_reminder.UtilClass.setCursorDrawableColor;
import static java.util.Objects.requireNonNull;

public class TagEditListViewFragment extends Fragment implements View.OnClickListener {

  static final String TAG = TagEditListViewFragment.class.getSimpleName();
  private MainActivity activity;
  private ActionBar actionBar;
  private MenuItem editItem;
  private MenuItem sortItem;
  private View footer;

  public static TagEditListViewFragment newInstance() {

    return new TagEditListViewFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
    if(activity.generalSettings != null) {
      int order = activity.order;
      TagEditListAdapter.order = order;
      if(order == 0 || order == 1 || order == 4) {
        TagEditListAdapter.checked_item_id = MainEditFragment.item.getWhich_tag_belongs();
      }
      else if(order == 3) {
        TagEditListAdapter.checked_item_id = MainEditFragment.list.getWhich_tag_belongs();
      }
    }
    else {
      FragmentManager manager = getFragmentManager();
      requireNonNull(manager);
      manager
        .beginTransaction()
        .remove(this)
        .commit();
    }
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    this.setHasOptionsMenu(true);
  }

  @Nullable
  @Override
  public View onCreateView(
    @NonNull LayoutInflater inflater,
    @Nullable ViewGroup container,
    Bundle savedInstanceState
  ) {

    View view = inflater.inflate(R.layout.listview, container, false);
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
          ColorPickerListAdapter.from_list_tag_edit = false;
          if(TagEditListAdapter.is_editing) {
            new AlertDialog.Builder(activity)
              .setTitle(R.string.is_editing_title)
              .setMessage(R.string.is_editing_message)
              .show();

            return true;
          }
          else if(TagEditListAdapter.is_sorting) {
            new AlertDialog.Builder(activity)
              .setTitle(R.string.is_sorting_title)
              .setMessage(R.string.is_sorting_message)
              .show();

            return true;
          }
        }

        return false;
      }
    });

    TagEditListAdapter.is_first = true;
    activity.listView = view.findViewById(R.id.listView);
    activity.listView.setDragListener(activity.tagEditListAdapter.dragListener);
    activity.listView.setSortable(true);
    footer = View.inflate(activity, R.layout.tag_list_footer, null);
    if(activity.listView.getFooterViewsCount() == 0 && !TagEditListAdapter.is_editing
      && !TagEditListAdapter.is_sorting) {
      activity.listView.addFooterView(footer);
    }
    ConstraintLayout addTagItem = footer.findViewById(R.id.add_tag_item);
    addTagItem.setOnClickListener(this);
    activity.listView.setAdapter(activity.tagEditListAdapter);

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    actionBar = activity.getSupportActionBar();
    requireNonNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    if(TagEditListAdapter.is_editing) {
      actionBar.setDisplayHomeAsUpEnabled(false);
    }
    else {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
    actionBar.setTitle(R.string.tag);

    AdView adView = view.findViewById(R.id.adView);
    adView.setVisibility(View.GONE);

    return view;
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.tag_edit_menu, menu);

    // 編集メニューの実装
    editItem = menu.findItem(R.id.edit);
    Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.ic_edit_24dp);
    requireNonNull(drawable);
    drawable = drawable.mutate();
    drawable.setColorFilter(new PorterDuffColorFilter(
      activity.menu_item_color,
      PorterDuff.Mode.SRC_IN
    ));
    editItem.setIcon(drawable);

    // ソートメニューの実装
    sortItem = menu.findItem(R.id.sort);
    drawable = ContextCompat.getDrawable(activity, R.drawable.ic_sort_24dp);
    requireNonNull(drawable);
    drawable = drawable.mutate();
    drawable.setColorFilter(new PorterDuffColorFilter(
      activity.menu_item_color,
      PorterDuff.Mode.SRC_IN
    ));
    sortItem.setIcon(drawable);
    if(TagEditListAdapter.is_editing) {
      sortItem.setVisible(false);
    }
    else {
      sortItem.setVisible(true);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch(item.getItemId()) {
      case R.id.edit: {

        TagEditListAdapter.is_editing = !TagEditListAdapter.is_editing;
        if(TagEditListAdapter.is_editing) {
          activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
          actionBar.setDisplayHomeAsUpEnabled(false);
          sortItem.setVisible(false);
          activity.listView.removeFooterView(footer);
        }
        else {
          activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
          actionBar.setDisplayHomeAsUpEnabled(true);
          sortItem.setVisible(true);
          if(activity.listView.getFooterViewsCount() == 0) {
            activity.listView.addFooterView(footer);
          }
        }
        activity.tagEditListAdapter.notifyDataSetChanged();
        return true;
      }
      case R.id.sort: {

        TagEditListAdapter.is_sorting = !TagEditListAdapter.is_sorting;
        if(TagEditListAdapter.is_sorting) {
          activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
          actionBar.setDisplayHomeAsUpEnabled(false);
          editItem.setVisible(false);
          activity.listView.removeFooterView(footer);
        }
        else {
          activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
          actionBar.setDisplayHomeAsUpEnabled(true);
          editItem.setVisible(true);
          if(activity.listView.getFooterViewsCount() == 0) {
            activity.listView.addFooterView(footer);
          }

          int size = TagEditListAdapter.tagList.size();
          boolean is_updated = false;
          for(int i = 0; i < size; i++) {
            Tag tag = TagEditListAdapter.tagList.get(i);
            if(tag.getOrder() != i) {
              tag.setOrder(i);
              is_updated = true;
            }
          }

          if(is_updated) {
            activity.generalSettings.setTagList(new ArrayList<>(TagEditListAdapter.tagList));
            activity.updateSettingsDB();
          }
        }

        return true;
      }
      case android.R.id.home: {

        ColorPickerListAdapter.from_list_tag_edit = false;
        FragmentManager manager = getFragmentManager();
        requireNonNull(manager);
        manager.popBackStack();
        return true;
      }
      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  @Override
  public void onClick(View v) {

    // ダイアログに表示するEditTextの設定
    LinearLayout linearLayout = new LinearLayout(activity);
    linearLayout.setOrientation(LinearLayout.VERTICAL);
    final EditText editText = new EditText(activity);
    setCursorDrawableColor(activity, editText);
    editText.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(
      activity.accent_color,
      PorterDuff.Mode.SRC_IN
    ));
    editText.setHint(R.string.tag_hint);
    editText.setLayoutParams(new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    ));
    linearLayout.addView(editText);
    int paddingPx = getPxFromDp(activity, 20);
    linearLayout.setPadding(paddingPx, 0, paddingPx, 0);

    final AlertDialog dialog = new AlertDialog.Builder(activity)
      .setTitle(R.string.add_tag)
      .setView(linearLayout)
      .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {

          String name = editText.getText().toString();
          if(name.equals("")) {
            name = getString(R.string.default_tag);
          }
          Tag tag = new Tag();
          tag.setName(name);
          activity.generalSettings.getTagList().add(1, tag);
          int size = activity.generalSettings.getTagList().size();
          for(int i = 0; i < size; i++) {
            activity.generalSettings.getTagList().get(i).setOrder(i);
          }
          TagEditListAdapter.tagList = new ArrayList<>(activity.generalSettings.getTagList());
          activity.tagEditListAdapter.notifyDataSetChanged();
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
}
