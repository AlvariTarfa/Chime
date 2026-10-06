package com.savatech.chimelauncher.feature.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.IconCache
import com.savatech.chimelauncher.data.settings.IconShape
import com.savatech.chimelauncher.core.theme.iconShapeCornerPercent
import androidx.compose.material3.MaterialTheme

@Composable
fun AppIcon(
    app: AppInfo,
    iconCache: IconCache,
    size: Dp,
    iconShape: IconShape = IconShape.CIRCLE,
    iconPackPackage: String? = null,
) {
    val icon: ImageBitmap? by produceState(
        initialValue = null,
        app,
        iconCache,
        iconShape,
        iconPackPackage,
    ) {
        value = null
        value = iconCache.get(app, iconPackPackage = iconPackPackage)
    }
    val shape = iconShapeCornerPercent(iconShape)?.let { percent ->
        if (percent == 50) CircleShape else RoundedCornerShape(percent)
    }
    if (icon == null) {
        Box(
            Modifier
                .size(size)
                .then(if (shape == null) Modifier else Modifier.clip(shape))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Image(
            bitmap = requireNotNull(icon),
            contentDescription = null,
            modifier = Modifier.size(size).then(if (shape == null) Modifier else Modifier.clip(shape)),
            contentScale = ContentScale.Fit,
        )
    }
}
