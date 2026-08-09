package com.example.omni.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omni.data.db.OmniDatabase
import com.example.omni.data.repository.OmniRepository
import com.example.omni.domain.model.*
import com.example.omni.domain.orchestrator.SwarmEngineState
import com.example.omni.domain.orchestrator.SwarmOrchestrator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommandCentreViewModel(application: Application) : AndroidViewModel(application) {

    private val db = OmniDatabase.getInstance(application)
    val repository = OmniRepository(db.omniDao())
    val orchestrator = SwarmOrchestrator(repository)

    val engineState: StateFlow<SwarmEngineState> = orchestrator.engineState

    val allProjects = repository.allProjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    private val _userPrompt = MutableStateFlow("")
    val userPrompt: StateFlow<String> = _userPrompt.asStateFlow()

    private val _selectedRole = MutableStateFlow(SwarmAgentRole.ARCHITECT)
    val selectedRole: StateFlow<SwarmAgentRole> = _selectedRole.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProject = _selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getProject(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentFiles = _selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getProjectFiles(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val agentDecisions = _selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getAgentDecisions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val snapshots = _selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getSnapshots(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val buildHistory = _selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getBuildHistory(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allProjects.collect { projects ->
                if (projects.isEmpty()) {
                    val defaultId = repository.createProject(
                        name = "Omni App One",
                        description = "Native Android App powered by Swarm Multi-Agent Architecture.",
                        initialPrompt = "Build a cyber-brutalist multi-agent Android builder application"
                    )
                    _selectedProjectId.value = defaultId
                } else if (_selectedProjectId.value == null) {
                    _selectedProjectId.value = projects.first().id
                }
            }
        }
    }

    fun setPrompt(prompt: String) {
        _userPrompt.value = prompt
    }

    fun setSelectedRole(role: SwarmAgentRole) {
        _selectedRole.value = role
    }

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
    }

    fun createNewProject(name: String, description: String, prompt: String) {
        viewModelScope.launch {
            val newId = repository.createProject(name, description, prompt)
            _selectedProjectId.value = newId
        }
    }

    fun triggerSwarmAction() {
        val projectId = _selectedProjectId.value ?: return
        val prompt = _userPrompt.value.ifBlank { "Generate cyber-brutalist feature component" }

        viewModelScope.launch {
            orchestrator.runSwarmPipeline(
                projectId = projectId,
                userPrompt = prompt,
                selectedRole = _selectedRole.value
            )
        }
    }

    fun approveProposal(proposal: SwarmAgentProposal, snapshotId: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            orchestrator.approveAndApplyMutation(projectId, proposal, snapshotId)
        }
    }

    fun rejectProposal(proposalId: String) {
        viewModelScope.launch {
            repository.updateDecisionStatus(proposalId, MutationStatus.REJECTED)
            orchestrator.resetState()
        }
    }

    fun rollbackToSnapshot(snapshotId: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            orchestrator.executeRollback(projectId, snapshotId, "Manual user rollback requested from Command Centre")
        }
    }
}
