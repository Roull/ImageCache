package org.example.misc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.net.URL;

public enum ImageLRUCache {
    INSTANCE;

    Cache<URL, CachedImage> imageCache;

    ImageLRUCache() {
        imageCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .build();
    }

    public Cache<URL, CachedImage> getCache() {
        return imageCache;
    }
}