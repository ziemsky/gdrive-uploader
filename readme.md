# GDrive Uploader
Small service scanning a directory for files and uploading them to Google Drive, deleting the originals. Remote files
are automatically grouped in daily folders and rotated, with oldest folders deleted to meet disk space quota.

Use case: image files captured by security cameras and saved into local directory; the service discovers them and
secures in Google Drive.

# Status
Unusable - just started working on it.

## Licence
Entire source code in this repository is licenced to use as specified in [MIT licence][mit licence].

Summary of the intention for allowed use of the code from this repository: 
* Feel free to use it in any form (source code or binary) and for any purpose (personal use or commercial).
* Feel free to use entire files or snippets of the code with or without modifications or simply use it as examples to
  inspire your own solutions.
* You don't have to state my authorship in any way and you don't have to include any specific licence.
* Don't hold me responsible for any results of using this code.

For more details of this licence see:
* The [LICENCE][licence file] file included in this project.
* [Licence][mit licence] section of [opensource.org].