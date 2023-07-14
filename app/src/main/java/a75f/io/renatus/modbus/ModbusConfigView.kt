package a75f.io.renatus.modbus

import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.RegisterItem
import a75f.io.renatus.compose.SpinnerView
import a75f.io.renatus.compose.ToggleButton
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider

/**
 * Created by Manjunath K on 13-07-2023.
 */

class ModbusConfigView : BaseDialogFragment() {

    private lateinit var viewModel: ModbusConfigViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val rootView = ComposeView(requireContext())
        viewModel = ViewModelProvider(this)[ModbusConfigViewModel::class.java]
        viewModel.configModelDefinition(requireContext())

        rootView.apply {
            setContent {
                RootView()
            }
            return rootView
        }
    }


    override fun getIdString(): String {
        return ModbusConfigView::class.java.simpleName
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = 1165
            val height = 672
            dialog.window!!.setLayout(width, height)
        }
    }

    @Composable
    fun RootView() {
        Column {
            Row {
                Box(
                    modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                ) {

                    Text(
                        modifier = Modifier
                            .padding(10.dp)
                            .height(100.dp)
                            .width(200.dp),
                        style = TextStyle(
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 33.5.sp,
                            color = Color(android.graphics.Color.parseColor("#E24301")),
                        ),
                        text = "MODBUS"
                    )
                }

            }
            Row {
                HeaderTextView("Equipment Type")
                SpinnerView(0, viewModel.deviceList) {
                    viewModel.fetchModelDetails(it)
                }
                HeaderTextView("Slave Id")
                SpinnerView(0, viewModel.slaveIdList) {}
            }

            MyGridRecyclerView(data = viewModel.equipDetails, gridColumns = 2)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Red)
                ) {
                    HeaderTextView("Equipment Type")
                }
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .background(Color.Green)
                ) {
                    HeaderTextView("Equipment Type")
                }
            }
        }
    }

    @Composable
    fun MyGridRecyclerView(data: MutableState<List<RegisterItem>>, gridColumns: Int) {
        LazyVerticalGrid(columns = GridCells.Fixed(gridColumns), content = {
            items(data.value) { item ->
                Row {
                    LabelTextView(text = item.param.name)
                    ToggleButton(defaultSelection = item.displayInUi) { item.displayInUi = it }
                }
            }
        })
    }


}
