package com.example.gestiondecompras.utils;

import android.content.Context;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveService {

    private final Drive driveService;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private static final String DRIVE_FILE_NAME = "GestionCompras_Backup.db";

    public GoogleDriveService(Context context, String accountName) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccountName(accountName);

        driveService = new Drive.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("Gestión de Compras")
                .build();
    }

    public interface DriveCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public void uploadBackup(java.io.File localFile, DriveCallback<String> callback) {
        executor.execute(() -> {
            try {
                callback.onSuccess(uploadBackupSync(localFile));
            } catch (IOException e) {
                callback.onError(e);
            }
        });
    }

    /** Sube (o reemplaza) la copia en Drive de forma síncrona. No llamar desde el hilo principal. */
    public String uploadBackupSync(java.io.File localFile) throws IOException {
        FileList result = driveService.files().list()
                .setQ("name = '" + DRIVE_FILE_NAME + "' and trashed = false")
                .setSpaces("drive")
                .execute();

        File fileMetadata = new File();
        fileMetadata.setName(DRIVE_FILE_NAME);

        FileContent mediaContent = new FileContent("application/octet-stream", localFile);

        File uploadedFile;
        if (result.getFiles().isEmpty()) {
            uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
        } else {
            String existingId = result.getFiles().get(0).getId();
            uploadedFile = driveService.files().update(existingId, null, mediaContent)
                    .setFields("id")
                    .execute();
        }
        return uploadedFile.getId();
    }

    public void downloadBackup(java.io.File localFile, DriveCallback<Void> callback) {
        executor.execute(() -> {
            try {
                FileList result = driveService.files().list()
                        .setQ("name = '" + DRIVE_FILE_NAME + "' and trashed = false")
                        .setSpaces("drive")
                        .execute();

                if (result.getFiles().isEmpty()) {
                    callback.onError(new IOException("No se encontró copia de seguridad en Google Drive"));
                    return;
                }

                String fileId = result.getFiles().get(0).getId();
                try (OutputStream outputStream = new FileOutputStream(localFile)) {
                    // Configurar la descarga directa para evitar el error 416
                    Drive.Files.Get getRequest = driveService.files().get(fileId);
                    getRequest.getMediaHttpDownloader().setDirectDownloadEnabled(true);
                    getRequest.executeMediaAndDownloadTo(outputStream);
                }
                callback.onSuccess(null);
            } catch (IOException e) {
                callback.onError(e);
            }
        });
    }
}
