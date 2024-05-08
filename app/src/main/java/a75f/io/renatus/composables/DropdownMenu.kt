package a75f.io.renatus.composables

import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownColor
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownScrollBarColor
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownUnderlineColor
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.ComposeUtil.Companion.secondaryColor
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DropDownWithLabel(
    label: String, list: List<String>, previewWidth: Int = 80, expandedWidth: Int = 100,
    onSelected: (Int) -> Unit, defaultSelection: Int = 0,spacerLimit:Int=80,paddingLimit:Int=0,heightValue:Int= 435
    , isHeader : Boolean = true, isEnabled : Boolean = true) {

    var modifiedList = ComposeUtil.getModifiedList(list)
    Row {
        Box(modifier = Modifier.wrapContentWidth(), contentAlignment = Alignment.Center) {
            if (isHeader) {
                HeaderTextView(text = label, padding = paddingLimit)
            } else {
                LabelTextView(text = label)
            }

        }

        Spacer(modifier = Modifier.width(spacerLimit.dp))

        var expanded by remember { mutableStateOf(false) }
        var selectedIndex by remember { mutableStateOf(defaultSelection) }
        val lazyListState = rememberLazyListState()
        val noOfItemsDisplayInDropDown = 8  // This is the number of items to be displayed in the dropdown layout
        val dropDownHeight = 435 // This is the default height of the dropdown for 8 items

        Box(modifier = Modifier
            .width((previewWidth+20).dp)
            .wrapContentSize(Alignment.TopStart)) {
            Column() {
                Row {
                    Text(
                        modifiedList[defaultSelection],
                        modifier = Modifier.width((previewWidth ).dp).height(35.dp)
                            .clickable(onClick = { if(isEnabled) expanded = true }, enabled = isEnabled),
                        fontSize = 22.sp,
                            maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                    )

                    Image(
                        painter = painterResource(id = R.drawable.angle_down_solid),
                        contentDescription = "Custom Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 8.dp))
                            .clickable { expanded = true },
                        colorFilter = ColorFilter.tint(if(isEnabled) primaryColor
                        else greyDropDownColor)
                    )
                }
                Divider(color = greyDropDownUnderlineColor)
            }

            var customHeight = getDropdownCustomHeight(list, noOfItemsDisplayInDropDown, dropDownHeight)
            Spacer(modifier = Modifier.height(5.dp))
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                        .width((expandedWidth).dp)
                        .height((customHeight).dp)
                        .background(Color.White)
                        .border(0.5.dp, Color.LightGray)
                        .shadow(1.dp, shape = RoundedCornerShape(2.dp))
                        .simpleVerticalScrollbar(lazyListState)

            ) {
                  LazyColumn(state = lazyListState,
                          modifier = Modifier
                                  .width((expandedWidth).dp)
                                  .height((customHeight).dp)) {

                      itemsIndexed(modifiedList) { index, s ->
                          DropdownMenuItem(onClick = {
                              selectedIndex = index
                              expanded = false
                              onSelected(selectedIndex) }, text = { Text(text = s, style = TextStyle(fontSize = 20.sp)) },
                              modifier = Modifier.background(if (index == selectedIndex) secondaryColor else Color.White),
                              contentPadding = PaddingValues(10.dp)
                          )
                       }
                    }
                LaunchedEffect(expanded) {
                    lazyListState.scrollToItem(selectedIndex)
                }
            }
        }
    }
}

// This method is used to draw the scrollbar for the dropdown list
@Composable
fun Modifier.simpleVerticalScrollbar(
        state: LazyListState,
        width: Dp = 6.dp,
): Modifier {
    val targetAlpha = 1f

    val alpha by animateFloatAsState( targetValue = targetAlpha )

    return drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index

        // Draw scrollbar if lazy column has content
        if (firstVisibleElementIndex != null) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight
            if(this.size.height > scrollbarHeight) {
                drawRect(
                        color = greyDropDownScrollBarColor,
                        topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                        size = Size(width.toPx(), scrollbarHeight),
                        alpha = alpha
                )
            }
        }
    }
}
// TODO: This method calculate the height of the dropdown based on the number of items in the list
// If the number of items in the list is less than the noOfItemsDisplayInDropDown, then the height of the dropdown is calculated based on the number of items in the list
private fun getDropdownCustomHeight(list: List<String>, noOfItemsDisplayInDropDown: Int, heightValue: Int): Int {
    var customHeight = heightValue
    if(list.isNotEmpty()) {
        if(list.size < noOfItemsDisplayInDropDown) {
            customHeight = (list.size * 55) + 5 // 55 is the height and padding for each dropdown item and 5 is the padding
        }
        return customHeight
    }
    return 0
}

