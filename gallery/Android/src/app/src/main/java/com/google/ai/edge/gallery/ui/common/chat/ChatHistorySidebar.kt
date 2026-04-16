/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.common.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.proto.ChatSession

@Composable
fun ChatHistorySidebar(
  dataStoreRepository: DataStoreRepository,
  onSessionClicked: (ChatSession) -> Unit,
  onNewChatClicked: () -> Unit,
  onDeleteSession: (ChatSession) -> Unit,
  modifier: Modifier = Modifier
) {
  val sessions by dataStoreRepository.getAllChatSessions().collectAsState(initial = emptyList())

  ModalDrawerSheet(
    modifier = modifier.fillMaxHeight().width(300.dp),
    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      // Sidebar Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        Icon(
          Icons.Rounded.History,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "Clinical History",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // New Chat Button
      NavigationDrawerItem(
        label = { Text("New Analysis") },
        selected = false,
        onClick = onNewChatClicked,
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        colors = NavigationDrawerItemDefaults.colors(
          unselectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
          unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(bottom = 16.dp)
      )

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      Text(
        text = "Recent Conversations",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
      )

      // Sessions List
      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(vertical = 4.dp)
      ) {
        items(sessions.sortedByDescending { it.timestamp }) { session ->
          ChatSessionItem(
            session = session,
            onClick = { onSessionClicked(session) },
            onDelete = { onDeleteSession(session) }
          )
        }
      }
      
      // Footer/Branding
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Project Airgap v1.0",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )
    }
  }
}

@Composable
private fun ChatSessionItem(
  session: ChatSession,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.small)
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 4.dp)
  ) {
    Icon(
      Icons.Rounded.ChatBubbleOutline,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = session.title.ifEmpty { "Clinical Scan" },
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "Gemma 4 • ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(session.timestamp))}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
    }
    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
      Icon(
        Icons.Rounded.DeleteOutline,
        contentDescription = "Delete",
        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
        modifier = Modifier.size(16.dp)
      )
    }
  }
}
