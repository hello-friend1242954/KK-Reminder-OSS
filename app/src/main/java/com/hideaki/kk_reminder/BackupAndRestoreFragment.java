package com.hideaki.kk_reminder;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.takisoft.fix.support.v7.preference.PreferenceCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import static com.hideaki.kk_reminder.UtilClass.MENU_POSITION;
import static com.hideaki.kk_reminder.UtilClass.REQUEST_CODE_SIGN_IN;
import static com.hideaki.kk_reminder.UtilClass.SUBMENU_POSITION;
import static java.util.Objects.requireNonNull;

public class BackupAndRestoreFragment extends BasePreferenceFragmentCompat
  implements Preference.OnPreferenceClickListener {

  @SuppressLint("SdCardPath")
  private static final String DATABASE_PATH = "/data/data/com.hideaki.kk_reminder/databases/";
  private static final String FILE_NAME = MyDatabaseHelper.DATABASE;
  private static final String FOLDER_NAME = "KK_Reminder_Backup";
  private static final String MIME_TYPE = "application/x-sqlite-3";

  private MainActivity activity;
  private PreferenceCategory preferenceCategory;
  private GoogleSignInAccount signInAccount;
  private GoogleSignInClient signInClient;
  private PreferenceScreen logout;
  private boolean is_backup;
  private int choice;
  private DriveServiceHelper driveServiceHelper;

  public static BackupAndRestoreFragment newInstance() {

    return new BackupAndRestoreFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.backup_and_restore);
    setHasOptionsMenu(true);

    preferenceCategory = (PreferenceCategory)findPreference("backup_and_restore");
    PreferenceScreen backup = (PreferenceScreen)findPreference("backup");
    PreferenceScreen restore = (PreferenceScreen)findPreference("restore");
    logout = (PreferenceScreen)findPreference("logout");

    backup.setOnPreferenceClickListener(this);
    restore.setOnPreferenceClickListener(this);
    logout.setOnPreferenceClickListener(this);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

    super.onViewCreated(view, savedInstanceState);

    // 設定項目間の区切り線の非表示
    setDivider(new ColorDrawable(Color.TRANSPARENT));
    setDividerHeight(0);
  }

  @Override
  public View onCreateView(
    LayoutInflater inflater,
    @Nullable ViewGroup container,
    @Nullable Bundle savedInstanceState
  ) {

    View view = super.onCreateView(inflater, container, savedInstanceState);
    requireNonNull(view);

    if(activity.isDarkMode) {
      view.setBackgroundColor(activity.backgroundMaterialDarkColor);
    }
    else {
      view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    }

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    requireNonNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.backup_and_restore);

    // ログイン状態の初期化
    signInAccount = GoogleSignIn.getLastSignedInAccount(activity);

    if(signInAccount != null) {
      preferenceCategory.addPreference(logout);
    }
    else {
      preferenceCategory.removePreference(logout);
    }

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {

    FragmentManager manager = getFragmentManager();
    requireNonNull(manager);
    manager.popBackStack();
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {

    switch(preference.getKey()) {

      case "backup": {

        if(signInAccount != null) {
          setDriveServiceHelper();
          backupToDrive();
        }
        else {
          is_backup = true;
          signIn();
        }
        return true;
      }
      case "restore": {

        if(signInAccount != null) {
          setDriveServiceHelper();
          restoreFromDrive();
        }
        else {
          is_backup = false;
          signIn();
        }
        return true;
      }
      case "logout": {

        signOut();
        return true;
      }
    }
    return false;
  }

  private void restoreFromDrive() {

    if(driveServiceHelper != null) {

      driveServiceHelper.queryFolder(FOLDER_NAME)
        .addOnSuccessListener(new OnSuccessListener<FileList>() {
          @Override
          public void onSuccess(FileList fileList) {

            List<File> folderList = fileList.getFiles();
            if(folderList.size() == 0) {
              Toast
                .makeText(activity, getString(R.string.backup_not_exists), Toast.LENGTH_LONG)
                .show();
            }
            else {
              driveServiceHelper.queryFiles(MIME_TYPE)
                .addOnSuccessListener(new OnSuccessListener<List<FileList>>() {
                  @Override
                  public void onSuccess(List<FileList> fileLists) {

                    List<String> backupList = new ArrayList<>();
                    List<String> backupIdList = new ArrayList<>();
                    for(FileList list : fileLists) {
                      for(File file : list.getFiles()) {
                        backupList.add(file.getName());
                        backupIdList.add(file.getId());
                      }
                    }
                    if(backupList.size() == 0) {
                      Toast.makeText(
                        activity, getString(R.string.backup_not_exists), Toast.LENGTH_LONG
                      ).show();
                    }
                    else {
                      displayDialog(backupList, backupIdList);
                    }
                  }
                })
                .addOnFailureListener(new OnFailureListener() {
                  @Override
                  public void onFailure(@NonNull Exception e) {

                    Toast
                      .makeText(
                        activity,
                        getString(R.string.fail_to_query),
                        Toast.LENGTH_LONG
                      )
                      .show();
                  }
                });
            }
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {

            Toast.makeText(activity, getString(R.string.fail_to_query), Toast.LENGTH_LONG).show();
          }
        });
    }
  }

  private void displayDialog(List<String> backupList, final List<String> backupIdList) {

    int size = backupList.size();
    if(size == 0) {
      Toast.makeText(activity, getString(R.string.backup_not_exists), Toast.LENGTH_LONG).show();
    }
    else {
      for(int i = 0; i < size; i++) {
        StringBuilder title = new StringBuilder(backupList.get(i));
        title.setCharAt(4, '/');
        title.setCharAt(7, '/');
        title.setCharAt(10, ' ');
        title.setCharAt(13, ':');
        title.setCharAt(16, ':');
        title.setCharAt(19, ':');
        backupList.set(i, title.substring(0, title.length() - 3));
      }

      choice = 0;
      String[] items = backupList.toArray(new String[0]);
      final SingleChoiceItemsAdapter adapter = new SingleChoiceItemsAdapter(items);
      final AlertDialog dialog = new AlertDialog.Builder(activity)
        .setTitle(R.string.choose_backup_data_message)
        .setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

          }
        })
        .setPositiveButton(R.string.determine, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            choice = SingleChoiceItemsAdapter.checked_position;

            readDriveFile(backupIdList.get(choice));
          }
        })
        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            Toast.makeText(activity, getString(R.string.canceled), Toast.LENGTH_LONG).show();
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {

            Toast.makeText(activity, getString(R.string.canceled), Toast.LENGTH_LONG).show();
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
    }
  }

  private void readDriveFile(String fileId) {

    if(driveServiceHelper != null) {
      java.io.File file = new java.io.File(DATABASE_PATH + FILE_NAME);
      driveServiceHelper.downloadFile(file, fileId)
        .addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {

            Toast
              .makeText(activity, getString(R.string.success_to_restore), Toast.LENGTH_LONG)
              .show();

            activity.setIntGeneralInSharedPreferences(MENU_POSITION, 0);
            activity.setIntGeneralInSharedPreferences(SUBMENU_POSITION, 0);
            activity.recreate();
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {

            Toast
              .makeText(activity, getString(R.string.fail_to_restore), Toast.LENGTH_LONG)
              .show();
          }
        });
    }
  }

  private void backupToDrive() {

    if(driveServiceHelper != null) {

      driveServiceHelper.queryFolder(FOLDER_NAME)
        .addOnSuccessListener(new OnSuccessListener<FileList>() {

          @Override
          public void onSuccess(FileList fileList) {

            List<File> folderList = fileList.getFiles();
            if(folderList.size() == 0) {

              driveServiceHelper.createFolder(FOLDER_NAME, null)
                .addOnSuccessListener(new OnSuccessListener<File>() {
                  @Override
                  public void onSuccess(File file) {

                    createBackupFile(file.getId());
                  }
                })
                .addOnFailureListener(new OnFailureListener() {
                  @Override
                  public void onFailure(@NonNull Exception e) {

                    Toast.makeText(
                      activity,
                      getString(R.string.fail_to_make_backup_folder),
                      Toast.LENGTH_LONG
                    ).show();
                  }
                });
            }
            else {
              createBackupFile(folderList.get(0).getId());
            }
          }
        })
        .addOnFailureListener(new OnFailureListener() {

          @Override
          public void onFailure(@NonNull Exception e) {

            Toast.makeText(activity, getString(R.string.fail_to_query), Toast.LENGTH_LONG).show();
          }
        });
    }
  }

  private void createBackupFile(String folderId) {

    if(driveServiceHelper != null) {

      java.io.File filePath = new java.io.File(DATABASE_PATH + FILE_NAME);
      FileContent fileContent = new FileContent(MIME_TYPE, filePath);
      String fileName = DateFormat.format("yyyy_MM_dd_kk_mm_ss", new Date()).toString() + ".db";
      driveServiceHelper.createFile(fileName, fileContent, folderId)
        .addOnSuccessListener(new OnSuccessListener<File>() {
          @Override
          public void onSuccess(File file) {

            Toast
              .makeText(activity, getString(R.string.create_new_backup), Toast.LENGTH_LONG)
              .show();
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {

            Toast
              .makeText(
                activity,
                getString(R.string.fail_to_create_new_backup),
                Toast.LENGTH_LONG
              )
              .show();
          }
        });
    }
  }

  private void signIn() {

    GoogleSignInOptions signInOptions =
      new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
        .build();
    signInClient = GoogleSignIn.getClient(activity, signInOptions);
    startActivityForResult(signInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
  }

  private void signOut() {

    if(signInClient != null) {

      signInClient.signOut()
        .addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {

            preferenceCategory.removePreference(logout);
            signInAccount = null;
            signInClient = null;
            Toast.makeText(activity, getString(R.string.logout_done), Toast.LENGTH_LONG).show();
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {

            Toast
              .makeText(activity, getString(R.string.fail_to_logout), Toast.LENGTH_LONG)
              .show();
          }
        });
    }
    else {
      preferenceCategory.removePreference(logout);
      signInAccount = null;
      Toast.makeText(activity, getString(R.string.logout_done), Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    super.onActivityResult(requestCode, resultCode, data);

    switch(requestCode) {

      case REQUEST_CODE_SIGN_IN: {

        if(resultCode != Activity.RESULT_OK) {
          return;
        }

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

        try {
          signInAccount = task.getResult(ApiException.class);
          preferenceCategory.addPreference(logout);

          setDriveServiceHelper();
          if(is_backup) {
            backupToDrive();
          }
          else {
            restoreFromDrive();
          }
        }
        catch(ApiException e) {

          e.printStackTrace();
          Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_LONG).show();
        }

        break;
      }
    }
  }

  private void setDriveServiceHelper() {

    if(signInAccount != null) {
      GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
        activity, Collections.singleton(DriveScopes.DRIVE_FILE));

      Account account = signInAccount.getAccount();
      requireNonNull(account);
      credential.setSelectedAccount(account);

      Drive googleDriveService = new Drive.Builder(
        AndroidHttp.newCompatibleTransport(),
        new GsonFactory(),
        credential
      )
        .setApplicationName("KK Reminder")
        .build();

      driveServiceHelper = new DriveServiceHelper(googleDriveService);
    }
  }
}