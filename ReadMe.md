# Image Cache

## Introduction
In this implementation, the image cache is created using Java's 
[LinkedHashMap](https://docs.oracle.com/javase/8/docs/api/java/util/LinkedHashMap.html)
as a [Read Through Cache](https://docs.oracle.com/cd/E15357_01/coh.360/e15723/cache_rtwtwbra.htm#COHDG5177).

It provides a method called as load which behaves as below:
1. Check : will check if the given key is present in the cache, if not present a cache miss is recorded.
2. Download : Resource(in this case image) is downloaded by making external call using 
[HTTPConnection](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html). 
    
    * If the size of image to be downloaded is less than the size of the cache, we don't download that image and 
        record as an error for this entry in the output file. 
    * Otherwise, we check the size of storage available on disk. 
        * If there is lesser space than the size of the object, we delete the Least Recently Used items from the cache
         and make just enough room on disk to download the image. CacheEviction counts are recorded.
        * Otherwise we just download the image to the disk
3. Update: Upon successful download the cache(map) is updated with the key and bytes as the value. 
4. If key is present(cache hit), the byte[] can be used by the caller instead of fetching from the disk. 
Cache hit is recorded.

When the capacity of cache(on disk) is not enough to place any more objects, we evict the 
oldest entry that was touched/used. Since LinkedHashMap maintains the entries according
to access order, we can remove the oldest entry from the map as well as remove the
corresponding resources from the disk.

## Getting Started
You can run the cache simulation in default mode by providing -d as an option(as show below).
When you run in the default mode, it uses the current direction as the path to search for parsing the input file, 
storage on disk for caching and generates output file in the same location.

You can provide arguments for input and output by using -i and -o options

## How to Run?
* Option 1 : Without any arguments to the tool

``
java -jar ./target/ImageCache-1.0-SNAPSHOT-jar-with-dependencies.jar
``

``
Running the image cache simulation in default Mode
Running the image cache simulation using input from file /Users/ramanoh/workspace/ImageCache/image-cache-test-input.txt
Generated output of image cache simulation to output file: /Users/ramanoh/workspace/ImageCache/image-cache-test-output.txt
Path for downloaded resources would be: /Users/ramanoh/workspace/ImageCache
``

* Option 2: Running using custom input file, custom outputfile path and custom path to downloaded resources
. Using options -i for input, -o for output, -p for path to downloaded resources. 
``
java -jar ./target/ImageCache-1.0-SNAPSHOT-jar-with-dependencies.jar -i /tmp/image-cache-test-input.txt  -o /tmp/image-cache-test-output_new.txt -p /tmp/
``

``
Running the image cache simulation using input from file /tmp/image-cache-test-input.txt
Generated output of image cache simulation to output file: /tmp/image-cache-test-output_new.txt
Path for downloaded resources would be: /tmp
``

* Option 3 : Running in default mode using option -d

``
java -jar ./target/ImageCache-1.0-SNAPSHOT-jar-with-dependencies.jar -d
``

``
Running the image cache simulation in default Mode
Running the image cache simulation using input from file /Users/ramanoh/workspace/ImageCache/image-cache-test-input.txt
Generated output of image cache simulation to output file: /Users/ramanoh/workspace/ImageCache/image-cache-test-output.txt
Path for downloaded resources would be: /Users/ramanoh/workspace/ImageCache
``
## After Running?
Please make sure you delete the resources that were downloaded on your 
disk if you no longer require those files. 

## Future Work
* BuddyWarming - Since the images/resources would actually be stored on the disk,
 incase of application shutdown, on subsequent restart of the application 
 we can have an async thread that will parse the contents of the disk(cache) 
 and can populate the map with the cache entries. So that cache hits increase on restart.
* Add Metrics
* Add other implementations of cache and provide a factory to make it configurable/pluggable architecture.
