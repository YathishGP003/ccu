package a75f.io.renatus.compose

import a75f.io.renatus.modbus.util.DISPLAY_UI
import a75f.io.renatus.modbus.util.PARAMETER
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Created by Manjunath K on 20-07-2023.
 */

@Composable
fun ParameterLabel(filterValue : String ="") {
    if(filterValue=="btu"||filterValue=="emr"){
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(2f)) { SubTitle(PARAMETER) }
            Box(modifier = Modifier.weight(1f)) { SubTitle(DISPLAY_UI) }
            Spacer(modifier=Modifier.width(50.dp))
            Box(modifier = Modifier.weight(2f)) { SubTitle(PARAMETER) }
            Box(modifier = Modifier.weight(1f)) { SubTitle(DISPLAY_UI) }
        }
    }
    else {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(2f)) { SubTitle(PARAMETER) }
            Box(modifier = Modifier.weight(1f)) { SubTitle(DISPLAY_UI) }
            Box(modifier = Modifier.weight(2f)) { SubTitle(PARAMETER) }
            Box(modifier = Modifier.weight(1f)) { SubTitle(DISPLAY_UI) }
        }
    }
}

@Composable
fun ParameterLabel_ForOneColumn() {
        Box(modifier = Modifier.padding(start = 0.dp)) { SubTitle(PARAMETER) }
        Box(modifier = Modifier.padding(start = 180.dp, end = 10.dp)) { SubTitle(DISPLAY_UI)  }
}