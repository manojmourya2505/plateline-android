package com.inouiw.plateline.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inouiw.plateline.ui.theme.*

/** Decode a saved photo. sample 4 gives a small thumbnail, 1 the full image. */
internal fun decodeImage(bytes: ByteArray, sample: Int = 1): ImageBitmap? {
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}

/** Full-screen view of one saved photo or paper. onReplace adds a button to pick a new one. */
@Composable
fun FullScreenPhoto(title: String, bytes: ByteArray, onClose: () -> Unit, onReplace: (() -> Unit)? = null) {
    val image = remember(bytes) { decodeImage(bytes) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (image != null) {
                Image(image, title, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = sora(16, 700), color = White, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Close", tint = White) }
            }
            if (onReplace != null) {
                Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp)) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Yellow).clickable(onClick = onReplace)
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Refresh, null, tint = Ink)
                        Spacer(Modifier.width(8.dp))
                        Text("Replace photo", style = sora(15, 700), color = Ink)
                    }
                }
            }
        }
    }
}
