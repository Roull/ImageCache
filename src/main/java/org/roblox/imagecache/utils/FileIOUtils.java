package org.roblox.imagecache.utils;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.roblox.imagecache.types.ResultData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

/**
 * Utils class that provides helper methods to download objects, read files, write files etc.
 */
@NoArgsConstructor
public class FileIOUtils {

    private static final Logger log = LoggerFactory.getLogger(FileIOUtils.class);

    private static final String ENC = "UTF8";
    private static final String DEFAULT_FILENAME = "image";
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * Reads the inputfile line by line and adds it to the list.
     *
     * @param fileName path of the file to read input from.
     *
     * @return List of strings that represents the input from reading the inputfiles.
     *
     * @throws  {@link RuntimeException} if unable to read from input file.
     */
    public List<String> readFileToList(@NonNull final String fileName) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("Expected non-empty fileName to parse");
        }
        final List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch (final IOException e) {
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
     * @throws RuntimeException {@link RuntimeException} if unable to write output file.
     */
    public void writeListToFile(@NonNull final String fileName, @NonNull final List<ResultData> resultDataList) {
        final Path path = Paths.get(fileName);
        for(final ResultData resultData : resultDataList) {
            try {
                Files.write(path, resultData.toString().getBytes(),
                        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
            } catch (final IOException e) {
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
    public void deleteIfExists(@NonNull final String fileName) throws IOException {
        final Path path = Paths.get(fileName);
        Files.deleteIfExists(path);
    }

    /**
     * Deletes file from the file system.
     *
     * @param file File to be deleted from disk.
     *
     * @throws {@link IOException} if file/directory could not be deleted.
     */
    private void removeFile(@NonNull final File file) throws IOException {
        if (Files.deleteIfExists(file.toPath())) {
            log.info("file removed from location " + file.getPath());
        } else {
            log.info("file does not exist at location" + file.getPath());
        }
    }

    public File generateFileLocation(final File repository, final URL url) throws IOException {
        final File parentDirectory = createParentDirectoryInRepository(repository, url);
        return new File(parentDirectory, buildFileName(url));
    }

    private File createParentDirectoryInRepository(final File repository, final URL url) throws IOException {
        final File parentDirectory;
        try {
            parentDirectory = new File(repository, URLEncoder.encode(url.toString(), ENC));
        } catch (final UnsupportedEncodingException e) {
            throw new IOException("Encoding not supported", e);
        }
        return parentDirectory;
    }

    private String buildFileName(final URL url) {
        String fileName;
        try {
            fileName = new File(url.getPath()).getName();
        } catch (final Exception e) {
            fileName = DEFAULT_FILENAME + UUID.randomUUID();
        }
        return fileName;
    }

    /**
     * retries deletion of resource from disk after retrying MAX_RETRY_ATTEMPTS
     *
     * @param resourceToDelete resource to delete from the disk.
     *
     * @return size that is freed up on disk after deletion of items.
     */
    public long deleteResourceOnDisk(@NonNull final File resourceToDelete) throws IOException {
        final long resourceFreeSize = resourceToDelete.length();
        int count = 0;
        while(true) {
            try {
                removeFile(resourceToDelete);
                removeFile(resourceToDelete.getParentFile());
                return resourceFreeSize;
            } catch (final IOException ex) {
                if (++count == MAX_RETRY_COUNT) throw ex;
            }
        }
    }

    public File createRepository(@NonNull final String repo) {
        final File repository = new File(repo);
        if (!repository.exists() && !repository.isDirectory() && !repository.canWrite()) {
            throw new IllegalStateException("Image Cache repository needs to be an existing writable directory: "
                    + repository.getAbsolutePath());
        }
        return repository;
    }
}

