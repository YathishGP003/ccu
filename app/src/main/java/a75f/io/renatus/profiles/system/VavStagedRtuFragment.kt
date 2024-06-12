package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.util.ProgressDialogUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VavStagedRtuFragment : StagedRtuFragment() {

    private val viewModel: VavStagedRtuViewModel by viewModels()
    private val SYSTEM_CONFIG_TAB: Int = 1
    companion object {
        val ID: String = VavStagedRtuFragment::class.java.simpleName
        fun newInstance(): VavStagedRtuFragment {
            return VavStagedRtuFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                viewModel.init(requireContext(), CCUHsApi.getInstance())
            }
        }
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }

    }
   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
       super.onViewCreated(view, savedInstanceState)
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
            }

            override fun onViewDetachedFromWindow(v: View) {
                if (Globals.getInstance().isTestMode()) {
                    Globals.getInstance().setTestMode(false);
                }
            }
        })
    }

    @Preview
    @Composable
    fun RootView() {
        if (!viewModel.modelLoaded) {
            if(Globals.getInstance().getSelectedTab() == SYSTEM_CONFIG_TAB) {
                ProgressDialogUtils.showProgressDialog(context, "Loading System Profile")
            }
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
            return
        }
        ProgressDialogUtils.hideProgressDialog()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        )
        {
            Image(
                painter = painterResource(id = R.drawable.input_vav_rtu_2),
                contentDescription = "Relays",
                modifier = Modifier
                    .padding(top = 75.dp, bottom = 5.dp, start = 20.dp)
                    .height(475.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
            ) {
                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 40.dp),
                    ) {
                        Text(text = "ENABLE", fontSize = 20.sp, color = ComposeUtil.greyColor)
                        Spacer(modifier = Modifier.width(270.dp))
                        Text(text = "MAPPING", fontSize = 20.sp, color = ComposeUtil.greyColor)
                        Spacer(modifier = Modifier.width(190.dp))
                        Text(text = "TEST SIGNAL", fontSize = 20.sp, color = ComposeUtil.greyColor)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    StagedRtuRelayMappingView(viewModel = viewModel)
                }
            }
        }
    }

}