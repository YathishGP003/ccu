package a75f.io.renatus.profiles.profileUtils

import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.profiles.CopyConfiguration.Companion.getModuleName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment


open class PasteBannerFragment : Fragment() {

    companion object {
        @Composable
        fun PasteCopiedConfiguration(
            onPaste: () -> Unit,
            onClose: () -> Unit
        ) {
            Box(
                modifier = Modifier
                    .background(color = Color(0xFFEBECED))
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 27.dp, end = 27.dp, top = 15.dp, bottom = 15.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val annotatedText = buildAnnotatedString {
                        append(stringResource(id = R.string.Toast_configuration))
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(getModuleName())
                        pop()
                        append(stringResource(id = R.string.Toast_copied_clipboard))
                        pushStringAnnotation(tag = "PASTE_TAG", annotation = "paste_action")
                        pushStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Medium,
                                color = ComposeUtil.primaryColor // Orange color
                            )
                        )
                        append(" PASTE")
                        pop()
                        pop()
                    }
                    ClickableText(
                        text = annotatedText,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 20.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.width(1050.dp),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(
                                tag = "PASTE_TAG",
                                start = offset,
                                end = offset
                            )
                                .firstOrNull()?.let {
                                    onPaste()
                                }
                        }
                    )

                    Spacer(modifier = Modifier.weight(0.3f))

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = ComposeUtil.primaryColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close",
                            colorFilter = ColorFilter.tint(ComposeUtil.primaryColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

}