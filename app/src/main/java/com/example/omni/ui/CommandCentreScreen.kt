package com.example.omni.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.omni.data.db.*
import com.example.omni.domain.model.*
import com.example.omni.domain.orchestrator.SwarmEngineState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandCentreScreen(
    viewModel: CommandCentreViewModel,
    onNavigateToProjectBuilder: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val project by viewModel.currentProject.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val userPrompt by viewModel.userPrompt.collectAsStateWithLifecycle()
    val selectedRole by viewModel.selectedRole.collectAsStateWithLifecycle()
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()

    val decisions by viewModel.agentDecisions.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    val files by viewModel.currentFiles.collectAsStateWithLifecycle()
    val buildHistory by viewModel.buildHistory.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewProjectDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00E676),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Psychology,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "OMNI SWARM COMMAND CENTRE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                project?.name ?: "No Project Selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Drawer")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewProjectDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New Project")
                    }
                    IconButton(onClick = onNavigateToProjectBuilder) {
                        Icon(Icons.Filled.FolderZip, contentDescription = "Project Builder")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Header Banner & Active Swarm Roles
            ProjectHeaderCard(
                project = project,
                allProjects = allProjects,
                onSelectProject = { viewModel.selectProject(it) }
            )

            // Agent Role Selector Bar
            SwarmRoleSelectorRow(
                selectedRole = selectedRole,
                onSelectRole = { viewModel.setSelectedRole(it) }
            )

            // Prompt Input & Action Trigger
            PromptInputCard(
                prompt = userPrompt,
                onPromptChange = { viewModel.setPrompt(it) },
                selectedRole = selectedRole,
                isEngineBusy = engineState !is SwarmEngineState.Idle,
                onTriggerPipeline = { viewModel.triggerSwarmAction() }
            )

            // Live Engine State & Safe Mutation Pipeline Status
            EngineStatusCard(
                engineState = engineState,
                onApprove = { proposal, snapshotId -> viewModel.approveProposal(proposal, snapshotId) },
                onReject = { proposalId -> viewModel.rejectProposal(proposalId) },
                onDismiss = { viewModel.orchestrator.resetState() }
            )

            // Project Memory Tabs Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PROJECT MEMORY & MUTATION AUDIT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.1.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Decisions (${decisions.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Snapshots (${snapshots.size})") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Files (${files.size})") }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Builds (${buildHistory.size})") }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    when (selectedTab) {
                        0 -> DecisionsTabContent(decisions = decisions)
                        1 -> SnapshotsTabContent(
                            snapshots = snapshots,
                            onRollback = { viewModel.rollbackToSnapshot(it) }
                        )
                        2 -> FilesTabContent(files = files)
                        3 -> BuildLogsTabContent(buildHistory = buildHistory)
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, desc, prompt ->
                viewModel.createNewProject(name, desc, prompt)
                showNewProjectDialog = false
            }
        )
    }
}

@Composable
private fun ProjectHeaderCard(
    project: ProjectEntity?,
    allProjects: List<ProjectEntity>,
    onSelectProject: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        project?.name ?: "Omni App",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        project?.description ?: "Native Swarm Project",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00E676).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF00E676))
                ) {
                    Text(
                        "STATUS: ${project?.status ?: "ACTIVE"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "ACTIVE SWARM AGENT CLUSTER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SwarmAgentRole.values().forEach { role ->
                    AgentBadgeChip(role = role)
                }
            }
        }
    }
}

@Composable
private fun AgentBadgeChip(role: SwarmAgentRole) {
    val color = when (role) {
        SwarmAgentRole.ARCHITECT -> Color(0xFF2196F3)
        SwarmAgentRole.DEVELOPER -> Color(0xFF4CAF50)
        SwarmAgentRole.TESTER -> Color(0xFFFF9800)
        SwarmAgentRole.REVIEWER -> Color(0xFF9C27B0)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                role.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SwarmRoleSelectorRow(
    selectedRole: SwarmAgentRole,
    onSelectRole: (SwarmAgentRole) -> Unit
) {
    Column {
        Text(
            "SELECT PRIMARY SWARM AGENT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SwarmAgentRole.values().forEach { role ->
                val isSelected = role == selectedRole
                val activeColor = when (role) {
                    SwarmAgentRole.ARCHITECT -> Color(0xFF2196F3)
                    SwarmAgentRole.DEVELOPER -> Color(0xFF4CAF50)
                    SwarmAgentRole.TESTER -> Color(0xFFFF9800)
                    SwarmAgentRole.REVIEWER -> Color(0xFF9C27B0)
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectRole(role) },
                    label = { Text(role.displayName, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (role) {
                                SwarmAgentRole.ARCHITECT -> Icons.Filled.Architecture
                                SwarmAgentRole.DEVELOPER -> Icons.Filled.Code
                                SwarmAgentRole.TESTER -> Icons.Filled.BugReport
                                SwarmAgentRole.REVIEWER -> Icons.Filled.VerifiedUser
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PromptInputCard(
    prompt: String,
    onPromptChange: (String) -> Unit,
    selectedRole: SwarmAgentRole,
    isEngineBusy: Boolean,
    onTriggerPipeline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SWARM AGENT PROMPT INTERFACE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Target: ${selectedRole.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe app feature, architecture change, or module requirement...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text("QUICK PROMPT PRESETS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AssistChip(
                        onClick = { onPromptChange("Design modular Room database and repository layer") },
                        label = { Text("Room Database Architecture") }
                    )
                }
                item {
                    AssistChip(
                        onClick = { onPromptChange("Build cyber-brutalist Material 3 dashboard screen") },
                        label = { Text("Cyber-Brutalist UI Screen") }
                    )
                }
                item {
                    AssistChip(
                        onClick = { onPromptChange("Create JUnit verification test suite for state flow") },
                        label = { Text("Unit Test Verification") }
                    )
                }
                item {
                    AssistChip(
                        onClick = { onPromptChange("Perform security audit for permissions and memory leaks") },
                        label = { Text("Security Audit Pass") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onTriggerPipeline,
                enabled = !isEngineBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isEngineBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("SWARM AGENT WORKING...")
                } else {
                    Icon(Icons.Filled.Bolt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("RUN SWARM AGENT PIPELINE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EngineStatusCard(
    engineState: SwarmEngineState,
    onApprove: (SwarmAgentProposal, String) -> Unit,
    onReject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = engineState !is SwarmEngineState.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (engineState) {
                    is SwarmEngineState.Analyzing -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "SWARM AGENT ACTIVE: ${engineState.currentAgent.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text("Analyzing codebase and preparing snapshot proposal...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    is SwarmEngineState.ProposalReady -> {
                        val prop = engineState.proposal
                        Text(
                            "SAFE MUTATION PROPOSAL READY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(prop.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(prop.description, style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("SNAPSHOT ID: ${engineState.snapshotId}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                Text("RATIONALE: ${prop.rationale}", style = MaterialTheme.typography.bodySmall)
                                Text("FILES TO MUTATE: ${prop.proposedFiles.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onApprove(prop, engineState.snapshotId) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(6.dp))
                                Text("APPROVE & APPLY", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onReject(prop.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("REJECT")
                            }
                        }
                    }
                    is SwarmEngineState.ApplyingMutation -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Applying safe mutation to repository...", fontWeight = FontWeight.Bold)
                        }
                    }
                    is SwarmEngineState.VerifyingBuild -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Running build verification pass...", fontWeight = FontWeight.Bold)
                        }
                    }
                    is SwarmEngineState.MutationSuccess -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF00E676))
                                Spacer(Modifier.width(8.dp))
                                Text("MUTATION VERIFIED & APPLIED", fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                            }
                            Text(engineState.message, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                                Text("DISMISS")
                            }
                        }
                    }
                    is SwarmEngineState.RollbackExecuted -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text("ROLLED BACK TO SNAPSHOT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Text("Snapshot ID: ${engineState.snapshotId}", style = MaterialTheme.typography.labelSmall)
                            Text("Reason: ${engineState.reason}", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                                Text("DISMISS")
                            }
                        }
                    }
                    is SwarmEngineState.Error -> {
                        Column {
                            Text("SWARM ERROR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text(engineState.message, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                                Text("DISMISS")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun DecisionsTabContent(decisions: List<AgentDecisionEntity>) {
    if (decisions.isEmpty()) {
        Text("No agent decisions recorded yet. Run a swarm prompt above.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            decisions.take(10).forEach { decision ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ROLE: ${decision.agentRole}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatusPill(status = decision.status)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(decision.proposalTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(decision.reasoning, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotsTabContent(
    snapshots: List<SnapshotEntity>,
    onRollback: (String) -> Unit
) {
    if (snapshots.isEmpty()) {
        Text("No snapshots available yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            snapshots.forEach { snapshot ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(snapshot.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(snapshot.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("ID: ${snapshot.id}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        }
                        OutlinedButton(
                            onClick = { onRollback(snapshot.id) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ROLLBACK", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesTabContent(files: List<ProjectFileEntity>) {
    if (files.isEmpty()) {
        Text("No files generated in project memory yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            files.forEach { file ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(file.filePath, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Version: v${file.version} | Type: ${file.fileType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildLogsTabContent(buildHistory: List<BuildHistoryEntity>) {
    if (buildHistory.isEmpty()) {
        Text("No builds executed yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            buildHistory.forEach { build ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("BUILD #${build.buildNumber}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            StatusPill(status = build.status)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(build.logOutput, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bgColor, textColor) = when (status) {
        "APPROVED", "VERIFIED", "SUCCESS" -> Color(0xFF00E676).copy(alpha = 0.2f) to Color(0xFF00E676)
        "PENDING_APPROVAL" -> Color(0xFFFF9800).copy(alpha = 0.2f) to Color(0xFFFF9800)
        "REJECTED", "FAILED", "ROLLED_BACK" -> Color(0xFFFF5252).copy(alpha = 0.2f) to Color(0xFFFF5252)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Swarm Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Initial Swarm Prompt") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description, prompt)
                    }
                }
            ) {
                Text("CREATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
