package a75f.io.renatus.modbus

import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil.Companion.greyColor
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownColor
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.HeaderLeftAlignedTextView
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.util.ProgressDialogUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment

/**
 * Created by Manjunath K on 03-08-2023.
 */
class ModelSelectionFragment : DialogFragment() {

    var itemsList = mutableStateListOf<Pair<String, Int>>()
    lateinit var onItemSelect: OnItemSelect
    lateinit var placeholder: String

    companion object {
        val ID: String = ModelSelectionFragment::class.java.simpleName
        var isPcn = false
        const val MAX_TEXT_LENGTH = 20
        var currentRegistersUsed: Int = 0
        var maxRegisterCount : Int = 50
        fun newInstance(
            items: MutableState<List<String>>,
            onItemSelect: OnItemSelect,
            placeholder: String
        ): ModelSelectionFragment {
            val fragment = ModelSelectionFragment()
            fragment.itemsList = items.value.map { it to 1 }.toMutableStateList()
            fragment.onItemSelect = onItemSelect
            fragment.placeholder = placeholder
            isPcn = false
            return fragment
        }

        fun showUIForPCN(
            items: MutableState<List<Pair<String, Int>>>,
            onItemSelect: OnItemSelect,
            placeholder: String,
            currentRegistersUsed: Int = 0,
            maxRegisterCount: Int = 50
        ): ModelSelectionFragment {
            val fragment = ModelSelectionFragment()
            fragment.itemsList = items.value.toMutableStateList()
            fragment.onItemSelect = onItemSelect
            fragment.placeholder = placeholder
            isPcn = true
            this.currentRegistersUsed = currentRegistersUsed
            this.maxRegisterCount = maxRegisterCount
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        ProgressDialogUtils.showProgressDialog(context, LOADING)
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent {
                ShowOptions(itemsList)
            }
            ProgressDialogUtils.hideProgressDialog()
            return rootView
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun ShowOptions(
        items: List<Pair<String, Int>>,
    ) {
        var searchText by remember { mutableStateOf("") }
        val filteredItems = if (searchText.isEmpty()) {
            items
        } else {
            items.filter { it.first.contains(searchText, ignoreCase = true) }
        }
        FlowRow(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = primaryColor,
                        unfocusedIndicatorColor = greyColor,
                        containerColor = Color.White
                    ),
                    modifier = Modifier
                        .width(900.dp)
                        .background(Color.Gray),
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.img_search_grey),
                            contentDescription = "Custom Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )

                // Close icon with 20dp start space
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(50.dp)
                        .clickable(onClick = { dialog!!.dismiss() }),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.font_awesome_close),
                        contentDescription = "Close Icon",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(primaryColor)
                    )
                }
            }

            if (isPcn) {
                HeaderLeftAlignedTextView(
                    "Total consumed registers: $currentRegistersUsed/50",
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 20.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight().padding(start = 10.dp, bottom = 20.dp)
                ) {
                    itemsIndexed(filteredItems) { index, item ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable {
                                        if (!isPcn || isEligibleToSelect(item)) {
                                            onItemSelect.onItemSelected(
                                                items.indexOf(item),
                                                item.first
                                            )
                                            dialog?.dismiss()
                                        }
                                    }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Main text takes remaining space
                                Text(
                                    text = if (item.first.length > MAX_TEXT_LENGTH) item.first.substring(0, MAX_TEXT_LENGTH) else item.first,
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (isEligibleToSelect(item)) Color.Black else greyDropDownColor
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).pointerInput(Unit) { },
                                )

                                // Registers text at the end
                                if (isPcn) {
                                    Text(
                                        text = "${item.second} registers",
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            color = if (isEligibleToSelect(item)) greyColor else greyDropDownColor
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.End
                                    )
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                if ((index + 1) % 3 != 0) {
                                    Divider(
                                        color = Color.LightGray,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(1.dp)
                                    )
                                }
                            }


                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = 0.9f)
                                    .padding(start = 10.dp),
                                color = Color.LightGray
                            )

                        }
                    }
                }

            }

        }

    }

    private fun isEligibleToSelect(item: Pair<String, Int>): Boolean {
       return ((item.second + currentRegistersUsed) <= maxRegisterCount)
    }

    fun getIdString(): String {
        return ID
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null)
            dialog!!.window!!.setLayout(1200, 700)
    }
}

