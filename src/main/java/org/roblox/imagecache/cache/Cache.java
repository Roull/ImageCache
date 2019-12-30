package org.roblox.imagecache.cache;

import lombok.NonNull;
import org.roblox.imagecache.types.ResultData;

import java.io.IOException;

public interface Cache {
    ResultData load(@NonNull String key) throws IOException;
}
