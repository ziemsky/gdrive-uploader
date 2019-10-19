[![Build Status](https://travis-ci.com/ziemsky/gdrive-uploader.svg?branch=master)](https://travis-ci.com/ziemsky/gdrive-uploader)

# GDrive Uploader
Small, JVM-based application which, once started, continuously scans a pre-configured directory for `*.jpg` files and
uploads them to Google Drive, deleting the originals, and then waiting for new ones to appear.

Remote files are automatically grouped in daily folders and rotated, with oldest folders deleted to meet disk space
quota.

Use case: image files captured by security cameras and saved into local directory; the service discovers them and
secures in Google Drive.

# Status
Unusable - just started working on it.

# Usage

## Running
Application can be executed by running:
```$bash
java -jar <path/to>/uploader.jar <ARGUMENTS>
```

`<ARGUMENTS>` allow to customise behaviour of the application and are optional as the application comes with
[certain defaults][application.conf] that you may choose to use or override. 

### Configuration
There is a number of ways available to provide the application with custom configuration.

#### Configuration file
Custom configuration file (in [HOCON] format) is a convenient way to override larger number of options.

See [application.conf] for complete list of supported options, default values, and for illustration of the format.

In that file, you are free to specify all or just some of the options used by the application. Any option you specify
overrides the default values, leaving any unspecified options to be automatically set to default values.

To make the application use the file, name it `application.conf` and do any of the following:
* place it in either of the following locations:
  * current directory, i.e. directory you are in when you execute the `java` command,
  * `/config` sub-directory of the current directory,
* place it in any directory of your choosing, and let the application know where to find it by
  providing it with the path to the file's parent directory (the path has to either be absolute or relative to the
  current directory, and has to end with `/`) specified as either of the following:
  * command line argument (substituting `<ARGUMENTS>` above): `--spring.config.additional-location=file:/<path/to/the/file's/parent/dir/>`
  * environment variable `SPRING_CONFIG_ADDITIONAL_LOCATION` set to `file:/<path/to/the/file's/parent/dir/>`;
    actual syntax for setting environment variable depends on the shell you are using. 

As the application relies on Spring Boot's support for externalized configuration, you can find more ways to set
configuration options described in Spring Boot's [documentation][spring-boot-ext-config].

#### Individual options
If only a small subset of options need overriding it can be more convenient to specify them directly in the command line
or as environment variables.

For example, if the config file looks like this:
```
uploader {
    monitoring {
        path = /some/default/path
        someOtherOption = 10
    }
}
```
...then you can override individual options by specifying them in place of `<ARGUMENTS>` in the command line like this:
```
--uploader.monitoring.path=/my/new/path --uploader.monitoring.someOtherOption=60 
```

To specify options as environment variables, uppercase their names and replace delimiters with underscores;
for example, the above two would become `UPLOADER_MONITORING_PATH` and `UPLOADER_MONITORING_POLLINGFREQUENCYSECS`,
respectively. Actual syntax for setting environment variable depends on the shell you are using.

#### Logging
Application uses Logback and, by default, only logs to console.

To change logging configuration use settings of Spring Boot, as described in [Spring Boot documentation][spring-boot-logging].

Typically,  

## Authorising application to access Google account
Before first run, application has to be authorised to access Google account.

To be documented.

# Development

## Running

Run Gradle tasks:

* `:application:bootRun` - Runs this project as a Spring Boot application without assembling the JAR file first.
* `:application:appStart` - Starts the application from the assembled JAR file as a background process. Use to run the
  app in the background for e2e tests; for normal run call `:application:bootRun` task.
* `:application:appStop` - Kills process identified by PID found in file application.pid. Uses system command 'kill' which,
  currently, limits its use to Unix-based systems.

Assuming executing Gradle wrapper (recommended) from project's root directory, examples with custom options passed in:
```
./gradlew :application:appStart -PappStartArgs='--spring.config.additional-location=file:/tmp/app/cnf/ --another.option.to.override=anotherValue\ withEscapedSpace'
```
When calling task `appStart` with arguments that contain spaces make sure to escape those spaces with `\` as above.  

```
./gradlew :application:bootRun --args='--uploader.monitoring.path=/tmp/nodir --another.option.to.override=<some-value>'
```

```
./gradlew :application:bootRun --args='--spring.config.additional-location=file:/<path/to/the/file's/parent/dir>'
```

## Testing
See [testing.md](testing.md)

# Licence
Entire source code in this repository is licenced to use as specified in [MIT licence][opensource.org-mit].

Summary of the intention for allowed use of the code from this repository: 
* Feel free to use it in any form (source code or binary) and for any purpose (personal use or commercial).
* Feel free to use entire files or snippets of the code with or without modifications or simply use it as examples to
  inspire your own solutions.
* You don't have to state my authorship in any way and you don't have to include any specific licence.
* Don't hold me responsible for any results of using this code.

For more details of this licence see:
* The [LICENCE](LICENCE) file included in this project.
* [MIT licence][opensource.org-mit] in [opensource.org].


[application.conf]:       https://github.com/ziemsky/gdrive-uploader/blob/master/application/src/main/resources/application.properties
[HOCON]:                  https://github.com/lightbend/config/blob/master/HOCON.md
[opensource.org]:         https://opensource.org/    
[opensource.org-mit]:     https://opensource.org/licenses/MIT
[spring-boot-ext-config]: https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html
[spring-boot-logging]:    https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-logging
[logback-config]:         http://logback.qos.ch/manual/configuration.html                