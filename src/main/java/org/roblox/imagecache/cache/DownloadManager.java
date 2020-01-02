package org.roblox.imagecache.cache;

import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import lombok.NonNull;
import org.roblox.imagecache.types.ResourceMetaData;
import org.roblox.imagecache.utils.FileIOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Class responsible for making HTTP calls and downloading the objects from web when they are
 * not present in cache. This class also handles redirections.
 */
public class DownloadManager {
    private final FileIOUtils fileIOUtils;
    private static final long TIMEOUT_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long CONNECT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);
    private static final int MAX_RETRY_COUNT = 3;

    public DownloadManager() {
        this.fileIOUtils = new FileIOUtils();
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
    public ResourceMetaData loadResource(final HttpURLConnection httpURLConnection, final String urlKey, final File repository)
            throws IOException {
        InputStream source = null;
        OutputStream destination = null;
        final URL url = new URL(urlKey);
        try {
            final File originalImageLocation = this.fileIOUtils.generateFileLocation(repository, url);
            // create parent folder that is unique for the original image
            final boolean mkdir = originalImageLocation.getParentFile().mkdir();
            destination = new FileOutputStream(originalImageLocation);
            source = httpURLConnection.getInputStream();
            final long curFileSize = ByteStreams.copy(source, destination);
            return new ResourceMetaData(originalImageLocation, curFileSize);
        }
        catch (final IOException e) {
            throw new IOException(String.format("Could not fetch image for url %s", url));
        } finally {
            cleanup(httpURLConnection, source, destination);
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

    /**
     * Handles HTTP redirection and throws after maxRetryAttemtps as speicified.
     *
     * @param urlKey url of the resource that needs to be fetched from the web.
     *
     * @return {@link HttpURLConnection} object from which we can copy the bytes to disk using streams.
     *
     * @throws IOException if file cannot be fetched from the server even after retries.
     */
    public HttpURLConnection getHttpURLConnection(@NonNull final String urlKey) throws IOException {
        URL originalURL = new URL(urlKey);
        final URL url = originalURL;
        HttpURLConnection httpURLConnection;
        int redirectCount = 0;
        int response;// Handle redirects manually, so HTTP→HTTPS and vice versa work.
        while (true) {
            httpURLConnection = (HttpURLConnection) originalURL.openConnection();
            httpURLConnection.setConnectTimeout(Math.toIntExact(CONNECT_TIMEOUT_MS));
            httpURLConnection.setReadTimeout(Math.toIntExact(TIMEOUT_MS));
            response = httpURLConnection.getResponseCode();

            if (isaRedirect(response)) {
                redirectCount++;
                if (redirectCount > MAX_RETRY_COUNT) {
                    throw new IOException(String.format("Too many redirects when retrieving from URL %s", url));
                } else {
                    final String location = httpURLConnection.getHeaderField(HttpHeaders.LOCATION);
                    originalURL = new URL(originalURL, location);
                    continue;
                }
            }
            break;
        }
        if (response != HttpURLConnection.HTTP_OK) {
            throw new IOException(String.format("HTTP %s when retrieving from URL %s (%d redirects, started at %s)",
                    response, originalURL, redirectCount, url));
        }
        return httpURLConnection;
    }

    private boolean isaRedirect(final int resp) {
        return resp == HttpURLConnection.HTTP_MOVED_PERM
                || resp == HttpURLConnection.HTTP_MOVED_PERM
                || resp == HttpURLConnection.HTTP_SEE_OTHER;
    }

    public long getContentLength(final HttpURLConnection httpURLConnection) {
        return httpURLConnection.getContentLengthLong();
    }
}