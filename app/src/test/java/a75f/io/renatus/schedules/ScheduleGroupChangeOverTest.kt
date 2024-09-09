package a75f.io.renatus.schedules

import a75f.io.logic.schedule.ScheduleGroup
import android.app.Activity
import androidx.fragment.app.FragmentManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
class ScheduleGroupChangeOverTest {

    private lateinit var scheduleGroupChangeOver: ScheduleGroupChangeOver

    @Before
    fun setUp() {
        val activity = mock(Activity::class.java)
        val childFragmentManager = mock(FragmentManager::class.java)
        scheduleGroupChangeOver = ScheduleGroupChangeOver(activity, 1, 2, 3, 4, null)
    }

    @Test
    fun getIdByScheduleGroup() {
        assertEquals(1, scheduleGroupChangeOver.getIdByScheduleGroup(ScheduleGroup.EVERYDAY.ordinal))
        assertEquals(2, scheduleGroupChangeOver.getIdByScheduleGroup(ScheduleGroup.WEEKDAY_WEEKEND.ordinal))
        assertEquals(3, scheduleGroupChangeOver.getIdByScheduleGroup(ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal))
        assertEquals(4, scheduleGroupChangeOver.getIdByScheduleGroup(ScheduleGroup.SEVEN_DAY.ordinal))
    }

    @Test
    fun getScheduleGroupById() {
        assertEquals(ScheduleGroup.EVERYDAY, scheduleGroupChangeOver.getScheduleGroupById(1))
        assertEquals(ScheduleGroup.WEEKDAY_WEEKEND, scheduleGroupChangeOver.getScheduleGroupById(2))
        assertEquals(ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY, scheduleGroupChangeOver.getScheduleGroupById(3))
        assertEquals(ScheduleGroup.SEVEN_DAY, scheduleGroupChangeOver.getScheduleGroupById(4))
    }
}