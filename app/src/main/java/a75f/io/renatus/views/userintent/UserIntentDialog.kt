package a75f.io.renatus.views.userintent

import a75f.io.api.haystack.Point
import a75f.io.logger.CcuLog
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.composables.DropDownWithoutLabel
import a75f.io.renatus.composables.DurationPicker
import a75f.io.renatus.compose.ButtonListRow
import a75f.io.renatus.compose.LabelTextViewForTable
import a75f.io.renatus.compose.SubTitleNoPadding
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.SAVE
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatSpinner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider


class UserIntentDialog(val pointId: String, private val spinnerView: View): BaseDialogFragment() {

    private lateinit var viewModel: UserIntentViewModel
    companion object {
        private var isUniqueDialogInDisplay = false

        fun isDialogAlreadyVisible(): Boolean {
            return isUniqueDialogInDisplay
        }
    }

    override fun getIdString() : String {
        return "UserIntentDialog-$pointId"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[UserIntentViewModel::class.java]
        viewModel.configModelView(pointId)
        isCancelable = false
        CcuLog.d("CCU_USER_INTENT", "UserIntentDialog.onCreateView() called with pointId: $pointId")
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent {
                RootView()
            }
        }
        return rootView
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        dialog?.window?.let { window ->
            val width = ViewGroup.LayoutParams.WRAP_CONTENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.setLayout(width, height)

            val density = resources.displayMetrics.density
            val yOffset = (52 * density).toInt()
            val params = window.attributes
            params.gravity = Gravity.TOP
            params.y = yOffset
            window.attributes = params
        }
    }

    @Composable
    private fun RootView() {
        if(viewModel.isLoadingComplete.value) {
            CcuLog.d("CCU_USER_INTENT", "Displaying Dialog")
            DialogView()
        } else {
            CcuLog.d("CCU_USER_INTENT", "Displaying Loader")
            LoadingDialog()
        }
    }

    @Composable
    private fun LoadingDialog() {
        Column(
            modifier = Modifier
                .width(200.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun DialogView() {
        Column(Modifier.clip(RoundedCornerShape(4.dp))
            .widthIn(max = 500.dp)) {
            Column(modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp)) {
                TitleView()
                ValueSelectorDropdown()
                DurationSetter()
            }
            ButtonListRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                textActionPairMap = mapOf(
                    CANCEL to Pair(true) {
                        viewModel.isSaveButtonClicked.value = false
                        dismiss()
                    },
                    SAVE to Pair(viewModel.isDurationValid.value) {
                        viewModel.saveUserIntent()
                        dismiss()
                    },
                )
            )
        }
    }

    @Composable
    private fun TitleView() {
        val levelDetails = "Writing at level 5"
        Column(modifier = Modifier
            .width(500.dp)
            .wrapContentHeight()) {
            SubTitleNoPadding(
                viewModel.pointName,
                fontSize = 24f,
                color = Color.Black,
                textAlignment = TextAlign.Start,
                textOverflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            LabelTextViewForTable(
                text = levelDetails,
                modifier = Modifier.align(Alignment.Start),
                fontSize = 18,
            )
        }
    }

    @Composable
    private fun ValueSelectorDropdown() {
        val valLabel = "Value"
        Column(modifier = Modifier.padding(top = 24.dp)) {
            SubTitleNoPadding(
                valLabel,
                fontSize = 18f,
                color = Color.Black
            )
            DropDownWithoutLabel(
                list = viewModel.spinnerOptions,
                maxLengthString = viewModel.longestItem,
                maxContainerWidth = 452.dp,
                onSelected = { selectedValue ->
                    viewModel.currentSelectedIndex.intValue = selectedValue
                },
                defaultSelection = viewModel.defaultSelectionIndex.intValue
            )
        }
    }

    @Composable
    private fun DurationSetter() {
        val durationLabel = "Duration"
        Column(modifier = Modifier.padding(top = 24.dp)) {
            SubTitleNoPadding(
                durationLabel,
                fontSize = 18f,
                color = Color.Black
            )
            Spacer(modifier = Modifier.padding(vertical = 12.dp))
            DurationPicker(
                onHHTimeFieldChange = { hhValue ->
                    viewModel.hhDurationVal.intValue = hhValue.toInt()
                    viewModel.validateDuration()
                },
                onMMTimeFieldChange = { mmValue ->
                    viewModel.mmDurationVal.intValue = mmValue.toInt()
                    viewModel.validateDuration()
                },
            )
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        CcuLog.d("CCU_USER_INTENT", "UserIntentDialog.show() called with pointId: $pointId and common flag set to true")
        isUniqueDialogInDisplay = true
        super.show(manager, tag)
    }

    override fun onDismiss(dialog: DialogInterface) {
        CcuLog.d("CCU_USER_INTENT", "UserIntentDialog.onDismiss() called with pointId: $pointId and common flag set to false")
        super.onDismiss(dialog)
        isUniqueDialogInDisplay = false
        //TODO: Once MVVM architecture is implemented, this logic should be modified instead of programmatically setting the spinner value
        if(viewModel.isSaveButtonClicked.value) {
            CcuLog.d("CCU_USER_INTENT", "UserIntentDialog.onDismiss() - Programmatically selecting spinner value for index: ${viewModel.currentSelectedIndex.intValue}")
            (spinnerView as AppCompatSpinner).setSelection(viewModel.currentSelectedIndex.intValue)
        }
    }
}