package org.roblox.imagecache.cache;

import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import lombok.NonNull;
import org.roblox.imagecache.types.ResourceData;
import org.roblox.imagecache.types.ResultData;
import org.roblox.imagecache.types.State;
import org.roblox.imagecache.utils.FileIOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Class {@code LRUCache} represents a cache that uses Least Recently Used Item for eviction strategy.
 * More information on the types of Cache Eviction can be found at:
 * <a href=
 * "https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)">
 * <p>
 * This Cache is implemented d by using Java's {@code LinkedHashMap}.
 * It used input provided reporsitory as the path to save the files to.
 * </p>
 */
public class LRUCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(LRUCache.class);

    private static final String ENC = "UTF8";
    private static final String DEFAULT_FILENAME = "image";
    private static final int TIMEOUT_MS = 1*60*1000;  // 1 minutes
    private static final int CONNECT_TIMEOUT_MS = 30*1000;  // 30 seconds

    private final Map<String, ResourceData> linkedHashMap;
    private final File repository;
    private final int maxCapacityInBytes;
    private long currentSizeInBytes;

    public LRUCache(int capacityInBytes, int numberOfItems, String repository) {
        this.repository = new File(repository);
        if (!this.repository.exists() && !this.repository.isDirectory() && !this.repository.canWrite()) {
            throw new IllegalStateException("Image Cache repository needs to be an existing writable directory: "
                    + this.repository.getAbsolutePath());
        }
        maxCapacityInBytes = capacityInBytes;
        currentSizeInBytes = 0;
        linkedHashMap = Collections.synchronizedMap(new LinkedHashMap<>(numberOfItems, 0.75f, true));
    }

    /**
     *
     * @param key
     * @return {@ResultData}
     * @throws IOException
     */
    public ResultData load(@NonNull String key) throws IOException {
        System.out.println("Trying to load object: " + key);
        //System.out.println(linkedHashMap.entrySet());
        if (linkedHashMap.containsKey(key)) {
            ResourceData cahedResource = linkedHashMap.get(key);
            return new ResultData(key, State.CACHED, cahedResource.getOriginalResourceBytes().length);
        } else {
            //download the image by making external service call
            File downloadedResource = downLoadImage(new URL(key));
            Path path = downloadedResource.toPath();
            linkedHashMap.put(key, new ResourceData(key, path.toString(), Files.readAllBytes(path)));
            return new ResultData(key, State.DOWNLOADED, downloadedResource.length());
        }
    }

    private File getFileLocation(URL url) throws IOException {
        File parentDirectory = getParentDirectoryPath(url);
        return new File(parentDirectory, buildFileName(url));
    }

    private File getParentDirectoryPath(URL url) throws IOException {
        File parentDirectory;
        try {
            parentDirectory = new File(repository, URLEncoder.encode(url.toString(), ENC));
        } catch (UnsupportedEncodingException e) {
            throw new IOException("Encoding not supported", e);
        }
        return parentDirectory;
    }

    private String buildFileName(URL url) {
        String fileName;
        try {
            fileName = new File(url.getPath()).getName();
        } catch (Exception e) {
            fileName = DEFAULT_FILENAME + UUID.randomUUID();
        }
        return fileName;
    }

    /**
     * Uses HTTPConnection and downloads the images from web to repo/disk.
     * @return
     */
    public File downLoadImage(URL url) throws IOException {
        HttpURLConnection con = null;
        InputStream source = null;
        OutputStream destination = null;
        try {
            con = getHttpURLConnection(url);
            source = con.getInputStream();
            long sizeOfResourceToDownload = con.getContentLengthLong();
            System.out.println("sizeOfResourceToDownload " + sizeOfResourceToDownload);
            if(sizeOfResourceToDownload > maxCapacityInBytes){
                throw new IllegalStateException("Size of object to be cached is larger than max capacity of cache size");
            }
            if(shouldEvict(sizeOfResourceToDownload)){
                List<ResourceData> entriesToDeleteFromCache = doEviction(sizeOfResourceToDownload);
                for(ResourceData resourceData : entriesToDeleteFromCache)
                    linkedHashMap.remove(resourceData.getResourceIdentifier());
            }
            File origImg = getFileLocation(url);
            // create parent folder that is unique for the original image
            origImg.getParentFile().mkdir();
            //linkedHashMap.rem
            destination = new FileOutputStream(origImg);
            long curFileSize = ByteStreams.copy(source, destination);
            updateCurrentCapacity(curFileSize);
            return origImg;
        } catch (IOException e) {
            throw new IOException(String.format("Could not fetch image for url %s", url));
        } finally {
            if(con!=null){
                con.disconnect();
            }
            if(source!=null){
                source.close();
            }
            if(destination!=null){
                destination.close();
            }
        }
    }

    private boolean shouldEvict(long sizeOfResourceToDownload) {
        System.out.println("currentSizeInBytes: " + currentSizeInBytes);
        boolean shouldEvict = this.currentSizeInBytes + sizeOfResourceToDownload > this.maxCapacityInBytes;
        System.out.println("shouldEvict: " + shouldEvict);
        return shouldEvict;
    }

    private List<ResourceData> doEviction(long requiredSize) {
        List<ResourceData> resourceDataListToDelete = new ArrayList<>();
        Iterator itr = linkedHashMap.values().iterator();
        long objectsFreedSized = 0;
        while(itr.hasNext() && requiredSize > objectsFreedSized){
            try {
                ResourceData resourceData = (ResourceData) itr.next();
                File resourceToDelete = getFileLocation(new URL(resourceData.getResourceIdentifier()));
                objectsFreedSized += resourceToDelete.length();
                long resourceFreeSize = resourceToDelete.length();
                System.out.println("Evicting object: " + resourceToDelete.getName() + " size: " + resourceFreeSize);
                FileIOUtils.removeFile(resourceToDelete);
                FileIOUtils.removeFile(resourceToDelete.getParentFile());
                currentSizeInBytes -= resourceFreeSize;
                resourceDataListToDelete.add(resourceData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Total objects freed from eviction by deletion on disk: "+ objectsFreedSized);
        return resourceDataListToDelete;
    }

    private void updateCurrentCapacity(long curFileSize) {
        currentSizeInBytes += curFileSize;
        System.out.println("After updateCurrentCapacity currentSizeInBytes: " + currentSizeInBytes);
    }

    private HttpURLConnection getHttpURLConnection(URL currentUrl) throws IOException {
        int redirectCount = 0;
        HttpURLConnection httpURLConnection;
        URL url = currentUrl;
        int resp;// Handle redirects manually, so HTTP→HTTPS and vice versa work.
        while (true) {
            httpURLConnection = (HttpURLConnection) currentUrl.openConnection();
            httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            httpURLConnection.setReadTimeout(TIMEOUT_MS);
            resp = httpURLConnection.getResponseCode();
            LOGGER.debug("URL {} gave HTTP response {}", currentUrl, resp);

            if (resp == HttpURLConnection.HTTP_MOVED_PERM
                    || resp == HttpURLConnection.HTTP_MOVED_PERM
                    || resp == HttpURLConnection.HTTP_SEE_OTHER) {
                redirectCount++;
                if (redirectCount > 10) {
                    throw new IOException(String.format("Too many redirects when retrieving from URL %s", url));
                } else {
                    String location = httpURLConnection.getHeaderField(HttpHeaders.LOCATION);
                    currentUrl = new URL(currentUrl, location);
                    continue;
                }
            }
            break;
        }
        if (resp != HttpURLConnection.HTTP_OK) {
            throw new IOException(String.format("HTTP %s when retrieving from URL %s (%d redirects, started at %s)",
                    resp, currentUrl, redirectCount, url));
        }
        return httpURLConnection;
    }
}
