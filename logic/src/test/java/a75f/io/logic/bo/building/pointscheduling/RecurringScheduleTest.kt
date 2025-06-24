package a75f.io.logic.bo.building.pointscheduling

import a75f.io.api.haystack.mock.MockCcuHsApi
import org.junit.Test
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HList
import org.projecthaystack.HRef
import org.projecthaystack.HStr


class RecurringScheduleTest {
    @Test
    fun createPointSchedule() {

        val days = mutableListOf<HDict>()

        days.add(0, HDictBuilder()
                .add("startHour", 8)
                .add("startMinute", 0)
                .add("endHour", 17)
                .add("endMinute", 0)
                .add("day", 0)
                .add("value", 1.5)
                .toDict())
        days.add(1, HDictBuilder()
            .add("startHour", 17)
            .add("startMinute", 0)
            .add("endHour", 18)
            .add("endMinute", 0)
            .add("day", 1)
            .add("value", 2.9)
            .toDict())
        days.add(2, HDictBuilder()
            .add("startHour", 8)
            .add("startMinute", 0)
            .add("endHour", 17)
            .add("endMinute", 0)
            .add("day", 2)
            .add("value", 2.7)
            .toDict()
        )


        val pointDefinition = mutableListOf<HDict>()

        pointDefinition.add(0,   HDictBuilder()
            .add("id", HRef.copy("db02e2a3-c5b9-4678-b50d-8203c5ea1101"))
            .add("dis", "Temp Sensor")
            .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
            .add("defaultValue", 11.5)
            .add("haystackQuery", "temp and sensor")
            .add("builderType", "Builder")
            .add("days", HList.make(days))
            .toDict())

        pointDefinition.add(1,   HDictBuilder()
            .add("id", HRef.copy("db02e2a3-c5b9-4678-b50d-8203c5ea1102"))
            .add("dis", "Temp Sensor1")
            .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("type"))))
            .add("defaultValue", 22.5)
            .add("haystackQuery", "temp and sensor")
            .add("builderType", "Builder")
            .add("days", HList.make(days))
            .toDict())

        val pointScheduleHdict = HDictBuilder()
            .add("id", HRef.copy("db02e2a3-c5b9-4678-b50d-8203c5ea1133"))
            .add("dis", "unit command schedule")
            .add("siteRef", HRef.copy("e518a994-9fc6-457a-b07b-960181e605ca"))
            .add("orgRef", HRef.copy("db02e2a3-c5b9-4678-b50d-8203c5ea1132"))
            .add("id", HRef.copy("db02e2a3-c5b9-4678-b50d-8203c5ea1133"))
            .add("schedule")
            .add("point")
            .add("scheduleGroup", 2)
            .add("pointDefinitions", HList.make(pointDefinition))
            .toDict()


        var mockHayStack = MockCcuHsApi()
        mockHayStack.addHDict("@db02e2a3-c5b9-4678-b50d-8203c5ea1133", pointScheduleHdict)
        val pointDict =mockHayStack
            .readHDict("id == " + HRef.make("db02e2a3-c5b9-4678-b50d-8203c5ea1133"))
        println("point dict = $pointDict")

        val pointMap =mockHayStack
            .readEntity("id == " + HRef.make("db02e2a3-c5b9-4678-b50d-8203c5ea1133"))
        println("point map = $pointMap")

    }

}