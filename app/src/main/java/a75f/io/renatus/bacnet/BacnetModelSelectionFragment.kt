package a75f.io.renatus.bacnet

import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.composables.VerticalScrollbar
import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api

class BacnetModelSelectionFragment : BaseDialogFragment() {

    companion object {
        const val ID = "BacnetModelSelectionFragment"
        val BrandOrange = Color(0xFFFF6600)

        fun newInstance(deviceId: String): BacnetModelSelectionFragment {
            val f = BacnetModelSelectionFragment()
            val args = Bundle()
            args.putString("deviceId", deviceId)
            f.arguments = args
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(1200, 700)
            setGravity(Gravity.CENTER)
            setBackgroundDrawableResource(android.R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.5f) // 50% gray dim
        }
    }

    override fun getIdString(): String {
        return BacnetModelSelectionFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val deviceId = requireArguments().getString("deviceId") ?: ""
        val rootView = ComposeView(requireContext())

        rootView.apply {
            setContent {
                ModelSelectionScreen(deviceId)
            }
            return rootView
        }
    }

    @Composable
    fun ModelSelectionScreen(deviceId: String) {
        val listState = rememberLazyListState()

        val modelList = remember {
            listOf(
                ModelRow("Schneider", "Multi Function Meter EN 8203", "100%"),
                ModelRow("Carrier", "stulz_iec_652", "94.3%"),
                ModelRow("Manufacturer Name", "Model Name", "90.1%"),
                ModelRow("Manufacturer Name", "Model Name", "84.2%"),
                ModelRow("Manufacturer Name", "Model Name", "84.2%"),
                ModelRow("Manufacturer Name", "Model Name", "84.2%"),
                ModelRow("Manufacturer Name", "Model Name", "84.2%"),
                ModelRow("Manufacturer Name", "Model Name", "84.2%"),
                ModelRow("Manufacturer Name", "Model Name", "84.2%"),
                ModelRow("Manufacturer Name", "Model Name", "84.2%"),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.angle_left_solid),
                    contentDescription = "Back",
                    tint = BrandOrange,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { dismiss() }
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Select Model",
                    style = TextStyle(
                        fontFamily = ComposeUtil.myFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(state = listState) {
                    item { TableHeader() }

                    items(modelList.size) { index ->
                        ModelItemRow(
                            row = modelList[index],
                            isEven = index % 2 == 0
                        )
                    }
                }

                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(6.dp)
                        .padding(vertical = 4.dp),
                    listState = listState
                )
            }
        }
    }

    @Composable
    fun TableHeader() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEBECED))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCell("Manufacturer", 1.2f)
            HeaderCell("Model Name", 2f)
            HeaderCell("Matching Score", 1f)
            Spacer(modifier = Modifier.width(40.dp))
        }
    }

    @Composable
    fun RowScope.HeaderCell(text: String, weight: Float) {
        Text(
            text = text,
            modifier = Modifier
                .weight(weight)
                .padding(horizontal = 16.dp), // Padding for spacing
            textAlign = TextAlign.Start, // Left Aligned
            style = TextStyle(
                fontFamily = ComposeUtil.myFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )
        )
    }

    @Composable
    fun ModelItemRow(row: ModelRow, isEven: Boolean) {
        val backgroundColor = if (isEven) Color(0xFFF9F9F9) else Color.White

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable { dismiss() }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.manufacturer,
                modifier = Modifier.weight(1.2f).padding(horizontal = 16.dp),
                textAlign = TextAlign.Start,
                style = itemTextStyle()
            )

            Text(
                text = row.modelName,
                modifier = Modifier.weight(2f).padding(horizontal = 16.dp),
                textAlign = TextAlign.Start,
                style = itemTextStyle()
            )

            Text(
                text = row.score,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                textAlign = TextAlign.Start,
                style = itemTextStyle()
            )

            Box(
                modifier = Modifier
                    .width(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = "Select",
                    tint = BrandOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    @Composable
    fun itemTextStyle() = TextStyle(
        fontFamily = ComposeUtil.myFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        color = Color.Black
    )
}

data class ModelRow(
    val manufacturer: String,
    val modelName: String,
    val score: String
)
