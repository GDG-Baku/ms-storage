package az.gdg.msstorage.service.impl;

import az.gdg.msstorage.exception.FileCreationException;
import az.gdg.msstorage.exception.NoFilesFoundException;
import az.gdg.msstorage.service.StorageService;
import az.gdg.msstorage.util.DriveUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

@Service
public class StorageServiceImpl implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageServiceImpl.class);
    private static final String BASE_LINK = "https://drive.google.com/uc?id=";
    private static final Drive DRIVE = DriveUtil.getDrive();

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
    public JSONObject uploadFile(String folderName, List<MultipartFile> multipartFiles) {
        logger.info("ActionLog.uploadFile.start");
        JSONObject jsonObject = new JSONObject();
        List<String> fileUrls = null;
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
            fileUrls = new ArrayList<>();
            for (MultipartFile multipartFile : multipartFiles) {
                file = DRIVE.files().create(fileMetadata,
                        new InputStreamContent(multipartFile.getContentType(),
                                new ByteArrayInputStream(multipartFile.getBytes())))
                        .setFields("id")
                        .execute();
                fileUrls.add(BASE_LINK + file.getId());
            }

        } catch (IOException exception) {
            logger.error("ActionLog.uploadFile.exception", exception);
        }

        if (file != null) {
            jsonObject.put("imageUrl", fileUrls);
            logger.info("ActionLog.uploadFile.success");
            return jsonObject;
        }
        logger.info("ActionLog.uploadFile.end");
        throw new FileCreationException("File couldn't be created");
    }

    @Override
    public void deleteFile(String id) {
        logger.info("ActionLog.deleteFile.start with id {}", id);
        try {
            if (getFileById(id) != null) {
                DRIVE.files().delete(id).execute();
            } else {
                throw new NoFilesFoundException("File doesn't exist with this id " + id);
            }
            logger.info("ActionLog.deleteFile.success with id {}", id);
        } catch (IOException exception) {
            logger.error("ActionLog.deleteFile.exception", exception);
        }
        logger.info("ActionLog.deleteFile.end with id {}", id);
    }

    @Override
    public void trashFile(String id) {
        logger.info("ActionLog.trashFile.start with id {}", id);
        File file = getFileById(id);
        if (file != null) {
            file.setTrashed(true);
            logger.info("ActionLog.trashFile.success with id {}", id);
        } else {
            throw new NoFilesFoundException("File doesn't exist with this id " + id);
        }
        logger.info("ActionLog.trashFile.end with id {}", id);
    }

    private File getFileById(String id) {
        logger.info("ActionLog.isFileExist.start with id {}", id);
        File file = null;
        try {
            file = DRIVE.files().get(id).execute();
        } catch (IOException exception) {
            logger.error("ActionLog.isFileExist.exception", exception);
        }
        logger.info("ActionLog.isFileExist.end with id {}", id);
        return file;
    }

    public String createFolder(String folderName) {
        logger.info("ActionLog.createFolder.start with folderName {}", folderName);

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
            file = DRIVE.files().create(fileMetadata).execute();
            DRIVE.permissions().create(file.getId(), adminPermission).execute();
            DRIVE.permissions().create(file.getId(), userPermission).execute();
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
        String folderId = getFolderIdByName(folderName);

        FileList result = DRIVE.files().list()
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
        FileList folders = null;
        try {
            folders = DRIVE.files().list()
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='" +
                            folderName + "' and trashed=false")
                    .setPageSize(1)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();

        } catch (IOException exception) {
            logger.error("ActionLog.getFolderIdByName.exception", exception);
        }
        if (folders != null && !folders.getFiles().isEmpty()) {
            return folders.getFiles().get(0).getId();
        }
        logger.info("ActionLog.getFolderIdByName.end with folderName {}", folderName);
        return "";
    }
}
