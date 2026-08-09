package com.example.omni.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OmniDao {
    // Projects
    @Query("SELECT * FROM omni_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM omni_projects WHERE id = :projectId")
    fun getProjectById(projectId: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM omni_projects WHERE id = :projectId")
    suspend fun getProjectSync(projectId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM omni_projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)

    // Project Files
    @Query("SELECT * FROM omni_project_files WHERE projectId = :projectId ORDER BY filePath ASC")
    fun getFilesForProject(projectId: String): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM omni_project_files WHERE projectId = :projectId AND filePath = :filePath")
    suspend fun getFileByPath(projectId: String, filePath: String): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>)

    @Query("DELETE FROM omni_project_files WHERE projectId = :projectId AND filePath = :filePath")
    suspend fun deleteFile(projectId: String, filePath: String)

    // Agent Decisions
    @Query("SELECT * FROM omni_agent_decisions WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getDecisionsForProject(projectId: String): Flow<List<AgentDecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: AgentDecisionEntity)

    @Query("UPDATE omni_agent_decisions SET status = :status WHERE id = :decisionId")
    suspend fun updateDecisionStatus(decisionId: String, status: String)

    // Snapshots
    @Query("SELECT * FROM omni_snapshots WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getSnapshotsForProject(projectId: String): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM omni_snapshots WHERE id = :snapshotId")
    suspend fun getSnapshotById(snapshotId: String): SnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity)

    // Build History
    @Query("SELECT * FROM omni_build_history WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getBuildHistoryForProject(projectId: String): Flow<List<BuildHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildHistory(build: BuildHistoryEntity)
}
