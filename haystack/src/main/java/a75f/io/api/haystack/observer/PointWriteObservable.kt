package a75f.io.api.haystack.observer

import org.projecthaystack.HStr
import org.projecthaystack.HVal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object PointWriteObservable {

    private val listenersMap = ConcurrentHashMap<String, CopyOnWriteArrayList<PointSubscriber>>()

    fun subscribe(pointId: String, subscriber: PointSubscriber) {
        val list = listenersMap.getOrPut(pointId) { CopyOnWriteArrayList() }
        if (!list.contains(subscriber)) {
            list.add(subscriber)
        }
    }

    fun unsubscribe(pointId: String, subscriber: PointSubscriber) {
        listenersMap[pointId]?.remove(subscriber)
    }

    fun notifyWritableChange(pointId: String, value: HVal) {
        listenersMap[pointId]?.forEach {
            it.onWritablePointChanged(pointId, value)
        }
    }

    fun notifyWritableChange(pointId: String) {
        listenersMap[pointId]?.forEach {
            it.onWritablePointChanged(pointId, HStr.make(""))
        }
    }

    fun clear() {
        listenersMap.clear()
    }
}