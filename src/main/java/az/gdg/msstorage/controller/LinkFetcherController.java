package az.gdg.msstorage.controller;

import az.gdg.msstorage.service.LinkFetcher;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/storage")
public class LinkFetcherController {
    private static final Logger logger = LoggerFactory.getLogger(LinkFetcherController.class);

    private final LinkFetcher linkFetcher;

    public LinkFetcherController(LinkFetcher linkFetcher) {
        this.linkFetcher = linkFetcher;
    }

    @CrossOrigin(exposedHeaders = "Access-Control-Allow-Origin")
    @GetMapping("/team-images")
    public ResponseEntity<Map<String, String>> getImages() {
        logger.debug("Get images start");
        return new ResponseEntity<>(linkFetcher.getImages(), HttpStatus.OK);
    }


    @CrossOrigin(exposedHeaders = "Access-Control-Allow-Origin")
    @GetMapping("/terms-and-conditions")
    public ResponseEntity<JSONObject> getTermsAndConditions() {
        logger.debug("Get terms and conditions start");
        return new ResponseEntity<>(linkFetcher.getTermsAndConditions(), HttpStatus.OK);
    }

    @CrossOrigin(exposedHeaders = "Access-Control-Allow-Origin")
    @PostMapping("/upload")
    public ResponseEntity<JSONObject> uploadFile(@RequestParam String folderName,
                                                 @RequestPart MultipartFile multipartFile) {
        logger.debug("Upload file to folderName {} start", folderName);
        return new ResponseEntity<>(linkFetcher.uploadFile(folderName, multipartFile), HttpStatus.OK);
    }

    @CrossOrigin(exposedHeaders = "Access-Control-Allow-Origin")
    @DeleteMapping("/delete/{id}")
    public void deleteFile(@PathVariable String id) {
        logger.debug("Delete file by id {} start", id);
        linkFetcher.deleteFile(id);
    }
}

