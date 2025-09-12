package a75.io.logic.bo.util

import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.projecthaystack.HList
import org.projecthaystack.HStr
import java.lang.reflect.Method
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CustomScheduleUtilTest {

    // Reflective methods to be tested
    private lateinit var methodFindExternalProfileType: Method
    private lateinit var methodIsSchedulableTagRemovedPostUpdate: Method
    private lateinit var methodIsCustomScheduleRemovedPostUpdate: Method
    private lateinit var methodIsEventRemovedPostUpdate: Method
    private lateinit var methodIsCustomScheduleAndEventUnavailablePostUpdate: Method
    private lateinit var isExternalPointUpdatedToStopCustomControl: Method

    // Maps to simulate different equip configurations
    private lateinit var modbusEquipMap: HashMap<Any, Any>
    private lateinit var bacnetClientEquipMap: HashMap<Any, Any>
    private lateinit var connectNodeEquipMap: HashMap<Any, Any>
    private lateinit var emptyEquipMap: HashMap<Any, Any>
    private lateinit var noExternalTagEquipMap: HashMap<Any, Any>
    private lateinit var notModbusButConnectNodeEquipMap: HashMap<Any, Any>

    // Points to test schedulable tag removal logic
    private lateinit var pointWithSchedulableTag: Point
    private lateinit var pointWithoutSchedulableTag: Point

    // Points to test scheduleRef tag removal logic
    private lateinit var pointWithScheduleRefTag: Point
    private lateinit var pointWithoutScheduleRefTag: Point

    // Points to test event removal logic
    private lateinit var pointWithSingleEvent: Point
    private lateinit var pointWithoutSingleEvent: Point
    private lateinit var pointAWithMultipleEvents: Point
    private lateinit var pointAWithMultipleEventsPostSingleRemoval: Point
    private lateinit var pointAWithMultipleEventsPostMultipleRemoval: Point
    private lateinit var pointBWithMultipleEvents: Point
    private lateinit var pointBWithMultipleEventsPostSingleAddition: Point
    private lateinit var pointBWithMultipleEventsPostMultipleAddition: Point
    private lateinit var pointCWithMultipleEvents: Point
    private lateinit var pointCWithMultipleEventsAddedRemoved: Point

    // Points to test the method which calls for schedulableTag, scheduleREf tag and event removal logic check
    private lateinit var pointWithSchedulableTagAndScheduleRef: Point
    private lateinit var pointWithSchedulableTagAndEvent: Point
    private lateinit var pointWithScheduleRefAndEvent: Point
    private lateinit var pointWithSchedulableTagScheduleRefAndEvent: Point
    private lateinit var pointWithoutSchedulableTagScheduleRefAndEvent: Point
    private lateinit var pointWithoutSchedulableTagAndEvent: Point
    private lateinit var pointWithoutScheduleRefAndEvent: Point
    private lateinit var pointWithoutSchedulableTagWithScheduleRefAndEvent: Point
    private lateinit var pointWithoutSchedulableTagWithoutScheduleRefAndEvent: Point
    private lateinit var pointWithoutSchedulableTagWithScheduleRefWithoutEvent: Point
    private lateinit var pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent: Point


    @Before
    fun setUpReflectiveFileAndMethods() {
        val utilClass = Class.forName("a75f.io.logic.bo.util.CustomScheduleUtilKt")
        methodFindExternalProfileType =
            utilClass.getDeclaredMethod("findExternalProfileType", HashMap::class.java)
        methodIsSchedulableTagRemovedPostUpdate = utilClass.getDeclaredMethod(
            "isSchedulableTagRemovedPostUpdate",
            Point::class.java,
            Point::class.java
        )
        methodIsCustomScheduleRemovedPostUpdate = utilClass.getDeclaredMethod(
            "isCustomScheduleRemovedPostUpdate",
            Point::class.java,
            Point::class.java
        )
        methodIsEventRemovedPostUpdate = utilClass.getDeclaredMethod(
            "isEventRemovedPostUpdate",
            Point::class.java,
            Point::class.java
        )
        methodIsCustomScheduleAndEventUnavailablePostUpdate = utilClass.getDeclaredMethod(
            "isCustomScheduleAndEventUnavailablePostUpdate",
            Point::class.java,
            Point::class.java
        )
        isExternalPointUpdatedToStopCustomControl = utilClass.getDeclaredMethod(
            "isExternalPointUpdatedToStopCustomControl",
            Point::class.java,
            Point::class.java
        )
    }

    @Before
    fun setUpEntityMaps() {
        modbusEquipMap = hashMapOf(
             Tags.MODBUS to "M"
        )
        notModbusButConnectNodeEquipMap = hashMapOf(
            Tags.MODBUS to "M",
            Tags.CONNECTMODULE to "M"
        )
        bacnetClientEquipMap = hashMapOf(
            Tags.BACNET_CONFIG to "M"
        )
        connectNodeEquipMap = hashMapOf(
            Tags.CONNECTMODULE to "M"
        )
        emptyEquipMap = HashMap()
        noExternalTagEquipMap = hashMapOf(
            Tags.MYSTAT to "M"
        )
    }

    @Before
    fun setUpPoints() {
        pointWithSchedulableTag = Point.Builder().setMarkers(arrayListOf(Tags.SCHEDULABLE)).build()
        pointWithoutSchedulableTag = Point.Builder().setMarkers(arrayListOf()).build()

        pointWithScheduleRefTag = Point.Builder().setScheduleRef("PLACEHOLDER").build()
        pointWithoutScheduleRefTag = Point.Builder().build()

        pointWithSingleEvent = Point.Builder().setEventRef(HList.make(arrayOf(HStr.make("event1")))).build()
        pointWithoutSingleEvent = Point.Builder().build()
        pointAWithMultipleEvents = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event1"), HStr.make("event2"), HStr.make("event3")))).build()
        pointAWithMultipleEventsPostSingleRemoval = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event2"), HStr.make("event3")))).build()
        pointAWithMultipleEventsPostMultipleRemoval = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event3")))).build()
        pointBWithMultipleEvents = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event4"), HStr.make("event5"), HStr.make("event6")))).build()
        pointBWithMultipleEventsPostSingleAddition = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event4"), HStr.make("event5"), HStr.make("event6"), HStr.make("event7")))).build()
        pointBWithMultipleEventsPostMultipleAddition = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event4"), HStr.make("event5"), HStr.make("event6"), HStr.make("event7"), HStr.make("event8")))).build()
        pointCWithMultipleEvents = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event9"), HStr.make("event10"), HStr.make("event11")))).build()
        pointCWithMultipleEventsAddedRemoved = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event10"), HStr.make("event11")))).build()

        // Points to test the method which calls for schedulableTag, scheduleREf tag and event removal logic check
        pointWithSchedulableTagAndScheduleRef = Point.Builder()
            .setMarkers(arrayListOf(Tags.SCHEDULABLE))
            .setScheduleRef("PLACEHOLDER")
            .build()
        pointWithSchedulableTagAndEvent = Point.Builder()
            .setMarkers(arrayListOf(Tags.SCHEDULABLE))
            .setEventRef(HList.make(arrayOf(HStr.make("event1"))))
            .build()
        pointWithScheduleRefAndEvent = Point.Builder()
            .setScheduleRef("PLACEHOLDER")
            .setEventRef(HList.make(arrayOf(HStr.make("event1"))))
            .build()
        pointWithSchedulableTagScheduleRefAndEvent = Point.Builder()
            .setMarkers(arrayListOf(Tags.SCHEDULABLE))
            .setScheduleRef("PLACEHOLDER")
            .setEventRef(HList.make(arrayOf(HStr.make("event1"))))
            .build()
        pointWithoutSchedulableTagScheduleRefAndEvent = Point.Builder()
            .setScheduleRef("PLACEHOLDER")
            .setEventRef(HList.make(arrayOf(HStr.make("event1"))))
            .build()
        pointWithoutSchedulableTagAndEvent = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event1"))))
            .build()
        pointWithoutScheduleRefAndEvent = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event1"))))
            .build()
        pointWithoutSchedulableTagWithScheduleRefAndEvent = Point.Builder()
            .setMarkers(arrayListOf(Tags.SCHEDULABLE))
            .setScheduleRef("PLACEHOLDER")
            .setEventRef(HList.make(arrayOf(HStr.make("event1"))))
            .build()
        pointWithoutSchedulableTagWithoutScheduleRefAndEvent = Point.Builder()
            .setEventRef(HList.make(arrayOf(HStr.make("event1"))))
            .build()
        pointWithoutSchedulableTagWithScheduleRefWithoutEvent = Point.Builder()
            .setMarkers(arrayListOf(Tags.SCHEDULABLE))
            .setScheduleRef("PLACEHOLDER")
            .build()
        pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent = Point.Builder().build()
    }

    @Test
    fun testFindExternalProfileType() {
        val method : Method = methodFindExternalProfileType
        method.isAccessible = true

        // Positive Cases:
        assertEquals(Tags.MODBUS, method.invoke(null, modbusEquipMap))
        assertEquals(Tags.BACNET, method.invoke(null, bacnetClientEquipMap))
        assertEquals(Tags.CONNECTMODULE, method.invoke(null, connectNodeEquipMap))
        assertEquals(Tags.CONNECTMODULE, method.invoke(null, notModbusButConnectNodeEquipMap))

        // Negative Cases:
        assertNotEquals(Tags.MODBUS, method.invoke(null, connectNodeEquipMap))
        assertEquals("" , method.invoke(null, noExternalTagEquipMap))
        assertEquals("", method.invoke(null, emptyEquipMap))
    }

    @Test
    fun testIsSchedulableTagRemovedPostUpdate() {
        val method = methodIsSchedulableTagRemovedPostUpdate
        method.isAccessible = true

        // Positive Cases:
        assertTrue { method.invoke(null, pointWithSchedulableTag, pointWithoutSchedulableTag) as Boolean}

        // Negative Cases:
        assertFalse { method.invoke(null, pointWithSchedulableTag, pointWithSchedulableTag) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTag, pointWithSchedulableTag) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTag, pointWithoutSchedulableTag) as Boolean }
    }

    @Test
    fun testIsCustomScheduleRemovedPostUpdate() {
        val method = methodIsCustomScheduleRemovedPostUpdate
        method.isAccessible = true

        // Positive Cases:
        assertTrue { method.invoke(null, pointWithScheduleRefTag, pointWithoutScheduleRefTag) as Boolean }

        // Negative Cases:
        assertFalse { method.invoke(null, pointWithScheduleRefTag, pointWithScheduleRefTag) as Boolean }
        assertFalse { method.invoke(null, pointWithoutScheduleRefTag, pointWithScheduleRefTag) as Boolean }
        assertFalse { method.invoke(null, pointWithoutScheduleRefTag, pointWithoutScheduleRefTag) as Boolean }
    }

    @Test
    fun testIsEventRemovedPostUpdate() {
        val method = methodIsEventRemovedPostUpdate
        method.isAccessible = true

        // Positive Cases:
        assertTrue { method.invoke(null, pointWithSingleEvent, pointWithoutSingleEvent) as Boolean }
        assertTrue { method.invoke(null, pointAWithMultipleEvents, pointAWithMultipleEventsPostSingleRemoval) as Boolean }
        assertTrue { method.invoke(null, pointAWithMultipleEventsPostSingleRemoval, pointAWithMultipleEventsPostMultipleRemoval) as Boolean }
        assertTrue { method.invoke(null, pointCWithMultipleEvents, pointCWithMultipleEventsAddedRemoved) as Boolean }

        // Negative Cases:
        assertFalse { method.invoke(null, pointBWithMultipleEvents, pointBWithMultipleEventsPostSingleAddition) as Boolean }
        assertFalse { method.invoke(null, pointBWithMultipleEventsPostSingleAddition, pointBWithMultipleEventsPostMultipleAddition) as Boolean }
        assertFalse { method.invoke(null, pointWithSingleEvent, pointWithSingleEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSingleEvent, pointWithSingleEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSingleEvent, pointWithoutSingleEvent) as Boolean }
    }

    @Test
    fun testIsCustomScheduleAndEventUnavailablePostUpdate() {
        val method = methodIsCustomScheduleAndEventUnavailablePostUpdate
        method.isAccessible = true

        // Positive Cases:
        assertTrue { method.invoke(null, pointWithoutSchedulableTagWithScheduleRefWithoutEvent, pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent) as Boolean }

        // Negative Cases:
        assertFalse { method.invoke(null, pointWithSchedulableTagScheduleRefAndEvent, pointWithSchedulableTagAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagWithScheduleRefAndEvent, pointWithSchedulableTagAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithSchedulableTagScheduleRefAndEvent, pointWithoutSchedulableTagScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagScheduleRefAndEvent, pointWithoutSchedulableTagWithoutScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagWithScheduleRefAndEvent, pointWithoutSchedulableTagWithoutScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithScheduleRefAndEvent, pointWithoutScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithSchedulableTagAndEvent, pointWithoutSchedulableTagAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithSchedulableTagAndScheduleRef, pointWithoutSchedulableTagScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent,
            pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithSchedulableTagAndScheduleRef,
            pointWithSchedulableTagScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithScheduleRefAndEvent,
            pointWithSchedulableTagScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagScheduleRefAndEvent,
            pointWithSchedulableTagScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagWithScheduleRefWithoutEvent
            , pointWithSchedulableTagAndScheduleRef) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent
            , pointWithSchedulableTagAndScheduleRef) as Boolean }
    }

    @Test
    fun testisExternalPointUpdatedToStopCustomControl() {
        val method = isExternalPointUpdatedToStopCustomControl
        method.isAccessible = true

        // Positive Cases:
        assertTrue { method.invoke(null, pointWithSchedulableTagAndScheduleRef,
            pointWithoutSchedulableTagScheduleRefAndEvent) as Boolean }
        assertTrue { method.invoke(null, pointWithSchedulableTagAndEvent,
            pointWithoutSchedulableTagAndEvent) as Boolean }
        assertTrue { method.invoke(null, pointWithScheduleRefAndEvent,
            pointWithoutScheduleRefAndEvent) as Boolean }
        assertTrue { method.invoke(null, pointWithSchedulableTagScheduleRefAndEvent,
            pointWithoutSchedulableTagScheduleRefAndEvent) as Boolean }
        assertTrue { method.invoke(null, pointWithoutSchedulableTagScheduleRefAndEvent,
            pointWithoutSchedulableTagWithoutScheduleRefAndEvent) as Boolean }
        assertTrue { method.invoke(null, pointWithoutSchedulableTagWithScheduleRefAndEvent,
            pointWithoutSchedulableTagWithoutScheduleRefAndEvent) as Boolean }
        assertTrue { method.invoke(null, pointWithoutSchedulableTagWithScheduleRefWithoutEvent,
            pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent) as Boolean }
        assertTrue { method.invoke(null, pointWithSchedulableTagScheduleRefAndEvent,
            pointWithSchedulableTagAndEvent) as Boolean }
        assertTrue { method.invoke(null, pointWithoutSchedulableTagWithScheduleRefAndEvent
            , pointWithSchedulableTagAndEvent) as Boolean }

        // Negative Cases:
        assertFalse { method.invoke(null, pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent,
            pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithSchedulableTagAndScheduleRef,
            pointWithSchedulableTagScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithScheduleRefAndEvent,
            pointWithSchedulableTagScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagScheduleRefAndEvent,
            pointWithSchedulableTagScheduleRefAndEvent) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagWithScheduleRefWithoutEvent
            , pointWithSchedulableTagAndScheduleRef) as Boolean }
        assertFalse { method.invoke(null, pointWithoutSchedulableTagWithoutScheduleRefWithoutEvent
            , pointWithSchedulableTagAndScheduleRef) as Boolean }
    }
}