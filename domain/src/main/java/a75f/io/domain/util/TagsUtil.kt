package a75f.io.domain.util

import a75f.io.domain.model.TagDef
import a75f.io.logger.CcuLog
import org.projecthaystack.HNum
import org.projecthaystack.HVal

object TagsUtil {
    fun getTagDefHVal(tag : TagDef) : HVal?{
        return when (tag.defaultValue) {
            is Int -> HNum.make(tag.defaultValue.toInt())
            is Double -> HNum.make(tag.defaultValue.toDouble())
            else -> {
                //CcuLog.i("CCU_DM", "Unknown tag type $tag")
                println("Unknown tag type $tag")
                null
            }
        }
    }
}