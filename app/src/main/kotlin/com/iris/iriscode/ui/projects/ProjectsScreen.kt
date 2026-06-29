package com.iris.iriscode.ui.projects

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.iris.iriscode.domain.model.Project
import com.iris.iriscode.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onProjectClick: (Project) -> Unit,
    onCreateProject: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val projects by viewModel.projects.collectAsState()
    var contextMenuProject by remember { mutableStateOf<Project?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IrisBackground)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IrisPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.FolderKanban,
                        contentDescription = null,
                        tint = IrisPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Projects",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(IrisSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.FolderOpen,
                                contentDescription = null,
                                tint = IrisTextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No projects yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = IrisTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to create your first project",
                            style = MaterialTheme.typography.bodySmall,
                            color = IrisTextSecondary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.requestDelete(project)
                                    false
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> IrisError
                                        else -> IrisSurfaceContainer
                                    },
                                    label = "swipe"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(color)
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Lucide.Trash2,
                                        tint = IrisBackground,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true
                        ) {
                            ProjectCard(
                                project = project,
                                onClick = { onProjectClick(project) },
                                onLongClick = { contextMenuProject = project }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateProject,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .navigationBarsPadding(),
            containerColor = IrisPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = "Create Project",
                modifier = Modifier.size(22.dp)
            )
        }
    }

    contextMenuProject?.let { project ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { contextMenuProject = null },
            offset = DpOffset(16.dp, 0.dp),
            modifier = Modifier
                .background(IrisSurface)
                .clip(RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = { Text("Rename", color = IrisText) },
                onClick = { contextMenuProject = null },
                leadingIcon = {
                    Icon(Lucide.Pencil, contentDescription = null, tint = IrisTextSecondary, modifier = Modifier.size(16.dp))
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = IrisError) },
                onClick = {
                    contextMenuProject = null
                    viewModel.requestDelete(project)
                },
                leadingIcon = {
                    Icon(Lucide.Trash2, contentDescription = null, tint = IrisError, modifier = Modifier.size(16.dp))
                }
            )
        }
    }

    if (state.showCreateSheet) {
        CreateProjectSheet(
            name = state.newProjectName,
            path = state.newProjectPath,
            onNameChange = viewModel::updateNewProjectName,
            onPathChange = viewModel::updateNewProjectPath,
            onCreate = viewModel::createProject,
            onDismiss = viewModel::hideCreateSheet
        )
    }

    state.showDeleteConfirm?.let { project ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Lucide.Trash2,
                        contentDescription = null,
                        tint = IrisError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Project")
                }
            },
            text = { Text("Are you sure you want to delete \"${project.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete", color = IrisError)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .clip(RoundedCornerShape(14.dp))
            .background(IrisSurfaceContainer)
            .padding(start = 0.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(IrisPrimary)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(IrisPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.FolderCode,
                contentDescription = null,
                tint = IrisPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = project.path,
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = IrisTextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}
