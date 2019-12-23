package org.roblox.imagecache;

import org.roblox.imagecache.cache.LRUCache;
import org.roblox.imagecache.types.ResultData;
import org.roblox.imagecache.utils.FileIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final int DEFAULT_NUM_ENTRIES_IN_CACHE = 100;
    private static final String DEFAULT_INPUT_FILE = "image-cache-test-input.txt";
    private static final String DEFAULT_OUTPUT_FILE = "image-cache-test-output.txt";

    public static void main(String args[]) {

        //1. Parse input file
        String defaultRepository = System.getProperty("user.dir");
        String inputFilePath = defaultRepository + File.separator + DEFAULT_INPUT_FILE;
        List<String> lines = FileIOUtils.readFileToList(inputFilePath);

        //2. Create Cache and simulate calls for load
        final int maxSizeInBytes = Integer.parseInt(lines.get(0));
        LRUCache cache = new LRUCache(maxSizeInBytes, DEFAULT_NUM_ENTRIES_IN_CACHE, defaultRepository);
        List<ResultData> results = new ArrayList<>();
        for(int i = 2; i < lines.size(); i++) {
            ResultData resultData = null;
            try {
                resultData = cache.load(lines.get(i));
            } catch (IOException e) {
                e.printStackTrace();
            }
            results.add(resultData);
        }

        //3. Write results of caching to output file
        String outputFilePath = defaultRepository + File.separator + DEFAULT_OUTPUT_FILE;
        FileIOUtils.deleteIfExists(outputFilePath);
        FileIOUtils.writeListToFile(outputFilePath, results);
    }
}
