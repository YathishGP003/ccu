package a75f.io.renatus.compose

import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil.Companion.greyColor
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 18-07-2023.
 */


@Composable
fun HeaderTextView(text: String, padding : Int = 5, fontSize : Int = 22,fontWeight: FontWeight = FontWeight.Bold, textAlignment: TextAlign = TextAlign.Center) {
    Text(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = padding.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize.sp,
            color = Color.Black,
            textAlign = textAlignment,
        ),
        text = text
    )
}

@Composable
fun HeaderTextViewMultiLine(text: String, padding : Int = 5, fontSize : Int = 22,fontWeight: FontWeight = FontWeight.Bold) {
    Text(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = padding.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
        ),
        text = text,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun HeaderTextViewCustom(text: String, padding : Int = 5) {
    val textColor = Color(0xFF666666)
    Text(
        modifier = Modifier
            .wrapContentSize()
            .padding(padding.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = textColor,
            textAlign = TextAlign.Left,
        ),
        text = text
    )
}

@Composable
fun HeaderTextViewNew(text: String, padding : Int = 5, fontSize : Int = 22,fontWeight: FontWeight = FontWeight.Bold, color: Color = Color.Black) {
    Text(
        modifier = Modifier
            .wrapContentSize()
            .padding(padding.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize.sp,
            color = color,
            textAlign = TextAlign.Center,
        ),
        text = text
    )
}


@Composable
fun HeaderLeftAlignedTextView(text: String, modifier : Modifier = Modifier.padding(5.dp) ) {
    Text(
        modifier = modifier
            .wrapContentSize(),
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
fun HeaderLeftAlignedTextViewNew(text: String) {
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
        text = text,
        maxLines = 1
    )
}

@Composable
fun HeaderLeftAlignedTextViewNew(text: String, fontSize : Int = 22, modifier: Modifier= Modifier) {
    Text(
        modifier = modifier
            .wrapContentSize(),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize.sp,
            color = Color.Black,
            textAlign = TextAlign.Left,
        ),
        text = text
    )
}

@Composable
fun HeaderLeftAlignedTextViewNew(text: AnnotatedString, fontSize : Int = 22, modifier: Modifier= Modifier) {
    Text(
        modifier = modifier
            .wrapContentSize(),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize.sp,
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
fun LabelTextView(text: String,widthValue:Int =210, fontSize : Int = 20) {
    Text(
        modifier = Modifier
            .padding(PaddingValues(start = 20.dp))
            .width(widthValue.dp),
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
fun LabelTextView(text: String,widthValue:Int =210, fontSize : Int = 20, textAlignment: TextAlign = TextAlign.Center) {
    Text(
        modifier = Modifier
            .padding(PaddingValues(start = 20.dp))
            .width(widthValue.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize.sp,
            color = Color.Black,
            textAlign = textAlignment
        ),
        text = text
    )
}

@Composable
fun LabelTextView(text: AnnotatedString,widthValue:Int =210, fontSize : Int = 20) {
    Text(
        modifier = Modifier
            .padding(PaddingValues(start = 20.dp))
            .width(widthValue.dp),
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
fun LabelTextViewForTable(text: String, modifier: Modifier, fontSize: Int = 20, textAlign: TextAlign = TextAlign.Start, textColor : Color = Color.Black) {
    Text(
        modifier = modifier,
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize.sp,
            color = textColor,
            textAlign = textAlign,
        ),
        maxLines = 1,
        text = text,
    )
}

@Composable
fun LabelTextViewForTable(text: AnnotatedString, modifier: Modifier = Modifier, fontSize: Int = 20, textAlign: TextAlign = TextAlign.Start, fontColor: Color = Color.Black) {
    Text(
        modifier = modifier,
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize.sp,
            color = fontColor,
            textAlign = textAlign
        ),
        text = text
    )
}

@Composable
fun LabelBoldTextViewForTable(text: String, modifier: Modifier = Modifier, fontSize: Int = 20, textAlign: TextAlign = TextAlign.Start, fontColor: Color = Color.White) {
    Text(
        modifier = modifier,
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            color = fontColor,
            textAlign = textAlign
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
fun GrayLabelTextColor(text: String,widthValue:Int =200, textAlignment: TextAlign = TextAlign.Left) {
    Text(
        modifier = Modifier
            .padding(PaddingValues(start = 16.dp))
            .width(widthValue.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            color = Color.Gray,
            textAlign = textAlignment
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
fun BoldStyledGreyTextView(text: String, fontSize : Int, textAlignment: TextAlign = TextAlign.Center) {
    Text(
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            color = Color.Gray,
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
fun TitleTextViewCustom(text: String, color: Color) {
    Text(
        modifier = Modifier
            .wrapContentSize(),
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 33.5.sp,
            color = color
        ),
        text = text
    )
}

@Composable
fun TitleTextViewCustomNoModifier(text: String, color: Color) {
    Text(
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            color = color
        ),
        text = text
    )
}

@Composable
fun SubTitle(text: String, fontSizeCustom : Double = 19.5, topPaddingValue: Int = 10, startPaddingValue : Int = 5) {

    Text(
        modifier = Modifier
            .height(50.dp)
            .padding(start = startPaddingValue.dp, top = topPaddingValue.dp),
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            fontFamily = myFontFamily,
            fontSize =  fontSizeCustom.sp,
            color = greyColor
        ),
        text = text
    )
}

@Composable
fun SubTitle(text: String,modifier: Modifier = Modifier) {

    Text(
        modifier = modifier
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
fun SubTitleNoPadding(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Float = 20.0f,
    fontWeight: FontWeight = FontWeight.Bold,
    color : Color = greyColor,
    textAlignment: TextAlign = TextAlign.Center,
    textOverflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {

    Text(
        modifier = modifier
            .wrapContentHeight(),
        style = TextStyle(
            textAlign = textAlignment,
            fontWeight = fontWeight,
            fontFamily = myFontFamily,
            fontSize =  fontSize.sp,
            color = color,
        ),
        text = text,
        overflow = textOverflow,
        maxLines = maxLines
    )
}


@Composable
fun SaveTextView(text: String,isChanged: Boolean = true, fontSize : Float = 20f, onClick: () -> Unit ) {
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
        Text(text = text, style =  TextStyle( fontFamily = myFontFamily, fontSize = fontSize.sp,  fontWeight = FontWeight.Normal))
        Spacer(modifier = Modifier.width(width = 8.dp))
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
fun SaveTextViewNewExtraBold(onClick: () -> Unit) {
    Button(
        onClick = {onClick()},
        colors = ButtonDefaults.buttonColors(
            contentColor = primaryColor,
            containerColor = Color.White // text color
        ),
        contentPadding = PaddingValues(top = 0.dp)
    ) {
        Image( painter = painterResource(id = R.drawable.font_awesome_close),
                contentDescription = "Custom Icon",
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 0.dp),
                colorFilter = ColorFilter.tint(primaryColor))
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
            modifier = modifier
                .fillMaxHeight()
                .width(300.dp),
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
            Modifier
                .width(280.dp)
                .padding(top = 0.dp, start = 13.dp)
                .offset(x = 5.dp, y = (-10).dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewWithClickCustom(text: MutableState<String>, onClick: () -> Unit, enableClick: Boolean, isCompress: Boolean) {
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
            modifier = modifier
                .fillMaxHeight()
                .width(300.dp),
            maxLines = 1,
            textStyle = TextStyle(
                fontFamily = myFontFamily,
                fontSize = 22.sp,
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
            Modifier
                .width(280.dp)
                .padding(top = 0.dp, start = 0.dp)
                .offset(x = 5.dp, y = (-10).dp))
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
                .width(105.dp)
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
            singleLine = true
        )
        Divider(
            Modifier
                .width(90.dp)
                .padding(top = 0.dp, start = 5.dp)
                .offset(x = 5.dp, y = (-10).dp))
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
            Modifier
                .width(85.dp)
                .padding(top = 0.dp, start = 5.dp)
                .offset(x = 5.dp, y = (-10).dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewWithClickNoLeadingSpace(text: MutableState<String>, onClick: () -> Unit, enableClick: Boolean, isCompress: Boolean) {
    val modifier = Modifier.clickable(onClick = {
        if (enableClick)
            onClick()
    })
    if (isCompress)
        modifier.width(200.dp)

    Column{

        TextField(
            value = text.value,
            onValueChange = { text.value = it },
            enabled = false,
            readOnly = !enableClick,
            modifier = modifier
                .offset(x = (-16).dp)
                .wrapContentHeight()
                .width(300.dp)
                .padding(start = 0.dp),
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
            Modifier
                .width(280.dp)
                .padding(top = 0.dp, start = 0.dp)
                .offset(y = (-10).dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewWithClickOptionNoLeadingSpace(text: MutableState<Int>, onClick: () -> Unit, enableClick: Boolean) {

    Column {

        TextField(
            value = text.value.toString(),
            onValueChange = { text.value = it.toInt() },
            enabled = false,
            readOnly = !enableClick,
            modifier = Modifier
                .offset(x = (-16).dp)
                .width(105.dp)
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
            singleLine = true
        )
        Divider(
            Modifier
                .width(85.dp)
                .padding(top = 0.dp, start = 0.dp)
                .offset(y = (-10).dp)
        )
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
                    .offset(x = 0.dp, y = offsetValue.dp),
                color = Color.Gray
            )
        }
    }
}

@Composable
fun TextViewWithToggle(text: String, defaultSelection: Boolean, modifier: Modifier = Modifier, onEnabled: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubTitleNoPadding(text = text)
        Spacer(modifier = Modifier.width(16.dp))
        ToggleButtonNoImplicitSpacing(defaultSelection, modifier, onEnabled = onEnabled)
    }
}

@Composable
fun TextViewWithDropdown(modifier: Modifier, imageList : List<Int>, text : String, fontSize: Int = 24, imageClickEvent: () -> Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        ImageViewComposableNoImageSpacing(imageList, "", modifier = Modifier.padding(end = 5.dp)) {
            imageClickEvent()
        }
        HeaderLeftAlignedTextViewNew(text = text, fontSize = fontSize)
    }
}

@Composable
fun TextViewWithDropdown(modifier: Modifier, imageList : List<Int>, text : AnnotatedString, fontSize: Int = 24, imageClickEvent: () -> Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        ImageViewComposableNoImageSpacing(imageList, "", modifier = Modifier.padding(end = 5.dp)) {
            imageClickEvent()
        }
        HeaderLeftAlignedTextViewNew(text = text, fontSize = fontSize)
    }
}

@Composable
fun TextViewWithHint(modifier: Modifier, text: AnnotatedString, hintText: String, fontSize: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LabelTextViewForTable(modifier = modifier, text = text, fontSize = fontSize)
        SubTitleNoPadding(text = hintText, fontSize = 18f, modifier = Modifier, fontWeight = FontWeight.Normal)
    }
}
