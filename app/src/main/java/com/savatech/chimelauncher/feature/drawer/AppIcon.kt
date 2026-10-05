package com.savatech.chimelauncher.feature.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.IconCache
import androidx.compose.material3.MaterialTheme

@Composable
fun AppIcon(app: AppInfo, iconCache: IconCache, size: Dp) {
    val icon: ImageBitmap? by produceState(initialValue = null, app, iconCache) {
        value = null
        value = iconCache.get(app)
    }
    if (icon == null) {
        Box(
            Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        )
    } else {
        Image(
            bitmap = requireNotNull(icon),
            contentDescription = null,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    }
}
