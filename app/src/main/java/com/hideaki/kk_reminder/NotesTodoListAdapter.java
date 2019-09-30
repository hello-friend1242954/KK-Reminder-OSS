package com.hideaki.kk_reminder;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;

import static com.hideaki.kk_reminder.UtilClass.NOTES_COMPARATOR;

public class NotesTodoListAdapter extends BaseAdapter {

  static List<Notes> notesList;
  private MainActivity activity;
  private NotesChecklistModeFragment fragment;
  DragListener dragListener;
  private int draggingPosition = -1;
  static boolean isSorting;

  NotesTodoListAdapter(MainActivity activity, NotesChecklistModeFragment fragment) {

    this.activity = activity;
    this.fragment = fragment;
    dragListener = new DragListener();
    isSorting = false;
  }

  private static class ViewHolder {

    ConstraintLayout notesItem;
    ImageView hamburgerIcon;
    AnimCheckBox checkBox;
    TextView string;
  }

  private class MyOnClickListener implements View.OnClickListener,
      AnimCheckBox.OnCheckedChangeListener {

    private int position;
    private Notes notes;
    private ViewHolder viewHolder;

    MyOnClickListener(int position, Notes notes, ViewHolder viewHolder) {

      this.position = position;
      this.notes = notes;
      this.viewHolder = viewHolder;
    }

    @Override
    public void onClick(View v) {

      viewHolder.checkBox.setChecked(true);
    }

    @Override
    public void onChange(AnimCheckBox view, boolean checked) {

      if(checked) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {

            onChangeProcessor();
          }
        }, 400);
      }
    }

    private void onChangeProcessor() {

      notes.setChecked(true);
      NotesDoneListAdapter.notesList.add(notes);
      Collections.sort(NotesDoneListAdapter.notesList, NOTES_COMPARATOR);
      notesList.remove(position);

      // todoListにおけるheaderの管理
      int todoListSize = notesList.size();
      if(todoListSize != 0 && fragment.sortableListView.getHeaderViewsCount() == 0) {
        fragment.sortableListView.addHeaderView(fragment.todoHeader);
        fragment.sortItem.setVisible(true);
      }
      else if(todoListSize == 0 && fragment.sortableListView.getHeaderViewsCount() != 0) {
        fragment.sortableListView.removeHeaderView(fragment.todoHeader);
        fragment.sortItem.setVisible(false);
      }

      // doneListにおけるheaderの管理
      int doneListSize = NotesDoneListAdapter.notesList.size();
      if(doneListSize != 0) {
        NotesDoneListAdapter.isFirst = true;
      }
      if(doneListSize != 0 && fragment.listView.getHeaderViewsCount() == 0) {
        fragment.listView.addHeaderView(fragment.doneHeader);
        fragment.deleteItem.setVisible(true);
        fragment.unselectItem.setVisible(true);
      }
      else if(doneListSize == 0 && fragment.listView.getHeaderViewsCount() != 0) {
        fragment.listView.removeHeaderView(fragment.doneHeader);
        fragment.deleteItem.setVisible(false);
        fragment.unselectItem.setVisible(false);
      }

      notifyDataSetChanged();
      fragment.notesDoneListAdapter.notifyDataSetChanged();

      if(activity.isItemExists(NotesChecklistModeFragment.item, MyDatabaseHelper.TODO_TABLE)) {
        activity.updateDB(NotesChecklistModeFragment.item, MyDatabaseHelper.TODO_TABLE);
      }
      else {
        MainEditFragment.item.setNotesList(new ArrayList<>(NotesChecklistModeFragment.item.getNotesList()));
      }
    }
  }

  class DragListener extends SortableListView.SimpleDragListener {

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

      Notes notes = notesList.get(positionFrom);
      notesList.remove(positionFrom);
      notesList.add(positionTo, notes);

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
  public int getCount() {

    return notesList.size();
  }

  @Override
  public Object getItem(int position) {

    return notesList.get(position);
  }

  @Override
  public long getItemId(int position) {

    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    final ViewHolder viewHolder;

    if(convertView == null || convertView.getTag() == null) {
      convertView = View.inflate(parent.getContext(), R.layout.notes_checklist_todo_item_layout,
          null
      );

      viewHolder = new ViewHolder();
      viewHolder.notesItem = convertView.findViewById(R.id.notes_item);
      viewHolder.hamburgerIcon = convertView.findViewById(R.id.hamburger_icon);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.string = convertView.findViewById(R.id.string);

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)convertView.getTag();
    }

    // 現在のビュー位置でのtagの取得とリスナーの初期化
    Notes notes = (Notes)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(position, notes, viewHolder);

    // リスナーの設定
    viewHolder.notesItem.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    // 各種表示処理
    if(activity.isDarkMode) {
      viewHolder.notesItem.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      viewHolder.string.setTextColor(activity.secondaryTextMaterialDarkColor);
    }
    viewHolder.string.setText(notes.getString());

    // チェック状態の初期化
    viewHolder.checkBox.setChecked(false);

    // タグの左にあるアイコンの表示設定
    if(isSorting) {
      viewHolder.hamburgerIcon.setVisibility(View.VISIBLE);
      viewHolder.checkBox.setVisibility(View.GONE);
    }
    else {
      viewHolder.hamburgerIcon.setVisibility(View.GONE);
      viewHolder.checkBox.setVisibility(View.VISIBLE);
    }

    // 並び替え中にドラッグしているアイテムが二重に表示されないようにする
    convertView.setVisibility(position == draggingPosition ? View.INVISIBLE : View.VISIBLE);

    return convertView;
  }
}
