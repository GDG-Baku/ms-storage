package az.gdg.msstorage.service;

import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.web.multipart.MultipartFile;

public interface LinkFetcher {

    Map<String, String> getImages();

    JSONObject getTermsAndConditions();

    JSONObject uploadFile(String folderName, MultipartFile multipartFile);

    void deleteFile(String id);
}
