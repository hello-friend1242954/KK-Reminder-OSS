package com.example.hideaki.reminder;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
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

import static com.example.hideaki.reminder.UtilClass.ITEM;
import static com.example.hideaki.reminder.UtilClass.LINE_SEPARATOR;
import static com.google.common.base.Preconditions.checkNotNull;

public class NotesEditModeFragment extends Fragment {

  static final String TAG = NotesEditModeFragment.class.getSimpleName();
  static Item item = null;
  static boolean is_editing;
  private MainActivity activity;
  private MenuItem doneItem;
  private MenuItem checklistModeItem;
  private EditText memo;
  private ActionBar actionBar;
  private MenuItem clearNotesItem;

  public static NotesEditModeFragment newInstance(Item item) {

    NotesEditModeFragment fragment = new NotesEditModeFragment();

    Bundle args = new Bundle();
    args.putSerializable(ITEM, item);
    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public void onAttach(Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    this.setHasOptionsMenu(true);

    Bundle args = getArguments();
    item = (Item)args.getSerializable(ITEM);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    if(MainEditFragment.is_popping) {
      getFragmentManager().popBackStack();
    }

    View view = inflater.inflate(R.layout.notes_edit_layout, container, false);
    view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    view.setFocusableInTouchMode(true);
    view.requestFocus();
    view.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {

          MainEditFragment.is_popping = true;
        }

        return false;
      }
    });

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    actionBar = activity.getSupportActionBar();
    checkNotNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.notes);

    memo = view.findViewById(R.id.notes);
    memo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {

        if(hasFocus) {
          is_editing = true;
          Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.ic_cancel_24dp);
          checkNotNull(drawable);
          drawable = drawable.mutate();
          actionBar.setHomeAsUpIndicator(drawable);

          clearNotesItem.setVisible(false);
          doneItem.setVisible(true);
          checklistModeItem.setVisible(false);
        }
        else {
          InputMethodManager manager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
          checkNotNull(manager);
          manager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
      }
    });
    StringBuilder stringBuilder = new StringBuilder();
    for(Notes notes : item.getNotesList()) {
      if(notes.isChecked()) {
        stringBuilder.append(notes.getString()).append(" *").append(LINE_SEPARATOR);
      }
      else stringBuilder.append(notes.getString()).append(LINE_SEPARATOR);
    }
    memo.setText(stringBuilder.toString());

    return view;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.notes_edit_mode_menu, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {

    super.onPrepareOptionsMenu(menu);

    checklistModeItem = menu.findItem(R.id.checklist_mode);

    doneItem = menu.findItem(R.id.done);
    doneItem.setVisible(false);

    clearNotesItem = menu.findItem(R.id.clear_notes);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch(item.getItemId()) {

      case R.id.clear_notes: {

        new AlertDialog.Builder(activity)
            .setTitle(R.string.clear_notes)
            .setMessage(R.string.clear_notes_message)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                NotesEditModeFragment.item.setNotesList(new ArrayList<Notes>());
                memo.setText(null);

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
              public void onClick(DialogInterface dialog, int which) {}
            })
            .show();

        return true;
      }
      case R.id.checklist_mode: {

        NotesEditModeFragment.item.setChecklist_mode(true);
        activity.showNotesFragment(NotesEditModeFragment.item, TAG);
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
          memo.setText(null);
          is_editing = false;
          actionBar.setHomeAsUpIndicator(activity.upArrow);
          clearNotesItem.setVisible(true);
          checklistModeItem.setVisible(true);
          doneItem.setVisible(false);
        }
        else {
          MainEditFragment.is_popping = true;
          getFragmentManager().popBackStack();
        }

        return true;
      }
    }
    return false;
  }
}
