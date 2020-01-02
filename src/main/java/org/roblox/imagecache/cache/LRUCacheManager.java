package org.roblox.imagecache.cache;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.roblox.imagecache.types.ResourceData;
import org.roblox.imagecache.types.ResourceMetaData;
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
public class LRUCacheManager implements Cache {
    private final Logger log = LoggerFactory.getLogger(LRUCacheManager.class);

    private static final long MAX_CACHE_CAPACITY = FileUtils.ONE_GB;
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
    private final DownloadManager downloadManager;

    /**
     * Creating LRU Cache using on disk storage as specified by the size in capacityInBytes and repository as the path
     * to store the resources.
     *  @param capacityInBytes valid positive integer for size in bytes that signifies the capacity of cache.
     *
     * @param numberOfItems valid positive integer which signifies the number of items in the cache with a load factor of 0.75.
     * @param repository the root location where the resources would be stored for caching on disk.
     * @param fileIOUtils
     */
    public LRUCacheManager(final long capacityInBytes,
                           final int numberOfItems,
                           @NonNull final String repository,
                           @NonNull final FileIOUtils fileIOUtils,
                           @NonNull final DownloadManager downloadManager) {
        this.fileIOUtils = fileIOUtils;
        this.repository = fileIOUtils.createRepository(repository);
        this.validate(capacityInBytes, numberOfItems);
        this.maxCapacityInBytes = capacityInBytes;
        this.linkedHashMap = Collections.synchronizedMap(new LinkedHashMap<>(numberOfItems, 0.75f, true));
        this.downloadManager = downloadManager;
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
     * TODO: Read through Cache
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
            final File downloadedResource = downLoadImage(key);
            final Path path = downloadedResource.toPath();
            this.linkedHashMap.put(key, new ResourceData(key, path.toString(), Files.readAllBytes(path)));
            return new ResultData(key, State.DOWNLOADED, downloadedResource.length());
        }
    }

    private File downLoadImage(final String url) throws IOException {
        final HttpURLConnection httpURLConnection = this.downloadManager.getHttpURLConnection(url);
        final long sizeOfResourceToDownload = this.downloadManager.getContentLength(httpURLConnection);
        handleEviction(sizeOfResourceToDownload);
        final ResourceMetaData metaData = this.downloadManager.loadResource(httpURLConnection, url, this.repository);
        updateCurrentCacheCapacity(metaData.getResourceSizeInBytes());
        return metaData.getDownloadedResource();
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

    private boolean shouldEvict(final long sizeOfResourceToDownload) {
        return this.currentSizeInBytes + sizeOfResourceToDownload > this.maxCapacityInBytes;
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
}
