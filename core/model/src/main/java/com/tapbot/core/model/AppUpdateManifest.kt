package com.tapbot.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Manifest defining the latest available version of TapBot and update policies.
 * Supports both camelCase and snake_case properties for maximum remote compatibility.
 */
@Serializable
data class AppUpdateManifest(
    val latestVersionCode: Long = 0L,
    val latestVersionName: String = "",
    val minRequiredVersionCode: Long = 0L,
    val forceUpdate: Boolean = false,
    val title: String = "TapBot Update Available",
    val releaseNotes: String = "",
    val releaseDate: String = "",
    val apkUrl: String = "",
    val downloadUrl: String = "",
    val githubReleaseUrl: String = "",
    val sha256: String? = null,
    // snake_case aliases for maximum remote flexibility
    @SerialName("latest_version_code") val latestVersionCodeSnake: Long? = null,
    @SerialName("latest_version_name") val latestVersionNameSnake: String? = null,
    @SerialName("min_required_version_code") val minRequiredVersionCodeSnake: Long? = null,
    @SerialName("force_update") val forceUpdateSnake: Boolean? = null,
    @SerialName("release_notes") val releaseNotesSnake: String? = null,
    @SerialName("release_date") val releaseDateSnake: String? = null,
    @SerialName("apk_url") val apkUrlSnake: String? = null,
    @SerialName("download_url") val downloadUrlSnake: String? = null,
    @SerialName("github_release_url") val githubReleaseUrlSnake: String? = null
) {
    val effectiveLatestVersionCode: Long
        get() = latestVersionCodeSnake ?: latestVersionCode

    val effectiveLatestVersionName: String
        get() = latestVersionNameSnake ?: latestVersionName

    val effectiveMinRequiredVersionCode: Long
        get() = minRequiredVersionCodeSnake ?: minRequiredVersionCode

    val effectiveForceUpdate: Boolean
        get() = forceUpdateSnake ?: forceUpdate

    val effectiveReleaseNotes: String
        get() = releaseNotesSnake ?: releaseNotes

    val effectiveReleaseDate: String
        get() = releaseDateSnake ?: releaseDate

    val effectiveApkUrl: String
        get() = (apkUrlSnake ?: apkUrl).ifBlank { effectiveDownloadUrl }

    val effectiveDownloadUrl: String
        get() = (downloadUrlSnake ?: downloadUrl).ifBlank { effectiveApkUrl }

    val effectiveGithubReleaseUrl: String
        get() = (githubReleaseUrlSnake ?: githubReleaseUrl).ifBlank {
            "https://github.com/VarshuDev/tapbot/releases"
        }

    /**
     * Returns true if a newer version exists compared to [currentVersionCode].
     */
    fun isUpdateAvailable(currentVersionCode: Long): Boolean {
        return effectiveLatestVersionCode > currentVersionCode
    }

    /**
     * Returns true if the update is mandatory and cannot be skipped or dismissed by the user.
     */
    fun isForceUpdate(currentVersionCode: Long): Boolean {
        return isUpdateAvailable(currentVersionCode) &&
            (effectiveForceUpdate || currentVersionCode < effectiveMinRequiredVersionCode)
    }
}
