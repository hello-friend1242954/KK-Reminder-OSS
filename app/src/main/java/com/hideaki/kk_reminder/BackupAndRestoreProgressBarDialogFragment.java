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
import android.widget.Toast;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import static com.hideaki.kk_reminder.UtilClass.LOCALE;
import static com.hideaki.kk_reminder.UtilClass.MENU_POSITION;
import static com.hideaki.kk_reminder.UtilClass.SUBMENU_POSITION;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static java.util.Objects.requireNonNull;

public class BackupAndRestoreProgressBarDialogFragment extends DialogFragment {

  private int progress;
  private int oldProgress;
  private boolean isFirst;
  private boolean isBackup;

  BackupAndRestoreProgressBarDialogFragment(boolean isBackup) {

    this.isBackup = isBackup;
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
    final int bottomPadding = getPxFromDp(activity, 8);
    customTitle.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
    customTitle.setTextSize(getPxFromDp(activity, 7));
    customTitle.setTypeface(Typeface.DEFAULT_BOLD);
    customTitle.setText(
      isBackup? R.string.backup_progress_bar_title : R.string.restore_progress_bar_title
    );
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

        progress = 0;
        oldProgress = progress;
        isFirst = true;
        Thread thread = new Thread() {

          @Override
          public void run() {

            while(true) {
              if(progress != oldProgress || isFirst) {
                isFirst = false;
                handler.post(new Runnable() {
                  @Override
                  public void run() {

                    progressBar.setProgress(progress);
                    String progressDescriptionResult = LOCALE.equals(Locale.JAPAN) ?
                      (isBackup? "データのバックアップ中: " : "データの復元中: ") :
                      (isBackup? "Backup: " : "Restore: ");
                    progressDescriptionResult += progress + "%";
                    progressDescription.setText(progressDescriptionResult);
                  }
                });
                oldProgress = progress;

                if(progress == 100) {
                  break;
                }
              }
              try {
                Thread.sleep(10);
              }
              catch(InterruptedException e) {
                Log.e("BackupRestoreProgress", Log.getStackTraceString(e));
              }
            }

            try {
              Thread.sleep(3500);
            }
            catch(InterruptedException e) {
              Log.e("BackupRestoreProgress", Log.getStackTraceString(e));
            }
            dialog.dismiss();

            if(!isBackup) {
              activity.setIntGeneralInSharedPreferences(MENU_POSITION, 0);
              activity.setIntGeneralInSharedPreferences(SUBMENU_POSITION, 0);
              activity.menuItem = activity.menu.getItem(activity.whichMenuOpen);
              handler.post(new Runnable() {
                @Override
                public void run() {

                  activity.navigationView.setCheckedItem(activity.menuItem);
                  activity.recreate();
                }
              });
            }
          }
        };
        thread.start();
      }
    });

    return dialog;
  }

  int getProgress() {

    return progress;
  }

  void setProgress(int progress) {

    this.progress = progress;
  }

  void addProgress(int progress) {

    this.progress += progress;
  }
}
