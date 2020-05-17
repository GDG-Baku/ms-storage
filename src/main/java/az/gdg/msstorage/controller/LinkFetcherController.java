package az.gdg.msstorage.controller;

import az.gdg.msstorage.service.LinkFetcher;

import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/storage")
public class LinkFetcherController {

    private final LinkFetcher linkFetcher;

    public LinkFetcherController(LinkFetcher linkFetcher) {
        this.linkFetcher = linkFetcher;
    }

    @CrossOrigin(exposedHeaders = "Access-Control-Allow-Origin")
    @GetMapping("/team-images")
    public ResponseEntity getImages() {
        return new ResponseEntity<Map>(linkFetcher.getImages(), HttpStatus.OK);
    }


    @CrossOrigin(exposedHeaders = "Access-Control-Allow-Origin")
    @GetMapping("/terms-and-conditions")
    public ResponseEntity getTermsAndConditions() {
        return new ResponseEntity<JSONObject>(linkFetcher.getTermsAndConditions(), HttpStatus.OK);
    }

}

