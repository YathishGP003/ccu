package a75f.io.renatus.views.MasterControl

import a75f.io.api.haystack.CCUHsApi
import a75f.io.renatus.R
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class UserLimitScaleFragment : DialogFragment() {


    companion object {

        private const val PARAMETER_ISHEATING: String = "isheating"
        fun newInstance(isHeating: Boolean): UserLimitScaleFragment {
            val userLimitScaleFragment = UserLimitScaleFragment()
            val args = Bundle()
            args.putBoolean(PARAMETER_ISHEATING, isHeating)
            userLimitScaleFragment.arguments = args
            return userLimitScaleFragment
        }


    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = 1165
            val height = 646
            dialog.window!!.setLayout(width, height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView: View = inflater.inflate(R.layout.master_control_user_limit_fragment, container, false)

        val btnClose = rootView.findViewById<ImageView>(R.id.btnClose)
        btnClose.setOnClickListener {
            val fragment: Fragment? = requireActivity().supportFragmentManager.findFragmentByTag("dialog")
            if (fragment != null) {
                val dialog = fragment as DialogFragment
                dialog.dismiss()
            }
        }
        val isHeating = arguments?.get(PARAMETER_ISHEATING) as Boolean
        var tempTag = "heating"
        if (!isHeating) {
            tempTag = "cooling"
            val title = rootView.findViewById<TextView>(R.id.textLabel)
            val mintitle = rootView.findViewById<TextView>(R.id.minTemp)
            val maxtitle = rootView.findViewById<TextView>(R.id.maxTemp)

            title.setText(R.string.cooling_userlimit_Title)
            mintitle.setText(R.string.cooling_user_limit_min)
            maxtitle.setText(R.string.cooling_user_limit_max)
        }

        val allZones = CCUHsApi.getInstance().readAllEntities("room")
        val allTempZones : ArrayList<HashMap<Any,Any>> = ArrayList()
        allZones.forEach{
            if(!checkIfNonTempEquipInZone(it))
                allTempZones.add(it)
        }

        val zoneDisplay: RecyclerView = rootView.findViewById(R.id.zonedata)
        zoneDisplay.layoutManager  = LinearLayoutManager(context)
        var zoneInfoAdapter = ZoneTempAdapter(allTempZones, tempTag)
        zoneDisplay.adapter = zoneInfoAdapter
        zoneDisplay.invalidate()

        val searchBar: EditText = rootView.findViewById(R.id.editZoneSearch)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val updateZonesList:ArrayList<HashMap<Any, Any>> = ArrayList()
                for( zone in allTempZones){
                    if((zone["dis"].toString()).contains(s.toString())){
                        updateZonesList.add(zone)
                    }
                }
                zoneInfoAdapter = ZoneTempAdapter(updateZonesList, tempTag)
                zoneDisplay.adapter = zoneInfoAdapter
                zoneDisplay.invalidate()
            }
        })

        return rootView
    }

    fun checkIfNonTempEquipInZone(zone: java.util.HashMap<Any?, Any>): Boolean {
        val equips = CCUHsApi.getInstance().readAllEntities(
            "equip and roomRef ==\""
                    + zone["id"].toString() + "\""
        )
        return equips.stream().anyMatch { equip: java.util.HashMap<Any, Any> ->
            MasterControlUtil.isNonTempModule(
                equip["profile"].toString()
            )
        }
    }

}
