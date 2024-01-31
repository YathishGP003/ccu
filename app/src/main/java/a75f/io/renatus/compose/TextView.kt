package a75f.io.renatus.compose

import a75f.io.renatus.compose.ComposeUtil.Companion.greyColor
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 18-07-2023.
 */


@Composable
fun HeaderTextView(text: String, padding : Int = 5) {
    Text(
        modifier = Modifier
            .wrapContentSize()
            .padding(padding.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
        ),
        text = text
    )
}


@Composable
fun HeaderLeftAlignedTextView(text: String) {
    Text(
        modifier = Modifier
            .wrapContentSize()
            .padding(5.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.Black,
            textAlign = TextAlign.Left,
        ),
        text = text
    )
}

@Composable
fun LabelTextView(text: String) {
    Text(
        modifier = Modifier
            .padding(PaddingValues(top = 5.dp, start = 20.dp))
            .width(450.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            color = Color.Black
        ),
        text = text
    )
}

@Composable
fun StyledTextView(text: String, fontSize : Int) {
    Text(
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize.sp,
            color = Color.Black
        ),
        text = text
    )
}

@Composable
fun TitleTextView(text: String) {
    Text(
        modifier = Modifier
            .padding(10.dp)
            .wrapContentSize(),
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 33.5.sp,
            color = primaryColor
        ),
        text = text
    )
}

@Composable
fun SubTitle(text: String) {

    Text(
        modifier = Modifier
            .height(50.dp)
            .padding(start = 5.dp),
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            fontFamily = myFontFamily,
            fontSize =  18.sp,
            color = greyColor
        ),
        text = text
    )
}

@Composable
fun SaveTextView(text: String,onClick: () -> Unit) {
    Button(
        onClick = {onClick()},
        colors = ButtonDefaults.buttonColors(
            contentColor = primaryColor,
            containerColor = Color.White // text color
        )
    ) {
        Spacer(modifier = Modifier.width(width = 8.dp))
        Text(text = text, style =  TextStyle( fontFamily = myFontFamily,fontSize = 20.sp,  fontWeight = FontWeight.Normal))
        Spacer(modifier = Modifier.width(width = 4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewWithClick(text: MutableState<String>, onClick: () -> Unit, enableClick: Boolean, isCompress: Boolean) {
    val modifier = Modifier.clickable(onClick = {
        if (enableClick)
            onClick()
    })
    if (isCompress)
        modifier.width(200.dp)
    TextField(
        value = text.value,
        onValueChange = { text.value = it},
        enabled = false,
        readOnly = !enableClick,
        modifier = modifier.height(50.dp),
        textStyle = TextStyle(fontFamily = myFontFamily,fontSize = 16.sp, color = Color.Black,textAlign = TextAlign.Center),
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Red,
            unfocusedIndicatorColor = Color.Gray,
            containerColor = Color.White,
            disabledTextColor = Color.Black,
            disabledLabelColor = Color.Black
        ),
        trailingIcon  = {
            Image(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Custom Icon",
                modifier = Modifier.size(24.dp)
            )
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewWithClickOption(text: MutableState<Int>, onClick: () -> Unit, enableClick: Boolean) {

    TextField(
        value = text.value.toString(),
        onValueChange = { text.value = it.toInt()},
        enabled = false,
        readOnly = !enableClick,
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = {
                if (enableClick)
                    onClick()
            }),
        textStyle = TextStyle(fontFamily = myFontFamily,fontSize = 16.sp, color = Color.Black, textAlign = TextAlign.Left),
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Red,
            unfocusedIndicatorColor = Color.Gray,
            containerColor = Color.White,
            disabledTextColor = Color.Black,
            disabledLabelColor = Color.Black
        ),
        trailingIcon  = {
            Image(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Custom Icon",
                modifier = Modifier.size(16.dp)
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewCompose(text: String) {
    TextField(
        value = text,
        onValueChange = { },
        enabled = false,
        readOnly = true,
        modifier = Modifier.width(100.dp),
        textStyle = TextStyle(fontFamily = myFontFamily,fontSize = 16.sp, color = Color.Black,textAlign = TextAlign.End),
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = primaryColor,
            unfocusedIndicatorColor = Color.Gray,
            containerColor = Color.White,
            disabledTextColor = Color.Black,
            disabledLabelColor = Color.Black
        ),
        trailingIcon  = {
            Image(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Custom Icon",
                modifier = Modifier.size(24.dp)
            )
        },
    )
}




@Composable
fun HeaderCenterLeftAlignedTextView(text: String) {
    Text(
        modifier = Modifier
            .width(250.dp)
            .padding(5.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.Black,
            textAlign = TextAlign.Left,
        ),
        text = text
    )
}
