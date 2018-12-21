package com.ziemsky.uploader.conf.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;

@Validated
@ConfigurationProperties("uploader")
public class ConfigProperties implements Config {
    // todo validate and deal with file not found on incorrect spring.config.additional-location / spring.config.location

    /**
     * Monitoring settings.
     */
    @NestedConfigurationProperty
    @NotNull
    private MonitoringProperties monitoring;

    @NestedConfigurationProperty
    @NotNull
    private GoogleProperties google;

    @Override public Monitoring monitoring() {
        return monitoring;
    }

    public MonitoringProperties getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(final MonitoringProperties monitoring) {
        this.monitoring = monitoring;
    }

    @Override public Google google() {
        return google;
    }

    public GoogleProperties getGoogle() {
        return google;
    }

    public void setGoogle(final GoogleProperties google) {
        this.google = google;
    }

    public static class MonitoringProperties implements Monitoring {

        /**
         * Path to the local directory monitored by the application.
         * Files placed in this location will be uploaded to the remote repository.
         */
        @NotNull
        private Path path;

        @Override public Path path() {
            return path;
        }

        public Path getPath() {
            return path;
        }

        public void setPath(final Path path) {
            this.path = path;
        }

        @Override public String toString() {
            return "MonitoringProperties{" +
                "path=" + path +
                '}';
        }
    }

    public static class GoogleProperties implements Google {

        @NestedConfigurationProperty
        @NotNull
        private DriveProperties drive;

        @Override public Drive drive() {
            return drive;
        }

        public DriveProperties getDrive() {
            return drive;
        }

        public void setDrive(final DriveProperties drive) {
            this.drive = drive;
        }

        @Override public String toString() {
            return "GoogleProperties{" +
                "drive=" + drive +
                '}';
        }
    }

    public static class DriveProperties implements Drive {

        @NotBlank
        private String applicationName;
        @NotBlank
        private String applicationUserName;
        @NotNull
        private Path tokensDirectory;
        @NotNull
        private Path credentialsFile;

        @Override public String applicationName() {
            return applicationName;
        }

        private String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(final String applicationName) {
            this.applicationName = applicationName;
        }

        @Override public String applicationUserName() {
            return applicationUserName;
        }

        private String getApplicationUserName() {
            return applicationUserName;
        }

        public void setApplicationUserName(final String applicationUserName) {
            this.applicationUserName = applicationUserName;
        }

        @Override public Path tokensDirectory() {
            return tokensDirectory;
        }

        private Path getTokensDirectory() {
            return tokensDirectory;
        }

        public void setTokensDirectory(final Path tokensDirectory) {
            this.tokensDirectory = tokensDirectory;
        }

        @Override public Path credentialsFile() {
            return credentialsFile;
        }

        private Path getCredentialsFile() {
            return credentialsFile;
        }

        public void setCredentialsFile(final Path credentialsFile) {
            this.credentialsFile = credentialsFile;
        }

        @Override public String toString() {
            return "DriveProperties{" +
                "applicationName='" + applicationName + '\'' +
                ", applicationUserName='" + applicationUserName + '\'' +
                ", tokensDirectory=" + tokensDirectory +
                ", credentialsFile=" + credentialsFile +
                '}';
        }
    }


    @Override public String toString() {
        return "ConfigProperties{" +
            "monitoring=" + monitoring +
            ", google=" + google +
            '}';
    }
}