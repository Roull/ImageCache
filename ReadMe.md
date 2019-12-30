# Image Cache

## Introduction
In this implementation, the cache is created using Java's 
[LinkedHashMap](https://docs.oracle.com/javase/8/docs/api/java/util/LinkedHashMap.html).
It provides method called as load which will check if the given key is present in the map, 
if not present/cache miss, download the resource by making external call using 
[HTTPConnection](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html)
If key is present(cache hit), the byte[] can be used by the caller instead of fetching from the disk.
When the capacity of cache(on disk) is not enough to place any more objects, we evict the 
oldest entry that was touched/used. Since LinkedHashMap maintains the entries according
to access order, we can remove the oldest entry from the map as well as remove the
corresponding resources from the disk.

## Getting Started


## How to Run?


## After Running?
Please make sure you delete the resources that were downloaded on your 
disk if you no longer require those resources. 

## Future Work
* BuddyWarming - Since the images/resources would actually be stored on the disk,
 incase of application shutdown, on subsequent restart of the application 
 we can have an async thread that will parse the contents of the disk(cache) 
 and can populate the map with the cache entries. So that cache hits increase on restart.
 * Add Metrics
* Add other implementations of cache and provide a factory to make it configurable
