package a75f.io.renatus.modbus

import a75f.io.renatus.R
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment

/**
 * Created by Manjunath K on 03-08-2023.
 */
class ModelSelectionFragment : DialogFragment() {

    var itemsList = mutableStateOf(emptyList<String>())
    lateinit var onItemSelect: OnItemSelect
    lateinit var placeholder: String

    companion object {
        val ID: String = ModelSelectionFragment::class.java.simpleName

        fun newInstance(
            items: MutableState<List<String>>,
            onItemSelect: OnItemSelect,
            placeholder: String
        ): ModelSelectionFragment {
            val fragment = ModelSelectionFragment()
            fragment.itemsList = items
            fragment.onItemSelect = onItemSelect
            fragment.placeholder = placeholder
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent {
                ShowOptions(itemsList.value)
            }
            ProgressDialogUtils.hideProgressDialog()
            return rootView
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun ShowOptions(
        items: List<String>
    ) {
        var searchText by remember { mutableStateOf("") }
        val filteredItems = if (searchText.isEmpty()) {
            items
        } else {
            items.filter { it.contains(searchText, ignoreCase = true) }
        }
        FlowRow(modifier = Modifier.padding(20.dp)) {
            Row {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Red,
                        unfocusedIndicatorColor = Color.Gray,
                        containerColor = Color.White

                    ),
                    modifier = Modifier
                        .width(900.dp)
                        .background(Color.Red),
                    trailingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Custom Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                )
                Box(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                       .clickable(onClick = {dialog!!.dismiss()} ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_baseline_close_24),
                        contentDescription = "Custom Icon",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 20.dp)
            ) {
                LazyVerticalGrid(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                    columns = GridCells.Fixed(3),
                    content = {
                        items(filteredItems) { item ->
                            Column {
                                Text(
                                    modifier = Modifier
                                        .padding(PaddingValues(top = 10.dp, bottom = 10.dp, start = 20.dp))
                                        .width(400.dp)
                                        .clickable {
                                            onItemSelect.onItemSelected(items.indexOf(item), item)
                                            dialog?.dismiss()
                                        },
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 18.sp,
                                        color = Color.Black
                                    ),
                                    text = if (item.length > 30) item.substring(0, 30) else item
                                )
                                Divider()
                            }
                        }
                    })
            }
        }

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

