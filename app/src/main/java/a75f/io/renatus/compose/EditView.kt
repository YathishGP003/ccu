package a75f.io.renatus.compose


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextFieldWhiteBgUnderline7(
    errorMessage: String? = null,
    showError: Boolean = false,
    onTextChanged: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        BasicTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                onTextChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), // Add padding for better visual appearance
            textStyle = MaterialTheme.typography.bodySmall,
            cursorBrush = SolidColor(Color.Black),
            singleLine = true,
            //imeAction = ImeAction.Done,
        )

//        if (showError && !errorMessage.isNullOrBlank()) {
//            LaunchedEffect(showError) {
//                // Display the error message as a Snackbar
//                val snackbarHostState = SnackbarHostState()
//                snackbarHostState.showSnackbar(
//                    message = errorMessage,
//                    duration = SnackbarDuration.Short
//                )
//            }
//        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextFieldWhiteBgUnderline6(
    errorMessage: String? = null,
    showError: Boolean = false,
    onTextChanged: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        BasicTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                onTextChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), // Add padding for better visual appearance
            textStyle = MaterialTheme.typography.bodySmall,
            cursorBrush = SolidColor(Color.Black),
            singleLine = true,
            //imeAction = ImeAction.Done,
        )

        if (showError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            ErrorBox(errorMessage = errorMessage!!)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorBox(errorMessage: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Blue
    ) {
        Column {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .drawBehind {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(0f, 0f),
                            size = this.size
                        )
                        val arrowSize = 12.dp.toPx()
                        val arrowPosition = 16.dp.toPx()
                        val arrowStartX = this.size.width - arrowPosition
                        val arrowEndX = this.size.width - arrowPosition - arrowSize
                        val arrowY = this.size.height - 1.dp.toPx()
                        drawLine(
                            color = Color.Red,
                            start = Offset(arrowStartX, arrowY),
                            end = Offset(arrowEndX, arrowY),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

//EditableTextFieldWhiteBgUnderline
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextFieldWhiteBgUnderline5(
    errorMessage: String? = null,
    showError: Boolean = false,
    onTextChanged: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        BasicTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                onTextChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), // Add padding for better visual appearance
            textStyle = MaterialTheme.typography.bodySmall,
            cursorBrush = SolidColor(Color.Black),
            singleLine = true,
            //imeAction = ImeAction.Done,
        )

        if (showError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Blue
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .drawBehind {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(0f, 0f),
                                    size = this.size
                                )
                                val arrowSize = 12.dp.toPx()
                                val arrowPosition = 16.dp.toPx()
                                val arrowStartX = this.size.width - arrowPosition
                                val arrowEndX = this.size.width - arrowPosition - arrowSize
                                val arrowY = this.size.height - 1.dp.toPx()
                                drawLine(
                                    color = Color.Red,
                                    start = Offset(arrowStartX, arrowY),
                                    end = Offset(arrowEndX, arrowY),
                                    strokeWidth = 1.dp.toPx()
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextFieldWhiteBgUnderline46(
    errorMessage: String? = null,
    showError: Boolean = false,
    onTextChanged: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Top
    ) {
        BasicTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                onTextChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), // Add padding for better visual appearance
            textStyle = MaterialTheme.typography.bodySmall,
            cursorBrush = SolidColor(Color.Black),
            singleLine = true,
            //imeAction = ImeAction.Done,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (showError) Color.Blue else Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .drawBehind {
                        if (showError) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(0f, 0f),
                                size = this.size
                            )
                            val arrowSize = 12.dp.toPx()
                            val arrowPosition = 16.dp.toPx()
                            val arrowStartX = this.size.width - arrowPosition
                            val arrowEndX = this.size.width - arrowPosition - arrowSize
                            val arrowY = this.size.height - 1.dp.toPx()
                            drawLine(
                                color = Color.Red,
                                start = Offset(arrowStartX, arrowY),
                                end = Offset(arrowEndX, arrowY),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                if (showError && !errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextField3() {
    var textValue by remember { mutableStateOf("") }

    TextField(
        value = textValue,
        onValueChange = { newText ->
            textValue = newText
        },
        label = { Text("Enter text") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                // Handle action when "Done" button on keyboard is pressed
            }
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextFieldWhiteBgUnderline(
    hintText: String = "Enter your text",
    errorMessage: String? = null,
    showError: Boolean = false,
    onTextChanged: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(TextFieldValue()) }

    //val hintText = "Enter your text"
    val hintColor = Color.Gray

    BasicTextField(
        value = textValue,
        onValueChange = {
                newText ->
            textValue = newText
            onTextChanged(newText.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Adjust padding as needed
        textStyle = TextStyle(
            fontSize = 22.sp,
            color = Color.Black,
            fontFamily = ComposeUtil.myFontFamily,
            fontWeight = FontWeight.Normal
        ),
        cursorBrush = SolidColor(Color.Black),
        singleLine = true,
        decorationBox = { innerTextField ->
            Column {
                // Render the hint text if the field is empty
                if (textValue.text.isEmpty()) {
                    Text(
                        text = hintText,
                        style = TextStyle(
                            fontSize = 22.sp,
                            color = hintColor,
                            fontFamily = ComposeUtil.myFontFamily,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                    )
                }

                // Render the actual BasicTextField
                innerTextField()

                // Add underline
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.Black)
                )

                errorMessage?.let {
                    if (showError) {
                        Text(
                            text = it,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = androidx.compose.ui.graphics.Color.Red,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                            ),
                            modifier = androidx.compose.ui.Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextFieldWhiteBgUnderlineOnlyNumbers(
    hintText: String = "Enter your text",
    errorMessage: String? = null,
    showError: Boolean = false,
    onTextChanged: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(TextFieldValue()) }

    BasicTextField(
        value = textValue,
        onValueChange = { newValue ->
            if (newValue.text.isEmpty() || newValue.text.toIntOrNull() != null) {
                textValue = newValue
                onTextChanged(newValue.text)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Adjust padding as needed
        textStyle = TextStyle(
            fontSize = 22.sp,
            color = Color.Black,
            fontWeight = FontWeight.Normal
        ),
        cursorBrush = SolidColor(Color.Black),
        singleLine = true,
        decorationBox = { innerTextField ->
            Column {
                // Render the hint text if the field is empty
                if (textValue.text.isEmpty()) {
                    Text(
                        text = hintText,
                        style = TextStyle(
                            fontSize = 22.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.padding(horizontal = 1.dp)
                    )
                }

                // Render the actual BasicTextField
                innerTextField()

                // Add underline
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.Black)
                )

                // Show error message if showError is true
                errorMessage?.let {
                    if (showError) {
                        Text(
                            text = it,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                // Handle 'Done' action if needed
            }
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextField(errorMessage: String? = null) {
    var textValue by remember { mutableStateOf(TextFieldValue()) }

    Column {
        BasicTextField(
            value = textValue.text,
            onValueChange = { newText ->
                textValue = textValue.copy(text = newText)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), // Add padding for better visual appearance
            textStyle = MaterialTheme.typography.bodySmall,
            cursorBrush = SolidColor(Color.Black),
            singleLine = true,
            //imeAction = ImeAction.Done
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
            color = Color.Black
        ) {}

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Blue
            )
        }
    }
}
