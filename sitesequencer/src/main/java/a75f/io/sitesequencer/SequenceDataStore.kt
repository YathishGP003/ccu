package a75f.io.sitesequencer

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import android.content.Context
import org.apache.commons.lang3.StringUtils


private const val PREFS_SEQUENCER_DEFS = "ccu_sequences"
public val TAG_CCU_SITE_SEQUENCER = "CCU_SITE_SEQUENCER"

class SequenceDataStore @JvmOverloads constructor(
    private val context: Context,
    private val haystack: CCUHsApi = CCUHsApi.getInstance(),
    private val parser: SequencerParser = SequencerParser()
) {

    private val sequencerSharedPrefs = context.getSharedPreferences(
        PREFS_SEQUENCER_DEFS,
        Context.MODE_PRIVATE
    )

    fun getSiteSequencerDefinitions(): List<SiteSequencerDefinition> {
        val siteSequencersDefString = sequencerSharedPrefs.getString(PREFS_SEQUENCER_DEFS, "")

//        CcuLog.d(
//            TAG_CCU_SITE_SEQUENCER,
//            "Parsing Site Sequencer:  $siteSequencersDefString, with $parser"
//        )

        try {
            return if (StringUtils.isNotBlank(siteSequencersDefString)) parser.parseSequencerString(
                siteSequencersDefString
            ) else ArrayList()
        } catch (ex: Exception) {
            CcuLog.e(
                TAG_CCU_SITE_SEQUENCER,
                "Unable to parsing Site Sequencer:  $siteSequencersDefString"
            )
            return ArrayList()
        }
    }

    fun saveSiteSequencerDefinitions(siteSequencerDefs: List<SiteSequencerDefinition>) {
        val siteSequencers = parser.sequencerDefsToString(siteSequencerDefs)
        //CcuLog.i(TAG_CCU_SITE_SEQUENCER, "Saving site sequencers:  $siteSequencers")
        sequencerSharedPrefs.edit().putString(PREFS_SEQUENCER_DEFS, siteSequencers).apply()
    }
}