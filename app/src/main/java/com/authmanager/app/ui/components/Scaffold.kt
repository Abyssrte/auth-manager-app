package com.authmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.authmanager.app.ui.theme.*

/** Consistent top bar used on every non-home screen — title, back arrow, refresh action. */
@Composable
fun AppTopBar(title: String, onBack: () -> Unit, onRefresh: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgRoot)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary,
            modifier = Modifier
                .clickable { onBack() }
                .padding(8.dp),
        )
        Text(title, style = TitleMedium, modifier = Modifier.weight(1f).padding(start = 4.dp))
        if (onRefresh != null) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh",
                tint = AccentBlue,
                modifier = Modifier
                    .clickable { onRefresh() }
                    .padding(8.dp),
            )
        }
    }
}
