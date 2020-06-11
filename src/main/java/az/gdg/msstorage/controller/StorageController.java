package az.gdg.msstorage.controller;

import az.gdg.msstorage.service.StorageService;

import java.util.Map;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/storage")
@CrossOrigin(exposedHeaders = "Access-Control-Allow-Origin")
public class StorageController {
    private static final Logger logger = LoggerFactory.getLogger(StorageController.class);

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/team-images")
    public ResponseEntity<Map<String, String>> getImages() {
        logger.debug("Get images start");
        return new ResponseEntity<>(storageService.getImages(), HttpStatus.OK);
    }

    @GetMapping("/terms-and-conditions")
    public ResponseEntity<JSONObject> getTermsAndConditions() {
        logger.debug("Get terms and conditions start");
        return new ResponseEntity<>(storageService.getTermsAndConditions(), HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<JSONObject> uploadFile(@RequestParam String folderName,
                                                 @RequestPart MultipartFile multipartFile) {
        logger.debug("Upload file to folderName {} start", folderName);
        return new ResponseEntity<>(storageService.uploadFile(folderName, multipartFile), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteFile(@PathVariable String id) {
        logger.debug("Delete file by id {} start", id);
        storageService.deleteFile(id);
    }

    @PutMapping("/trash/{id}")
    public void trashFile(@PathVariable String id) {
        logger.debug("Trash file by id {} start", id);
        storageService.trashFile(id);
    }
}

