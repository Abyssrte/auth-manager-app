package com.authmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.authmanager.app.ui.theme.*

/** Primary call-to-action button — blue fill. */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) AccentBlue else BorderSubtle)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TitleMedium, color = if (enabled) BgRoot else TextMuted)
    }
}

/** Secondary button — outlined, used for less prominent or destructive-adjacent actions. */
@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, isDestructive: Boolean = false, onClick: () -> Unit) {
    val borderColor = if (isDestructive) StatusRed else BorderSubtle
    val textColor = if (isDestructive) StatusRed else TextPrimary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TitleMedium, color = textColor)
    }
}

/** Card surface used to group content across every screen. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScopeAlias.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content,
    )
}

// Alias so SectionCard's lambda type reads cleanly without importing ColumnScope everywhere.
typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope

/** A monospace field with a tap-to-copy affordance — used for keys and hashes. */
@Composable
fun CopyableMonoField(label: String, value: String, modifier: Modifier = Modifier) {
    val clipboard: ClipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = LabelSmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgSurfaceElevated)
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                .clickable {
                    clipboard.setText(AnnotatedString(value))
                    copied = true
                }
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MonospaceKey,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (copied) StatusGreen else AccentBlue,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (copied) {
            Text("Copied to clipboard", style = LabelSmall.copy(color = StatusGreen), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

enum class BadgeTone { GREEN, RED, YELLOW, BLUE, NEUTRAL }

/** Small status pill — used for expiry status, block status, etc. */
@Composable
fun StatusBadge(text: String, tone: BadgeTone) {
    val (bg, fg) = when (tone) {
        BadgeTone.GREEN -> StatusGreenBg to StatusGreen
        BadgeTone.RED -> StatusRedBg to StatusRed
        BadgeTone.YELLOW -> StatusYellowBg to StatusYellow
        BadgeTone.BLUE -> AccentBlueBg to AccentBlue
        BadgeTone.NEUTRAL -> BgSurfaceElevated to TextSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = LabelSmall.copy(color = fg))
    }
}

/** Full-width banner for success/error feedback, dismissible. */
@Composable
fun Banner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val bg = if (isError) StatusRedBg else StatusGreenBg
    val fg = if (isError) StatusRed else StatusGreen
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onDismiss() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = BodyMedium.copy(color = fg), modifier = Modifier.weight(1f))
    }
}

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
    }
}

/** A single tappable menu row used on the three section-list screens. */
@Composable
fun MenuRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(icon, style = TitleLarge)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TitleMedium)
            Text(subtitle, style = BodyMedium)
        }
    }
}
