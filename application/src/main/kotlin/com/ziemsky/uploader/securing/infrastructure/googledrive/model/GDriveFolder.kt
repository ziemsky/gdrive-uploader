package com.ziemsky.uploader.securing.infrastructure.googledrive.model


data class GDriveFolder(val name: String, val id: String) {

    private val GDRIVE_ITEM_ID_PATTERN = Regex("^([0-9a-zA-Z_-])+$")

    init {
        require(name.isNotBlank()) {
            "Name should be non-empty and non-blank, but was '$name'."
        }

        require(GDRIVE_ITEM_ID_PATTERN.matches(id)) {
            "Id should be non-empty, non-blank and comprise only characters 0-9, a-z, A-Z, '-' and '_', but was '$id'."
        }
    }
}