package a75f.io.renatus.registration

import a75f.io.renatus.R
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.android.material.color.MaterialColors

class CountryAdapter(
    context: Context,
    private val countries: List<CountryItem>
) : ArrayAdapter<CountryItem>(context, R.layout.spinner_country_item, countries) {

    private val filteredItems = countries.toMutableList()
    private val inflater = LayoutInflater.from(context)
    private var selectedCountry: String? = null

    override fun getCount(): Int = filteredItems.size

    override fun getItem(position: Int): CountryItem = filteredItems[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.spinner_country_item, parent, false)
        val textView = view.findViewById<TextView>(R.id.textCountryName)
        val item = filteredItems[position]

        textView.text = item.label ?: ""

        // Highlight selected
        if (item.label.equals(selectedCountry, ignoreCase = true)) {
            textView.setBackgroundColor(
                MaterialColors.getColor(
                    context,
                    R.attr.orange_75f_secondary,
                    Color.RED
                ))
        } else {
            textView.setBackgroundColor(Color.TRANSPARENT)
            textView.setTypeface(textView.typeface, android.graphics.Typeface.NORMAL)
        }

        return view
    }

    fun setSelectedCountry(country: String?) {
        selectedCountry = country
        notifyDataSetChanged()
    }

    fun filter(query: String?) {
        filteredItems.clear()
        if (query.isNullOrBlank()) {
            filteredItems.addAll(countries)
        } else {
            val lower = query.lowercase()
            filteredItems.addAll(countries.filter { it.label?.lowercase()?.contains(lower) == true })
        }
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int {
        return filteredItems.indexOfFirst { it.label.equals(selectedCountry, ignoreCase = true) }
    }
}
