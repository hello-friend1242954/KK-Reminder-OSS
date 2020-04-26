package com.hideaki.kk_reminder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import static com.hideaki.kk_reminder.MyDatabaseHelper.DONE_TABLE;
import static com.hideaki.kk_reminder.MyDatabaseHelper.TODO_TABLE;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static java.util.Objects.requireNonNull;

public class SetAllProgressBarDialogFragment extends DialogFragment {

  private boolean isNotifySound;

  SetAllProgressBarDialogFragment(boolean isNotifySound) {

    this.isNotifySound = isNotifySound;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setCancelable(false);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    View view = View.inflate(getContext(), R.layout.my_progress_bar, null);
    final MainActivity activity = (MainActivity)getActivity();
    requireNonNull(activity);

    final ProgressBar progressBar = view.findViewById(R.id.progress_bar);
    progressBar.setProgressTintList(ColorStateList.valueOf(activity.accentColor));
    final TextView progressDescription = view.findViewById(R.id.progress_description);

    TextView customTitle = new TextView(activity);
    int leftPadding = getPxFromDp(activity, 8);
    int topPadding = getPxFromDp(activity, 20);
    int rightPadding = getPxFromDp(activity, 8);
    int bottomPadding = getPxFromDp(activity, 8);
    customTitle.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
    customTitle.setTextSize(getPxFromDp(activity, 7));
    customTitle.setTypeface(Typeface.DEFAULT_BOLD);
    if(isNotifySound) {
      customTitle.setText(activity.getString(R.string.set_all_notify_sound_progress_bar_title));
    }
    else {
      customTitle.setText(activity.getString(R.string.set_all_vibration_progress_bar_title));
    }

    if(activity.isDarkMode) {
      customTitle.setTextColor(
        ContextCompat.getColor(activity, R.color.primaryTextMaterialDark)
      );
      progressDescription.setTextColor(
        ContextCompat.getColor(activity, R.color.primaryTextMaterialDark)
      );
    }
    else {
      customTitle.setTextColor(ContextCompat.getColor(activity, android.R.color.black));
      progressDescription.setTextColor(ContextCompat.getColor(activity, android.R.color.black));
    }
    customTitle.setGravity(Gravity.CENTER);

    final AlertDialog dialog = new AlertDialog.Builder(activity)
      .setCustomTitle(customTitle)
      .setView(view)
      .create();

    final Handler handler = new Handler();
    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialogInterface) {

        Thread thread = new Thread() {

          @Override
          public void run() {

            List<ItemAdapter> todoItemList = activity.queryAllDB(TODO_TABLE);
            List<ItemAdapter> doneItemList = activity.queryAllDB(DONE_TABLE);

            int todoItemListSize = todoItemList.size();
            int size = todoItemListSize + doneItemList.size();
            for(int i = 0; i <= size; i++) {
              final int progress = i * 100 / size;
              handler.post(new Runnable() {
                @Override
                public void run() {

                  progressBar.setProgress(progress);
                  String progressDescriptionResult = LOCALE.equals(Locale.JAPAN) ?
                    "一括設定中: " : "Setting to all tasks: ";
                  progressDescriptionResult += progress + "%";
                  progressDescription.setText(progressDescriptionResult);
                }
              });

              if(i == size) {
                break;
              }

              if(i < todoItemListSize) {
                ItemAdapter todoItem = todoItemList.get(i);
                if(isNotifySound) {
                  todoItem.setSoundUri(SetAllFragment.uriSound.toString());
                }
                else {
                  todoItem.setVibrationPattern(SetAllFragment.vibrationStr);
                }
                activity.updateDB(todoItem, TODO_TABLE);
              }
              else {
                ItemAdapter doneItem = doneItemList.get(i - todoItemListSize);
                if(isNotifySound) {
                  doneItem.setSoundUri(SetAllFragment.uriSound.toString());
                }
                else {
                  doneItem.setVibrationPattern(SetAllFragment.vibrationStr);
                }
                activity.updateDB(doneItem, DONE_TABLE);
              }
            }
            try {
              Thread.sleep(3500);
            }
            catch(InterruptedException e) {
              Log.e("SetAllProgressBar", Log.getStackTraceString(e));
            }
            dialog.dismiss();
          }
        };
        thread.start();
      }
    });

    return dialog;
  }
}
