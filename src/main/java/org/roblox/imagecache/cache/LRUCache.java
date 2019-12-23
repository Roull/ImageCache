package org.roblox.imagecache.cache;

import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.roblox.imagecache.types.ResourceData;
import org.roblox.imagecache.types.ResultData;
import org.roblox.imagecache.types.State;
import org.roblox.imagecache.utils.FileIOUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

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
    //private static final Logger LOGGER = LoggerFactory.getLogger(LRUCache.class);

    private static final String ENC = "UTF8";
    private static final String DEFAULT_FILENAME = "image";
    private static final int TIMEOUT_MS = 1*60*1000;  // 1 minutes
    private static final int CONNECT_TIMEOUT_MS = 30*1000;  // 30 seconds
    private static final long MAX_CACHE_CAPACITY = FileUtils.ONE_GB; // 1GB
    public static final int MAX_RETRY_COUNT = 3;

    private final Map<String, ResourceData> linkedHashMap;
    private final File repository;
    private final int maxCapacityInBytes;
    private long currentSizeInBytes;

    /**
     * Creating LRU Cache using on disk storage as specified by the size in capacityInBytes and repository as the path
     * to store the resources.
     *
     * @param capacityInBytes valid positive integer for size in bytes that signifies the capacity of cache.
     *
     * @param numberOfItems valid positive integer which signifies the number of items in the cache with a load factor of 0.75.
     *
     * @param repository the root location where the resources would be stored for caching on disk.
     */
    public LRUCache(int capacityInBytes, int numberOfItems, @NonNull String repository) {
        this.repository = new File(repository);
        this.validate(capacityInBytes, numberOfItems);
        maxCapacityInBytes = capacityInBytes;
        currentSizeInBytes = 0;
        linkedHashMap = Collections.synchronizedMap(new LinkedHashMap<>(numberOfItems, 0.75f, true));
    }

    /**
     * validates the inputs.
     *  @param capacityInBytes
     * @param numberOfItems
     */
    private void validate(int capacityInBytes, int numberOfItems) {
        if (!this.repository.exists() && !this.repository.isDirectory() && !this.repository.canWrite()) {
            throw new IllegalStateException("Image Cache repository needs to be an existing writable directory: "
                    + this.repository.getAbsolutePath());
        }
        if(capacityInBytes < 0 || capacityInBytes > MAX_CACHE_CAPACITY) {
            throw new IllegalArgumentException(String.format("Value for cache capacity should be in %s and %s range", 0, MAX_CACHE_CAPACITY));
        }
        if(numberOfItems < 0) {
            throw new IllegalArgumentException("Value for number of items should be a positive integer");
        }
    }

    /**
     * If the key is not present in the cache, we will make an HTTP call to web and download the images as well as populate
     * the map with entry to the {@link ResourceData} object that consists of the actual bytes array as well as metadata.
     * If the resource is fetched by making external call we will return the {@link ResultData} with {@link State} as
     * State.DOWNLOADED, otherwise State.CACHED.
     *
     * @param key url string of the resource to be fetched from the cache.
     *
     * @return {@ResultData} Object consisting of the metadata along with the original byteArray and status of caching
     * result
     *
     * @throws IOException
     */
    public ResultData load(@NonNull String key) throws IOException {
        //Log.info("Trying to load object: " + key);
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

    private File generateFileLocation(URL url) throws IOException {
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
     * Uses HTTPConnection and downloads the resource/images from web to repo/disk.
     * Before downloading the resource from the web, we check the length of the resource to be downloaded and
     * verify that we have sufficient diskspace as well as cache space to download the object from the disk.
     *
     * @return
     *
     * @throws IllegalStateException when size of the object to be stored is greater than the cache size.
     */
    private File downLoadImage(URL url) throws IOException {
        HttpURLConnection con = null;
        InputStream source = null;
        OutputStream destination = null;
        try {
            con = getHttpURLConnection(url);
            source = con.getInputStream();
            long sizeOfResourceToDownload = con.getContentLengthLong();
            //Log.info("sizeOfResourceToDownload " + sizeOfResourceToDownload);
            if(sizeOfResourceToDownload > maxCapacityInBytes){
                throw new IllegalStateException("Size of object to be cached is larger than max capacity of cache size");
            }
            if(shouldEvict(sizeOfResourceToDownload)){
                List<ResourceData> entriesToDeleteFromCache = doEviction(sizeOfResourceToDownload);
                for(ResourceData resourceData : entriesToDeleteFromCache) {
                    linkedHashMap.remove(resourceData.getResourceIdentifier());
                }
            }
            File origImg = generateFileLocation(url);
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
        return this.currentSizeInBytes + sizeOfResourceToDownload > this.maxCapacityInBytes;
    }

    /**
     * Handles eviction of the least recently used entry in the cache and updates the size of the cache accordingly.
     *
     * @param requiredSize size of the cache that has to be available for the download to proceed.
     *
     * @return List of {@link ResourceData} objects that are supposed to be invalidated in cache since the original
     * resources are deleted on disk.
     */
    private List<ResourceData> doEviction(long requiredSize) {
        List<ResourceData> resourceDataListToDelete = new ArrayList<>();
        Iterator itr = linkedHashMap.values().iterator();
        long objectsFreedSized = 0;
        while(itr.hasNext() && requiredSize > objectsFreedSized){
            try {
                ResourceData resourceData = (ResourceData) itr.next();
                File resourceToDelete = generateFileLocation(new URL(resourceData.getResourceIdentifier()));
                objectsFreedSized += resourceToDelete.length();
                long resourceFreeSize = deleteResourceOnDisk(resourceToDelete);
                currentSizeInBytes -= resourceFreeSize;
                resourceDataListToDelete.add(resourceData);
            } catch (IOException e) {
                throw new RuntimeException("Unable to evict objects from cache");
            }
        }
        //Log.info("Total size of objects freed from eviction by deletion on disk: "+ objectsFreedSized);
        return resourceDataListToDelete;
    }

    /**
     * retries deletion of resource from disk after retrying MAX_RETRY_ATTEMPTS
     *
     * @param resourceToDelete resource to delete from the disk.
     *
     * @return size that is freed up on disk after deletion of items.
     */
    private long deleteResourceOnDisk(@NonNull File resourceToDelete) throws IOException {
        long resourceFreeSize = resourceToDelete.length();
        int count = 0;
        while(true) {
            try {
                //Log.info("Evicting object: " + resourceToDelete.getName() + " size: " + resourceFreeSize);
                FileIOUtils.removeFile(resourceToDelete);
                FileIOUtils.removeFile(resourceToDelete.getParentFile());
                return resourceFreeSize;
            } catch (IOException ex) {
                if (++count == MAX_RETRY_COUNT) throw ex;
            }
        }
    }

    /**
     * Updates the current cache size.
     *
     * @param curFileSize size of the object that was just downloaded.
     */
    private void updateCurrentCapacity(long curFileSize) {
        currentSizeInBytes += curFileSize;
        //Log.info("After updateCurrentCapacity currentSizeInBytes: " + currentSizeInBytes);
    }

    /**
     * Handles HTTP redirection and throws after maxRetryAttemtps as speicified.
     *
     * @param currentUrl url of the resource that needs to be fetched from the web.
     *
     * @return {@link HttpURLConnection} object from which we can copy the bytes to disk using streams.
     *
     * @throws IOException if file cannot be fetched from the server even after retries.
     */
    private HttpURLConnection getHttpURLConnection(@NonNull URL currentUrl) throws IOException {
        int redirectCount = 0;
        HttpURLConnection httpURLConnection;
        URL url = currentUrl;
        int resp;// Handle redirects manually, so HTTP→HTTPS and vice versa work.
        while (true) {
            httpURLConnection = (HttpURLConnection) currentUrl.openConnection();
            httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            httpURLConnection.setReadTimeout(TIMEOUT_MS);
            resp = httpURLConnection.getResponseCode();

            if (isaRedirect(resp)) {
                redirectCount++;
                if (redirectCount > MAX_RETRY_COUNT) {
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

    private boolean isaRedirect(int resp) {
        return resp == HttpURLConnection.HTTP_MOVED_PERM
                || resp == HttpURLConnection.HTTP_MOVED_PERM
                || resp == HttpURLConnection.HTTP_SEE_OTHER;
    }
}
