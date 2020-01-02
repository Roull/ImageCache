package org.roblox.imagecache;

import org.roblox.imagecache.cache.DownloadManager;
import org.roblox.imagecache.cache.LRUCacheManager;
import org.roblox.imagecache.types.ResultData;
import org.roblox.imagecache.types.State;
import org.roblox.imagecache.utils.FileIOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Entry point with the main function to simulation cache loading.
 *
 */
public class Main {

    private static final int DEFAULT_NUM_ENTRIES_IN_CACHE = 100;
    private static final String DEFAULT_INPUT_FILE = "image-cache-test-input.txt";
    private static final String DEFAULT_OUTPUT_FILE = "image-cache-test-output.txt";
    private static final FileIOUtils fileIOUtils = new FileIOUtils();
    private static final DownloadManager downloadManager = new DownloadManager();

    public static void main(final String[] args) {
        //loadPropertiesFromClassPath();

        final String defaultRepository = System.getProperty("user.dir");
        //final String inputFilePath = defaultRepository + File.separator + DEFAULT_INPUT_FILE;
        //TODO: use paths : Path currentPath = Paths.get(System.getProperty("user.dir"));
        //Path filePath = Paths.get(currentPath.toString(), "data", "foo.txt");
        //System.out.println(filePath.toString());

        final String inputFilePath = "/Users/ramanoh/workspace/ImageCache" + File.separator + DEFAULT_INPUT_FILE;

        //1. Parse input file
        final List<String> inputImageUrls = fileIOUtils.readFileToList(inputFilePath);

        //2. Create Cache and simulate calls for load
        final List<ResultData> results = processInput(inputImageUrls, defaultRepository);

        //3. Write results of caching to output file
        generateOutputData(defaultRepository, results);
    }

    //TODO: Load image from properties instead
    private static void loadPropertiesFromClassPath() {
        try (final InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {

            final Properties prop = new Properties();

            if (input == null) {
                System.out.println("Sorry, unable to find config.properties in class path");
            }

            //load a properties file from class path, inside static method
            prop.load(input);

            //get the property value and print it out
            System.out.println("Reading config.properties from input file : " + prop.getProperty("input.fileName"));
            System.out.println("Writing output to file: " + prop.getProperty("output.fileName"));
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private static List<ResultData> processInput(final List<String> inputImageUrls, final String defaultRepository) {
        //TODO: Documentation expected input format what is line 0, line 1, line 2?
        //TODO: For every main call would cache be reinitializeD?
        final int maxSizeInBytes = Integer.parseInt(inputImageUrls.get(0));
        final LRUCacheManager cache = new LRUCacheManager(maxSizeInBytes, DEFAULT_NUM_ENTRIES_IN_CACHE, defaultRepository, fileIOUtils, downloadManager);
        final List<ResultData> results = new ArrayList<>();
        for(int i = 2; i < inputImageUrls.size(); i++) {
            ResultData resultData;
            try {
                resultData = cache.load(inputImageUrls.get(i));
            } catch (final Exception e) {
                //If there are any exceptions while trying to load the object we shall just mark those as error,
                //and continue processing for the rest of the inputs.
                resultData = ResultData.builder().url(inputImageUrls.get(i)).sizeInBytes(0).state(State.ERROR).build();
            }
            results.add(resultData);
        }
        return results;
    }

    private static void generateOutputData(final String defaultRepository, final List<ResultData> results) {
        try {
            final String outputFilePath = defaultRepository + File.separator + DEFAULT_OUTPUT_FILE;
            fileIOUtils.deleteIfExists(outputFilePath);
            fileIOUtils.writeListToFile(outputFilePath, results);
        } catch (final IOException ex){
            throw new RuntimeException("Unable to write output file due to exception ", ex.getCause());
        }
    }
}
