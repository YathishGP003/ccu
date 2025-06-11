@file:Suppress("IMPLICIT_CAST_TO_ANY")

package a75f.io.sanity.ui

import a75f.io.sanity.R
import a75f.io.sanity.framework.SanityResultType
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SanityResultAdapter(
    context: Context,
    private val items: List<SanityResultViewItem>
) : ArrayAdapter<SanityResultViewItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_result, parent, false)

        val testNameText = itemView.findViewById<TextView>(R.id.testNameText)
        val descriptionText = itemView.findViewById<TextView>(R.id.descriptionText)
        val resultText = itemView.findViewById<TextView>(R.id.resultText)

        val item = getItem(position)!!

        testNameText.text = item.testName
        descriptionText.text = item.description
        resultText.text = item.result.name

        val color = when (item.result) {
            SanityResultType.PASSED -> {
                Color.parseColor("#4CAF50") // Green
            }

            SanityResultType.FAILED -> {
                Color.parseColor("#F44336") // Red
            }

            SanityResultType.PENDING -> {
                Color.parseColor("#9E9E9E") // Grey
            }
        }
        resultText.setTextColor(color)
        return itemView
    }
}
