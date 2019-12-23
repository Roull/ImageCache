package org.example.misc;

import lombok.Getter;
import lombok.NonNull;

import java.net.URI;

public class ArtifactData {

    private final @NonNull @Getter URI diskLocation;
    private final @Getter long sizeInBytes;

    public ArtifactData(@NonNull URI diskLocation, long sizeInBytes) {
        this.diskLocation = diskLocation;
        this.sizeInBytes = sizeInBytes;
    }
}
