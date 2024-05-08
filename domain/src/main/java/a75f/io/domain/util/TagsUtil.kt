package a75f.io.domain.util

import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelTagDef
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HNum
import org.projecthaystack.HVal

object TagsUtil {
    fun getTagDefHVal(tag : ModelTagDef) : HVal?{
        return when (tag.defaultValue) {
            is Int -> HNum.make(tag.defaultValue as Int)
            is Double -> HNum.make(tag.defaultValue as Double)
            else -> {
                if (tag.kind == TagType.NUMBER && tag.defaultValue != null) {
                    return HNum.make(tag.defaultValue as Int)
                }
                CcuLog.i("CCU_DM", "Unknown tag type $tag")
                null
            }
        }
    }
}