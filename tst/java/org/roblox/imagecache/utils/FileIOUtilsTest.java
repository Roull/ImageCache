package org.roblox.imagecache.utils;

import org.junit.Assert;
import org.junit.Test;
import java.util.List;

public class FileIOUtilsTest {
    private final FileIOUtils fileUtils = new FileIOUtils();

    private static final String INPUT_FILE_PATH = System.getProperty("user.dir") + "/test-resources/valid_input.txt";

    @Test(expected = NullPointerException.class)
    public void test_readFile_throws_null(){
        this.fileUtils.readFileToList(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_readFile_throws_emptyFileName(){
        this.fileUtils.readFileToList("");
    }

    @Test
    public void test_read_success(){
        final List<String> lines = this.fileUtils.readFileToList(INPUT_FILE_PATH);
        Assert.assertTrue(lines.size() > 1);
    }
}