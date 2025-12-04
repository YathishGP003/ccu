package a75f.io.renatus.bacnet

import a75f.io.logic.util.bacnet.BacnetConfigConstants.MSTP_CONFIGURATION
import a75f.io.renatus.R
import a75f.io.renatus.UtilityApplication
import a75f.io.renatus.bacnet.util.IP_CONFIGURATION
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Colors & Constants ---
private val BrandOrange = Color(0xFFDC4C00)
private val BrandGrayText = Color(0xFF6B6B6B)
private val BrandLightGray = Color(0xFFE0E0E0)
private val DisabledText = Color(0xFFB3B3B3)
private val PlaceholderGray = Color(0xFF9E9E9E)
private val SelectionHighlight = Color(0xFFFFF3E0)

// Initialization Checks
private val disableMstp = !UtilityApplication.isBacnetMstpInitialized()
private val disableIp = !UtilityApplication.isBACnetIntialized()

@Composable
fun BacnetPickConfigurationScreen(
    viewModel: BacnetPickConfigViewModel,
    onStartAutoDiscovery: () -> Unit,
    onContinueManual: () -> Unit,
    onBackClick: () -> Unit
) {
    val selectedConfig by viewModel.selectedConfig.collectAsState()

    // Fonts
    val latoBold = FontFamily(Font(R.font.lato_bold))
    val latoRegular = FontFamily(Font(R.font.lato_regular))

    // Dropdown State
    var expanded by remember { mutableStateOf(false) }

    // Logic: Is there a valid selection?
    val isPlaceholderMode = selectedConfig.isEmpty() || selectedConfig == "Select Configuration"
    val isButtonEnabled = !isPlaceholderMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header ---
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.angle_left_solid),
                contentDescription = "Back",
                tint = BrandOrange,
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.CenterStart)
                    .clickable { onBackClick() }
            )

            Text(
                text = "BACnet | Pick Configuration",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = latoBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Choose between AI-assisted Smart Auto Mapping or Manual setup",
            fontSize = 20.sp,
            color = BrandGrayText,
            fontFamily = latoRegular,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // --- Main Content ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {

            // === Left Column (Auto) ===
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ai_icons),
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Smart Auto-Mapping\n(AI-assisted)",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    fontFamily = latoBold,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Let AI detect and configure your\nsetup automatically for faster and\nsmarter mapping.",
                    fontSize = 18.sp,
                    color = BrandGrayText,
                    textAlign = TextAlign.Center,
                    fontFamily = latoRegular,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(42.dp))

                // --- CUSTOM DROPDOWN ---
                Box(
                    modifier = Modifier.width(340.dp)
                ) {
                    // Trigger Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPlaceholderMode) "Select Configuration" else selectedConfig,
                                color = if (isPlaceholderMode) PlaceholderGray else Color.Black,
                                fontSize = 18.sp,
                                fontFamily = latoRegular
                            )

                            Icon(
                                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Dropdown Arrow",
                                tint = BrandOrange,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Divider(
                            thickness = 1.5.dp,
                            color = Color.Black
                        )
                    }

                    // Popup Menu
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .width(340.dp)
                            .background(Color.White)
                    ) {
                        // --- MSTP OPTION ---
                        val isMstpSelected = selectedConfig == MSTP_CONFIGURATION
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "MSTP Configuration",
                                        fontSize = 16.sp,
                                        fontFamily = latoRegular,
                                        color = if (disableMstp) DisabledText else Color.Black
                                    )
                                    if (disableMstp) {
                                        Text(
                                            text = "Not Initialized",
                                            fontSize = 14.sp,
                                            fontFamily = latoRegular,
                                            color = DisabledText,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.setConfig(MSTP_CONFIGURATION)
                                expanded = false
                            },
                            enabled = !disableMstp, // Disables click if not initialized
                            modifier = Modifier.background(
                                if (isMstpSelected) SelectionHighlight else Color.White
                            )
                        )

                        // --- IP OPTION ---
                        val isIpSelected = selectedConfig == IP_CONFIGURATION
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "IP Configuration",
                                        fontSize = 16.sp,
                                        fontFamily = latoRegular,
                                        color = if (disableIp) DisabledText else Color.Black
                                    )
                                    if (disableIp) {
                                        Text(
                                            text = "Not Initialized",
                                            fontSize = 14.sp,
                                            fontFamily = latoRegular,
                                            color = DisabledText,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.setConfig(IP_CONFIGURATION)
                                expanded = false
                            },
                            enabled = !disableIp, // Disables click if not initialized
                            modifier = Modifier.background(
                                if (isIpSelected) SelectionHighlight else Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Start Button
                Text(
                    text = "START AUTO DISCOVERY",
                    fontFamily = latoBold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isButtonEnabled) BrandOrange else DisabledText,
                    modifier = Modifier.clickable(enabled = isButtonEnabled) { onStartAutoDiscovery() }
                )

                Spacer(modifier = Modifier.height(70.dp))
            }

            // === Center Divider ===
            Spacer(modifier = Modifier.width(60.dp))

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                VerticalDashedDivider(
                    color = BrandLightGray,
                    strokeWidth = 2.dp,
                    dashLength = 8.dp,
                    gapLength = 8.dp,
                    modifier = Modifier.fillMaxHeight(0.85f)
                )
            }

            Spacer(modifier = Modifier.width(60.dp))

            // === Right Column (Manual) ===
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.gear_multiple),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Manual Configuration",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    fontFamily = latoBold,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Proceed with a guided manual setup\nto configure a BACnet Zone.",
                    fontSize = 18.sp,
                    color = BrandGrayText,
                    textAlign = TextAlign.Center,
                    fontFamily = latoRegular,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "CONTINUE MANUALLY",
                    color = BrandOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = latoBold,
                    modifier = Modifier.clickable { onContinueManual() }
                )

                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}

@Composable
fun VerticalDashedDivider(
    color: Color,
    strokeWidth: Dp,
    dashLength: Dp,
    gapLength: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.width(strokeWidth)) {
        val effect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), gapLength.toPx()), 0f)
        drawLine(
            color = color,
            start = Offset(x = size.width / 2f, y = 0f),
            end = Offset(x = size.width / 2f, y = size.height),
            strokeWidth = strokeWidth.toPx(),
            pathEffect = effect
        )
    }
}