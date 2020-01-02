package org.roblox.imagecache.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

@AllArgsConstructor
public class ResourceMetaData {
    @Getter
    File downloadedResource;
    @Getter
    long resourceSizeInBytes;
}
