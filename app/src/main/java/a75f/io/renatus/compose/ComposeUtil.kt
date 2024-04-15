package a75f.io.renatus.compose

import a75f.io.logic.Globals
import a75f.io.renatus.R
import a75f.io.renatus.util.CCUUiUtil
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
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



    }
}