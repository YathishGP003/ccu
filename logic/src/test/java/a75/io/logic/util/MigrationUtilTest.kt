package a75.io.logic.util

import a75f.io.alerts.AlertManager
import a75f.io.api.haystack.Alert
import a75f.io.logic.util.MigrationUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MigrationUtilTest {

    @Test
    fun `only duplicated alert is deleted`() {
        // given
        val alert1 = Alert()
        alert1.setmTitle("DEVICE DEAD")
        alert1.setEquipId("@123")
        alert1.setStartTime(1637161581)

        val alert2 = Alert()
        alert2.setmTitle("DEVICE DEAD")
        alert2.setEquipId("@123")
        alert2.setStartTime(alert1.getStartTime() + 100)

        // when
        //  - two alerts of the same type are unsynced
        //  - and duplicated alerts are removed
        val alertManager = mockk<AlertManager>(relaxed = true) {
            every { unsyncedAlerts } returns listOf(alert1, alert2)
        }
        MigrationUtil.removeDuplicateAlerts(alertManager)

        // then the older alert is NOT deleted, but the newer alert is deleted
        verify (inverse = true) {
            alertManager.deleteAlert(alert1)
        }
        verify (exactly = 1) {
            alertManager.deleteAlert(alert2)
        }
    }

    @Test
    fun `only duplicated alert is deleted - null equipIds`() {
        // given
        val alert1 = Alert()
        alert1.setmTitle("DEVICE DEAD")
        alert1.setEquipId(null)
        alert1.setStartTime(1637161581)

        val alert2 = Alert()
        alert2.setmTitle("DEVICE DEAD")
        alert2.setEquipId(null)
        alert2.setStartTime(alert1.getStartTime() + 100)

        // when
        //  - two alerts of the same type are unsynced
        //  - and duplicated alerts are removed
        val alertManager = mockk<AlertManager>(relaxed = true) {
            every { unsyncedAlerts } returns listOf(alert1, alert2)
        }
        MigrationUtil.removeDuplicateAlerts(alertManager)

        // then the older alert is NOT deleted, but the newer alert is deleted
        verify (inverse = true) {
            alertManager.deleteAlert(alert1)
        }
        verify (exactly = 1) {
            alertManager.deleteAlert(alert2)
        }
    }

    @Test
    fun `duplicated alerts do not exist - different titles`() {
        // given
        val alert1 = Alert()
        alert1.setmTitle("DEVICE DEAD")
        alert1.setEquipId("@123")
        alert1.setStartTime(1637161581)

        val alert2 = Alert()
        alert2.setmTitle("DEVICE REBOOT")
        alert2.setEquipId("@123")
        alert2.setStartTime(alert1.getStartTime() + 100)

        // when
        //  - two alerts with different titles are unsynced
        //  - and duplicated alerts are removed
        val alertManager = mockk<AlertManager>(relaxed = true) {
            every { unsyncedAlerts } returns listOf(alert1, alert2)
        }
        MigrationUtil.removeDuplicateAlerts(alertManager)

        // then neither alert is deleted
        verify (inverse = true) {
            alertManager.deleteAlert(any())
        }
    }

    @Test
    fun `duplicated alerts do not exist - same titles, different equipIds`() {
        // given
        val alert1 = Alert()
        alert1.setmTitle("DEVICE DEAD")
        alert1.setEquipId("@123")
        alert1.setStartTime(1637161581)

        val alert2 = Alert()
        alert2.setmTitle("DEVICE DEAD")
        alert2.setEquipId("@456")
        alert2.setStartTime(alert1.getStartTime() + 100)

        // when
        //  - two alerts with the same title, but different equip ids are unsynced
        //  - and duplicated alerts are removed
        val alertManager = mockk<AlertManager>(relaxed = true) {
            every { unsyncedAlerts } returns listOf(alert1, alert2)
        }
        MigrationUtil.removeDuplicateAlerts(alertManager)

        // then neither alert is deleted
        verify (inverse = true) {
            alertManager.deleteAlert(any())
        }
    }
}