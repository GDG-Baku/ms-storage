package az.gdg.msstorage.service;

import java.util.Map;

import org.json.simple.JSONObject;

public interface LinkFetcher {

    Map<String, String> getImages();

    JSONObject getTermsAndConditions();
}
