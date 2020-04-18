package com.hideaki.kk_reminder;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;

import static com.hideaki.kk_reminder.UtilClass.NOTES_COMPARATOR;

public class NotesDoneListAdapter extends BaseAdapter {

  static List<NotesAdapter> notesList;
  private MainActivity activity;
  private NotesChecklistModeFragment fragment;
  static boolean isFirst;

  NotesDoneListAdapter(MainActivity activity, NotesChecklistModeFragment fragment) {

    this.activity = activity;
    this.fragment = fragment;
    isFirst = true;
  }

  private static class ViewHolder {

    ConstraintLayout notesItem;
    AnimCheckBox checkBox;
    TextView string;
  }

  private class MyOnClickListener
    implements View.OnClickListener, AnimCheckBox.OnCheckedChangeListener {

    private int position;
    private NotesAdapter notes;
    private ViewHolder viewHolder;

    MyOnClickListener(int position, NotesAdapter notes, ViewHolder viewHolder) {

      this.position = position;
      this.notes = notes;
      this.viewHolder = viewHolder;
    }

    @Override
    public void onClick(View v) {

      viewHolder.checkBox.setChecked(false);
    }

    @Override
    public void onChange(AnimCheckBox view, boolean checked) {

      if(!checked) {
        isFirst = false;
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

      notes.setIsChecked(false);
      NotesTodoListAdapter.notesList.add(notes);
      Collections.sort(NotesTodoListAdapter.notesList, NOTES_COMPARATOR);
      notesList.remove(position);

      // todoListにおけるheaderの管理
      int todoListSize = NotesTodoListAdapter.notesList.size();
      if(todoListSize != 0 && fragment.sortableListView.getHeaderViewsCount() == 0) {
        fragment.sortableListView.addHeaderView(fragment.todoHeader);
        fragment.sortItem.setVisible(true);
      }
      else if(todoListSize == 0 && fragment.sortableListView.getHeaderViewsCount() != 0) {
        fragment.sortableListView.removeHeaderView(fragment.todoHeader);
        fragment.sortItem.setVisible(false);
      }

      // doneListにおけるheaderの管理
      int doneListSize = notesList.size();
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
      fragment.notesTodoListAdapter.notifyDataSetChanged();

      if(activity.isItemExists(NotesChecklistModeFragment.item, MyDatabaseHelper.TODO_TABLE)) {
        activity.updateDB(NotesChecklistModeFragment.item, MyDatabaseHelper.TODO_TABLE);
      }
      else {
        MainEditFragment.item.setNotesList(new ArrayList<>(NotesChecklistModeFragment.item.getNotesList()));
      }
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
      convertView =
        View.inflate(parent.getContext(), R.layout.notes_checklist_done_item_layout, null);

      viewHolder = new ViewHolder();
      viewHolder.notesItem = convertView.findViewById(R.id.notes_item);
      viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
      viewHolder.string = convertView.findViewById(R.id.string);

      convertView.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)convertView.getTag();
    }

    // 現在のビュー位置でのtagの取得とリスナーの初期化
    NotesAdapter notes = (NotesAdapter)getItem(position);
    MyOnClickListener listener = new MyOnClickListener(position, notes, viewHolder);

    // リスナーの設定
    viewHolder.notesItem.setOnClickListener(listener);
    viewHolder.checkBox.setOnCheckedChangeListener(listener);

    // チェック状態の初期化
    if(isFirst) {
      viewHolder.checkBox.setChecked(true, false);
    }
    else {
      viewHolder.checkBox.setChecked(true);
    }

    // 各種表示処理
    if(activity.isDarkMode) {
      viewHolder.notesItem.setBackgroundColor(activity.backgroundFloatingMaterialDarkColor);
      viewHolder.string.setTextColor(activity.secondaryTextMaterialDarkColor);
    }
    viewHolder.string.setText(notes.getString());

    return convertView;
  }
}