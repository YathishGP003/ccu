package a75f.io.domain

import io.seventyfivef.domainmodeler.client.ModelDiff
import io.seventyfivef.domainmodeler.client.ModelTagDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.Version
import io.seventyfivef.domainmodeler.common.point.BaseConfiguration
import io.seventyfivef.domainmodeler.common.point.ModelAssociation
import io.seventyfivef.domainmodeler.common.point.NoConstraint
import io.seventyfivef.ph.core.PointType
import io.seventyfivef.ph.core.TagType
import org.junit.Test

/**
 * Created by Manjunath K on 21-06-2023.
 */

class DomainNameTest  {

    @Test
    fun domainTest(){
        val left = SeventyFiveFProfileDirective(
            id = "1",
            name = "75F Model",
            domainName = "model1",
            tagNames = setOf("tag1"),
            tags = setOf(ModelTagDef("tag1", TagType.MARKER),),
            points = listOf(
                SeventyFiveFProfilePointDef(
                    id = "11",
                    domainName = "point1",
                    name = "point1",
                    tagNames = setOf("tag2"),
                    rootTagNames = setOf("tag2"),
                    tags = setOf(ModelTagDef("tag2", TagType.STR, "test")),
                    kind = PointType.NUMBER,
                    valueConstraint = NoConstraint(),
                    presentationData = mapOf(),
                    defaultUnit = "",
                    defaultValue = 0,
                    configuration = BaseConfiguration(),
                )
            ),
            equips = listOf(),
            associatedWithDevice = ModelAssociation("device-1", Version(0, 0, 1)),
            associatedWithTuner = ModelAssociation("tuner-1", Version(0, 0, 1))
        )
        val right = SeventyFiveFProfileDirective(
            id = "1",
            name = "75F Model",
            domainName = "model1",
            tagNames = setOf("tag1"),
            tags = setOf(ModelTagDef("tag1", TagType.MARKER),),
            points = listOf(
                SeventyFiveFProfilePointDef(
                    id = "11",
                    domainName = "point1",
                    name = "point1",
                    tagNames = setOf("tag2"),
                    rootTagNames = setOf("tag2"),
                    tags = setOf(ModelTagDef("tag2", TagType.STR, "test")),
                    kind = PointType.NUMBER,
                    valueConstraint = NoConstraint(),
                    presentationData = mapOf(),
                    defaultUnit = "",
                    defaultValue = 0,
                    configuration = BaseConfiguration(),
                )
            ),
            equips = listOf(),
            associatedWithDevice = ModelAssociation("device-1", Version(0, 0, 1)),
            associatedWithTuner = ModelAssociation("tuner-1", Version(0, 0, 1))
        )

        // When
        val diff = ModelDiff(left, right, groupByTagNames = false)
        val x =diff
    }
}