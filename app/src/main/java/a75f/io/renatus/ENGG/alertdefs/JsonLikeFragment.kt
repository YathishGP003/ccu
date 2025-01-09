package a75f.io.renatus.ENGG.alertdefs

import a75f.io.logger.CcuLog
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SEQUENCE_RESULT_JSON
import a75f.io.renatus.R
import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

class JsonLikeFragment : Fragment() {
    var sequenceResultJson = ""

    private lateinit var jsonList: List<HashMap<String, Any>>
    private lateinit var detailsAdapter : JsonAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sequenceResultJson = arguments?.getString(SEQUENCE_RESULT_JSON).toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.alert_log_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.img_close).setOnClickListener(View.OnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        })

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        jsonList = convertJsonToList(sequenceResultJson)
        sortElements(jsonList)
        detailsAdapter = JsonAdapter(jsonList, requireContext())
        recyclerView.adapter = detailsAdapter


        val searchView = view.findViewById<SearchView>(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                CcuLog.d(BundleConstants.TAG, "onQueryTextSubmit-->$query")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                CcuLog.d(BundleConstants.TAG, "onQueryTextChange-->$newText")
                if (newText != null) {
                    //alertLogSequenceAdapter.filter(newText)
                    val query = newText.toString()
                    filterJsonList(query)
                }
                return true
            }
        })
    }

    private fun filterJsonList(query: String) {
        val filteredList = jsonList.filter { jsonObject ->
            jsonObject.any { (key, value) ->
                key.contains(query, ignoreCase = true) || value.toString()
                    .contains(query, ignoreCase = true)
            }
        }
        detailsAdapter.updateList(filteredList)
    }

    private fun convertJsonToList(jsonString: String): List<HashMap<String, Any>> {
        val gson = Gson()
        val type = object : TypeToken<List<HashMap<String, Any>>>() {}.type
        return gson.fromJson(jsonString, type)
    }

    private fun sortElements(list : List<HashMap<String, Any>>){
        for(item in list){
            list[0].toSortedMap()
        }
    }

    private fun convertJsonToIndexedList(jsonString: String): List<HashMap<String, Any>> {
        val gson = Gson()
        val type = object : TypeToken<List<HashMap<String, Any>>>() {}.type
        val list: List<HashMap<String, Any>> = gson.fromJson(jsonString, type)

        // Add the index as a key for each item
        return list.mapIndexed { index, map ->
            HashMap(map).apply { this["index"] = index }
        }
    }
}

class JsonAdapter(private var entities: List<HashMap<String, Any>>, private val context: Context) :
    RecyclerView.Adapter<JsonAdapter.JsonViewHolder>() {

    private val keyColor = ContextCompat.getColor(context, R.color.log_key_blue)
    private val valueColor = ContextCompat.getColor(context, R.color.log_value_red)

    class JsonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvJsonObject: TextView = view.findViewById(R.id.tvJsonObject)
        val tvIndex: TextView = view.findViewById(R.id.key)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JsonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_json_object, parent, false)
        return JsonViewHolder(view)
    }

    override fun onBindViewHolder(holder: JsonViewHolder, position: Int) {
        val entity = entities[position].toSortedMap()
        val jsonObject = JSONObject(entity as Map<*, *>?) // Convert to JSON Object
        val styledJson = getStyledJsonString(jsonObject)
        holder.tvJsonObject.text = styledJson
        val index = "$position  :"
        holder.tvIndex.text = index
    }

    private fun getStyledJsonString(jsonObject: JSONObject): SpannableStringBuilder {
        val styledText = SpannableStringBuilder()
        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)

            // Append the key in a specific color
            val keyStart = styledText.length
            styledText.append("$key :  ")
            styledText.setSpan(
                ForegroundColorSpan(keyColor), // Blue for keys
                keyStart,
                styledText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Append the value in another color
            val valueStart = styledText.length
            styledText.append("$value\n")
            styledText.setSpan(
                ForegroundColorSpan(valueColor), // Green for values
                valueStart,
                styledText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return styledText
    }

    override fun getItemCount(): Int = entities.size
    fun updateList(newEntities: List<HashMap<String, Any>>) {
        this.entities = newEntities
        notifyDataSetChanged()
    }
}


