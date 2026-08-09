package com.example.omni.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "omni_projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val targetPlatform: String = "Android Native (Kotlin/Compose)",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val activeSnapshotId: String? = null,
    val buildCount: Int = 0,
    val status: String = "ACTIVE"
)

@Entity(tableName = "omni_project_files")
data class ProjectFileEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val filePath: String,
    val fileType: String,
    val content: String,
    val version: Int = 1,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "omni_agent_decisions")
data class AgentDecisionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val agentRole: String, // ARCHITECT, DEVELOPER, TESTER, REVIEWER
    val prompt: String,
    val reasoning: String,
    val proposalTitle: String,
    val changesSummary: String,
    val status: String, // PENDING_APPROVAL, APPROVED, APPLIED, VERIFIED, REJECTED, ROLLED_BACK
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "omni_snapshots")
data class SnapshotEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val label: String,
    val description: String,
    val snapshotFilesJson: String, // JSON payload storing file map snapshot
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "omni_build_history")
data class BuildHistoryEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val buildNumber: Int,
    val status: String, // SUCCESS, FAILED, BUILDING
    val snapshotId: String,
    val logOutput: String,
    val apkUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
