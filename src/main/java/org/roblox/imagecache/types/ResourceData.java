package org.roblox.imagecache.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Class that represents Resource that will be cached. Trying to Keep this generic since the resource can be anything
 * like an image or file that would potentially be fetched by making an external service call.
 *
 */
@AllArgsConstructor
@Getter
public class ResourceData {
    /**
     * Identifier of the resource that will be cached/downloaded.
     */
    private String resourceIdentifier;
    /**
     * Path to the actual resource on disk.
     */
    private String resourcePath;
    /**
     * bytes that can be stored in memory which can be be used by the caller the way they want it.
     */
    private byte[] originalResourceBytes;
}
