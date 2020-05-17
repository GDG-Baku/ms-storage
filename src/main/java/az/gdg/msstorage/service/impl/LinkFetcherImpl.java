package az.gdg.msstorage.service.impl;

import az.gdg.msstorage.config.DriveConfig;
import az.gdg.msstorage.exception.NoFilesFoundException;
import az.gdg.msstorage.service.LinkFetcher;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

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
            files = getFiles(folderName, mimeType);
            for (File f : files) {
                if (Boolean.FALSE.equals(f.getTrashed())) {
                    String link = baseLink + f.getId();
                    logger.info(f.getName());
                    links.put(f.getName(), link);
                    logger.info(f.getWebViewLink());
                }
            }
        } catch (IOException | GeneralSecurityException exception) {
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
            file = getFiles(folderName, mimeType).get(0);
            if (Boolean.FALSE.equals(file.getTrashed())) {
                jsonObject.put("termsAndConditions", baseLink + file.getId());
                return jsonObject;
            }
        } catch (IOException | GeneralSecurityException exception) {
            exception.getStackTrace();
        }
        return jsonObject;
    }


    private List<File> getFiles(String folderName, String mimeType) throws GeneralSecurityException, IOException {
        DriveConfig driveConfig = new DriveConfig();
        FileList folder = driveConfig.getDrive().files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false")
                .setPageSize(1)
                .setFields("nextPageToken, files(id, name)")
                .execute();

        if (folder.getFiles() != null && !folder.getFiles().isEmpty()) {
            FileList result = driveConfig.getDrive().files().list()
                    .setQ("'" + folder.getFiles().get(0).getId() + "' in parents and mimeType contains '" + mimeType + "'")
                    .setFields("nextPageToken, files(id, trashed, name, mimeType, thumbnailLink, webViewLink)")
                    .execute();

            List<File> files = result.getFiles();

            if (files != null && !files.isEmpty()) {
                return files;
            }
        }
        throw new NoFilesFoundException("There is no file in requested folder");
    }
}
