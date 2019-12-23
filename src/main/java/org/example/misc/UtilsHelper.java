package org.example.misc;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import static org.roblox.imagecache.utils.FileIOUtils.removeFile;

public class UtilsHelper {
    private static final Logger log = LoggerFactory.getLogger(UtilsHelper.class);
    private static final int CONNECTION_TIME_OUT_IN_MILLIS = 60000;
    private static final int READ_TIME_OUT_IN_MILLIS = 60000;
    private static final int MAX_RETRIES = 3;
    /*
     * This function is used to download images from web URL
     * and save it to disk.
     */
    public void copyURLToFile(URL url, File destinationArtifactFile) throws IOException {

        File tempArtifactFile = generateTempFile(destinationArtifactFile);

        int retries = 0;
        boolean retry = false;

        do {
            try {
                log.info(String.format("downloading from web to " + destinationArtifactFile.toPath()));

                FileUtils.copyURLToFile(url, tempArtifactFile, CONNECTION_TIME_OUT_IN_MILLIS, READ_TIME_OUT_IN_MILLIS);
                retry = false;
            } catch (Exception e) {
                removeFile(tempArtifactFile);

                if (retries == MAX_RETRIES - 1) {
                    //log.error(String.format("downloading from web has failed after %d retries: ", MAX_RETRIES), e);
                    throw new RuntimeException(
                            String.format("downloading from s3 location %s has failed", url.toString()), e);
                } else {
                    log.info("downloading from web has failed, retrying");
                    retry = true;
                }
            }
        } while (retry && retries++ < MAX_RETRIES);

        log.info(String.format("successfully downloaded from web to " + destinationArtifactFile.toPath()));

        writeFile(tempArtifactFile, destinationArtifactFile);
    }

    private static File generateTempFile(File file) {
        return new File(file.getPath() + UUID.randomUUID().toString());
    }

    /*
     * Helper function to rename temp file to destination file, and remove temp
     * file if destination file already exists
     */
    private static void writeFile(File tempFile, File destinationFile) throws IOException {
        try {
            FileUtils.moveFile(tempFile, destinationFile);
        } catch (FileExistsException e) {
            // destination file already exists, delete temp file
            log.info(String.format("file already exists for resource data at location %s", destinationFile.getPath()));
            removeFile(tempFile);
        }
    }
}
