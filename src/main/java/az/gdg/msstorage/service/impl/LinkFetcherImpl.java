package az.gdg.msstorage.service.impl;

import az.gdg.msstorage.config.DriveConfig;
import az.gdg.msstorage.exception.FileCreationException;
import az.gdg.msstorage.exception.NoFilesFoundException;
import az.gdg.msstorage.service.LinkFetcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

@Service
public class LinkFetcherImpl implements LinkFetcher {

    private static final Logger logger = LoggerFactory.getLogger(LinkFetcherImpl.class);
    private static final String BASE_LINK = "https://drive.google.com/uc?id=";

    @Override
    public Map<String, String> getImages() {
        logger.info("ActionLog.getImages.start");
        String folderName = "team-images";
        String mimeType = "image/";
        List<File> files;
        Map<String, String> links = new HashMap<>();
        try {
            files = getFilesInParticularFolder(folderName, mimeType);
            for (File f : files) {
                if (Boolean.FALSE.equals(f.getTrashed())) {
                    String link = BASE_LINK + f.getId();
                    links.put(f.getName(), link);
                }
            }
            logger.info("ActionLog.getImages.success");
        } catch (IOException exception) {
            logger.error("ActionLog.getImages.exception", exception);
        }
        logger.info("ActionLog.getImages.end");
        return links;
    }

    @Override
    public JSONObject getTermsAndConditions() {
        logger.info("ActionLog.getTermsAndConditions.start");
        String folderName = "GDG Terms and Conditions";
        String mimeType = "application/pdf";
        JSONObject jsonObject = new JSONObject();
        File file;
        try {
            file = getFilesInParticularFolder(folderName, mimeType).get(0);
            if (Boolean.FALSE.equals(file.getTrashed())) {
                jsonObject.put("termsAndConditions", BASE_LINK + file.getId());
                logger.info("ActionLog.getTermsAndConditions.success");
                return jsonObject;
            }
        } catch (IOException exception) {
            logger.error("ActionLog.getTermsAndConditions.exception", exception);
        }
        logger.info("ActionLog.getTermsAndConditions.end");
        return jsonObject;
    }

    @Override
    public JSONObject uploadFile(String folderName, MultipartFile multipartFile) {
        logger.info("ActionLog.uploadFile.start");
        DriveConfig driveConfig = new DriveConfig();
        JSONObject jsonObject = new JSONObject();
        String fileName = UUID.randomUUID().toString();
        String parentFolderId = getFolderIdByName(folderName);

        if (parentFolderId == null || parentFolderId.equals("")) {
            parentFolderId = createFolder(folderName);
        }

        File fileMetadata = new File();
        fileMetadata.setName(folderName + "-" + fileName);
        fileMetadata.setParents(Collections.singletonList(parentFolderId));

        File file = null;
        try {
            file = driveConfig.getDrive().files().create(fileMetadata,
                    new InputStreamContent(multipartFile.getContentType(),
                            new ByteArrayInputStream(multipartFile.getBytes())))
                    .setFields("id")
                    .execute();
        } catch (IOException exception) {
            logger.error("ActionLog.uploadFile.exception", exception);
        }

        if (file != null) {
            jsonObject.put("imageUrl", BASE_LINK + file.getId());
            logger.info("ActionLog.uploadFile.success");
            return jsonObject;
        }
        logger.info("ActionLog.uploadFile.end");
        throw new FileCreationException("File couldn't be created");
    }

    @Override
    public void deleteFile(String id) {
        logger.info("ActionLog.deleteFile.start with id {}", id);
        DriveConfig driveConfig = new DriveConfig();
        try {
            driveConfig.getDrive().files().delete(id).execute();
            logger.info("ActionLog.deleteFile.success with id {}", id);
        } catch (IOException e) {
            logger.error("ActionLog.deleteFile.exception", e);
        }
        logger.info("ActionLog.deleteFile.end with id {}", id);
    }

    public String createFolder(String folderName) {
        logger.info("ActionLog.createFolder.start with folderName {}", folderName);
        DriveConfig driveConfig = new DriveConfig();

        Permission adminPermission = new Permission();
        adminPermission.setRole("reader");
        adminPermission.setType("user");
        adminPermission.setEmailAddress("gdg.rubber.duck@gmail.com");

        Permission userPermission = new Permission();
        userPermission.setRole("reader");
        userPermission.setType("anyone");

        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = null;
        try {
            file = driveConfig.getDrive().files().create(fileMetadata).execute();
            driveConfig.getDrive().permissions().create(file.getId(), adminPermission).execute();
            driveConfig.getDrive().permissions().create(file.getId(), userPermission).execute();
        } catch (IOException exception) {
            logger.error("ActionLog.createFolder.exception", exception);
        }
        if (file != null) {
            logger.info("ActionLog.createFolder.success with folderName {}", folderName);
            return file.getId();
        }
        logger.info("ActionLog.createFolder.end with folderName {}", folderName);
        throw new FileCreationException("Folder couldn't be created");
    }

    private List<File> getFilesInParticularFolder(String folderName, String mimeType) throws IOException {
        logger.info("ActionLog.getFilesInParticularFolder.start with folderName {}", folderName);
        DriveConfig driveConfig = new DriveConfig();
        String folderId = getFolderIdByName(folderName);

        FileList result = driveConfig.getDrive().files().list()
                .setQ("'" + folderId + "' in parents and mimeType contains '" + mimeType + "'")
                .setFields("nextPageToken, files(id, trashed, name, mimeType)")
                .execute();

        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            logger.info("ActionLog.getFilesInParticularFolder.success with folderName {}", folderName);
            return files;
        }
        logger.info("ActionLog.getFilesInParticularFolder.end with folderName {}", folderName);
        throw new NoFilesFoundException("There is no file in requested folder");
    }

    private String getFolderIdByName(String folderName) {
        logger.info("ActionLog.getFolderIdByName.start with folderName {}", folderName);
        DriveConfig driveConfig = new DriveConfig();
        FileList folders = null;
        try {
            folders = driveConfig.getDrive().files().list()
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false")
                    .setPageSize(1)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();

        } catch (IOException exception) {
            logger.error("ActionLog.getFolderIdByName.exception", exception);
        }
        if (folders != null && !folders.getFiles().isEmpty()) {
            logger.info("ActionLog.getFolderIdByName.success with folderName {}", folderName);
            return folders.getFiles().get(0).getId();
        }
        logger.info("ActionLog.getFolderIdByName.end with folderName {}", folderName);
        return "";
    }
}
