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
import static com.hideaki.kk_reminder.UtilClass.IS_COPIED_FROM_OLD_VERSION;
import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static java.util.Objects.requireNonNull;

public class CopyFromOldVersionProgressBarDialogFragment extends DialogFragment {

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
    customTitle.setText(activity.getString(R.string.copy_from_old_version_progress_bar_title));
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

            // generalSettingsへの代入はMainActivity#onCreate()内で既に行っているので必要ない
            // todoItemListも同様
            List<ItemAdapter> todoItemList = activity.todoItemList;
            List<ItemAdapter> doneItemList = activity.queryAllDB(DONE_TABLE);

            int todoItemListSize = todoItemList.size();
            int size = todoItemListSize + doneItemList.size() + 1;
            for(int i = 0; i <= size; i++) {
              final int progress = i * 100 / size;
              handler.post(new Runnable() {
                @Override
                public void run() {

                  progressBar.setProgress(progress);
                  String progressDescriptionResult = LOCALE.equals(Locale.JAPAN) ?
                    "データベース更新中: " : "Updating Database: ";
                  progressDescriptionResult += progress + "%";
                  progressDescription.setText(progressDescriptionResult);
                }
              });

              if(i == size) {
                break;
              }

              if(i == 0) {
                activity.updateSettingsDB();
              }
              else if(i < todoItemListSize + 1) {
                activity.updateDB(todoItemList.get(i - 1), TODO_TABLE);
              }
              else {
                activity.updateDB(doneItemList.get(i - todoItemListSize - 1), DONE_TABLE);
              }
            }
            activity.setBooleanGeneralInSharedPreferences(IS_COPIED_FROM_OLD_VERSION, false);
            try {
              Thread.sleep(3500);
            }
            catch(InterruptedException e) {
              Log.e("CopyProgressBar", Log.getStackTraceString(e));
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
