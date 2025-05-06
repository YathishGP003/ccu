package a75f.io.logic.preconfig

import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.api.Domain
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PreconfigTest {

    private val testPreconfigDataValid = """
                {
  "sitePreConfigId": "SPC-456123789",
  "siteName": "Evergreen Systems HQ",
  "ccuName": "HQ-CCU-North",
  "orgName": "Evergreen Tech LLC",
  "timeZone": "America/Los_Angeles",
  "siteAddress": {
    "geoCity": "Portland",
    "geoState": "Oregon",
    "geoCiuntry": "USA",
    "geoAddr": "742 Evergreen Terrace",
    "geoPostalCode": "97201"
  },
  "fmEmail": "fm.manager@evergreentech-example.com",
  "installerEmail": "installer.service@evergreentech-example.com",
  "relayMappingSet": [
    "coolingStage1",
    "fanStage1",
  ],
  "floor": "3rd Floor",
  "zones": [
    "NorthWing",
    "SouthWing",
    "LabArea"
  ],
  "pizzaType": "VEGAN_SPECIAL",
  "createdBy": {
    "userId": "a1234567-b89c-12d3-e456-426614170999",
    "firstName": "Morgan",
    "lastName": "Reed",
    "emailAddress": "morgan.reed@evergreentech-example.com"
  },
  "activationCode": "EVG-TEST-456789",
  "createdAt": "2025-04-16T15:00:00.000Z",
  "expiresAt": "2025-05-01T15:00:00.000Z",
  "activationCodeCreatedBy": {
    "userId": "a1234567-b89c-12d3-e456-426614170999",
    "firstName": "Morgan",
    "lastName": "Reed",
    "emailAddress": "morgan.reed@evergreentech-example.com"
  }
}
"""

    private val testPreconfigDataInValid = """
                {
  "sitePreConfigId": "SPC-456123789",
  "siteName": "Evergreen Systems HQ",
  "orgName": "Evergreen Tech LLC",
  "timeZone": "America/Los_Angeles",
  "siteAddress": {
    "geoCity": "Portland",
    "geoState": "Oregon",
    "geoCiuntry": "USA",
    "geoAddr": "742 Evergreen Terrace",
    "geoPostalCode": "97201"
  },
  "fmEmail": "fm.manager@evergreentech-example.com",
  "installerEmail": "installer.service@evergreentech-example.com",
  "relayMappingSet": [
    "coolingStage1",
    "fanStage1"
  ]
}
"""

    @Test
    fun `Parse json to check fields`() {
        val gson = Gson()
        val data = gson.fromJson(testPreconfigDataValid, PreconfigurationData::class.java)
        println("Parsed Preconfiguration Data: $data")

        assert(data.siteName == "Evergreen Systems HQ")
        assert(data.ccuName == "HQ-CCU-North")
        assert(data.orgName == "Evergreen Tech LLC")
        assert(data.timeZone == "America/Los_Angeles")
        assert(data.siteAddress.geoCity == "Portland")
        assert(data.floor == "3rd Floor")
    }

    @Test
    fun `throws InvalidPreconfigurationDataException when ccu name is missing`() {
        val gson = Gson()
        val data = gson.fromJson(testPreconfigDataInValid, PreconfigurationData::class.java)

        val preconfigurationHandler = PreconfigurationHandler()

        val exception = assertThrows(InvalidPreconfigurationDataException::class.java) {
            preconfigurationHandler.validatePreconfigurationData(data)
        }

        assertEquals("CCU name cannot be empty", exception.message)
    }
    @Test
    fun testHandlePreconfigData() {
        println("Handling preconfiguration data: $testPreconfigDataValid")
        val gson = Gson()
        val data = gson.fromJson(testPreconfigDataValid, PreconfigurationData::class.java)

        val ccuHsApi = MockCcuHsApi()
        Domain.hayStack = ccuHsApi
        val preconfigHandler = PreconfigurationHandler()
        preconfigHandler.handlePreconfiguration(data, ccuHsApi)

        assert(ccuHsApi.readEntity("site").isNotEmpty())
        assert(ccuHsApi.readEntity("ccu").isNotEmpty())
        assert(ccuHsApi.readEntity("floor").isNotEmpty())
        assert(ccuHsApi.readEntity("zone").size > 1)
        assert(ccuHsApi.readEntity("equip").isEmpty())
    }

}