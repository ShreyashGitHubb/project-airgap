/*
 * Copyright 2026 Google LLC
 */

package com.google.ai.edge.gallery.ui.common.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A persistent badge that reinforces the 'Zero-Trust' nature of the app.
 */
@Composable
fun MedicalPrivacyBadge(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFFE8F5E9).copy(alpha = 0.8f)) // Soft Mint Background
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Icon(
      imageVector = Icons.Rounded.Shield,
      contentDescription = "Shield Icon",
      tint = Color(0xFF2E7D32), // Medical Green
      modifier = Modifier.size(14.dp)
    )
    Text(
      text = "Local Processing Only",
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
      ),
      color = Color(0xFF2E7D32)
    )
  }
}
