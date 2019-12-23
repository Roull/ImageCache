//package org.roblox.imagecache;
//
//import lombok.Builder;
//import lombok.NonNull;
//import org.apache.commons.collections4.map.LinkedMap;
//
//import java.io.File;
//import java.net.URI;
//
///**
// * Cache implemented on disk using LRU eviction since the images can be of large size.
// */
//@Builder
//public class OnDiskLRUCache {
//    /*
//     * This is the disk usage percentage up to which eviction happens
//     */
//    private final int permittedDiskUsageSizeInBytes;
//
//    /*
//     * This linked hash map provides quick lookup of artifact data and keeps
//     * track of usage order
//     */
//    public SynchronizedLinkedMap<ResourceIdentifier, ArtifactData> cachedArtifacts;
//
//    private final @NonNull FileIOUtils fileCopyUtils;
//    /*
//     * root directory for storing cached artifacts
//     */
//    private final URI artifactDirectory;
//
//    public OnDiskLRUCache(int permittedDiskUsageSizeInBytes, @NonNull URI artifactDirectory) {
//        this.permittedDiskUsageSizeInBytes = permittedDiskUsageSizeInBytes;
//        this.cachedArtifacts = new SynchronizedLinkedMap<>(new LinkedMap<ResourceIdentifier, ArtifactData>());
//        this.artifactDirectory = artifactDirectory;
//        fileCopyUtils = new FileIOUtils();
//    }
//
//    /**
//     * This is the entry point for getting an Artifact from the on-disk cache.
//     *
//     * If the artifactData keyed by the resourceIdentifier is in the
//     * "cachedArtifacts" map, it returns that artifactData, else it fetches the
//     * artifact from web request and downloads it.
//     */
////    public ArtifactData getArtifact(ResourceIdentifier artifactIdentifier) {
////        try {
////            if (cachedArtifacts.containsKey(artifactIdentifier)) {
////                /*
////                 * In memory, but validate that the file is on disk; if not,
////                 * refetch
////                 */
////                ArtifactData potentialArtifactData = cachedArtifacts.get(artifactIdentifier);
////                if (potentialArtifactData != null &&
////                        new File(potentialArtifactData.getDiskLocation()).exists()) {
////                    /*
////                     * Recording a cache hit
////                     */
////                    //metrics.addCount(MetricsUtils.CACHE_HIT, 1, Unit.ONE);
////                    //recordNewCall(artifactIdentifier);
////
////                    /*
////                     * When the artifact is in memory and it's validated, we
////                     * call recordNewCall on the artifact, which elevates it to
////                     * the front of the linkedMap AND updates the usage data. If
////                     * we just take the potentialArtifactData which is what was
////                     * already in memory, we will return the old version which
////                     * is incorrect -number of calls will be off by one, and
////                     * last called time will be the last time it was called, not
////                     * the current time. Therefore we need to call get again.
////                     */
////                    log.info("Artifact data is returned from memory [ ArtifactIdentifier: " + artifactIdentifier + "]");
////
////                    artifactData = cachedArtifacts.get(artifactIdentifier);
////                } else {
////                    log.info("Artifact files are not on disk, proceed to fetch from MASS [ ArtifactIdentifier: "
////                            + artifactIdentifier + "]");
////                    artifactData = fetchArtifact(artifactIdentifier, optionalGetArtifactResponse, metrics);
////                }
////            }
//}
