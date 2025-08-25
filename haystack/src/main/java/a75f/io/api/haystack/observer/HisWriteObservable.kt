package a75f.io.api.haystack.observer

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object HisWriteObservable {

    private val listenersMap = ConcurrentHashMap<String, CopyOnWriteArrayList<PointSubscriber>>()

    fun subscribe(pointId: String, subscriber: PointSubscriber) {
        val list = listenersMap.getOrPut(pointId)  { CopyOnWriteArrayList() }
        if (!list.contains(subscriber)) {
            list.add(subscriber)
        }
    }

    fun unsubscribe(pointId: String, subscriber: PointSubscriber) {
        listenersMap[pointId]?.remove(subscriber)
    }

    fun notifyChange(pointId: String, value: Double) {
        listenersMap[pointId]?.forEach { it.onHisPointChanged(pointId, value) }
    }
}