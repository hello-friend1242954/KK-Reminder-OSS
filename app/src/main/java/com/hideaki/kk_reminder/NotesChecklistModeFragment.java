package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static com.hideaki.kk_reminder.UtilClass.ITEM;
import static com.hideaki.kk_reminder.UtilClass.NOTES_COMPARATOR;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static java.util.Objects.requireNonNull;

public class NotesChecklistModeFragment extends Fragment {

  static final String TAG = NotesChecklistModeFragment.class.getSimpleName();
  static ItemAdapter item = null;
  private MainActivity activity;
  NotesTodoListAdapter notesTodoListAdapter;
  NotesDoneListAdapter notesDoneListAdapter;
  NonScrollSortableListView sortableListView;
  View todoHeader;
  NonScrollListView listView;
  View doneHeader;
  private MenuItem editModeItem;
  private ActionBar actionBar;
  MenuItem deleteItem;
  MenuItem unselectItem;
  MenuItem sortItem;
  private boolean isEmptyViewAdded;
  private View emptyView;

  public static NotesChecklistModeFragment newInstance(ItemAdapter item) {

    NotesChecklistModeFragment fragment = new NotesChecklistModeFragment();

    Bundle args = new Bundle();
    args.putSerializable(ITEM, item.getItem());
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
    item = new ItemAdapter(args.getSerializable(ITEM));

    notesTodoListAdapter = new NotesTodoListAdapter(activity, this);
    notesDoneListAdapter = new NotesDoneListAdapter(activity, this);
  }

  @Override
  public View onCreateView(
    @NonNull LayoutInflater inflater,
    ViewGroup container,
    Bundle savedInstanceState
  ) {

    if(MainEditFragment.isNotesPopping) {
      FragmentManager manager = getFragmentManager();
      requireNonNull(manager);
      manager.popBackStack();
    }
    View view = inflater.inflate(R.layout.notes_checklist_layout, container, false);
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

          if(NotesTodoListAdapter.isSorting) {
            new AlertDialog.Builder(activity)
              .setTitle(R.string.is_sorting_title)
              .setMessage(R.string.is_sorting_message)
              .show();

            return true;
          }

          MainEditFragment.isNotesPopping = true;
        }

        return false;
      }
    });

    // NotesTodoListとNotesDoneListの保持するnotesListの初期化
    NotesTodoListAdapter.notesList = new ArrayList<>();
    NotesDoneListAdapter.notesList = new ArrayList<>();
    for(NotesAdapter notes : item.getNotesList()) {
      if(notes.isChecked()) {
        NotesDoneListAdapter.notesList.add(notes);
      }
      else {
        NotesTodoListAdapter.notesList.add(notes);
      }
    }
    Collections.sort(NotesTodoListAdapter.notesList, NOTES_COMPARATOR);
    Collections.sort(NotesDoneListAdapter.notesList, NOTES_COMPARATOR);

    // NotesTodoListの初期化
    NotesTodoListAdapter.isSorting = false;
    sortableListView = view.findViewById(R.id.notes_todo);
    todoHeader = View.inflate(activity, R.layout.notes_todo_list_header, null);
    todoHeader.setOnClickListener(null);
    todoHeader.setBackgroundColor(activity.accentColor);
    TextView todoHeaderTitle = todoHeader.findViewById(R.id.todo);
    todoHeaderTitle.setTextColor(activity.secondaryTextColor);
    if(NotesTodoListAdapter.notesList.size() != 0 && sortableListView.getHeaderViewsCount() == 0
      && !NotesTodoListAdapter.isSorting) {
      sortableListView.addHeaderView(todoHeader);
    }
    sortableListView.setDragListener(notesTodoListAdapter.myDragListener);
    sortableListView.setAdapter(notesTodoListAdapter);

    // NotesDoneListの初期化
    listView = view.findViewById(R.id.notes_done);
    doneHeader = View.inflate(activity, R.layout.notes_done_list_header, null);
    doneHeader.setOnClickListener(null);
    doneHeader.setBackgroundColor(activity.accentColor);
    TextView doneHeaderTitle = doneHeader.findViewById(R.id.done);
    doneHeaderTitle.setTextColor(activity.secondaryTextColor);
    if(NotesDoneListAdapter.notesList.size() != 0 && listView.getHeaderViewsCount() == 0) {
      listView.addHeaderView(doneHeader);
    }
    listView.setAdapter(notesDoneListAdapter);

    emptyView = View.inflate(activity, R.layout.notes_list_empty_layout, null);
    isEmptyViewAdded = false;
    if(NotesChecklistModeFragment.item.getNotesList().size() == 0) {
      LinearLayout linearLayout = new LinearLayout(activity);
      linearLayout.setOrientation(LinearLayout.VERTICAL);
      LinearLayout.LayoutParams layoutParams =
        new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.MATCH_PARENT
        );
      layoutParams.gravity = Gravity.CENTER;
      emptyView.setLayoutParams(layoutParams);
      linearLayout.addView(emptyView);
      int paddingPx = getPxFromDp(activity, 75);
      linearLayout.setPadding(0, 0, 0, paddingPx);
      ((ViewGroup)sortableListView.getParent()).addView(linearLayout, layoutParams);
      isEmptyViewAdded = true;
    }

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    Drawable drawable = toolbar.getOverflowIcon();
    requireNonNull(drawable);
    drawable = drawable.mutate();
    drawable.setColorFilter(new PorterDuffColorFilter(
      activity.menuItemColor,
      PorterDuff.Mode.SRC_IN
    ));
    toolbar.setOverflowIcon(drawable);
    activity.setSupportActionBar(toolbar);
    actionBar = activity.getSupportActionBar();
    requireNonNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.notes);

    return view;
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.notes_checklist_mode_menu, menu);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {

    super.onPrepareOptionsMenu(menu);

    sortItem = menu.findItem(R.id.sort);
    Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.ic_sort_24dp);
    requireNonNull(drawable);
    drawable = drawable.mutate();
    drawable.setColorFilter(new PorterDuffColorFilter(
      activity.menuItemColor,
      PorterDuff.Mode.SRC_IN
    ));
    sortItem.setIcon(drawable);

    editModeItem = menu.findItem(R.id.edit_mode);
    drawable = ContextCompat.getDrawable(activity, R.drawable.ic_edit_24dp);
    requireNonNull(drawable);
    drawable = drawable.mutate();
    drawable.setColorFilter(new PorterDuffColorFilter(
      activity.menuItemColor,
      PorterDuff.Mode.SRC_IN
    ));
    editModeItem.setIcon(drawable);

    deleteItem = menu.findItem(R.id.delete_checked_items);

    unselectItem = menu.findItem(R.id.unselect_all_items);

    // Todoリストのアイテム数に応じた表示処理
    if(NotesTodoListAdapter.notesList.size() == 0) {
      sortItem.setVisible(false);
    }
    else {
      sortItem.setVisible(true);
    }

    // Doneリストのアイテム数に応じた表示処理
    if(NotesDoneListAdapter.notesList.size() == 0) {
      deleteItem.setVisible(false);
      unselectItem.setVisible(false);
    }
    else {
      deleteItem.setVisible(true);
      unselectItem.setVisible(true);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch(item.getItemId()) {

      case R.id.delete_checked_items: {

        final AlertDialog dialog = new AlertDialog.Builder(activity)
          .setTitle(R.string.delete_checked_items)
          .setMessage(R.string.delete_checked_items_message)
          .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

              NotesDoneListAdapter.notesList = new ArrayList<>();
              if(listView.getHeaderViewsCount() != 0) {
                deleteItem.setVisible(false);
                unselectItem.setVisible(false);
                listView.removeHeaderView(doneHeader);
              }
              notesDoneListAdapter.notifyDataSetChanged();

              NotesChecklistModeFragment.item.setNotesList(new ArrayList<>(NotesTodoListAdapter.notesList));

              if(
                NotesChecklistModeFragment.item.getNotesList().size() == 0 &&
                  !isEmptyViewAdded
              ) {
                LinearLayout linearLayout = new LinearLayout(activity);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams layoutParams =
                  new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                  );
                layoutParams.gravity = Gravity.CENTER;
                emptyView.setLayoutParams(layoutParams);
                linearLayout.addView(emptyView);
                int paddingPx = getPxFromDp(activity, 75);
                linearLayout.setPadding(0, 0, 0, paddingPx);
                ((ViewGroup)sortableListView.getParent()).addView(linearLayout, layoutParams);
                isEmptyViewAdded = true;
              }

              if(activity.isItemExists(
                NotesChecklistModeFragment.item,
                MyDatabaseHelper.TODO_TABLE
              )) {
                activity.updateDB(NotesChecklistModeFragment.item, MyDatabaseHelper.TODO_TABLE);
              }
              else {
                MainEditFragment.item.setNotesList(
                  new ArrayList<>(NotesChecklistModeFragment.item.getNotesList())
                );
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

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
          }
        });

        dialog.show();

        return true;
      }
      case R.id.unselect_all_items: {

        for(NotesAdapter notes : NotesDoneListAdapter.notesList) {
          notes.setIsChecked(false);
          NotesTodoListAdapter.notesList.add(notes);
        }
        Collections.sort(NotesTodoListAdapter.notesList, NOTES_COMPARATOR);
        NotesDoneListAdapter.notesList = new ArrayList<>();
        notesTodoListAdapter.notifyDataSetChanged();
        notesDoneListAdapter.notifyDataSetChanged();

        if(sortableListView.getHeaderViewsCount() == 0) {
          sortItem.setVisible(true);
          sortableListView.addHeaderView(todoHeader);
        }

        if(listView.getHeaderViewsCount() != 0) {
          deleteItem.setVisible(false);
          unselectItem.setVisible(false);
          listView.removeHeaderView(doneHeader);
        }

        if(activity.isItemExists(NotesChecklistModeFragment.item, MyDatabaseHelper.TODO_TABLE)) {
          activity.updateDB(NotesChecklistModeFragment.item, MyDatabaseHelper.TODO_TABLE);
        }
        else {
          MainEditFragment.item.setNotesList(
            new ArrayList<>(NotesChecklistModeFragment.item.getNotesList())
          );
        }

        return true;
      }
      case R.id.edit_mode: {

        NotesChecklistModeFragment.item.setChecklistMode(false);
        activity.updateDB(NotesChecklistModeFragment.item, MyDatabaseHelper.TODO_TABLE);
        activity.showNotesFragment(NotesChecklistModeFragment.item);
        return true;
      }
      case R.id.sort: {

        NotesTodoListAdapter.isSorting = !NotesTodoListAdapter.isSorting;
        if(NotesTodoListAdapter.isSorting) {
          activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
          actionBar.setDisplayHomeAsUpEnabled(false);
          editModeItem.setVisible(false);
          deleteItem.setVisible(false);
          unselectItem.setVisible(false);
          if(sortableListView.getHeaderViewsCount() != 0) {
            sortableListView.removeHeaderView(todoHeader);
          }
          listView.setVisibility(View.GONE);
        }
        else {
          activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
          actionBar.setDisplayHomeAsUpEnabled(true);
          editModeItem.setVisible(true);
          if(NotesDoneListAdapter.notesList.size() != 0) {
            deleteItem.setVisible(true);
            unselectItem.setVisible(true);
          }
          if(NotesTodoListAdapter.notesList.size() != 0 &&
            sortableListView.getHeaderViewsCount() == 0) {
            sortableListView.addHeaderView(todoHeader);
          }
          listView.setVisibility(View.VISIBLE);


          List<NotesAdapter> doneList = NotesDoneListAdapter.notesList;
          List<Integer> doneOrderList = new ArrayList<>();
          for(NotesAdapter notes : doneList) {
            doneOrderList.add(notes.getOrder());
          }

          List<Integer> sortedIndex = new ArrayList<>();
          int allListSize = NotesChecklistModeFragment.item.getNotesList().size();
          for(int i = 0; i < allListSize; i++) {
            sortedIndex.add(i);
          }

          int doneListSize = doneList.size();
          for(int i = 0; i < doneListSize; i++) {
            sortedIndex.remove(doneOrderList.get(i));
          }
          Collections.sort(sortedIndex);

          List<NotesAdapter> notesList = NotesTodoListAdapter.notesList;
          int size = notesList.size();
          boolean isUpdated = false;
          for(int i = 0; i < size; i++) {
            NotesAdapter notes = notesList.get(i);
            if(notes.getOrder() != sortedIndex.get(i)) {
              notes.setOrder(sortedIndex.get(i));
              isUpdated = true;
            }
          }

          notesList = new ArrayList<>(NotesTodoListAdapter.notesList);
          notesList.addAll(NotesDoneListAdapter.notesList);
          Collections.sort(notesList, NOTES_COMPARATOR);

          if(isUpdated) {
            NotesChecklistModeFragment.item.setNotesList(new ArrayList<>(notesList));
            if(activity.isItemExists(
              NotesChecklistModeFragment.item,
              MyDatabaseHelper.TODO_TABLE
            )) {
              activity.updateDB(NotesChecklistModeFragment.item, MyDatabaseHelper.TODO_TABLE);
            }
            else {
              MainEditFragment.item.setNotesList(
                new ArrayList<>(NotesChecklistModeFragment.item.getNotesList())
              );
            }
          }
        }

        notesTodoListAdapter.notifyDataSetChanged();
        notesDoneListAdapter.notifyDataSetChanged();
        return true;
      }
      case android.R.id.home: {

        MainEditFragment.isNotesPopping = true;
        FragmentManager manager = getFragmentManager();
        requireNonNull(manager);
        manager.popBackStack();

        return true;
      }
    }
    return false;
  }
}
