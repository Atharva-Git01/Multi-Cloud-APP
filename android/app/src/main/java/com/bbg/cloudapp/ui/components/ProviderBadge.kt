package com.bbg.cloudapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.ui.theme.BoxColor
import com.bbg.cloudapp.ui.theme.DropboxColor
import com.bbg.cloudapp.ui.theme.GoogleDriveColor
import com.bbg.cloudapp.ui.theme.MegaColor
import com.bbg.cloudapp.ui.theme.OneDriveColor
import com.bbg.cloudapp.ui.theme.PCloudColor

@Composable
fun ProviderBadge(
    provider: CloudProvider,
    modifier: Modifier = Modifier
) {
    val bgColor = providerColor(provider)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = provider.emoji, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = provider.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = bgColor
        )
    }
}

fun providerColor(provider: CloudProvider): Color {
    return when (provider) {
        CloudProvider.GOOGLE -> GoogleDriveColor
        CloudProvider.ONEDRIVE -> OneDriveColor
        CloudProvider.MEGA -> MegaColor
        CloudProvider.BOX -> BoxColor
        CloudProvider.PCLOUD -> PCloudColor
        CloudProvider.DROPBOX -> DropboxColor
    }
}
