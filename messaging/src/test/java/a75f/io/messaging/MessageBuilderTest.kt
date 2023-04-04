package a75f.io.messaging

import a75f.io.data.message.Message
import a75f.io.messaging.exceptions.InvalidMessageFormatException
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.json.JSONArray
import org.json.JSONObject

import org.junit.Test
import kotlin.test.assertFailsWith

class MessageBuilderTest {

    private val updatePointMsg = "{\"channel\":\"ba328874-6d8d-47f7-a4a5-386f74124233\",\"timetoken\":1679453749690,\"messageId\":\"1679453749690-0\",\"message\":{\"val\":\"76\",\"level\":10,\"id\":\"846275dc-e6ae-4401-b4a1-313b30dee0e3\",\"command\":\"updatePoint\",\"who\":\"web_Samjith \"},\"traceparent\":\"01-c169a11dd468929cd0e93a99524963cd-b69015e5c292364f-01\"}"
    private val updateEntityMsg = "{\"channel\":\"c57f5bec-d3a6-446b-b061-144ffaa3cc95\",\"timetoken\":1679087721460,\"messageId\":\"1679087721460-0\",\"message\":{\"ids\":[\"4c57933c-b05b-4a28-b1cf-e5d8da17ca66\"],\"command\":\"updateEntity\"}}"
    private val syncMessage =  "{\"channel\":\"c57f5bec-d3a6-446b-b061-144ffaa3cc95\",\"timetoken\":1679087721471,\"messageId\":\"1679087721471-0\",\"message\":{\"siteId\":\"c57f5bec-d3a6-446b-b061-144ffaa3cc95\",\"command\":\"sync\"}}"
    private val remoteCmdSaveLogs = "{\"channel\":\"c57f5bec-d3a6-446b-b061-144ffaa3cc95\",\"timetoken\":1679087432502,\"messageId\":\"1679087432502-0\",\"message\":{\"remoteCmdType\":\"save_ccu_logs\",\"command\":\"remoteCommand\",\"level\":\"system\",\"who\":\"web_internal_Samjith \",\"id\":\"df9f4353-f773-4421-801f-71577b30e033\",\"siteRef\":\"@c57f5bec-d3a6-446b-b061-144ffaa3cc95\"}}"
    private val remoteEntity = "{\"channel\":\"fca97cb0-05d5-4fa0-b645-83034658edc7\",\"timetoken\":1679353233848,\"messageId\":\"1679353233848-0\",\"message\":{\"ids\":[{\"val\":\"5e8fc9a2-eca6-4c20-8f61-1180d4ccc2d5\"},{\"val\":\"e40320cd-fa44-4e70-b35b-0a92b6f256ee\"}],\"command\":\"removeEntity\"}}"
    private val updateSchedule = "{\"channel\":\"ba328874-6d8d-47f7-a4a5-386f74124233\",\"timetoken\":1679501490358,\"messageId\":\"1679501490358-0\",\"message\":{\"id\":\"d3fdb2c5-d982-4468-9389-cb85b21bbcfd\",\"command\":\"updateSchedule\"}}"
    private val customAlertDef = "{\"channel\":\"ba328874-6d8d-47f7-a4a5-386f74124233\",\"timetoken\":1679511401326,\"messageId\":\"1679511401326-0\",\"message\":{\"command\":\"newCustomAlertDefinition\",\"definitionId\":\"641b4f68255a887a48b59c62\"}}"
    private val customAlertDel = "{\"channel\":\"ba328874-6d8d-47f7-a4a5-386f74124233\",\"timetoken\":1679512958991,\"messageId\":\"1679512958991-0\",\"message\":{\"command\":\"removeCustomAlertDefinition\",\"definitionId\":\"641b4f68255a887a48b59c62\"}}"

    private val nullCommandMsg = "{\"channel\":\"c57f5bec-d3a6-446b-b061-144ffaa3cc95\",\"timetoken\":1679087721460,\"messageId\":\"1679087721460-0\",\"message\":{\"ids\":[\"4c57933c-b05b-4a28-b1cf-e5d8da17ca66\"]}}"

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForValidUpdatePointMessage() {
        val msgJson: JsonObject = JsonParser.parseString(updatePointMsg).asJsonObject
        val message = jsonToMessage(msgJson)
        assertThat(message).isEqualTo(
            Message(messageId="1679453749690-0",
                command="updatePoint",
                id="846275dc-e6ae-4401-b4a1-313b30dee0e3",
                ids=null,
                value="76",
                who="web_Samjith ",
                level=10,
                remoteCmdType=null,
                remoteCmdLevel=null,
                timeToken=1679453749690,
                handlingStatus=false,
                retryCount=0,
                error="")
        )

        val messageJson = messageToJson(message);
        println(messageJson)
    }


    @Test
    fun messageFromJson_checkAllFieldsAreParsedForValidUpdateEntityMessage() {
        val msgJson: JsonObject = JsonParser.parseString(updateEntityMsg).asJsonObject
        val message = jsonToMessage(msgJson)
        assertThat(message).isEqualTo(
            Message(messageId="1679087721460-0",
                command="updateEntity",
                id=null,
                ids= listOf("4c57933c-b05b-4a28-b1cf-e5d8da17ca66"),
                value=null,
                who=null,
                level=null,
                remoteCmdType=null,
                remoteCmdLevel=null,
                timeToken=1679087721460,
                handlingStatus=false,
                retryCount=0,
                error="")
        )
    }

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForValidSyncMessage() {
        val msgJson: JsonObject = JsonParser.parseString(syncMessage).asJsonObject
        val message = jsonToMessage(msgJson)
        assertThat(message).isEqualTo(
            Message(messageId="1679087721471-0",
                command="sync",
                id="c57f5bec-d3a6-446b-b061-144ffaa3cc95",
                ids=null,
                value=null,
                who=null,
                level=null,
                remoteCmdType=null,
                remoteCmdLevel=null,
                timeToken=1679087721471,
                handlingStatus=false,
                retryCount=0,
                error="")
        )
    }

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForValidRemoteCmdMessage() {
        val msgJson: JsonObject = JsonParser.parseString(remoteCmdSaveLogs).asJsonObject
        val message = jsonToMessage(msgJson)
        assertThat(message).isEqualTo(
            Message(messageId="1679087432502-0",
                command="remoteCommand",
                id="df9f4353-f773-4421-801f-71577b30e033",
                ids=null,
                value=null,
                who="web_internal_Samjith ",
                level=null,
                remoteCmdType="save_ccu_logs",
                remoteCmdLevel="system",
                timeToken=1679087432502,
                handlingStatus=false,
                retryCount=0,
                error="")
        )
    }

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForValidRemoveEntityMessage() {
        val msgJson: JsonObject = JsonParser.parseString(remoteEntity).asJsonObject
        val message = jsonToMessage(msgJson)
        print(message)

        val entityItem1 = JsonObject()
        entityItem1.addProperty("val","5e8fc9a2-eca6-4c20-8f61-1180d4ccc2d5")
        val entityItem2 = JsonObject()
        entityItem2.addProperty("val","e40320cd-fa44-4e70-b35b-0a92b6f256ee")

        assertThat(message).isEqualTo(
            Message(messageId="1679353233848-0",
                command="removeEntity",
                id=null,
                ids= listOf(entityItem1.toString(), entityItem2.toString()),
                value=null,
                who=null,
                level=null,
                remoteCmdType=null,
                remoteCmdLevel=null,
                version=null,
                timeToken=1679353233848,
                handlingStatus=false,
                retryCount=0,
                error="")

        )
    }

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForValidUpdateScheduleMessage() {
        val msgJson: JsonObject = JsonParser.parseString(updateSchedule).asJsonObject
        val message = jsonToMessage(msgJson)
        print(message)
        assertThat(message).isEqualTo(
            Message(messageId="1679501490358-0",
                command="updateSchedule",
                id="d3fdb2c5-d982-4468-9389-cb85b21bbcfd",
                ids=null,
                value=null,
                who=null,
                level=null,
                remoteCmdType=null,
                remoteCmdLevel=null,
                version=null,
                timeToken=1679501490358,
                handlingStatus=false,
                retryCount=0,
                error="")

        )
    }

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForValidCustomAlertDefMessage() {
        val msgJson: JsonObject = JsonParser.parseString(customAlertDef).asJsonObject
        val message = jsonToMessage(msgJson)
        assertThat(message).isEqualTo(
            Message(messageId="1679511401326-0",
                command="newCustomAlertDefinition",
                id="641b4f68255a887a48b59c62",
                ids=null,
                value=null,
                who=null,
                level=null,
                remoteCmdType=null,
                remoteCmdLevel=null,
                timeToken=1679511401326,
                handlingStatus=false,
                retryCount=0,
                error="")
        )
    }

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForValidCustomAlertDelMessage() {
        val msgJson: JsonObject = JsonParser.parseString(customAlertDel).asJsonObject
        val message = jsonToMessage(msgJson)
        assertThat(message).isEqualTo(
            Message(messageId="1679512958991-0",
                command="removeCustomAlertDefinition",
                id="641b4f68255a887a48b59c62",
                ids=null,
                value=null,
                who=null,
                level=null,
                remoteCmdType=null,
                remoteCmdLevel=null,
                timeToken=1679512958991,
                handlingStatus=false,
                retryCount=0,
                error="")
        )
    }

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForInvalidMessageId() {
        val msgJson: JsonObject = JsonParser.parseString(remoteCmdSaveLogs).asJsonObject
        msgJson.addProperty("messageId","")
        assertFailsWith<InvalidMessageFormatException> {
            jsonToMessage(msgJson)
        }

    }

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForInvalidCommand() {
        val msgJson: JsonObject = JsonParser.parseString(nullCommandMsg).asJsonObject
        assertFailsWith<InvalidMessageFormatException> {
            jsonToMessage(msgJson)
        }

    }
}