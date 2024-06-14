package a75f.io.renatus.compose

import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil.Companion.greyColor
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 18-07-2023.
 */


@Composable
fun HeaderTextView(text: String, padding : Int = 5, fontSize : Int = 22) {
    Text(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = padding.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
        ),
        text = text
    )
}

@Composable
fun HeaderTextViewNew(text: String, padding : Int = 5) {
    Text(
        modifier = Modifier
            .wrapContentSize()
            .padding(padding.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
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
fun BoldHeader(text: String) {
    Text(
        modifier = Modifier
            .wrapContentSize(),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = Color.Black,
            textAlign = TextAlign.Left,
        ),
        text = text
    )
}

@Composable
fun LabelTextView(text: String,widthValue:Int =210) {
        Text(
            modifier = Modifier
                .padding(PaddingValues(start = 20.dp))
                .width(widthValue.dp),
            style = TextStyle(
                fontFamily = myFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
                color = Color.Black
            ),
            text = text
        )
}

@Composable
fun LabelTextViewForModbus(text: String) {
    Text(
        modifier = Modifier
            .padding(PaddingValues(top = 5.dp, start = 15.dp))
            .width(385.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            color = Color.Black
        ),
        text = text
    )
}

@Composable
fun StyledTextView(text: String, fontSize : Int, textAlignment: TextAlign = TextAlign.Center) {
    Text(
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize.sp,
            color = Color.Black,
            textAlign = textAlignment
        ),
        text = text,
    )
}
@Composable
fun GrayLabelTextColor(text: String,widthValue:Int =200) {
    Text(
        modifier = Modifier
            .padding(PaddingValues(start = 16.dp))
            .width(widthValue.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            color = Color.Gray
        ),
        text = text
    )
}
@Composable
fun BoldStyledTextView(text: String, fontSize : Int, textAlignment: TextAlign = TextAlign.Center) {
    Text(
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize.sp,
            color = Color.Black,
            textAlign = textAlignment
        ),
        text = text,
    )
}


@Composable
fun VersionTextView(text: String) {
    Text(
        modifier = Modifier
            .wrapContentSize()
            .padding(start = 5.dp, top = 15.dp, end = 5.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            color = Color.Gray,
            textAlign = TextAlign.End,
        ),
        text = text
    )
}


@Composable
fun TitleTextView(text: String) {
    Text(
        modifier = Modifier
            .padding(top = 0.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)
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
            .padding(start = 5.dp, top = 10.dp),
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            fontFamily = myFontFamily,
            fontSize =  19.5.sp,
            color = greyColor
        ),
        text = text
    )
}

@Composable
fun SaveTextView(text: String,isChanged: Boolean = true,onClick: () -> Unit) {
    Button(
        enabled = isChanged,
        onClick = {onClick()},
        colors = ButtonDefaults.buttonColors(
            contentColor = primaryColor,
            containerColor = Color.Transparent, // text color
            disabledContentColor = Color.Gray,
            disabledContainerColor = Color.Transparent
        )
    ) {
        Spacer(modifier = Modifier.width(width = 8.dp))
        Text(text = text, style =  TextStyle( fontFamily = myFontFamily,fontSize = 20.sp,  fontWeight = FontWeight.Normal))
        Spacer(modifier = Modifier.width(width = 4.dp))
    }
}

@Composable
fun SaveTextViewNew(text: String,onClick: () -> Unit) {
    Button(
        onClick = {onClick()},
        colors = ButtonDefaults.buttonColors(
            contentColor = primaryColor,
            containerColor = Color.White // text color
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, style =  TextStyle( fontFamily = myFontFamily,fontSize = 22.sp,  fontWeight = FontWeight.Normal))
    }
}

@Composable
fun SaveTextViewNewExtraBold(text: String,onClick: () -> Unit) {
    Button(
        onClick = {onClick()},
        colors = ButtonDefaults.buttonColors(
            contentColor = primaryColor,
            containerColor = Color.White // text color
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, style =  TextStyle( fontFamily = myFontFamily,fontSize = 22.sp,  fontWeight = FontWeight.ExtraBold))
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

    Column {

        TextField(
            value = text.value,
            onValueChange = { text.value = it },
            enabled = false,
            readOnly = !enableClick,
            modifier = modifier.fillMaxHeight().width(300.dp),
            maxLines = 1,
            textStyle = TextStyle(
                fontFamily = myFontFamily,
                fontSize = 19.sp,
                color = Color.Black,
                textAlign = TextAlign.Start
            ),
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = primaryColor,
                textColor = primaryColor,
                placeholderColor = primaryColor,
                unfocusedIndicatorColor = primaryColor,
                disabledIndicatorColor = Color.Transparent,
                containerColor = Color.Transparent,
                cursorColor = primaryColor,
                disabledTextColor = Color.Black,
                disabledLabelColor = Color.Black
            ),
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.angle_down_solid),
                    colorFilter = ColorFilter.tint(primaryColor),
                    contentDescription = "Custom Icon",
                    modifier = Modifier.size(20.dp)

                )
            },
        )
        Divider(
            Modifier.width(280.dp).padding(top = 0.dp, start = 13.dp).offset(x = 5.dp, y = (-10).dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewWithClickOption(text: MutableState<Int>, onClick: () -> Unit, enableClick: Boolean) {

    Column {

        TextField(
            value = text.value.toString(),
            onValueChange = { text.value = it.toInt() },
            enabled = false,
            readOnly = !enableClick,
            modifier = Modifier
                .width(100.dp)
                .clickable(onClick = {
                    if (enableClick)
                        onClick()
                }),
            textStyle = TextStyle(
                fontFamily = myFontFamily,
                fontSize = 19.sp,
                color = Color.Black,
                textAlign = TextAlign.Left
            ),
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = primaryColor,
                textColor = primaryColor,
                placeholderColor = primaryColor,
                unfocusedIndicatorColor = primaryColor,
                disabledIndicatorColor = Color.Transparent,
                containerColor = Color.Transparent,
                cursorColor = primaryColor,
                disabledTextColor = Color.Black,
                disabledLabelColor = Color.Black
            ),
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.angle_down_solid),
                    colorFilter = ColorFilter.tint(primaryColor),
                    contentDescription = "Custom Icon",
                    modifier = Modifier.size(20.dp)
                )
            },
        )
        Divider(
            Modifier.width(85.dp).padding(top = 0.dp, start = 5.dp).offset(x = 5.dp, y = (-10).dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewCompose(text: String) {
    Column {

        TextField(
            value = text,
            onValueChange = { },
            enabled = false,
            readOnly = true,
            modifier = Modifier.width(100.dp),
            textStyle = TextStyle(
                fontFamily = myFontFamily,
                fontSize = 16.sp,
                color = Color.Black,
                textAlign = TextAlign.End
            ),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.Transparent,
                focusedIndicatorColor = primaryColor,
                textColor = primaryColor,
                placeholderColor = primaryColor,
                unfocusedIndicatorColor = primaryColor,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = primaryColor,
                disabledTextColor = Color.Black,
                disabledLabelColor = Color.Black
            ),
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.angle_down_solid),
                    contentDescription = "Custom Icon",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(primaryColor)
                )
            },
        )
        Divider(
            Modifier.width(85.dp).padding(top = 0.dp, start = 5.dp).offset(x = 5.dp, y = (-10).dp))
    }
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

@Composable
fun VerticalDivider(heightValue:Int=20,offsetValue:Int=0){
    Box(modifier=Modifier.height(heightValue.dp),
        contentAlignment = Alignment.Center) {
        Row(modifier=Modifier.width(1.dp)
            //verticalAlignment = Alignment.CenterVertically,
            //horizontalArrangement = Arrangement.Center
        ) {
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .height(heightValue.dp)
                    .offset(x=0.dp,y=offsetValue.dp),
                color = Color.Gray
            )
        }
    }
}
