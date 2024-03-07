package a75f.io.renatus.hyperstat.ui

import a75f.io.renatus.R
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

class AnalogOutAdapter(context: Context, resource: Int, objects: List<*>) :
    ArrayAdapter<Any>(context, resource, objects) {

    private val disabledPositions = mutableSetOf<Int>()

    override fun isEnabled(position: Int): Boolean {
        return !disabledPositions.contains(position)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        parent.setPadding(0, 7, 0, 5)
        val view = super.getView(position, convertView, parent)
        view.isEnabled = !isEnabled(position)
        if (!isEnabled(position)) {
            view.alpha = 0.5f
        } else {
            view.alpha = 1.0f
        }
        view.setBackgroundResource(R.drawable.custmspinner)
        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        parent.setPadding(0, 0, 0, 0)
        val view = super.getView(position, convertView, parent)
        view.setPadding(5, 0, 30, 0)
        return view
    }

    fun setItemEnabled(position: Int, enabled: Boolean) {
        if (enabled) {
            disabledPositions.remove(position)
        } else {
            disabledPositions.add(position)
        }
        notifyDataSetChanged()
    }
}


