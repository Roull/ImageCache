package org.roblox.imagecache.LRUCache;

import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.roblox.imagecache.cache.DownloadManager;
import org.roblox.imagecache.cache.LRUCacheManager;
import org.roblox.imagecache.types.ResourceMetaData;
import org.roblox.imagecache.types.ResultData;
import org.roblox.imagecache.types.State;
import org.roblox.imagecache.utils.FileIOUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class LRUCacheManagerTest {
    @Mock
    private FileIOUtils fileIOUtils;

    @Mock
    private DownloadManager downloadManager;

    @Mock
    private HttpURLConnection mockHttpURLConnection;

    @Mock
    private File mockRepo;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test(expected = IllegalArgumentException.class)
    public void testCacheCreationFailsForNegativeCapacity() {
        when(this.fileIOUtils.createRepository(anyString())).thenReturn(any(File.class));
        new LRUCacheManager(-1, 100, "defaultRepository", this.fileIOUtils, this.downloadManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheCreationFailsForMaxCapacityGreater() {
        when(this.fileIOUtils.createRepository(anyString())).thenReturn(any(File.class));
        new LRUCacheManager(FileUtils.ONE_PB, 100, "defaultRepository", this.fileIOUtils, this.downloadManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheCreationFailsForItemSizeNegative() {
        when(this.fileIOUtils.createRepository(anyString())).thenReturn(any(File.class));
        new LRUCacheManager(100, -1, "defaultRepository", this.fileIOUtils, this.downloadManager);
    }

    @Test
    public void testCacheLoad() throws IOException {
        //setup
        when(this.fileIOUtils.createRepository(anyString())).thenReturn(this.mockRepo);
        final LRUCacheManager cache = new LRUCacheManager(10, 10, "defaultRepository", this.fileIOUtils, this.downloadManager);

        //Loading for the first time, expect the status to be download.
        when(this.downloadManager.getHttpURLConnection(anyString())).thenReturn(this.mockHttpURLConnection);
        when(this.downloadManager.getContentLength(this.mockHttpURLConnection)).thenReturn(10L);

        // Create a temporary file.
        final File tempFile = this.tempFolder.newFile("tempFile.txt");
        when(this.fileIOUtils.generateFileLocation(any(File.class),any(URL.class))).thenReturn(tempFile);
        when(this.fileIOUtils.deleteResourceOnDisk(any(File.class))).thenReturn(10L);

        final ResourceMetaData resourceMetaData = new ResourceMetaData(tempFile, 10);
        when(this.downloadManager.loadResource(any(HttpURLConnection.class), anyString(), any(File.class))).thenReturn(resourceMetaData);

        //test
        final String testKey = "http://i.test.com/test.jpg";
        ResultData resultData = cache.load(testKey);

        //verify: cache miss
        Assert.assertEquals(State.DOWNLOADED, resultData.getState());
        Assert.assertEquals(0, cache.getCacheEvictionCounter());
        Assert.assertEquals(0, cache.getCacheHitsCounter());
        Assert.assertEquals(1, cache.getCacheMissCounter());

        //test
        //Loading the same resource for the second time, expected the status to be cache.
        resultData = cache.load(testKey);
        //verify: cache hit
        Assert.assertEquals(State.CACHE, resultData.getState());
        Assert.assertEquals(0, cache.getCacheEvictionCounter());
        Assert.assertEquals(1, cache.getCacheHitsCounter());

        //test
        final String testKey2 = "http://i.test.com/test2.jpg";
        resultData = cache.load(testKey2);

        //verify: evicted the first object that was downloaded.
        Assert.assertEquals(State.DOWNLOADED, resultData.getState());
        Assert.assertEquals(1, cache.getCacheEvictionCounter());
    }

    @Test(expected = IllegalStateException.class)
    public void testCacheLoadFailsDueToInsufficientCacheCapacity() throws IOException {
        //setup : cache empty, size is 10, size of object to be downloaded would be 20
        when(this.fileIOUtils.createRepository(anyString())).thenReturn(this.mockRepo);
        final LRUCacheManager cache = new LRUCacheManager(10, 10, "defaultRepository", this.fileIOUtils, this.downloadManager);
        when(this.downloadManager.getHttpURLConnection(anyString())).thenReturn(this.mockHttpURLConnection);
        when(this.downloadManager.getContentLength(this.mockHttpURLConnection)).thenReturn(20L);

        //test
        final String testKey = "http://i.test.com/test.jpg";
        cache.load(testKey);

        //verify : object not downloaded from http call
        verify(this.downloadManager.loadResource(any(HttpURLConnection.class), anyString(), any(File.class)), times(0));
    }
}