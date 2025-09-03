package a75f.io.renatus.composables

import a75f.io.renatus.compose.ComposeUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


const val NO_COMPRESSOR = "The O/B changeover relay is mapped, but the compressor is not mapped."
const val NO_OB_REALLY = "The compressor is mapped, but the O/B changeover relay is not mapped."
const val NO_MAT_SENSOR = "The DCV Damper is mapped, but Mixed Air Temperature sensor is not mapped."

@Composable
fun ErrorDialog(
    msg: String,
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = { onDismissRequest() },
        properties= DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false)
    ) {
        Box(modifier= Modifier
            .size(900.dp, 1300.dp)
            .padding(start = 44.dp, end = 44.dp),
            contentAlignment = Alignment.Center
        )
        {
            Surface(
                modifier = Modifier
                    .padding(16.dp),
                color = Color.White,
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(
                            Icons.Rounded.Warning,
                            "Warning Icon",
                            modifier = Modifier.size(60.dp),
                            tint = ComposeUtil.primaryColor
                        )
                        Spacer(modifier= Modifier.width(16.dp))
                        Column {
                            Text("Error", style= TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp) )
                            Text(buildAnnotatedString {
                                withStyle(style= SpanStyle(fontSize= 20.sp )) {
                                    append("Invalid HSS Configuration")
                                }
                            })
                        }
                        Spacer(modifier= Modifier.width(32.dp))
                    }

                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(buildAnnotatedString {
                            withStyle(style= SpanStyle(fontSize= 20.sp )) {
                                append(msg)
                            }
                        })
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text("GOT IT", color= ComposeUtil.primaryColor, fontSize = 22.sp)
                        }
                    }
                }

            }
        }

    }
}