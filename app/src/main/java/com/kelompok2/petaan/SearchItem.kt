package com.kelompok2.petaan

import kotlinx.serialization.Serializable

@Serializable
data class SearchItem (
    val subject: String,
    val location: String,
    val objectId: String,
    var image: ByteArray? = null
)