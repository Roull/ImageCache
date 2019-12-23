package org.example.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import lombok.NonNull;
import org.roblox.imagecache.types.State;

public class ImageCacheApp {
    private final File repo;
    private static final String ENC = "UTF8";
    private static final String DEFAULT_FILENAME = "image";
    private static final int TIMEOUT_MS = 2*60*1000;  // 2 minutes
    private static final int CONNECT_TIMEOUT_MS = 30*1000;  // 30 seconds
    private Cache<URL, CachedImage> imageLRUCache;

    public ImageCacheApp(String repository) {
        repo = new File(repository);
        if (!repo.exists() && !repo.isDirectory() && !repo.canWrite()) {
            throw new IllegalStateException("Image Cache repository needs to be an existing, writable directory: "
                    + repo.getAbsolutePath());
        }
        imageLRUCache = ImageLRUCache.INSTANCE.getCache();
    }

    public CachedImage load(@NonNull final URL url) throws ExecutionException {
        return imageLRUCache.get(url, new Callable<CachedImage>() {
            @Override
            public CachedImage call() throws Exception {
                return get(url);
            }
        });
    }

    private CachedImage get(@NonNull URL url) throws IOException {
        File imgFile = location(url);
        if (!imgFile.exists()) {
            // download from the web using HTTPConnection
            downLoadImage(url);
            return new CachedImage(url, imgFile, State.DOWNLOADED, imgFile.length());
        }
        return new CachedImage(url, imgFile, State.CACHED, imgFile.length());
    }

    /**
     * Creates a location for the image URL with a suffix.
     */
    public File location(URL url) throws IOException {
        File folder;
        try {
            folder = new File(repo, URLEncoder.encode(url.toString(), ENC));
        } catch (UnsupportedEncodingException e) {
            throw new IOException("Encoding not supported", e);
        }
        return new File(folder, buildFileName(url));
    }

    private static String buildFileName(URL url) {
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
     */
    public void downLoadImage(URL url) throws IOException {
        try {
            HttpURLConnection con = getHttpURLConnection(url);
            InputStream source = con.getInputStream();

            // create parent folder that is unique for the original image
            File origImg = location(url);
            origImg.getParentFile().mkdir();
            OutputStream destination = new FileOutputStream(origImg);
            ByteStreams.copy(source, destination);
        } catch (IOException e) {
            throw new IOException(String.format("Could not fetch image for url %s", url));
        }
    }

    private HttpURLConnection getHttpURLConnection(URL currentUrl) throws IOException {
        int redirectCount = 0;
        HttpURLConnection con;
        URL url = currentUrl;
        int resp;// Handle redirects manually, so HTTP→HTTPS and vice versa work.
        while (true) {
            con = (HttpURLConnection) currentUrl.openConnection();
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(TIMEOUT_MS);
            resp = con.getResponseCode();
            //LOG.debug("URL {} gave HTTP response {}", currentUrl, resp);

            if (resp == HttpURLConnection.HTTP_MOVED_PERM
                    || resp == HttpURLConnection.HTTP_MOVED_PERM
                    || resp == HttpURLConnection.HTTP_SEE_OTHER) {
                redirectCount++;

                if (redirectCount > 10) {
                    throw new IOException(String.format("Too many redirects when retrieving from URL %s", url));
                } else {
                    String location = con.getHeaderField(HttpHeaders.LOCATION);
                    currentUrl = new URL(currentUrl, location);
                    continue;
                }
            }
            break;
        }

        if (resp != HttpURLConnection.HTTP_OK) {
            throw new IOException(String.format("HTTP %s when retrieving from URL %s (%d redirects, started at %s)", resp, currentUrl, redirectCount, url));
        }
        return con;
    }
}
