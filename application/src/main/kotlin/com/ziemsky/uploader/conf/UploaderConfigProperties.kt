package com.ziemsky.uploader.conf

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.validation.annotation.Validated
import java.nio.file.Path
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

interface UploaderConfigProperties {
    /**
     * @return Configuration related to monitoring of inbound files.
     */
    fun monitoring(): Monitoring

    fun rotation(): Rotation

    fun google(): Google
}

interface Monitoring {
    fun path(): Path
}

interface Rotation {
    fun maxDailyFolders(): Int
    fun cron(): String
}

interface Google {
    fun drive(): Drive
}

interface Drive {

    fun applicationName(): String

    fun applicationUserName(): String

    fun tokensDirectory(): Path

    fun credentialsFile(): Path
}

@Validated
@ConfigurationProperties("uploader")
class MutableUploaderConfigProperties : UploaderConfigProperties {
    // todo validate and deal with file not found on incorrect spring.config.additional-location / spring.config.location

    /**
     * Monitoring settings.
     */
    @NestedConfigurationProperty
    val monitoring: MonitoringProperties = MonitoringProperties()

    @NestedConfigurationProperty
    val rotation: RotationProperties = RotationProperties()

    @NestedConfigurationProperty
    val google: GoogleProperties = GoogleProperties()

    override fun monitoring(): Monitoring {
        return monitoring
    }

    override fun rotation(): Rotation {
        return rotation
    }

    override fun google(): Google {
        return google
    }

    override fun toString(): String {
        return "MutableUploaderConfigProperties(monitoring=$monitoring, rotation=$rotation, google=$google)"
    }


    class MonitoringProperties : Monitoring {

        /**
         * Path to the local directory monitored by the application.
         * Files placed in this location will be uploaded to the remote repository.
         */
        @NotNull
        lateinit var path: Path

        override fun path(): Path {
            return path
        }

        override fun toString(): String {
            return "MonitoringProperties(path=$path)"
        }


    }

    class GoogleProperties : Google {

        @NestedConfigurationProperty
        val drive: DriveProperties = DriveProperties()

        override fun drive(): Drive {
            return drive
        }

        override fun toString(): String {
            return "GoogleProperties(drive=$drive)"
        }

    }

    class DriveProperties : Drive {

        @NotBlank
        lateinit var applicationName: String

        @NotBlank
        lateinit var applicationUserName: String

        @NotNull
        lateinit var tokensDirectory: Path

        @NotNull
        lateinit var credentialsFile: Path

        override fun applicationName(): String {
            return applicationName
        }

        override fun applicationUserName(): String {
            return applicationUserName
        }

        override fun tokensDirectory(): Path {
            return tokensDirectory
        }

        override fun credentialsFile(): Path {
            return credentialsFile
        }

        override fun toString(): String {
            return "DriveProperties(applicationName='$applicationName', applicationUserName='$applicationUserName', tokensDirectory=$tokensDirectory, credentialsFile=$credentialsFile)"
        }

    }

    class RotationProperties: Rotation {

        @Positive
        var maxDailyFolders: Int = 5

        @NotBlank
        var cron: String = "0 1 */3 * * *"

        override fun maxDailyFolders(): Int {
            return maxDailyFolders
        }

        override fun cron(): String {
            return cron
        }

        override fun toString(): String {
            return "RotationProperties(maxDailyFolders=$maxDailyFolders, cron='$cron')"
        }
    }
}