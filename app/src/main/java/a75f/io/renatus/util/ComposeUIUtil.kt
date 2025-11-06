package a75f.io.renatus.util

import a75f.io.logic.Globals
import a75f.io.renatus.R
import a75f.io.renatus.modbus.util.ALERT
import a75f.io.renatus.modbus.util.OK
import android.content.Context
import android.text.Spanned
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

val highPriorityDispatcher = Executors.newSingleThreadExecutor(object : ThreadFactory {
    private val counter = AtomicInteger(0)

    override fun newThread(r: Runnable): Thread {
        return Thread(r, "HighPriorityThread-${counter.incrementAndGet()}").apply {
            priority = Thread.MAX_PRIORITY
        }
    }
}).asCoroutineDispatcher()

fun isSystemTab(): Boolean {
    return Globals.getInstance().getSelectedTab() == 1
}

@Composable
fun GifLoader(modifier: Modifier = Modifier, gifResource: Int) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                Glide.with(context)
                    .asGif()
                    .load(gifResource)
                    .apply(RequestOptions().centerInside())
                    .into(this)
            }
        }
    )
}

@Composable
fun AddProgressGif(){
    if(isSystemTab()) {
        Dialog(
            onDismissRequest = { false },
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier.wrapContentWidth().semantics { contentDescription = "testgifBox" },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GifLoader(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(50.dp)
                            .semantics { contentDescription = "testgifLoader" },
                        gifResource = R.drawable.loader_75f
                    )
                    Text(
                        text = "Loading System Profile",
                        color = White,
                        fontSize = 20.sp,
                        modifier = Modifier.wrapContentWidth().semantics { contentDescription = "testgifTest" }
                    )
                }
            }
        }
    }
}

fun showErrorDialog(context: Context, message: Spanned) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle(ALERT)
    builder.setIcon(R.drawable.ic_warning)
    builder.setMessage(message)
    builder.setCancelable(false)
    builder.setPositiveButton(OK) { dialog, _ ->
        dialog.dismiss()
    }
    builder.create().show()
}

@Composable
fun ErrorToastMessage(
    message: String,
    onDismiss: () -> Unit = {}
) {
    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, -10), // adjust height from bottom
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2C2C2C)) // dark gray
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                            append("Error! ")
                        }
                        withStyle(style = SpanStyle(color = Color.White)) {
                            append(message)
                        }
                    },
                    fontSize = 20.sp
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(2000)
        onDismiss()
    }
}