package az.gdg.msstorage.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

public class DriveUtil {

    private DriveUtil() {

    }

    private static final Logger logger = LoggerFactory.getLogger(DriveUtil.class);

    private static final String APPLICATION_NAME = "ms-storage";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);


    private static GoogleCredential getCredentials() throws IOException {
        String st = System.getenv("GOOGLE_CREDENTIALS");
        InputStream stream = new ByteArrayInputStream(st.getBytes());
        return GoogleCredential.fromStream(stream).createScoped(SCOPES);
    }

    public static Drive getDrive() {
        logger.info("ActionLog.getDrive.start");
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Drive drive = new Drive.Builder(httpTransport, JSON_FACTORY, getCredentials())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            logger.info("ActionLog.getDrive.success");
            return drive;
        } catch (GeneralSecurityException | IOException exception) {
            logger.info("ActionLog.getDrive.end");
            return null;
        }
    }
}
