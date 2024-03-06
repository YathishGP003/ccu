package a75f.io.renatus.compose

import a75f.io.logic.Globals
import a75f.io.renatus.R
import a75f.io.renatus.util.CCUUiUtil
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Created by Manjunath K on 16-08-2023.
 */
class ComposeUtil {
    companion object {
        val  primaryColor = getThemeColor()
        var greyColor = Color(android.graphics.Color.parseColor("#666666"))
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
    }
}