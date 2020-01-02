package org.roblox.imagecache.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

/**
 * Class that represents the metadata for the objects that are downloaded by DownloadManager as result of
 * making external network call.
 */
@AllArgsConstructor
public class ResourceMetaData {

    @Getter
    File downloadedResource;

    @Getter
    long resourceSizeInBytes;
}
