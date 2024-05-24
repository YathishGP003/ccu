package a75f.io.renatus.composables

import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.VerticalDivider
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
import androidx.compose.material3.AlertDialog
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

@Composable
fun DeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    toDelete: String
) {
    Dialog(onDismissRequest = { onDismissRequest() },
        properties= DialogProperties(
            dismissOnClickOutside = false)
    ) {
        Box(modifier= Modifier
            .size(1500.dp, 1300.dp)
            .padding(start = 44.dp, end = 32.dp),
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
                            tint = Color.Gray
                        )
                        Spacer(modifier= Modifier.width(16.dp))
                        Text(buildAnnotatedString {
                            withStyle(style= SpanStyle(fontSize= 20.sp )) {
                                append("Are you sure you want to delete ")
                            }
                            withStyle(style= SpanStyle(fontWeight=FontWeight.Bold, fontSize= 20.sp )) {
                                append(toDelete + "?")
                            }
                        }, modifier = Modifier.align(Alignment.CenterVertically))
                        Spacer(modifier= Modifier.width(32.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text("CANCEL",color= ComposeUtil.primaryColor)
                        }
                        Spacer(modifier= Modifier.width(20.dp))
                        VerticalDivider(15,18)
                        Spacer(modifier = Modifier.width(20.dp))
                        TextButton(onClick = onConfirmation) {
                            Text("DELETE", color = ComposeUtil.primaryColor)
                        }
                    }
                }

            }
        }

    }
}