package com.ziemsky.uploader.application.conf

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

    fun upload(): Upload
}

interface Upload {
    fun maxConcurrentUploads(): Int
}

interface Monitoring {
    fun path(): Path
}

interface Rotation {
    fun maxDailyFolders(): Int
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

    @NestedConfigurationProperty
    val upload: UploadProperties = UploadProperties()

    override fun monitoring(): Monitoring = monitoring

    override fun rotation(): Rotation = rotation

    override fun google(): Google = google

    override fun upload(): Upload = upload

    override fun toString(): String =
            "MutableUploaderConfigProperties(monitoring=$monitoring, rotation=$rotation, google=$google, upload=$upload)"


    class MonitoringProperties : Monitoring {

        /**
         * Path to the local directory monitored by the application.
         * Files placed in this location will be uploaded to the remote repository.
         */
        @NotNull
        lateinit var path: Path

        override fun path(): Path = path

        override fun toString(): String = "MonitoringProperties(path=$path)"
    }


    class GoogleProperties : Google {

        @NestedConfigurationProperty
        val drive: DriveProperties = DriveProperties()

        override fun drive(): Drive = drive

        override fun toString(): String = "GoogleProperties(drive=$drive)"
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

        override fun applicationName(): String = applicationName

        override fun applicationUserName(): String = applicationUserName

        override fun tokensDirectory(): Path = tokensDirectory

        override fun credentialsFile(): Path = credentialsFile

        override fun toString(): String =
                "DriveProperties(applicationName='$applicationName', applicationUserName='$applicationUserName', tokensDirectory=$tokensDirectory, credentialsFile=$credentialsFile)"
    }

    class RotationProperties: Rotation {

        @Positive
        var maxDailyFolders: Int = 5

        override fun maxDailyFolders(): Int = maxDailyFolders

        override fun toString(): String = "RotationProperties(maxDailyFolders=$maxDailyFolders)"
    }

    class UploadProperties: Upload {

        @Positive
        var maxConcurrentUploads: Int = 2

        override fun maxConcurrentUploads(): Int = maxConcurrentUploads
        override fun toString(): String {
            return "UploadProperties(maxConcurrentUploads=$maxConcurrentUploads)"
        }


    }
}