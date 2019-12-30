package org.roblox.imagecache.cache;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.roblox.imagecache.types.ResourceData;
import org.roblox.imagecache.types.ResultData;
import org.roblox.imagecache.types.State;
import org.roblox.imagecache.utils.FileIOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
 * It uses input provided repository as the path to save the files to.
 * </p>
 */
public class LRUCache implements Cache {
    private final Logger log = LoggerFactory.getLogger(LRUCache.class);

    private static final int TIMEOUT_MS = 1*60*1000;  // 1 minutes
    private static final int CONNECT_TIMEOUT_MS = 30*1000;  // 30 seconds
    private static final long MAX_CACHE_CAPACITY = FileUtils.ONE_GB; // 1GB
    private static final int MAX_RETRY_COUNT = 3;

    private final Map<String, ResourceData> linkedHashMap;
    private final FileIOUtils fileIOUtils;
    private final File repository;
    private final long maxCapacityInBytes;
    private long currentSizeInBytes;
    @Getter
    private int cacheHitsCounter;
    @Getter
    private int cacheMissCounter;
    @Getter
    private int cacheEvictionCounter;
    private final MetricRegistry metricRegistry;

    /**
     * Creating LRU Cache using on disk storage as specified by the size in capacityInBytes and repository as the path
     * to store the resources.
     *  @param capacityInBytes valid positive integer for size in bytes that signifies the capacity of cache.
     *
     * @param numberOfItems valid positive integer which signifies the number of items in the cache with a load factor of 0.75.
     * @param repository the root location where the resources would be stored for caching on disk.
     * @param fileIOUtils
     */
    public LRUCache(final long capacityInBytes, final int numberOfItems, @NonNull final String repository,
                    @NonNull final FileIOUtils fileIOUtils) {
        this.fileIOUtils = fileIOUtils;
        this.repository = fileIOUtils.createRepository(repository);
        this.validate(capacityInBytes, numberOfItems);
        this.maxCapacityInBytes = capacityInBytes;
        this.linkedHashMap = Collections.synchronizedMap(new LinkedHashMap<>(numberOfItems, 0.75f, true));
        //TODO: Add metrics
        this.metricRegistry = new MetricRegistry();
    }

    /**
     * validates the inputs.
     *
     * @param capacityInBytes input size of cache.
     * @param numberOfItems number of items in the cache size.
     */
    private void validate(final long capacityInBytes, final int numberOfItems) {
        if(capacityInBytes < 0 || capacityInBytes > MAX_CACHE_CAPACITY) {
            throw new IllegalArgumentException(String.format("Value for cache capacity should be in %s and %s range", 0,
                    MAX_CACHE_CAPACITY));
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
    @Override
    public ResultData load(@NonNull final String key) throws IOException {
        this.log.info("Trying to load object: " + key);
        if (this.linkedHashMap.containsKey(key)) {
            // cache hit
            this.cacheHitsCounter++;
            final ResourceData cahedResource = this.linkedHashMap.get(key);
            return new ResultData(key, State.CACHE, cahedResource.getOriginalResourceBytes().length);
        } else {
            this.cacheMissCounter++;
            //cache miss, download the image by making external service call
            final File downloadedResource = downLoadImage(new URL(key));
            final Path path = downloadedResource.toPath();
            this.linkedHashMap.put(key, new ResourceData(key, path.toString(), Files.readAllBytes(path)));
            return new ResultData(key, State.DOWNLOADED, downloadedResource.length());
        }
    }

    /**
     * Uses HTTPConnection and downloads the resource/images from web to repo/disk.
     * Before downloading the resource from the web, we check the length of the resource to be downloaded and
     * verify that we have sufficient diskspace as well as cache space to download the object from the disk.
     *
     * @return File reference to the object that was downloaded by making HTTP call
     *
     * @throws IllegalStateException when size of the object to be stored is greater than the cache size.
     */
    private File downLoadImage(final URL url) throws IOException {
        HttpURLConnection httpURLConnection = null;
        InputStream source = null;
        OutputStream destination = null;
        try {
            httpURLConnection = getHttpURLConnection(url);
            source = httpURLConnection.getInputStream();
            final long sizeOfResourceToDownload = httpURLConnection.getContentLengthLong();
            handleEviction(sizeOfResourceToDownload);
            final File origImg = this.fileIOUtils.generateFileLocation(this.repository, url);
            // create parent folder that is unique for the original image
            final boolean mkdir = origImg.getParentFile().mkdir();
            destination = new FileOutputStream(origImg);
            final long curFileSize = ByteStreams.copy(source, destination);
            updateCurrentCacheCapacity(curFileSize);
            return origImg;
        } catch (final IOException e) {
            throw new IOException(String.format("Could not fetch image for url %s", url));
        } finally {
            cleanup(httpURLConnection, source, destination);
        }
    }

    private void handleEviction(final long sizeOfResourceToDownload) throws IOException {
        if(sizeOfResourceToDownload > this.maxCapacityInBytes){
            throw new IllegalStateException("Size of object to be cached is larger than max capacity of cache size");
        }
        if(shouldEvict(sizeOfResourceToDownload)){
            final List<ResourceData> entriesToDeleteFromCache = doEviction(sizeOfResourceToDownload);
            for(final ResourceData resourceData : entriesToDeleteFromCache) {
                this.linkedHashMap.remove(resourceData.getResourceIdentifier());
            }
            this.cacheEvictionCounter += entriesToDeleteFromCache.size();
        }
    }

    private void cleanup(final HttpURLConnection con, final InputStream source, final OutputStream destination) throws IOException {
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

    private boolean shouldEvict(final long sizeOfResourceToDownload) {
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
    private List<ResourceData> doEviction(final long requiredSize) {
        final List<ResourceData> resourceDataListToDelete = new ArrayList<>();
        final Iterator<ResourceData> itr = this.linkedHashMap.values().iterator();
        long objectsFreedSized = 0;
        while(itr.hasNext() && requiredSize > objectsFreedSized){
            try {
                final ResourceData resourceData = itr.next();
                final File resourceToDelete = this.fileIOUtils.generateFileLocation(this.repository,
                        new URL(resourceData.getResourceIdentifier()));
                objectsFreedSized += resourceToDelete.length();
                final long resourceFreeSize = this.fileIOUtils.deleteResourceOnDisk(resourceToDelete);
                this.currentSizeInBytes -= resourceFreeSize;
                resourceDataListToDelete.add(resourceData);
            } catch (final IOException e) {
                throw new RuntimeException("Unable to evict objects from cache");
            }
        }
        this.log.info("Total size of objects freed from cache by deletion on disk: "+ objectsFreedSized);
        return resourceDataListToDelete;
    }

    /**
     * Updates the current cache size.
     *
     * @param curFileSize size of the object that was just downloaded.
     */
    private void updateCurrentCacheCapacity(final long curFileSize) {
        this.currentSizeInBytes += curFileSize;
        this.log.info("After updating size of cache, currentSizeInBytes: " + this.currentSizeInBytes);
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
        final URL url = currentUrl;
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
                    final String location = httpURLConnection.getHeaderField(HttpHeaders.LOCATION);
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

    private boolean isaRedirect(final int resp) {
        return resp == HttpURLConnection.HTTP_MOVED_PERM
                || resp == HttpURLConnection.HTTP_MOVED_PERM
                || resp == HttpURLConnection.HTTP_SEE_OTHER;
    }
}
