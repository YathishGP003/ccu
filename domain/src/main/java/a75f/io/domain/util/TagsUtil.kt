package a75f.io.domain.util

import io.seventyfivef.domainmodeler.client.ModelTagDef
import org.projecthaystack.HNum
import org.projecthaystack.HVal

object TagsUtil {
    fun getTagDefHVal(tag : ModelTagDef) : HVal?{
        return when (tag.defaultValue) {
            is Int -> HNum.make(tag.defaultValue as Int)
            is Double -> HNum.make(tag.defaultValue as Double)
            else -> {
                //CcuLog.i("CCU_DM", "Unknown tag type $tag")
                println("Unknown tag type $tag")
                null
            }
        }
    }
}