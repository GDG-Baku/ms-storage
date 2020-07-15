package az.gdg.msstorage.service;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    Map<String, String> getImages();

    JSONObject getTermsAndConditions();

    JSONObject uploadFile(String folderName, List<MultipartFile> multipartFiles);

    void deleteFile(String id);

    void trashFile(String id);
}
