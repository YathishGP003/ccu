package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 26-05-2025.
 */

// It is just to mock the point
class MockPoint(private var value: Double) : Point("fake Name","equipRef") {
    override fun readHisVal(): Double = value
    override fun readPriorityVal(): Double = value
}
