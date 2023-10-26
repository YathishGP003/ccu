package a75f.io.renatus.hyperstatsplit.ui
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

class AnalogOutAdapter(context: Context, resource: Int, objects: Array<String>) :
    ArrayAdapter<String>(context, resource, objects) {
    private val disabledPositions = mutableSetOf<Int>()
    override fun isEnabled(position: Int): Boolean {
        return !disabledPositions.contains(position)
    }
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        view.isEnabled = !isEnabled(position)
        if (!isEnabled(position)) {
            view.alpha = 0.5f
        } else {
            view.alpha = 1.0f
        }
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
