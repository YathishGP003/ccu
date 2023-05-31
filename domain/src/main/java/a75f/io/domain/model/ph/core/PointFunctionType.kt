package a75f.io.domain.model.ph.core

enum class PointFunctionType(val marker: String) {
    COMMAND(Tags.CMD),
    SENSOR(Tags.SENSOR),
    SET_POINT(Tags.SP);
}
