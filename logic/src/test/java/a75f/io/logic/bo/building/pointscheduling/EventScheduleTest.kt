package a75f.io.logic.bo.building.pointscheduling

import a75f.io.api.haystack.mock.MockCcuHsApi
import org.junit.Test
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HList
import org.projecthaystack.HRef
import org.projecthaystack.HStr

class EventScheduleTest {
    @Test
    fun eventPointSchedule() {

        val range = HDictBuilder()
            .add("startTime", HStr.make("08:00"))
            .add("endTime", HStr.make("17:00"))
            .add("startDate", HStr.make("2021-01-01"))
            .add("endDate", HStr.make("2021-01-01"))
            .toDict()

        val pointDefinition = mutableListOf<HDict>()

        pointDefinition.add(0,   HDictBuilder()
            .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605d1"))
            .add("dis", "Temp Sensor")
            .add("haystackQuery", "temp and sensor")
            .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
            .add("defaultValue", 11.5)
            .add("builderType", "Builder")
            .toDict())


        pointDefinition.add(1,   HDictBuilder()
            .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605de2"))
            .add("dis", "Temp Sensor1")
            .add("haystackQuery", "temp and sensor1 and sensor2")
            .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
            .add("defaultValue", 10.5)
            .add("builderType", "Builder")
            .toDict())


        val eventScheduleDict = HDictBuilder()
            .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605e1"))
            .add("siteRef", HRef.copy("e518a994-9fc6-457a-b07b-960181e605ca"))
            .add("dis", "Event Schedule sample")
            .add("orgRef", HRef.copy("e518a994-9fc6-457a-b07b-960181e605o1"))
            .add("range", range)
            .add("event")
            .add("point")
            .add("pointDefinitions", HList.make(pointDefinition))
            .toDict()

        var mockHayStack = MockCcuHsApi()
        mockHayStack.addHDict("@e518a994-9fc6-457a-b07b-960181e605e1", eventScheduleDict)
       val eventDict =mockHayStack
            .readHDict("id == " + HRef.make("e518a994-9fc6-457a-b07b-960181e605e1"))
        println("event dict = $eventDict")

        val eventMao =mockHayStack
            .readEntity("id == " + HRef.make("e518a994-9fc6-457a-b07b-960181e605e1"))
        println("event map = $eventMao")
    }
}