package a75f.io.renatus.compose

import a75f.io.logic.Globals
import a75f.io.renatus.R
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuViewModel
import a75f.io.renatus.profiles.system.advancedahu.Option
import a75f.io.renatus.util.CCUUiUtil
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign

/**
 * Created by Manjunath K on 16-08-2023.
 */
class ComposeUtil {
    companion object {
        val  primaryColor = getThemeColor()
        val  secondaryColor = getSecondaryThemeColor()
        var greyColor = Color(android.graphics.Color.parseColor("#666666"))
        val greyDropDownColor = Color(android.graphics.Color.parseColor("#B6B6B6"))
        val greyDropDownScrollBarColor = Color(android.graphics.Color.parseColor("#B6B6B6"))
        val greyDropDownUnderlineColor = Color(android.graphics.Color.parseColor("#CCCCCC"))
        val greySearchIcon = Color(android.graphics.Color.parseColor("#999999"))
        val grey05 = Color(android.graphics.Color.parseColor("#EBECED"))
        val grey06 = Color(android.graphics.Color.parseColor("#F9F9F9"))
        val textColor = Color(android.graphics.Color.parseColor("#333333"))
        val myFontFamily = FontFamily(
            Font(R.font.lato_light, FontWeight.Light),
            Font(R.font.lato_regular, FontWeight.Normal),
            Font(R.font.lato_regular, FontWeight.Medium),
            Font(R.font.lato_bold, FontWeight.Bold)
        )

        private fun getThemeColor(): Color {
            var primaryColor = Color(android.graphics.Color.parseColor("#E24301"))
            if (CCUUiUtil.isDaikinEnvironment(Globals.getInstance().applicationContext))
                primaryColor = Color(android.graphics.Color.parseColor("#FF0097E0"))
            if (CCUUiUtil.isCarrierThemeEnabled(Globals.getInstance().applicationContext))
                primaryColor = Color(android.graphics.Color.parseColor("#1891F6"))
            if (CCUUiUtil.isAiroverseThemeEnabled(Globals.getInstance().applicationContext))
                primaryColor = Color(android.graphics.Color.parseColor("#6FC498"))
            return primaryColor
        }

        //Function to modify the list of strings as per UX which are to be displayed in the dropdown
        //logic can be updated to accomodate formatting of new strings in future if needed  as per UX
        fun getModifiedList(list: List<String>): List<String> {
            val modifiedStrings = mutableListOf<String>()
            for (string in list) {
                var modifiedStringBuilder = StringBuilder(string.capitalize())
                for (index in 1 until modifiedStringBuilder.length) {
                    if (modifiedStringBuilder[index].isUpperCase() && modifiedStringBuilder[index - 1].isLowerCase()){
                        modifiedStringBuilder.insert(index, ' ')
                        break
                    }
                }
                if (!modifiedStringBuilder.first().isDigit() && modifiedStringBuilder.last().isDigit()) {
                    if(!(modifiedStringBuilder.length > 1 && modifiedStringBuilder[modifiedStringBuilder.length - 2].isDigit()))
                        modifiedStringBuilder.insert(modifiedStringBuilder.length - 1, ' ')
                }
                if (modifiedStringBuilder.last().isLetter() && modifiedStringBuilder.last().isLowerCase() && modifiedStringBuilder.length > 1 && modifiedStringBuilder[modifiedStringBuilder.length - 2].isDigit()) {
                    modifiedStringBuilder.insert(modifiedStringBuilder.length - 1, ' ')
                    modifiedStringBuilder.setCharAt(modifiedStringBuilder.length - 1, modifiedStringBuilder[modifiedStringBuilder.length - 1].toUpperCase())
                }
                modifiedStrings.add(modifiedStringBuilder.toString())
            }
            return modifiedStrings
        }

        private fun getSecondaryThemeColor(): Color {
            var secondaryColor = Color(android.graphics.Color.parseColor("#1AE5561A"))
            if (CCUUiUtil.isDaikinEnvironment(Globals.getInstance().applicationContext))
                secondaryColor = Color(android.graphics.Color.parseColor("#1A0097E0"))
            if (CCUUiUtil.isCarrierThemeEnabled(Globals.getInstance().applicationContext))
                secondaryColor = Color(android.graphics.Color.parseColor("#1A1891F6"))
            if (CCUUiUtil.isAiroverseThemeEnabled(Globals.getInstance().applicationContext))
                secondaryColor = Color(android.graphics.Color.parseColor("#1A6FC498"))
            return secondaryColor
        }


        @Composable
        fun DashDivider(height: Dp = 50.dp) {
            LazyColumn(modifier = Modifier.width(1200.dp).height(height)) {
                items(1) {
                    Column(
                        modifier = Modifier.width(1200.dp).height(height)
                    ) {
                        DashDivider(
                            color = greyDropDownUnderlineColor,
                            thickness = 2.dp,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
        @Composable
        fun DashDivider(
            thickness: Dp,
            color: Color,
            phase: Float = 10f,
            intervals: FloatArray = floatArrayOf(10f, 10f),
            modifier: Modifier
        ) {
            Canvas(modifier = modifier.fillMaxWidth()) {
                val dividerHeight = thickness.toPx()
                drawRoundRect(
                    color = color,
                    style = Stroke(
                        width = dividerHeight,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals,
                            phase
                        )
                    )
                )
            }
        }

    }
}

@Composable
fun Title(title: String,modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TitleTextView(title)
    }
}


@Composable
fun singleOptionConfiguration(
    minLabel: String,
    itemList: List<Option>,
    unit: String,
    minDefault: String,
    onMinSelected: (Option) -> Unit = {},
    viewModel: AdvancedHybridAhuViewModel? = null
) {
    Row(
        modifier = Modifier
            .width(550.dp)
            .padding(bottom = 10.dp)
    ) {
        Box(modifier = Modifier
            .weight(1f)
            .padding(top = 10.dp)) {
            StyledTextView(
                minLabel, fontSize = 20, textAlignment = TextAlign.Left
            )
        }
        Box(modifier = Modifier
            .weight(1f)
            .padding(top = 5.dp)) {
            SpinnerElementOption(defaultSelection = minDefault,
                items = itemList,
                unit = unit,
                itemSelected = { onMinSelected(it) }, viewModel = viewModel)
        }
    }
}


@Composable
fun StagedFanConfiguration(
    label1: String,
    label2: String,
    itemList: List<Option>,
    unit: String,
    minDefault: String,
    maxDefault: String,
    onlabel1Selected: (Option) -> Unit = {},
    onlabel2Selected: (Option) -> Unit = {},
    stage1Enabled: Boolean,
    stage2Enabled: Boolean,
    viewModel: AdvancedHybridAhuViewModel? = null
) {
    val twoOptionSelected = stage1Enabled && stage2Enabled
    Row(
        modifier = Modifier
            .then(if (twoOptionSelected) Modifier.fillMaxWidth() else Modifier.width(550.dp))
            .padding(bottom = 10.dp)
    ) {
        if(stage1Enabled) {
            Box(modifier = Modifier
                .weight(1f)
                .padding(top = 10.dp)) {
                StyledTextView(
                    label1, fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(top = 5.dp)) {
                SpinnerElementOption(defaultSelection = minDefault,
                    items = itemList,
                    unit = unit,
                    itemSelected = { onlabel1Selected(it) }, viewModel = viewModel)
            }
        }

        if(stage2Enabled) {
            Box(modifier = Modifier
                .then(if (!twoOptionSelected) Modifier.weight(1f) else Modifier.width(550.dp))
                .weight(1f)
                .padding(top = 10.dp)) {
                StyledTextView(
                    label2, fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SpinnerElementOption(defaultSelection = maxDefault,
                    items = itemList,
                    unit = unit,
                    itemSelected = { onlabel2Selected(it) }, viewModel = viewModel)
            }
        }
    }
}