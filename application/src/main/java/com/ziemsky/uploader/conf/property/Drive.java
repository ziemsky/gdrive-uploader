package com.ziemsky.uploader.conf.property;

import java.nio.file.Path;

public interface Drive {

    String applicationName();

    String applicationUserName();

    Path tokensDirectory();

    Path credentialsFile();
}
