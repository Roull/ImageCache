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
import java.util.List;

/**
 * Utils class that provides helper methods to download objects, read files, write files etc.
 */
public final class FileIOUtils {

    private static final Logger log = LoggerFactory.getLogger(FileIOUtils.class);

    /**
     * Reads the inputfile line by line and adds it to the list.
     *
     * @param fileName path of the file to read input from.
     *
     * @return List of strings that represents the input from reading the inputfiles.
     *
     * @throws  {@link RuntimeException} if unable to read from input file.
     */
    public static List<String> readFileToList(@NonNull String fileName) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to read input file from file %s due to %s", fileName, e.getCause()));
        }
        return lines;
    }

    /**
     * Writes the given result data objects to the output file.
     *
     * @param fileName file to which the output has to be written to.
     *
     * @param resultDataList list of {@link ResultData} objects that need to be written to output.
     *
     * @throws  {@link RuntimeException} if unable to write output file.
     */
    public static void writeListToFile(@NonNull String fileName, @NonNull List<ResultData> resultDataList) {
        Path path = Paths.get(fileName);
        for(ResultData resultData : resultDataList) {
            try {
                Files.write(path, resultData.toString().getBytes(),
                        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Unable to write output to file %s due to %s", fileName, e.getCause()));
            }
        }
    }

    /**
     * Deletes the resource pointed by given fileName argument.
     *
     * @param fileName fileName which has to be deleted from the disk.
     *
     * @throws {@link IOException} if file/directory could not be deleted.
     */
    public static void deleteIfExists(@NonNull String fileName) throws IOException {
        Path path = Paths.get(fileName);
        Files.deleteIfExists(path);
    }

    /**
     * Deletes file from the file system.
     *
     * @param file File to be deleted from disk.
     *
     * @throws {@link IOException} if file/directory could not be deleted.
     */
    public static void removeFile(@NonNull File file) throws IOException {
        if (Files.deleteIfExists(file.toPath())) {
            log.info("file removed from location " + file.getPath());
        } else {
            log.info("file does not exist at location" + file.getPath());
        }
    }
}

