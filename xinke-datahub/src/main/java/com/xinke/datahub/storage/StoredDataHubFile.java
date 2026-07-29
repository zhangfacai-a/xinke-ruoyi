package com.xinke.datahub.storage;

import java.nio.file.Path;

public record StoredDataHubFile(Path path, String hash, long size, String extension) { }
