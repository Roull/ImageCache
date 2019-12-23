package org.roblox.imagecache.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 */
@AllArgsConstructor
@Getter
public class ResourceData {
    /**
     *
     */
    private String resourceIdentifier;
    /**
     *
     */
    private String resourcePath;
    /**
     *
     */
    private byte[] originalResourceBytes;
}
