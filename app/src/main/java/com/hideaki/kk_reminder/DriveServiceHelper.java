package com.hideaki.kk_reminder;

import androidx.annotation.Nullable;

import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class DriveServiceHelper {

  private final Executor executor = Executors.newSingleThreadExecutor();
  private final Drive driveService;

  DriveServiceHelper(Drive driveService) {

    this.driveService = driveService;
  }

  Task<List<FileList>> queryFiles(final String mimeType) {

    return Tasks.call(executor, new Callable<List<FileList>>() {

      @Override
      public List<FileList> call() throws Exception {

        List<FileList> fileLists = new ArrayList<>();
        String pageToken = null;
        do {
          FileList result = driveService.files().list()
              .setQ("mimeType = '" + mimeType + "' and trashed = false")
              .setSpaces("drive")
              .setFields("nextPageToken, files(id, name)")
              .setPageToken(pageToken)
              .execute();

          fileLists.add(result);
          pageToken = result.getNextPageToken();
        }
        while(pageToken != null);

        return fileLists;
      }
    });
  }

  Task<FileList> queryFolder(final String folderName) {

    return Tasks.call(executor, new Callable<FileList>() {

      @Override
      public FileList call() throws Exception {

        return driveService.files().list()
            .setQ("mimeType = '" + DriveFolder.MIME_TYPE + "' and name = '" + folderName + "'"
                + " and trashed = false")
            .setSpaces("drive")
            .execute();
      }
    });
  }

  Task<File> createFile(
      final String fileName, final AbstractInputStreamContent content,
      @Nullable final String folderId
  ) {

    return Tasks.call(executor, new Callable<File>() {
      @Override
      public File call() throws Exception {

        List<String> root;
        if(folderId == null) {
          root = Collections.singletonList("root");
        }
        else {
          root = Collections.singletonList(folderId);
        }

        File metadata = new File()
            .setParents(root)
            .setName(fileName);

        File file = driveService.files().create(metadata, content).execute();
        if(file == null) {
          throw new IOException("Null result when requesting file creation.");
        }

        return file;
      }
    });
  }

  Task<File> createFolder(final String folderName, @Nullable final String folderId) {

    return Tasks.call(executor, new Callable<File>() {
      @Override
      public File call() throws Exception {

        List<String> root;
        if(folderId == null) {
          root = Collections.singletonList("root");
        }
        else {
          root = Collections.singletonList(folderId);
        }

        File metadata = new File()
            .setParents(root)
            .setMimeType(DriveFolder.MIME_TYPE)
            .setName(folderName);

        File file;
        file = driveService.files().create(metadata).execute();
        if(file == null) {
          throw new IOException("Null result when requesting folder creation.");
        }

        return file;
      }
    });
  }

  Task<Void> downloadFile(final java.io.File targetFile, final String fileId) {

    return Tasks.call(executor, new Callable<Void>() {
      @Override
      public Void call() throws Exception {

        OutputStream outputStream = new FileOutputStream(targetFile);
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);

        return null;
      }
    });
  }
}