package a75f.io.renatus.composables

import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.LabelTextViewForTable
import a75f.io.renatus.compose.StyledTextView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun rememberPickerState() = remember { PickerState() }

class PickerState {
    var selectedItem by mutableStateOf("")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Picker(
    items: List<String>,
    header : String,
    state: PickerState = rememberPickerState(),
    modifier: Modifier = Modifier,
    onChanged: (String) -> Unit = {},
    startIndex: Int = 0,
    visibleItemsCount: Int = 3,
    textModifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    dividerColor: Color = primaryColor,
) {

    val visibleItemsMiddle = visibleItemsCount / 2
    val listScrollCount = Integer.MAX_VALUE
    val listScrollMiddle = listScrollCount / 2
    val listStartIndex = listScrollMiddle - listScrollMiddle % items.size - visibleItemsMiddle + startIndex

    fun getItem(index: Int) = items[index % items.size]

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = listStartIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightPixels = remember { mutableStateOf(0) }
    val itemHeightDp = pixelsToDp(itemHeightPixels.value)

    val fadingEdgeGradient = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.5f to Color.Black,
            1f to Color.Transparent
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> getItem(index + visibleItemsMiddle) }
            .distinctUntilChanged()
            .collect { item ->
                state.selectedItem = item
                onChanged(item)
            }
    }

    Box(modifier = modifier) {
        Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            StyledTextView(text = header, fontSize = 16)
        }
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(top = 60.dp))
                .height(itemHeightDp * visibleItemsCount)
                .fadingEdge(fadingEdgeGradient)
        ) {
            items(listScrollCount) { index ->
                Text(
                    text = getItem(index),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = textStyle,
                    modifier = Modifier
                        .onSizeChanged { size -> itemHeightPixels.value = size.height }
                        .then(textModifier)
                )
            }
        }


        Divider(
            color = dividerColor,
            modifier = Modifier.offset(y = itemHeightDp * visibleItemsMiddle + 60.dp).width(50.dp).align(Alignment.TopCenter)

        )

        Divider(
            color = dividerColor,
            modifier = Modifier.offset(y = itemHeightDp * (visibleItemsMiddle + 1) + 60.dp).width(50.dp).align(Alignment.TopCenter)
        )




    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TempOffsetPicker(
    items: List<String>,
    header : String,
    state: PickerState = rememberPickerState(),
    modifier: Modifier = Modifier,
    onChanged: (String) -> Unit = {},
    startIndex: Int = 0,
    visibleItemsCount: Int = 3,
    textModifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    dividerColor: Color = primaryColor,
    labelWidth : Int = 210
) {

    val visibleItemsMiddle = visibleItemsCount / 2
    val listScrollCount = Integer.MAX_VALUE
    val listScrollMiddle = listScrollCount / 2
    val listStartIndex = listScrollMiddle - listScrollMiddle % items.size - visibleItemsMiddle + startIndex

    fun getItem(index: Int) = items[index % items.size]

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = listStartIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightPixels = remember { mutableStateOf(0) }
    val itemHeightDp = pixelsToDp(itemHeightPixels.value)

    val fadingEdgeGradient = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.5f to Color.Black,
            1f to Color.Transparent
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> getItem(index + visibleItemsMiddle) }
            .distinctUntilChanged()
            .collect { item ->
                state.selectedItem = item
                onChanged(item)
            }
    }

    Box(modifier = modifier) {
        Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LabelTextView(text = header,labelWidth)
        }
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(top = 40.dp))
                .height(itemHeightDp * visibleItemsCount)
                .fadingEdge(fadingEdgeGradient)
        ) {
            items(listScrollCount) { index ->
                Text(
                    text = getItem(index),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = textStyle,
                    modifier = Modifier
                        .onSizeChanged { size -> itemHeightPixels.value = size.height }
                        .then(textModifier)
                )
            }
        }


        Divider(
            color = dividerColor,
            modifier = Modifier.offset(y = itemHeightDp * visibleItemsMiddle + 40.dp).width(50.dp).align(Alignment.TopCenter)

        )

        Divider(
            color = dividerColor,
            modifier = Modifier.offset(y = itemHeightDp * (visibleItemsMiddle + 1) + 40.dp).width(50.dp).align(Alignment.TopCenter)
        )




    }

}

private fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

@Composable
private fun pixelsToDp(pixels: Int) = with(LocalDensity.current) { pixels.toDp() }


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeFieldPicker (
    header: String,
    modifier: Modifier,
    items: List<String>,
    startIndex: Int = 0,
    visibleItemsCount: Int = 3,
    state: PickerState = rememberPickerState(),
    textModifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    dividerColor: Color = primaryColor,
    onChanged: (String) -> Unit = {},
) {
    val visibleItemsMiddle = visibleItemsCount / 2
    val listScrollCount = Integer.MAX_VALUE
    val listScrollMiddle = listScrollCount / 2
    val listStartIndex = listScrollMiddle - listScrollMiddle % items.size - visibleItemsMiddle + startIndex

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = listStartIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightPixels = remember { mutableStateOf(0) }
    val itemHeightDp = pixelsToDp(itemHeightPixels.value)

    val fadingEdgeGradient = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.5f to Color.Black,
            1f to Color.Transparent
        )
    }

    fun getItem(index: Int) = items[index % items.size]

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> getItem(index + visibleItemsMiddle) }
            .distinctUntilChanged()
            .collect { item ->
                state.selectedItem = item
                onChanged(item)
            }
    }

    Column(modifier = modifier) {
        LabelTextViewForTable(
            text = header,
            modifier = textModifier,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .wrapContentWidth()
                    .height(itemHeightDp * visibleItemsCount)
                    .fadingEdge(fadingEdgeGradient)
            ) {
                items(listScrollCount) { index ->
                    Text(
                        text = getItem(index),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = textStyle,
                        modifier = Modifier
                            .onSizeChanged { size -> itemHeightPixels.value = size.height }
                            .then(textModifier),
                        textAlign = TextAlign.Center
                    )
                }
            }


            Divider(
                color = dividerColor,
                modifier = Modifier.offset(y = itemHeightDp * visibleItemsMiddle).width(80.dp).align(Alignment.TopCenter)

            )

            Divider(
                color = dividerColor,
                modifier = Modifier.offset(y = itemHeightDp * (visibleItemsMiddle + 1)).width(80.dp).align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun DurationPicker(
    onHHTimeFieldChange: (String) -> Unit = {},
    onMMTimeFieldChange: (String) -> Unit = {},
) {
    val hhValuesPickerState = rememberPickerState()
    val mmValuesPickerState = rememberPickerState()
    Row{
        Column{
            TimeFieldPicker(
                header = "HH",
                state = hhValuesPickerState,
                items = (0..168).toList().map(Int::toString),
                startIndex = 0,
                visibleItemsCount = 3,
                modifier = Modifier.wrapContentSize(),
                textModifier = Modifier.padding(8.dp).width(80.dp),
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                onChanged = onHHTimeFieldChange
            )
        }
        Spacer(modifier = Modifier.padding(horizontal = 24.dp))
        Column {
            TimeFieldPicker(
                header = "MM",
                state = mmValuesPickerState,
                items = (0..59).toList().map(Int::toString),
                startIndex = 0,
                visibleItemsCount = 3,
                modifier = Modifier.wrapContentSize(),
                textModifier = Modifier.padding(8.dp).width(80.dp),
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                onChanged = onMMTimeFieldChange
            )
        }
    }
}