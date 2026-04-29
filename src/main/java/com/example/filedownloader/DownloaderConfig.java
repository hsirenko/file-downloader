package com.example.filedownloader;

import java.net.URI;
import java.nio.file.Path;

record DownloaderConfig(
    URI uri,
    Path outputPath,
    int threads,
    int chunkSize
) {
    
}