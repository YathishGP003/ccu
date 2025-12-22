package a75f.io.renatus.util

import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.renatus.R
import a75f.io.renatus.composables.PinPassword
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.modbus.util.ALERT
import a75f.io.renatus.modbus.util.OK
import android.content.Context
import android.text.Spanned
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                        withStyle(style = SpanStyle(color = White)) {
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

@Composable
fun ShowPinDialog(
    onDismiss: () -> Unit,
    selectedPinTitle: String,
    pinDigits: MutableList<Int>,
    onSave: () -> Unit,

) {
    var saveState by remember { mutableStateOf(false) }
    val context = Globals.getInstance().applicationContext
    CcuLog.i(L.TAG_CCU_DOMAIN, "ShowPinDialog called with title: $selectedPinTitle and pinDigits: ${pinDigits.toList()}")


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .height(370.dp)
                .background(White, RoundedCornerShape(4.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("${context.getString(R.string.pin_lock)} $selectedPinTitle", fontSize = 23.sp, color = Color.Black, fontWeight = FontWeight.Bold,fontFamily = myFontFamily , modifier = Modifier.padding(start = 10.dp) )
            Box(modifier = Modifier.padding(start = 20.dp)) {
                PinSection(
                    onSaveState = { saveState = it },
                    pinDigits = pinDigits
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(onClick = onDismiss,modifier = Modifier.align(alignment = Alignment.CenterVertically)) {
                    Text(context.getString(R.string.button_cancel), color = primaryColor, fontSize = 22.sp, fontFamily = myFontFamily)
                }

                Divider(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterVertically)
                        .height(20.dp)
                        .width(2.dp),
                    color = Color.LightGray
                )

                TextButton(onClick = { onSave() }, enabled = saveState, modifier = Modifier.align(alignment = Alignment.CenterVertically))
                {
                    Text(
                        context.getString(R.string.button_save),
                        color = if (saveState) primaryColor else Color.Gray,
                        fontSize = 22.sp, fontFamily = myFontFamily
                    )
                }
            }
        }
    }
}


@Composable
fun PinSection(
    onSaveState: (Boolean) -> Unit,
    pinDigits: MutableList<Int>
) {
    // Store the original pin state for comparison
    val originalPin = remember { mutableStateListOf(0, 0, 0, 0) }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(4) { index ->
            PinPassword(
                items = (0..9).map { it.toString() },
                modifier = Modifier.weight(1f),
                textModifier = Modifier.width(30.dp),
                startIndex = pinDigits[index],
                onChanged = { selected ->
                    val selectedInt = selected.toInt()
                    // Update the pin digit
                    pinDigits[index] = selectedInt
                    // Compare current PIN with original PIN or the pin 0,0,0,0 making save btn to disable state
                    val isChanged =  (pinDigits.toList()== listOf(0,0,0,0) || pinDigits.toList() != originalPin.toList())
                    onSaveState(isChanged)
                    originalPin[index]= selectedInt
                }
            )
        }
    }
}

@Composable
fun Gif75Loader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            AndroidView(
                factory = { context ->
                    a75f.io.renatus.views.GifView(context, null).apply {
                        id = View.generateViewId()
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        when {
                            CCUUiUtil.isDaikinEnvironment(context) ->
                                setImageResource(R.drawable.daikin_loader)
                            CCUUiUtil.isCarrierThemeEnabled(context) ->
                                setImageResource(R.drawable.carrier_loader)
                            CCUUiUtil.isAiroverseThemeEnabled(context) ->
                                setImageResource(R.drawable.airoverse_loader)
                            else ->
                                setImageResource(R.drawable.loader1)
                        }
                    }
                },
                modifier = Modifier.wrapContentSize()
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
data class Option(val index: Int, val value: String, val dis: String? = null)