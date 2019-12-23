package org.roblox.imagecache.types;

/**
 * State to indicate whether the resource was downloaded or cached.
 */
public enum State {
    /**
     * Indicates that the resource was fetched by making external service call.
     */
    DOWNLOADED,
    /**
     * Indicates that the resource was fetched from local cache and not had to be fetched by making external service call.
     */
    CACHED,

    /**
     * Indicates that there was error in fetching the resource.
     */
    ERROR
}
