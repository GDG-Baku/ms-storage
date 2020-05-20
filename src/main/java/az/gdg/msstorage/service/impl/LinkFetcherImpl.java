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

    @Override
    public Map<String, String> getImages() {
        String folderName = "team-images";
        String mimeType = "image/";
        List<File> files;
        Map<String, String> links = new HashMap<>();
        String baseLink = "https://drive.google.com/uc?id=";

        try {
            files = getFilesInParticularFolder(folderName, mimeType);
            for (File f : files) {
                if (Boolean.FALSE.equals(f.getTrashed())) {
                    String link = baseLink + f.getId();
                    logger.info(f.getName());
                    links.put(f.getName(), link);
                    logger.info(f.getWebViewLink());
                }
            }
        } catch (IOException exception) {
            exception.getStackTrace();
        }
        return links;
    }

    @Override
    public JSONObject getTermsAndConditions() {
        String folderName = "GDG Terms and Conditions";
        String mimeType = "application/pdf";
        String baseLink = "https://drive.google.com/uc?id=";
        JSONObject jsonObject = new JSONObject();
        File file;
        try {
            file = getFilesInParticularFolder(folderName, mimeType).get(0);
            if (Boolean.FALSE.equals(file.getTrashed())) {
                jsonObject.put("termsAndConditions", baseLink + file.getId());
                return jsonObject;
            }
        } catch (IOException exception) {
            exception.getStackTrace();
        }
        return jsonObject;
    }

    @Override
    public JSONObject uploadFile(String folderName, MultipartFile multipartFile) {
        DriveConfig driveConfig = new DriveConfig();
        JSONObject jsonObject = new JSONObject();
        String uploadedFileLink = "https://drive.google.com/uc?id=";
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file != null) {
            jsonObject.put("imageUrl", uploadedFileLink + file.getId());
            return jsonObject;
        }
        throw new FileCreationException("File couldn't be created");
    }

    public String createFolder(String folderName) {
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
            exception.printStackTrace();
        }
        if (file != null) {
            return file.getId();
        }
        throw new FileCreationException("Folder couldn't be created");
    }

    private List<File> getFilesInParticularFolder(String folderName, String mimeType) throws IOException {
        DriveConfig driveConfig = new DriveConfig();
        String folderId = getFolderIdByName(folderName);

        FileList result = driveConfig.getDrive().files().list()
                .setQ("'" + folderId + "' in parents and mimeType contains '" + mimeType + "'")
                .setFields("nextPageToken, files(id, trashed, name, mimeType)")
                .execute();

        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files;
        }
        throw new NoFilesFoundException("There is no file in requested folder");
    }

    private String getFolderIdByName(String folderName) {
        DriveConfig driveConfig = new DriveConfig();
        FileList folders = null;
        try {
            folders = driveConfig.getDrive().files().list()
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false")
                    .setPageSize(1)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();

        } catch (IOException exception) {
            exception.printStackTrace();
        }
        if (folders != null && !folders.getFiles().isEmpty()) {
            return folders.getFiles().get(0).getId();
        }
        return "";
    }
}
