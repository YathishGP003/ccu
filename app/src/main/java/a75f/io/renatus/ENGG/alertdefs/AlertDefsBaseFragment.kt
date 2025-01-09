package a75f.io.renatus.ENGG.alertdefs

import a75f.io.renatus.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AlertDefsBaseFragment : Fragment() {

    private val TAG = "AlertDefsBaseFragment"

    override fun onCreateView(
        inflator: LayoutInflater, container: ViewGroup?, saveInstanceState: Bundle?
    ): View? {
        return inflator.inflate(R.layout.lyt_alert_defs_base, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var fragment: Fragment = AlertDefsFragment()
        setUpFragment(fragment)
    }

    private fun setUpFragment(fragment: Fragment) {
        val fragmentManager = childFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.container_property_fragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}