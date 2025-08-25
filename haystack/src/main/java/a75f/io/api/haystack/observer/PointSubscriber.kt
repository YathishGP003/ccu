package a75f.io.api.haystack.observer


interface PointSubscriber {
    fun onHisPointChanged(pointId: String, value: Double)

    fun onWritablePointChanged(pointId: String, value: Any)
}