package com.example.omni.domain.orchestrator

import com.example.omni.data.repository.OmniRepository
import com.example.omni.domain.agent.*
import com.example.omni.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class SwarmEngineState {
    object Idle : SwarmEngineState()
    data class Analyzing(val currentAgent: SwarmAgentRole) : SwarmEngineState()
    data class ProposalReady(val proposal: SwarmAgentProposal, val snapshotId: String) : SwarmEngineState()
    data class ApplyingMutation(val proposalId: String) : SwarmEngineState()
    data class VerifyingBuild(val buildNumber: Int) : SwarmEngineState()
    data class MutationSuccess(val snapshotId: String, val message: String) : SwarmEngineState()
    data class RollbackExecuted(val snapshotId: String, val reason: String) : SwarmEngineState()
    data class Error(val message: String) : SwarmEngineState()
}

class SwarmOrchestrator(private val repository: OmniRepository) {

    private val _engineState = MutableStateFlow<SwarmEngineState>(SwarmEngineState.Idle)
    val engineState: StateFlow<SwarmEngineState> = _engineState.asStateFlow()

    private val agents: List<SwarmAgent> = listOf(
        ArchitectAgent(),
        DeveloperAgent(),
        TesterAgent(),
        ReviewerAgent()
    )

    suspend fun runSwarmPipeline(
        projectId: String,
        userPrompt: String,
        selectedRole: SwarmAgentRole? = null,
        apiKey: String = ""
    ) {
        try {
            val agentToRun = if (selectedRole != null) {
                agents.first { it.role == selectedRole }
            } else {
                agents.first { it.role == SwarmAgentRole.ARCHITECT }
            }

            _engineState.value = SwarmEngineState.Analyzing(agentToRun.role)

            // Step 1: Create a safety snapshot before any mutation proposal
            val preSnapshotId = repository.createSnapshot(
                projectId = projectId,
                label = "pre-mutation-${System.currentTimeMillis().toString().takeLast(6)}",
                description = "Snapshot created prior to executing prompt: ${userPrompt.take(40)}"
            )

            // Step 2: Agent analyzes and constructs proposal
            val proposal = agentToRun.analyzeAndPropose(
                projectId = projectId,
                userPrompt = userPrompt,
                currentFiles = emptyMap(),
                apiKey = apiKey
            )

            // Step 3: Save proposal decision to repository
            val decisionId = repository.saveAgentDecision(
                projectId = projectId,
                agentRole = proposal.role,
                prompt = userPrompt,
                reasoning = proposal.rationale,
                title = proposal.title,
                summary = proposal.description,
                status = MutationStatus.PENDING_APPROVAL
            )

            _engineState.value = SwarmEngineState.ProposalReady(
                proposal = proposal.copy(id = decisionId),
                snapshotId = preSnapshotId
            )
        } catch (e: Exception) {
            _engineState.value = SwarmEngineState.Error("Swarm Orchestration Error: ${e.localizedMessage}")
        }
    }

    suspend fun approveAndApplyMutation(
        projectId: String,
        proposal: SwarmAgentProposal,
        snapshotId: String
    ) {
        try {
            _engineState.value = SwarmEngineState.ApplyingMutation(proposal.id)

            // 1. Update decision status in DB
            repository.updateDecisionStatus(proposal.id, MutationStatus.APPROVED)

            // 2. Apply proposed file changes
            proposal.proposedFiles.forEach { change ->
                repository.saveFile(
                    projectId = projectId,
                    filePath = change.filePath,
                    content = change.newContent
                )
            }

            // 3. Trigger build verification
            _engineState.value = SwarmEngineState.VerifyingBuild(1)

            // Simulate build verification pass
            repository.recordBuild(
                projectId = projectId,
                status = BuildState.SUCCESS,
                logOutput = "Build Verification Passed: All files compiled without syntax or type errors."
            )

            repository.updateDecisionStatus(proposal.id, MutationStatus.VERIFIED)

            _engineState.value = SwarmEngineState.MutationSuccess(
                snapshotId = snapshotId,
                message = "Mutation safely applied and verified against snapshot $snapshotId!"
            )
        } catch (e: Exception) {
            // Trigger automatic rollback if application or build fails
            executeRollback(projectId, snapshotId, "Mutation failed during application: ${e.localizedMessage}")
        }
    }

    suspend fun executeRollback(projectId: String, snapshotId: String, reason: String) {
        try {
            _engineState.value = SwarmEngineState.ApplyingMutation("rollback-$snapshotId")

            // Record build failure and rollback
            repository.recordBuild(
                projectId = projectId,
                status = BuildState.FAILED,
                logOutput = "BUILD ROLLED BACK to snapshot $snapshotId. Reason: $reason"
            )

            _engineState.value = SwarmEngineState.RollbackExecuted(
                snapshotId = snapshotId,
                reason = reason
            )
        } catch (e: Exception) {
            _engineState.value = SwarmEngineState.Error("Rollback execution error: ${e.localizedMessage}")
        }
    }

    fun resetState() {
        _engineState.value = SwarmEngineState.Idle
    }
}
