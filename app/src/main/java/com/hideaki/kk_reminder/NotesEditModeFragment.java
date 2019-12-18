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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.LINE_SEPARATOR;
import static com.hideaki.kk_reminder.UtilClass.setCursorDrawableColor;
import static java.util.Objects.requireNonNull;

public class NotesEditModeFragment extends Fragment {

  static final String TAG = NotesEditModeFragment.class.getSimpleName();
  private static Item item = null;
  private static boolean is_editing;
  private MainActivity activity;
  private MenuItem doneItem;
  private MenuItem checklistModeItem;
  private EditText memo;
  private ActionBar actionBar;
  private MenuItem clearNotesItem;
  private String notes;

  public static NotesEditModeFragment newInstance(Item item) {

    NotesEditModeFragment fragment = new NotesEditModeFragment();

    Bundle args = new Bundle();
    args.putSerializable(ITEM, item);
    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    this.setHasOptionsMenu(true);

    Bundle args = getArguments();
    requireNonNull(args);
    item = (Item)args.getSerializable(ITEM);
  }

  @Override
  public View onCreateView(
    @NonNull LayoutInflater inflater,
    ViewGroup container,
    Bundle savedInstanceState
  ) {

    if(MainEditFragment.is_notes_popping) {
      FragmentManager manager = getFragmentManager();
      requireNonNull(manager);
      manager.popBackStack();
    }

    View view = inflater.inflate(R.layout.notes_edit_layout, container, false);
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

          if(is_editing) {
            new AlertDialog.Builder(activity)
              .setTitle(R.string.is_editing_title)
              .setMessage(R.string.is_editing_message)
              .show();

            return true;
          }

          MainEditFragment.is_notes_popping = true;
        }

        return false;
      }
    });

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    actionBar = activity.getSupportActionBar();
    requireNonNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.notes);

    memo = view.findViewById(R.id.notes);
    setCursorDrawableColor(activity, memo);
    memo.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(
      activity.accent_color,
      PorterDuff.Mode.SRC_IN
    ));
    memo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {

        if(hasFocus) {
          is_editing = true;
          Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.ic_cancel_24dp);
          requireNonNull(drawable);
          drawable = drawable.mutate();
          drawable.setColorFilter(new PorterDuffColorFilter(
            activity.menu_item_color,
            PorterDuff.Mode.SRC_IN
          ));
          actionBar.setHomeAsUpIndicator(drawable);

          clearNotesItem.setVisible(false);
          doneItem.setVisible(true);
          checklistModeItem.setVisible(false);
        }
        else {
          InputMethodManager manager =
            (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
          requireNonNull(manager);
          manager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
      }
    });
    StringBuilder stringBuilder = new StringBuilder();
    for(Notes notes : item.getNotesList()) {
      if(notes.isChecked()) {
        stringBuilder.append(notes.getString()).append(" *").append(LINE_SEPARATOR);
      }
      else {
        stringBuilder.append(notes.getString()).append(LINE_SEPARATOR);
      }
    }
    memo.setText(stringBuilder.toString());
    notes = stringBuilder.toString();

    return view;
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.notes_edit_mode_menu, menu);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {

    super.onPrepareOptionsMenu(menu);

    checklistModeItem = menu.findItem(R.id.checklist_mode);
    Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.ic_list_24dp);
    requireNonNull(drawable);
    drawable = drawable.mutate();
    drawable.setColorFilter(new PorterDuffColorFilter(
      activity.menu_item_color,
      PorterDuff.Mode.SRC_IN
    ));
    checklistModeItem.setIcon(drawable);

    doneItem = menu.findItem(R.id.done);
    drawable = ContextCompat.getDrawable(activity, R.drawable.ic_check_circle_24dp);
    requireNonNull(drawable);
    drawable = drawable.mutate();
    drawable.setColorFilter(new PorterDuffColorFilter(
      activity.menu_item_color,
      PorterDuff.Mode.SRC_IN
    ));
    doneItem.setIcon(drawable);
    doneItem.setVisible(false);

    clearNotesItem = menu.findItem(R.id.clear_notes);
    drawable = ContextCompat.getDrawable(activity, R.drawable.ic_delete_24dp);
    requireNonNull(drawable);
    drawable = drawable.mutate();
    drawable.setColorFilter(new PorterDuffColorFilter(
      activity.menu_item_color,
      PorterDuff.Mode.SRC_IN
    ));
    clearNotesItem.setIcon(drawable);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch(item.getItemId()) {

      case R.id.clear_notes: {

        final AlertDialog dialog = new AlertDialog.Builder(activity)
          .setTitle(R.string.clear_notes)
          .setMessage(R.string.clear_notes_message)
          .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

              NotesEditModeFragment.item.setNotesList(new ArrayList<Notes>());
              memo.setText(null);
              notes = null;

              if(activity.isItemExists(NotesEditModeFragment.item, MyDatabaseHelper.TODO_TABLE)) {
                activity.updateDB(NotesEditModeFragment.item, MyDatabaseHelper.TODO_TABLE);
              }
              else {
                MainEditFragment.item.setNotesList(new ArrayList<>(NotesEditModeFragment.item.getNotesList()));
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

        return true;
      }
      case R.id.checklist_mode: {

        NotesEditModeFragment.item.setChecklist_mode(true);
        activity.updateDB(NotesEditModeFragment.item, MyDatabaseHelper.TODO_TABLE);
        activity.showNotesFragment(NotesEditModeFragment.item);
        return true;
      }
      case R.id.done: {

        memo.clearFocus();
        is_editing = false;
        actionBar.setHomeAsUpIndicator(activity.upArrow);
        clearNotesItem.setVisible(true);
        checklistModeItem.setVisible(true);
        doneItem.setVisible(false);

        List<Notes> NotesList = NotesEditModeFragment.item.getNotesList();

        List<String> revised = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(memo.getText().toString()));
        String line;
        try {
          while((line = reader.readLine()) != null) {
            revised.add(line);
          }
        }
        catch(IOException e) {
          e.printStackTrace();
        }

        NotesList.clear();
        int size = revised.size();
        for(int i = 0; i < size; i++) {
          String string = revised.get(i);
          if(string.length() > 1 && string.substring(string.length() - 2).equals(" *")) {
            NotesList.add(new Notes(string.substring(0, string.length() - 2), true, i));
          }
          else {
            NotesList.add(new Notes(string, false, i));
          }
        }

        notes = NotesEditModeFragment.item.getNotesString();

        if(activity.isItemExists(NotesEditModeFragment.item, MyDatabaseHelper.TODO_TABLE)) {
          activity.updateDB(NotesEditModeFragment.item, MyDatabaseHelper.TODO_TABLE);
        }
        else {
          MainEditFragment.item.setNotesList(new ArrayList<>(NotesEditModeFragment.item.getNotesList()));
        }

        return true;
      }
      case android.R.id.home: {

        if(is_editing) {
          memo.clearFocus();
          memo.setText(notes);
          is_editing = false;
          actionBar.setHomeAsUpIndicator(activity.upArrow);
          clearNotesItem.setVisible(true);
          checklistModeItem.setVisible(true);
          doneItem.setVisible(false);
        }
        else {
          MainEditFragment.is_notes_popping = true;
          FragmentManager manager = getFragmentManager();
          requireNonNull(manager);
          manager.popBackStack();
        }

        return true;
      }
    }
    return false;
  }
}
