package org.roblox.imagecache;

import org.roblox.imagecache.cache.LRUCache;
import org.roblox.imagecache.types.ResultData;
import org.roblox.imagecache.types.State;
import org.roblox.imagecache.utils.FileIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point with the main function to simulation cache loading.
 *
 */
public class Main {

    private static final int DEFAULT_NUM_ENTRIES_IN_CACHE = 100;
    private static final String DEFAULT_INPUT_FILE = "image-cache-test-input.txt";
    private static final String DEFAULT_OUTPUT_FILE = "image-cache-test-output.txt";

    public static void main(String args[]) {

        String defaultRepository = System.getProperty("user.dir");
        String inputFilePath = defaultRepository + File.separator + DEFAULT_INPUT_FILE;

        //1. Parse input file
        List<String> inputImageUrls = FileIOUtils.readFileToList(inputFilePath);

        //2. Create Cache and simulate calls for load
        List<ResultData> results = processInput(inputImageUrls, defaultRepository);

        //3. Write results of caching to output file
        generateOutputData(defaultRepository, results);
    }

    private static List<ResultData> processInput(List<String> inputImageUrls, String defaultRepository) {
        final int maxSizeInBytes = Integer.parseInt(inputImageUrls.get(0));
        LRUCache cache = new LRUCache(maxSizeInBytes, DEFAULT_NUM_ENTRIES_IN_CACHE, defaultRepository);
        List<ResultData> results = new ArrayList<>();
        for(int i = 2; i < inputImageUrls.size(); i++) {
            ResultData resultData;
            try {
                resultData = cache.load(inputImageUrls.get(i));
            } catch (Exception e) {
                //If there are any exceptions while trying to load the object we shall just mark those as error,
                //and continue processing for the rest of the inputs.
                resultData = ResultData.builder().url(inputImageUrls.get(i)).sizeInBytes(0).state(State.ERROR).build();
            }
            results.add(resultData);
        }
        return results;
    }

    private static void generateOutputData(String defaultRepository, List<ResultData> results) {
        try {
            String outputFilePath = defaultRepository + File.separator + DEFAULT_OUTPUT_FILE;
            FileIOUtils.deleteIfExists(outputFilePath);
            FileIOUtils.writeListToFile(outputFilePath, results);
        } catch (IOException ex){
            throw new RuntimeException("Unable to write output file due to exception ", ex.getCause());
        }
    }
}
