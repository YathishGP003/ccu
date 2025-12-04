package a75f.io.renatus.bacnet

import a75f.io.renatus.R
import a75f.io.renatus.bacnet.models.BacnetDevice
import a75f.io.renatus.composables.VerticalScrollbar
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.modbus.util.DEVICE_ID
import a75f.io.renatus.modbus.util.DEVICE_IP
import a75f.io.renatus.modbus.util.DEVICE_MAC_ADDRESS
import a75f.io.renatus.modbus.util.DEVICE_NAME
import a75f.io.renatus.modbus.util.DEVICE_NETWORK
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.OnItemSelectBacnetDevice
import a75f.io.renatus.util.ProgressDialogUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.fragment.app.DialogFragment

class BacnetDeviceSelectionFragment : DialogFragment() {

    var itemsList = mutableStateOf(emptyList<BacnetDevice>())
    lateinit var onItemSelect: OnItemSelectBacnetDevice
    lateinit var placeholder: String
    private var isMstpView: Boolean = false

    companion object {
        val ID: String = BacnetDeviceSelectionFragment::class.java.simpleName

        fun newInstance(
            items: MutableState<List<BacnetDevice>>,
            onItemSelect: OnItemSelectBacnetDevice,
            placeholder: String,
            isMstpView: Boolean = false
        ): BacnetDeviceSelectionFragment {
            val fragment = BacnetDeviceSelectionFragment()
            fragment.itemsList = items
            fragment.onItemSelect = onItemSelect
            fragment.placeholder = placeholder
            fragment.isMstpView = isMstpView
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
                ShowOptions(itemsList.value)
            }
            ProgressDialogUtils.hideProgressDialog()
            return rootView
        }
    }

    @Composable
    fun ShowOptions(
        items: List<BacnetDevice>
    ) {
        val listState = rememberLazyListState()
        var searchText by remember { mutableStateOf("") }
        val filteredItems = if (searchText.isEmpty()) {
            items
        } else {
            items.filter {
                it.deviceId.contains(searchText, ignoreCase = true)
            }
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
                Text(
                    text = "Select Device",
                    style = TextStyle(
                        fontFamily = ComposeUtil.myFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.width(24.dp))

                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchText.isEmpty()) {
                        Text(
                            text = "Search for Device",
                            color = Color.Gray,
                            fontSize = 18.sp,
                            fontFamily = ComposeUtil.myFontFamily
                        )
                    }
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            color = Color.Black,
                            fontFamily = ComposeUtil.myFontFamily
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_close_24),
                    contentDescription = "Close",
                    tint = Color(0xFFFF6600), // Orange
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { dialog!!.dismiss() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color.LightGray, thickness = 1.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    if (isMstpView) {
                        item { MstpDeviceListItemHeader() }
                        items(filteredItems.size) { index ->
                            MstpDeviceListItem(
                                device = filteredItems[index],
                                isEven = index % 2 == 0,
                                index = index
                            )
                        }
                    } else {
                        item { DeviceListItemHeader() }
                        items(filteredItems.size) { index ->
                            DeviceListItem(
                                device = filteredItems[index],
                                isEven = index % 2 == 0,
                                index = index
                            )
                        }
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

    fun getIdString(): String {
        return ID
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(1200, 700)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.5f)
        }
    }

    @Composable
    fun DeviceListItem(device: BacnetDevice, isEven: Boolean, index: Int) {
        val backgroundColor = if (isEven) Color(0xFFF9F9F9) else Color.White
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable(onClick = {
                    onItemSelect.onItemSelected(index, device)
                    dismiss()
                })
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${device.deviceId}",
                modifier = Modifier.weight(0.7f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "${device.deviceIp}/${device.devicePort}",
                modifier = Modifier.weight(1.5f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "${device.deviceNetwork}",
                modifier = Modifier.weight(0.8f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "${device.deviceName}",
                modifier = Modifier.weight(2f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                ),
            )

            Box(
                modifier = Modifier.width(30.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = "Navigate",
                    tint = Color(0xFFFF6600),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    @Composable
    fun DeviceListItemHeader() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEBECED))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DEVICE_ID,
                modifier = Modifier.weight(0.7f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
            Text(
                text = DEVICE_IP,
                modifier = Modifier.weight(1.5f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
            Text(
                text = DEVICE_NETWORK,
                modifier = Modifier.weight(0.8f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
            Text(
                text = DEVICE_NAME,
                modifier = Modifier.weight(2f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                ),
            )
            Spacer(modifier = Modifier.width(30.dp))
        }
    }

    @Composable
    fun MstpDeviceListItem(device: BacnetDevice, isEven: Boolean, index: Int) {
        val backgroundColor = if (isEven) Color(0xFFF9F9F9) else Color.White
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable(onClick = {
                    onItemSelect.onItemSelected(index, device)
                    dismiss()
                })
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${device.deviceId}",
                modifier = Modifier.weight(0.8f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "${device.deviceMacAddress}",
                modifier = Modifier.weight(1.5f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )

            Text(
                text = "${device.deviceName}",
                modifier = Modifier.weight(2f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                ),
            )
            Box(
                modifier = Modifier.width(30.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = "Navigate",
                    tint = Color(0xFFFF6600), // Orange
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    @Composable
    fun MstpDeviceListItemHeader() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEBECED)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DEVICE_ID,
                modifier = Modifier.weight(0.8f).padding(vertical = 12.dp, horizontal = 8.dp).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
            Text(
                text = DEVICE_MAC_ADDRESS,
                modifier = Modifier.weight(1.5f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )

            Text(
                text = DEVICE_NAME,
                modifier = Modifier.weight(2f).padding(start = 16.dp),
                textAlign = TextAlign.Start,
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                ),
            )
            Spacer(modifier = Modifier.width(30.dp))
        }
    }
}