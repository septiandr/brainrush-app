package com.ga.brainrush.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabItem(icon = Icons.Filled.Home, label = "Beranda", selected = selected == 0) {
                onSelect(0)
            }
            TabItem(icon = Icons.Filled.Notifications, label = "Notifikasi", selected = selected == 1) {
                onSelect(1)
            }
        }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Color(0xFF4CAF50) else Color.Unspecified)
        Text(text = label, color = if (selected) Color(0xFF4CAF50) else Color.Unspecified)
    }
}