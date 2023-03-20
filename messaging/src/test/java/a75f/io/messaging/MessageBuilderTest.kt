package a75f.io.messaging

import a75f.io.data.message.Message
import a75f.io.messaging.exceptions.InvalidMessageFormatException
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import org.junit.Test
import kotlin.test.assertFailsWith

class MessageBuilderTest {

    private val updateEntityMsg = "{\"channel\":\"c57f5bec-d3a6-446b-b061-144ffaa3cc95\",\"timetoken\":1679087721460,\"messageId\":\"1679087721460-0\",\"message\":{\"ids\":[\"4c57933c-b05b-4a28-b1cf-e5d8da17ca66\"],\"command\":\"updateEntity\"}}"
    private val syncMessage =  "{\"channel\":\"c57f5bec-d3a6-446b-b061-144ffaa3cc95\",\"timetoken\":1679087721471,\"messageId\":\"1679087721471-0\",\"message\":{\"siteId\":\"c57f5bec-d3a6-446b-b061-144ffaa3cc95\",\"command\":\"sync\"}}"
    private val remoteCmdSaveLogs = "{\"channel\":\"c57f5bec-d3a6-446b-b061-144ffaa3cc95\",\"timetoken\":1679087432502,\"messageId\":\"1679087432502-0\",\"message\":{\"remoteCmdType\":\"save_ccu_logs\",\"command\":\"remoteCommand\",\"level\":\"system\",\"who\":\"web_internal_Samjith \",\"id\":\"df9f4353-f773-4421-801f-71577b30e033\",\"siteRef\":\"@c57f5bec-d3a6-446b-b061-144ffaa3cc95\"}}"
    private val remoteEntity = "{\"channel\":\"fca97cb0-05d5-4fa0-b645-83034658edc7\",\"timetoken\":1679353233848,\"messageId\":\"1679353233848-0\",\"message\":{\"ids\":[{\"val\":\"5e8fc9a2-eca6-4c20-8f61-1180d4ccc2d5\"},{\"val\":\"e40320cd-fa44-4e70-b35b-0a92b6f256ee\"}],\"command\":\"removeEntity\"}}"
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
            Message(messageId="1679087721460-0",
                command="updateEntity",
                id="c57f5bec-d3a6-446b-b061-144ffaa3cc95",
                ids=null,
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
        assertThat(message).isEqualTo(
            Message(messageId="1679353233848-0",
                command="removeEntity",
                id=null,
                ids= listOf("{val=5e8fc9a2-eca6-4c20-8f61-1180d4ccc2d5}", "{val=e40320cd-fa44-4e70-b35b-0a92b6f256ee}"),
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
    fun messageFromJson_checkAllFieldsAreParsedForInvalidMessageId() {
        val msgJson: JsonObject = JsonParser.parseString(remoteCmdSaveLogs).asJsonObject
        msgJson.addProperty("messageId","")
        assertFailsWith<InvalidMessageFormatException> {
            jsonToMessage(msgJson)
        }

    }

    @Test
    fun messageFromJson_checkAllFieldsAreParsedForInvalidCommand() {
        val msgJson: JsonObject = JsonParser.parseString(updateEntityMsg).asJsonObject
        msgJson.asJsonObject.get("message").asJsonObject.addProperty("command","")
        assertFailsWith<InvalidMessageFormatException> {
            jsonToMessage(msgJson)
        }

    }
}