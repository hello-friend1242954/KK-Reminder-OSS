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
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import bolts.Continuation;
import bolts.TaskCompletionSource;

import static com.hideaki.kk_reminder.MyDatabaseHelper.DATABASE;
import static com.hideaki.kk_reminder.UtilClass.BOOLEAN_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.INT_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.MENU_POSITION;
import static com.hideaki.kk_reminder.UtilClass.REQUEST_CODE_SIGN_IN;
import static com.hideaki.kk_reminder.UtilClass.STRING_GENERAL;
import static com.hideaki.kk_reminder.UtilClass.SUBMENU_POSITION;
import static java.util.Objects.requireNonNull;

public class BackupAndRestoreFragment extends BasePreferenceFragmentCompat
  implements Preference.OnPreferenceClickListener {

  @SuppressLint("SdCardPath")
  private static final String ROOT_FOLDER_NAME = "KK_Reminder_Backup";
  private static final String MIME_TYPE_DATABASE = "application/x-sqlite-3";
  private static final String MIME_TYPE_XML = "application/xml";
  private static final String SUCCESS = "SUCCESS";
  private static final String NO_FILE = "NO_FILE";

  private MainActivity activity;
  private PreferenceCategory preferenceCategory;
  private GoogleSignInAccount signInAccount;
  private GoogleSignInClient signInClient;
  private PreferenceScreen logout;
  private boolean isBackup;
  private int choice;
  private DriveServiceHelper driveServiceHelper;
  private BackupAndRestoreProgressBarDialogFragment backupAndRestoreDialog;
  private Map<String, String> backupFilesMap;

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
          isBackup = true;
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
          isBackup = false;
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

      driveServiceHelper
        .queryFolder(ROOT_FOLDER_NAME)
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
              // データベースのバックアップファイルの検索
              driveServiceHelper
                .queryFiles(MIME_TYPE_DATABASE)
                .addOnSuccessListener(new OnSuccessListener<List<FileList>>() {
                  @Override
                  public void onSuccess(List<FileList> fileLists) {

                    backupFilesMap = new LinkedHashMap<>();
                    for(FileList list : fileLists) {
                      for(File file : list.getFiles()) {
                        backupFilesMap.put(file.getName(), file.getId());
                      }
                    }
                    if(backupFilesMap.isEmpty()) {
                      Toast.makeText(
                        activity, getString(R.string.backup_not_exists), Toast.LENGTH_LONG
                      ).show();
                    }
                    else {
                      displayDialog();
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

  private void displayDialog() {

    final List<String> targetItemList = new ArrayList<>();
    List<String> itemList = new ArrayList<>();
    for(String key : backupFilesMap.keySet()) {
      if(key.contains(DATABASE)) {
        key = key.substring(0, key.length() - DATABASE.length() - 1);
      }
      else {
        key = key.substring(0, key.length() - 3);
      }
      targetItemList.add(key);
      StringBuilder title = new StringBuilder(key);
      title.setCharAt(4, '/');
      title.setCharAt(7, '/');
      title.setCharAt(10, ' ');
      title.setCharAt(13, ':');
      title.setCharAt(16, ':');
      itemList.add(title.toString());
    }

    choice = 0;
    final String[] items = itemList.toArray(new String[0]);
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

          backupAndRestoreDialog =
            new BackupAndRestoreProgressBarDialogFragment(false);
          backupAndRestoreDialog.show(
            activity.getSupportFragmentManager(), "restore_progress_bar"
          );

          choice = SingleChoiceItemsAdapter.checkedPosition;

          // SharedPreferencesファイルのクエリ
          final String targetBackupDateStr = targetItemList.get(choice);
          driveServiceHelper
            .queryFiles(MIME_TYPE_XML)
            .addOnSuccessListener(new OnSuccessListener<List<FileList>>() {
              @Override
              public void onSuccess(List<FileList> fileLists) {

                for(FileList list : fileLists) {
                  for(File file : list.getFiles()) {
                    backupFilesMap.put(file.getName(), file.getId());
                  }
                }
                backupAndRestoreDialog.setProgress(20);

                List<bolts.Task<String>> tasks = new ArrayList<>();

                for(String key : backupFilesMap.keySet()) {
                  if(key.contains(targetBackupDateStr)) {
                    if(key.contains(".db")) {
                      // データベースファイルの復元
                      String databasePath =
                        activity.getDatabasePath(DATABASE).getAbsolutePath();
                      tasks.add(readDriveFile(backupFilesMap.get(key), databasePath));
                    }
                    else {
                      // SharedPreferencesファイルの復元
                      final String sharedPreferencesDirectory =
                        activity.getFilesDir().getParent() + "/shared_prefs/";
                      final String[] sharedPreferencesFiles =
                        {INT_GENERAL, BOOLEAN_GENERAL, STRING_GENERAL};
                      for(String sharedPreferencesFile : sharedPreferencesFiles) {
                        if(key.contains(sharedPreferencesFile)) {
                          String sharedPreferencesPath =
                            sharedPreferencesDirectory + sharedPreferencesFile + ".xml";

                          tasks.add(readDriveFile(backupFilesMap.get(key), sharedPreferencesPath));
                          break;
                        }
                      }
                    }
                  }
                }

                bolts.Task
                  .whenAllResult(tasks)
                  .onSuccess(new Continuation<List<String>, Void>() {
                    @Override
                    public Void then(bolts.Task<List<String>> task) {

                      if(backupAndRestoreDialog.getProgress() != 100) {
                        backupAndRestoreDialog.setProgress(100);
                      }
                      List<String> results = task.getResult();
                      for(String result : results) {
                        Log.i("readDriveFile", result);
                      }

                      Toast
                        .makeText(
                          activity,
                          getString(R.string.success_to_restore),
                          Toast.LENGTH_LONG
                        )
                        .show();

                      return null;
                    }
                  })
                  .continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(bolts.Task<Void> task) {

                      if(task.isFaulted()) {
                        backupAndRestoreDialog.dismiss();
                        Toast
                          .makeText(
                            activity,
                            getString(R.string.fail_to_restore),
                            Toast.LENGTH_LONG
                          )
                          .show();

                        activity.setIntGeneralInSharedPreferences(MENU_POSITION, 0);
                        activity.setIntGeneralInSharedPreferences(SUBMENU_POSITION, 0);
                        activity.menuItem = activity.menu.getItem(activity.whichMenuOpen);
                        new Handler().postDelayed(new Runnable() {
                          @Override
                          public void run() {

                            activity.navigationView.setCheckedItem(activity.menuItem);
                            activity.recreate();
                          }
                        }, 3500);
                      }
                      return null;
                    }
                  });
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {

                backupAndRestoreDialog.dismiss();
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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
      }
    });

    dialog.show();
  }

  private bolts.Task<String> readDriveFile(String fileId, final String path) {

    if(driveServiceHelper != null) {
      final TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

      java.io.File file = new java.io.File(path);
      driveServiceHelper
        .downloadFile(file, fileId)
        .addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {

            String resultStr;
            if(path.contains(DATABASE)) {
              resultStr = DATABASE;
            }
            else if(path.contains(INT_GENERAL)) {
              resultStr = INT_GENERAL;
            }
            else if(path.contains(BOOLEAN_GENERAL)) {
              resultStr = BOOLEAN_GENERAL;
            }
            else if(path.contains(STRING_GENERAL)) {
              resultStr = STRING_GENERAL;
            }
            else {
              throw new IllegalArgumentException("Arg path is fraud value: " + path);
            }
            resultStr += ": " + SUCCESS;

            backupAndRestoreDialog.addProgress(20);
            taskCompletionSource.setResult(resultStr);
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {

            taskCompletionSource.setError(new IllegalStateException());
          }
        });

      return taskCompletionSource.getTask();
    }
    else {
      return bolts.Task.forError(new IllegalStateException());
    }
  }

  private void backupToDrive() {

    if(driveServiceHelper != null) {

      driveServiceHelper
        .queryFolder(ROOT_FOLDER_NAME)
        .addOnSuccessListener(new OnSuccessListener<FileList>() {

          @Override
          public void onSuccess(FileList fileList) {

            List<File> folderList = fileList.getFiles();
            if(folderList.size() == 0) {

              driveServiceHelper
                .createFolder(ROOT_FOLDER_NAME, null)
                .addOnSuccessListener(new OnSuccessListener<File>() {
                  @Override
                  public void onSuccess(File file) {

                    createBackupFolder(file.getId());
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
              createBackupFolder(folderList.get(0).getId());
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

  private void createBackupFolder(final String folderId) {

    if(driveServiceHelper != null) {

      backupAndRestoreDialog = new BackupAndRestoreProgressBarDialogFragment(true);
      backupAndRestoreDialog.show(activity.getSupportFragmentManager(), "backup_progress_bar");

      final String backupFolderName
        = DateFormat.format("yyyy_MM_dd_kk_mm_ss", new Date()).toString();

      driveServiceHelper
        .createFolder(backupFolderName, folderId)
        .addOnSuccessListener(new OnSuccessListener<File>() {
          @Override
          public void onSuccess(final File file) {

            backupAndRestoreDialog.setProgress(20);

            List<bolts.Task<String>> tasks = new ArrayList<>();

            // データベースファイルのバックアップ
            String databasePath =
              activity.getDatabasePath(DATABASE).getAbsolutePath();
            tasks.add(
              createBackupFile(file.getId(), backupFolderName, databasePath, MIME_TYPE_DATABASE)
            );
            // SharedPreferencesファイルのバックアップ
            final String sharedPreferencesDirectory =
              activity.getFilesDir().getParent() + "/shared_prefs/";
            final String[] sharedPreferencesFiles = {INT_GENERAL, BOOLEAN_GENERAL, STRING_GENERAL};
            for(String sharedPreferencesFile : sharedPreferencesFiles) {
              String sharedPreferencesPath =
                sharedPreferencesDirectory + sharedPreferencesFile + ".xml";
              tasks.add(
                createBackupFile(
                  file.getId(),
                  backupFolderName,
                  sharedPreferencesPath,
                  MIME_TYPE_XML
                )
              );
            }

            bolts.Task
              .whenAllResult(tasks)
              .onSuccess(new Continuation<List<String>, Void>() {
                @Override
                public Void then(bolts.Task<List<String>> task) {

                  List<String> results = task.getResult();
                  int size = results.size();
                  for(int i = 0; i < size; i++) {
                    String result = results.get(i);
                    if(i == 0) {
                      String resultStr = DATABASE + ": " + result;
                      if(SUCCESS.equals(result)) {
                        Log.i("createBackupFile", resultStr);
                      }
                      else if(NO_FILE.equals(result)) {
                        Log.w("createBackupFile", resultStr);
                        throw new IllegalStateException("Database file not exist");
                      }
                    }
                    else {
                      String resultStr = sharedPreferencesFiles[i - 1] + ": " + result;
                      if(SUCCESS.equals(result)) {
                        Log.i("createBackupFile", resultStr);
                      }
                      else if(NO_FILE.equals(result)) {
                        Log.w("createBackupFile", resultStr);
                      }
                    }
                  }

                  Toast
                    .makeText(activity, getString(R.string.create_new_backup), Toast.LENGTH_LONG)
                    .show();
                  return null;
                }
              })
              .continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(bolts.Task<Void> task) {

                  if(task.isFaulted()) {
                    backupAndRestoreDialog.dismiss();
                    Toast
                      .makeText(
                        activity,
                        getString(R.string.fail_to_create_new_backup),
                        Toast.LENGTH_LONG
                      )
                      .show();
                  }
                  return null;
                }
              });
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {

            backupAndRestoreDialog.dismiss();
            Toast
              .makeText(
                activity,
                getString(R.string.fail_to_make_backup_folder),
                Toast.LENGTH_LONG
              )
              .show();
          }
        });
    }
  }

  private bolts.Task<String> createBackupFile(
    String folderId,
    String backupFolderName,
    final String path,
    String mimeType
  ) {
    if(driveServiceHelper != null) {
      final TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

      java.io.File filePath = new java.io.File(path);
      if(!filePath.exists()) {
        backupAndRestoreDialog.addProgress(20);
        return bolts.Task.forResult(NO_FILE);
      }
      FileContent fileContent = new FileContent(mimeType, filePath);
      String prefix = "_";
      if(path.contains(DATABASE)) {
        prefix += DATABASE;
      }
      else if(path.contains(INT_GENERAL)) {
        prefix += INT_GENERAL + ".xml";
      }
      else if(path.contains(BOOLEAN_GENERAL)) {
        prefix += BOOLEAN_GENERAL + ".xml";
      }
      else if(path.contains(STRING_GENERAL)) {
        prefix += STRING_GENERAL + ".xml";
      }
      else {
        throw new IllegalArgumentException("Arg path is fraud value: " + path);
      }
      String fileName = backupFolderName + prefix;
      driveServiceHelper
        .createFile(fileName, fileContent, folderId)
        .addOnSuccessListener(new OnSuccessListener<File>() {
          @Override
          public void onSuccess(File file) {

            backupAndRestoreDialog.addProgress(20);
            taskCompletionSource.setResult(SUCCESS);
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {

            taskCompletionSource.setError(new IllegalStateException());
          }
        });

      return taskCompletionSource.getTask();
    }
    else {
      return bolts.Task.forError(new IllegalStateException());
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

    if(requestCode == REQUEST_CODE_SIGN_IN) {
      if(resultCode != Activity.RESULT_OK) {
        return;
      }

      Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

      try {
        signInAccount = task.getResult(ApiException.class);
        preferenceCategory.addPreference(logout);

        setDriveServiceHelper();
        if(isBackup) {
          backupToDrive();
        }
        else {
          restoreFromDrive();
        }
      }
      catch(ApiException e) {

        Log.e("BackupFrag#onActResult", Log.getStackTraceString(e));
        Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_LONG).show();
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