package org.example.misc;

import org.roblox.imagecache.types.State;

import java.io.File;
import java.net.URL;

public class CachedImage  {
    private final URL url;
    private final File location;
    private State state;
    private long sizeInBytes;

    public CachedImage(URL url, File location, State state, long sizeInBytes) {
        this.url = url;
        this.location = location;
        this.state = state;
        this.sizeInBytes = sizeInBytes;
    }
}