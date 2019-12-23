package org.roblox.imagecache.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Result object which stores the status of the data that was resulting from cache load operation.
 * The toString method is overriden inorder to conform to the expected output format in the output file.
 */
@Getter
@AllArgsConstructor
@Builder
public class ResultData {

    /**
     * Actual url key that is used in the cache.
     */
    private String url;

    /**
     * Indicates the status of cache load operation.
     */
    @Setter
    private State state;

    /**
     * Indicates the size in bytes of the object that was fetched.
     */
    private long sizeInBytes;

    @Override
    public String toString() {
        return url + " " + state + " " + sizeInBytes + "\r\n";
    }
}
