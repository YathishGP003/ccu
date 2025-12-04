package a75f.io.renatus.bacnet

import a75f.io.logic.bo.building.system.client.MultiReadResultItem
import a75f.io.logic.bo.building.system.client.ObjectIdentifierBacNetResp

data class BacnetData(
    val deviceProps: List<DeviceProp>,
    val points: List<Point>
)

data class DeviceProp(
    val object_identifier: ObjectIdentifierBacNetResp,
    val results: List<MultiReadResultItem>
)

data class Point(
    val object_identifier: ObjectIdentifierBacNetResp,
    val results: List<MultiReadResultItem>
)