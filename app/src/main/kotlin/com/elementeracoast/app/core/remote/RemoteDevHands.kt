package com.elementeracoast.app.core.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class RemoteDevSettings(
    @SerialName("github_read") val githubRead: Boolean = true,
    @SerialName("github_write") val githubWrite: Boolean = false,
    @SerialName("github_dangerous") val githubDangerous: Boolean = false,
    @SerialName("ci_actions") val ciActions: Boolean = true,
    @SerialName("apk_artifact") val apkArtifact: Boolean = true,
    @SerialName("wolf_update") val wolfUpdate: Boolean = true,
    @SerialName("notion_read") val notionRead: Boolean = true,
    @SerialName("notion_write") val notionWrite: Boolean = true,
    @SerialName("notion_delete") val notionDelete: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RemoteDevSettingsResponse(
    val ok: Boolean = true,
    val settings: RemoteDevSettings = RemoteDevSettings(),
    @SerialName("run_id") val runId: String? = null
)

@Serializable
data class RemoteGithubRepoCheck(
    val repo: String = "",
    val readable: Boolean = false,
    @SerialName("default_branch") val defaultBranch: String? = null,
    val private: Boolean? = null,
    @SerialName("error_type") val errorType: String? = null
)

@Serializable
data class RemoteGithubSelfCheck(
    @SerialName("github_token_present") val githubTokenPresent: Boolean = false,
    @SerialName("allowed_repos") val allowedRepos: List<String> = emptyList(),
    @SerialName("github_read_enabled") val githubReadEnabled: Boolean = true,
    @SerialName("repo_metadata_readable") val repoMetadataReadable: Boolean? = null,
    @SerialName("can_read_default_branch") val canReadDefaultBranch: Boolean? = null,
    @SerialName("can_read_actions") val canReadActions: Boolean? = null,
    val repos: List<RemoteGithubRepoCheck> = emptyList()
)

@Serializable
data class RemoteNotionSelfCheck(
    @SerialName("notion_token_present") val notionTokenPresent: Boolean = false,
    @SerialName("root_page_id_present") val rootPageIdPresent: Boolean = false,
    @SerialName("notion_read_enabled") val notionReadEnabled: Boolean = true,
    @SerialName("root_page_readable") val rootPageReadable: Boolean? = null,
    @SerialName("root_page_title") val rootPageTitle: String? = null,
    @SerialName("can_append_test_block") val canAppendTestBlock: Boolean? = null,
    @SerialName("write_test_requires_confirmation") val writeTestRequiresConfirmation: Boolean = true,
    @SerialName("error_type") val errorType: String? = null
)

@Serializable
data class RemoteDevSelfCheckResponse(
    val ok: Boolean = true,
    val settings: RemoteDevSettings = RemoteDevSettings(),
    val github: RemoteGithubSelfCheck = RemoteGithubSelfCheck(),
    val notion: RemoteNotionSelfCheck = RemoteNotionSelfCheck(),
    @SerialName("run_ids") val runIds: List<String> = emptyList()
)

@Serializable
data class RemoteNativeUpdate(
    val repo: String? = null,
    @SerialName("workflow_run_id") val workflowRunId: Long? = null,
    @SerialName("run_number") val runNumber: Long? = null,
    @SerialName("commit_sha") val commitSha: String? = null,
    @SerialName("artifact_id") val artifactId: Long? = null,
    @SerialName("artifact_name") val artifactName: String? = null,
    @SerialName("artifact_created_at") val artifactCreatedAt: String? = null,
    @SerialName("artifact_expires_at") val artifactExpiresAt: String? = null,
    @SerialName("version_code") val versionCode: Int? = null,
    @SerialName("version_name") val versionName: String? = null,
    @SerialName("application_id") val applicationId: String? = null,
    @SerialName("stable_signing") val stableSigning: Boolean? = null,
    @SerialName("apk_sha256") val apkSha256: String? = null,
    @SerialName("apk_filename") val apkFilename: String? = null,
    @SerialName("overwrite_installable") val overwriteInstallable: Boolean? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("update_time") val updateTime: String? = null,
    @SerialName("update_notes") val updateNotes: String? = null,
    @SerialName("known_risk") val knownRisk: String? = null,
    val reason: String? = null
)

@Serializable
data class RemoteDevUpdate(
    @SerialName("pwa_cache_version") val pwaCacheVersion: String? = null,
    val native: RemoteNativeUpdate? = null,
    val available: Boolean = false,
    val reason: String? = null,
    val release: String? = null
)

@Serializable
data class RemoteDevUpdateResponse(
    val ok: Boolean = true,
    val update: RemoteDevUpdate = RemoteDevUpdate(),
    @SerialName("run_id") val runId: String? = null
)

@Serializable
data class RemoteDevRun(
    val id: String = "",
    @SerialName("target_system") val targetSystem: String = "",
    @SerialName("action_name") val actionName: String = "",
    @SerialName("target_ref") val targetRef: String? = null,
    @SerialName("operation_type") val operationType: String = "read",
    @SerialName("confirmation_required") val confirmationRequired: Boolean = false,
    @SerialName("confirmation_confirmed") val confirmationConfirmed: Boolean = false,
    val status: String = "",
    @SerialName("input_summary") val inputSummary: JsonElement? = null,
    @SerialName("output_summary") val outputSummary: JsonElement? = null,
    @SerialName("error_summary") val errorSummary: String? = null,
    val related: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null
)

@Serializable
data class RemoteDevRunsResponse(
    val ok: Boolean = true,
    val runs: List<RemoteDevRun> = emptyList()
)

@Serializable
data class RemoteDevActionRequest(
    val action: String,
    val params: JsonObject = JsonObject(emptyMap()),
    @SerialName("confirm_text") val confirmText: String? = null
)

@Serializable
data class RemoteDevActionResponse(
    val ok: Boolean = true,
    val result: JsonElement? = null,
    @SerialName("run_id") val runId: String? = null
)
