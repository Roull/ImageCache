package org.roblox.imagecache;

import org.junit.Test;
import org.roblox.imagecache.utils.FileIOUtils;

public class FileIOUtilsTest {
    FileIOUtils fileUtils;

    @Test(expected = NullPointerException.class)
    public void test_readFile_throws_null(){
        fileUtils.readFileToList(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_readFile_throws_emptyFileName(){
        fileUtils.readFileToList("");
    }
}