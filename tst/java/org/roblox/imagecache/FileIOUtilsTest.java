package org.roblox.imagecache;

import org.junit.Assert;
import org.junit.Test;
import org.roblox.imagecache.utils.FileIOUtils;

import java.util.List;

public class FileIOUtilsTest {
    FileIOUtils fileUtils;

    private static final String INPUT_FILE_PATH = System.getProperty("user.dir") + "/test-resources/valid_input.txt";

    @Test(expected = NullPointerException.class)
    public void test_readFile_throws_null(){
        fileUtils.readFileToList(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_readFile_throws_emptyFileName(){
        fileUtils.readFileToList("");
    }

    @Test
    public void test_read_success(){
        List<String> lines = fileUtils.readFileToList(INPUT_FILE_PATH);
        Assert.assertTrue(lines.size() > 1);
    }
}