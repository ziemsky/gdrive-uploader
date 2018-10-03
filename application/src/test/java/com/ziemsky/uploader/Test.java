package com.ziemsky.uploader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// todo remove
public class Test {


    private Path inboundDir = Paths.get("/tmp/inbound");

    @org.junit.Test
    public void name() throws IOException {
        for (int i = 0; i < 15; i++) {
            Path path = Paths.get(inboundDir.toString(), "test_" + i + ".jpg");
            Files.deleteIfExists(path);
            Files.createFile(path);
        }
    }
}
