package com.ziemsky.uploader;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageChannel;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static org.springframework.integration.dsl.Pollers.fixedDelay;
import static org.springframework.integration.file.dsl.Files.inboundAdapter;

@Configuration
public class UploaderConfig {

    final Logger log = LoggerFactory.getLogger(UploaderConfig.class);

    private @Value("${java.io.tmpdir:'/tmp'}/inbound") Path inboundDir;

    // todo make the cameras rename the files to jpg after upload
    // to prevent files being picked up by uploader before they've been fully uploaded by
    // cameras

    @Bean IntegrationFlow inboundFileReaderEndpoint() {

        log.info("Monitoring {}", inboundDir);

        return IntegrationFlows.from(inboundAdapter(
            // todo switch from polling to watch service? See docs for caveats!:
            // https://docs.spring.io/spring-integration/reference/html/files.html#watch-service-directory-scanner
            inboundDir.toFile(),
            Comparator.comparing(
                File::getName,
                (leftFileName, rightFileName) -> -1 * leftFileName.compareTo(rightFileName)
            ))
                .patternFilter("*.jpg"),
            endpointConfig -> endpointConfig.poller(fixedDelay(3, TimeUnit.SECONDS))
        )
            .channel(filesQueueChannel())
            .get();
    }


    @Bean IntegrationFlow outboundFileUploaderEndpoint() {
        return IntegrationFlows
            .from(this::filesQueueChannel)
            // .from(this::filesQueueChannel, sourcePollingChannelAdapterSpec -> sourcePollingChannelAdapterSpec.poller(Pollers.fixedDelay(10, TimeUnit.SECONDS)))
            // .handle(gDriveUploader())
            .handle(gDriveUploader())

            .get();
    }

    @NotNull private GenericHandler<File> gDriveUploader() {
        return (payload, headers) -> {
            log.debug("PAYLOAD: " + payload);
            log.debug("HEADERS: " + headers);
            return null;
        };
    }

    @Bean MessageChannel filesQueueChannel() {
        return MessageChannels.publishSubscribe().get();
    }
}
