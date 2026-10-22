package com.rvetiveau.rvscorecount.domain.scan

/** Reads the card numbers shown on a photo, on the device and offline. */
interface TrumpReader {
    /**
     * The numbers within [numbers] read on the photo stored at [photoPath], each once; empty when
     * none is recognized. The photo is deleted once decoded; throws when it cannot be.
     */
    suspend fun read(photoPath: String, numbers: IntRange): Set<Int>
}
