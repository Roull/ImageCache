package org.roblox.imagecache;

import org.apache.commons.cli.*;
import org.roblox.imagecache.cache.DownloadManager;
import org.roblox.imagecache.cache.LRUCacheManager;
import org.roblox.imagecache.types.ResultData;
import org.roblox.imagecache.types.State;
import org.roblox.imagecache.utils.FileIOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
    private static final FileIOUtils fileIOUtils = new FileIOUtils();
    private static final DownloadManager downloadManager = new DownloadManager();
    private static final String DEFAULT_REPOSITORY = System.getProperty("user.dir");

    public static void main(final String[] args) {

        final CommandLine cmd = setUpOptions(args);
        if(!cmd.hasOption("input") && !cmd.hasOption("output") && !cmd.hasOption("path")) {
            System.out.println("Running the image cache simulation in default Mode");
        } else if (cmd.getOptions().length > 0 && cmd.getOptions().length < 3) {
            System.out.println("Expected either paths to both input and output file or no arguments to run in default mode");
            System.exit(1);
        }
        String inputFilePath = DEFAULT_REPOSITORY + File.separator + DEFAULT_INPUT_FILE;
        String outputFilePath = DEFAULT_REPOSITORY + File.separator + DEFAULT_OUTPUT_FILE;
        inputFilePath = cmd.getOptionValue("input", inputFilePath);
        outputFilePath = cmd.getOptionValue("output", outputFilePath);
        final String cacheRepository = cmd.getOptionValue("path", DEFAULT_REPOSITORY);
        System.out.println("Running the image cache simulation using input from file " + Paths.get(inputFilePath).toAbsolutePath());
        System.out.println("Generated output of image cache simulation to output file: " + Paths.get(outputFilePath).toAbsolutePath());
        System.out.println("Path for downloaded resources would be: " + Paths.get(cacheRepository).toAbsolutePath());

        //1. Parse input file
        final List<String> inputImageUrls = fileIOUtils.readFileToList(inputFilePath);

        //2. Create Cache and simulate calls for load
        final List<ResultData> results = processInput(inputImageUrls, cacheRepository);

        //3. Write results of caching to output file
        generateOutputData(outputFilePath, results);
    }

    private static CommandLine setUpOptions(final String[] args) {
        final Options options = new Options();

        final Option input = new Option("i", "input", true, "input file path");
        input.setRequired(false);
        options.addOption(input);

        final Option output = new Option("o", "output", true, "output file");
        output.setRequired(false);
        options.addOption(output);

        final Option path = new Option("p", "path", true, "path to downloaded resources");
        output.setRequired(false);
        options.addOption(path);

        final Option defaultOption = new Option("d", "default", false,
                "uses input file " + DEFAULT_INPUT_FILE + " from given problem statement and output " +
                        "file(image-cache-test-output.txt) will be generated in the current directory");
        defaultOption.setRequired(false);
        options.addOption(defaultOption);

        final CommandLineParser parser = new BasicParser();
        final HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Image Cache Utility", options);
            System.exit(1);
        }
        return cmd;
    }

    /**
     *
     * @param inputImageUrls list of strings that are parsed from the inputFile, Based on the spec expects that
     *                       index 0 consists of the size of the cache, index 1 consists of the number of URLS to be
     *                       fetched for cache simulation and index 2 onwards the independent URLS to be cached.
     *
     * @param defaultRepository the location on disk where the resources that are to be cached can be stored.
     *
     * @return List of {@link org.roblox.imagecache.types.ResultData} that represents the result of caching for each
     * of the URLS from the inputImagesUrls list.
     */
    private static List<ResultData> processInput(final List<String> inputImageUrls, final String defaultRepository) {
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

    private static void generateOutputData(final String outputFilePath, final List<ResultData> results) {
        try {
            fileIOUtils.deleteIfExists(outputFilePath);
            fileIOUtils.writeListToFile(outputFilePath, results);
        } catch (final IOException ex){
            throw new RuntimeException("Unable to write output file due to exception ", ex.getCause());
        }
    }
}
