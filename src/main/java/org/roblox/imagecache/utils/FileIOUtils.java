package org.roblox.imagecache.utils;

import lombok.NonNull;
import org.roblox.imagecache.types.ResultData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

/**
 * Utils class that provides helper methods to download objects from web to cache.
 */
public final class FileIOUtils {

    private static final Logger log = LoggerFactory.getLogger(FileIOUtils.class);

    public static List<String> readFileToList(@NonNull String fileName) {
        List<String> lines = Collections.emptyList();
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // TODO: Handle exceptions
            e.printStackTrace();
        }
        return lines;
    }
    public static void writeListToFile(@NonNull String fileName, @NonNull List<ResultData> resultDataList) {
        Path path = Paths.get(fileName);
        for(ResultData resultData : resultDataList) {
            try {
                Files.write(path, resultData.toString().getBytes(),
                        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
            } catch (IOException e) {
                // TODO: Handle exceptions
                e.printStackTrace();
            }
        }
    }
    public static void deleteIfExists(@NonNull String fileName){
        Path path = Paths.get(fileName);
        try {
            Files.deleteIfExists(path);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void removeFile(@NonNull File file) {
        try {
            if (Files.deleteIfExists(file.toPath())) {
                log.info("file removed from location " + file.getPath());
            } else {
                log.info("file does not exist at location" + file.getPath());
            }
        } catch (Exception e) {
            log.error(String.format("removal of file at %s failed", file.getPath()));
            throw new RuntimeException(String.format("removal of temp file at %s failed", file.getPath()));
        }
    }
}
