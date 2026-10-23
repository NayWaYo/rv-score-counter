package com.rvetiveau.rvscorecount.domain.model

/** A backup file once read and checked in full (Story 12.2), before anything is replaced. */
sealed interface BackupReading {
    /** Saved on [createdAt] (epoch milliseconds), holding [content]. */
    data class Valid(val createdAt: Long, val content: BackupContent) : BackupReading
    data object NotABackup : BackupReading
    /** Saved by a newer version of the app. */
    data object NewerFormat : BackupReading
    /** A backup with invalid values, duplicates or broken references. */
    data object Damaged : BackupReading
    data object Unreadable : BackupReading
}
