package org.example.misc;

import org.checkerframework.checker.nullness.qual.NonNull;

public class ResourceIdentifier {
    private final @NonNull String resourceId;

    public ResourceIdentifier(@NonNull String resourceId) {
        this.resourceId = resourceId;
    }
}
