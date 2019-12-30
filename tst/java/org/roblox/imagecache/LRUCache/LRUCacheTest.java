package org.roblox.imagecache.LRUCache;

import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.roblox.imagecache.cache.LRUCache;
import org.roblox.imagecache.types.ResultData;
import org.roblox.imagecache.types.State;
import org.roblox.imagecache.utils.FileIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class LRUCacheTest {
    @Mock
    private FileIOUtils fileIOUtils;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test(expected = IllegalArgumentException.class)
    public void testCacheCreationFailsForNegativeCapacity() {
        when(this.fileIOUtils.createRepository(anyString())).thenReturn(any(File.class));
        new LRUCache(-1, 100, "defaultRepository", this.fileIOUtils);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheCreationFailsForMaxCapacityGreater() {
        when(this.fileIOUtils.createRepository(anyString())).thenReturn(any(File.class));
        new LRUCache(FileUtils.ONE_PB, 100, "defaultRepository", this.fileIOUtils);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheCreationFailsForItemSizeNegative() {
        when(this.fileIOUtils.createRepository(anyString())).thenReturn(any(File.class));
        new LRUCache(100, -1, "defaultRepository", this.fileIOUtils);
    }

    /**
     * Simulating an integration test here instead of unit test
     *for testing the E2E functionality of cache load.
     * Note: due to limited time, writing functional test rather than unit test
     * TODO: want to write a unit test for the same with mocking enabled.
     */
    @Test
    public void testCacheLoad() throws IOException {
        final String defaultRepository = System.getProperty("user.dir");
        final FileIOUtils fileIOUtils = new FileIOUtils();
        final File repo = fileIOUtils.createRepository(defaultRepository);
        final LRUCache cache = new LRUCache(10000000, 100, defaultRepository, fileIOUtils);
        final String inputFilePath = System.getProperty("user.dir") + "/test-resources/load_input.txt";
        final List<String> inputImageUrls = fileIOUtils.readFileToList(inputFilePath);
        //Loading for the first time, expect the status to be download.
        ResultData resultData = cache.load(inputImageUrls.get(0));
        Assert.assertEquals(State.DOWNLOADED, resultData.getState());

        //Loading the same resource for the second time, expected the status to be cache.
        resultData = cache.load(inputImageUrls.get(0));
        Assert.assertEquals(State.CACHE, resultData.getState());
    }

    /**
     * Simulating an integration test here instead of unit test
     * for testing the E2E functionality of cache load with eviction.
     */
    @Test(expected = IllegalStateException.class)
    public void testCacheLoadFailsDueToInsufficientCacheCapacity() throws IllegalStateException, IOException {
        final String defaultRepository = System.getProperty("user.dir");
        final FileIOUtils fileIOUtils = new FileIOUtils();
        final File repo = fileIOUtils.createRepository(defaultRepository);
        final LRUCache cache = new LRUCache(100, 100, defaultRepository, fileIOUtils);
        final String inputFilePath = System.getProperty("user.dir") + "/test-resources/load_input.txt";
        final List<String> inputImageUrls = fileIOUtils.readFileToList(inputFilePath);
        try {
            cache.load(inputImageUrls.get(1));
        } catch (final IllegalStateException | IOException e) {
            final String expected = "Size of object to be cached is larger than max capacity of cache size";
            Assert.assertEquals(expected, e.getMessage());
            throw e;
        }
        Assert.fail("expected to throw");
    }

    /**
     * Simulating an integration test here instead of unit test
     * for testing the E2E functionality of cache load with eviction.
     */
    @Test
    public void testCacheEviction() throws IOException {
        final String defaultRepository = System.getProperty("user.dir");
        final FileIOUtils fileIOUtils = new FileIOUtils();
        final File repo = fileIOUtils.createRepository(defaultRepository);
        final LRUCache cache = new LRUCache(2464218, 100, defaultRepository, fileIOUtils);
        final String inputFilePath = System.getProperty("user.dir") + "/test-resources/load_input.txt";
        final List<String> inputImageUrls = fileIOUtils.readFileToList(inputFilePath);
        cache.load(inputImageUrls.get(0));//2464218 -> cache Miss
        cache.load(inputImageUrls.get(0));//2464218 -> cache Hit
        cache.load(inputImageUrls.get(1));//470489 -> cache Miss, also triggers CacheEviction of LRU
        Assert.assertEquals(1, cache.getCacheEvictionCounter());
        Assert.assertEquals(1, cache.getCacheHitsCounter());
        Assert.assertEquals(2, cache.getCacheMissCounter());
    }
}