package com.ziemsky.uploader.securing.infrastructure.googledrive.model


data class GDriveFolder(val name: String, val id: String) {

    private val GDRIVE_ITEM_ID_PATTERN = Regex("^([0-9a-zA-Z_-]){33}$")

    init {
        require(name.isNotBlank()) {
            "Name should be at least one character long and comprising not only white space characters, but was '$name'."
        }

        require(GDRIVE_ITEM_ID_PATTERN.matches(id)) {
            "Id should be 33 characters long, comprising only characters 0-9, a-z, A-Z, '-' and '_', but was '$id'."
        }
    }
}