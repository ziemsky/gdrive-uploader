package com.ziemsky.uploader.conf.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.nio.file.Path;

@ConfigurationProperties("uploader")
public class ConfigProperties implements Config {

    /**
     * Monitoring settings.
     */
    @NestedConfigurationProperty
    private MonitoringProperties monitoring;

    @Override public Monitoring monitoring() {
        return monitoring;
    }

    public MonitoringProperties getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(final MonitoringProperties monitoring) {
        this.monitoring = monitoring;
    }

    public static class MonitoringProperties implements Monitoring {

        /**
         * Path to the local directory monitored by the application.
         * Files placed in this location will be uploaded to the remote repository.
         */
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
    }
}