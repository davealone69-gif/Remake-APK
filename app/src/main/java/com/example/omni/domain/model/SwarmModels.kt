package com.example.omni.domain.model

enum class SwarmAgentRole(val displayName: String, val badgeColorHex: String) {
    ARCHITECT("Architect", "#2196F3"),
    DEVELOPER("Developer", "#4CAF50"),
    TESTER("Tester", "#FF9800"),
    REVIEWER("Reviewer", "#9C27B0")
}

enum class MutationStatus {
    PENDING_APPROVAL,
    APPROVED,
    APPLIED,
    VERIFIED,
    REJECTED,
    ROLLED_BACK
}

enum class BuildState {
    IDLE,
    BUILDING,
    SUCCESS,
    FAILED
}

data class SwarmAgentProposal(
    val id: String,
    val projectId: String,
    val role: SwarmAgentRole,
    val title: String,
    val description: String,
    val rationale: String,
    val proposedFiles: List<ProposedFileChange>,
    val timestamp: Long = System.currentTimeMillis(),
    var status: MutationStatus = MutationStatus.PENDING_APPROVAL
)

data class ProposedFileChange(
    val filePath: String,
    val oldContent: String,
    val newContent: String,
    val changeSummary: String
)

data class SnapshotData(
    val snapshotId: String,
    val projectId: String,
    val label: String,
    val filesJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
