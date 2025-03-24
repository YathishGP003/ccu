package a75f.io.messaging.handler;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.client.HClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.interfaces.BuildingOccupancyListener;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.messaging.MessageHandler;

public class AddEntityHandler implements MessageHandler {
    public static final String CMD = "addEntity";
    public static void addEntity(JsonObject msgObject, long timeToken){
        List<HRef> entityIds = new ArrayList<>();
        msgObject.get("ids").getAsJsonArray().forEach( msgJson -> {
            CcuLog.i(L.TAG_CCU_MESSAGING, " AddEntityHandler "+msgJson.toString());
            String uid = msgJson.toString().replaceAll("\"", "");
            entityIds.add(HRef.copy(StringUtils.prependIfMissing(uid, "@")));
        });

        if (!entityIds.isEmpty()){
            remoteFetchEntities(entityIds);
        }

    }

    private static void remoteFetchEntities(List<HRef> entityIds) {

        CcuLog.i(L.TAG_CCU_MESSAGING, "AddEntityHandler remoteFetchEntities >> ");

        HRef[] ids = entityIds.toArray(new HRef[0]);
        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HGrid entitiesGrid = hClient.readByIds(ids, true);
        if (entitiesGrid == null) {
            CcuLog.e(L.TAG_CCU_MESSAGING, "AddEntityHandler remoteFetchEntities: null ");
            return;
        }

        Iterator it = entitiesGrid.iterator();
        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            String entityId = row.get("id").toString();

            if (!row.has("createdByApplication")
                    || !row.get("createdByApplication").toString().equalsIgnoreCase("SITE_MANAGER")) {
                CcuLog.e(L.TAG_CCU_MESSAGING, "Add entity handler, createdByApplication not found or it is not site_manager returning");
                return;
            }

            if (!row.has("equipRef")
                    || (CCUHsApi.getInstance().readMapById(row.get("equipRef").toString()) == null)) {
                CcuLog.e(L.TAG_CCU_MESSAGING, "Add entity handler, equipRef not found or not ccu equipRef returning");
                return;
            }

            if (isModbusInputOrDiscreteInput(row)) {
                CcuLog.e(L.TAG_CCU_MESSAGING, "a Ignore Message !"+ entityId);
                return;
            }

            if (row.has("equip") || row.has("device")) {
                CcuLog.e(L.TAG_CCU_MESSAGING, "Add entity handler, this is equip or device returning");
               return;
            } else if (row.has("physical") && row.get("physical") != null) {
                CcuLog.d(L.TAG_CCU_MESSAGING, "Add entity handler, add physical point->"+entityId);
                RawPoint point = new RawPoint.Builder().setHDict((HDict) row).build();
                CCUHsApi.getInstance().tagsDb.addPointWithId(point, entityId);
            } else {
                CcuLog.d(L.TAG_CCU_MESSAGING, "Add entity handler, add normal point->"+entityId);
                Point point = new Point.Builder().setHDict((HDict) row).build();
                CCUHsApi.getInstance().tagsDb.addPointWithId(point, entityId);
            }

            CcuLog.i(L.TAG_CCU_MESSAGING,"<< AddEntityHandler Entities Imported");
        }
    }

    private static boolean isModbusInputOrDiscreteInput(HRow row) {

        if((row.has("registerType") && (row.get("registerType").toString().equals("inputRegister")
                || row.get("registerType").toString().equals("discreteInput"))) &&
                (row.has("writable") )){
            return true;
        } else if (row.has("logical")) {
            HashMap<Object, Object> physicalEntity = CCUHsApi.getInstance().readEntity
                    ("physical and pointRef == \"" + row.get("id").toString() + "\"");
            if (physicalEntity != null && physicalEntity.containsKey("registerType") &&
                    (physicalEntity.get("registerType").toString().equals("inputRegister")
                    || physicalEntity.get("registerType").toString().equals("discreteInput")) &&
                    (row.has("writable"))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public List<String> getCommand() {
        return Collections.singletonList(CMD);
    }

    @Override
    public void handleMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {
        long timeToken = jsonObject.get("timeToken").getAsLong();
        addEntity(jsonObject, timeToken);
    }

    @Override
    public boolean ignoreMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {

        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        AtomicBoolean validMessage = new AtomicBoolean(false);
        jsonObject.get("ids").getAsJsonArray().forEach( msgJson -> {
            CcuLog.i(L.TAG_CCU_MESSAGING, " AddEntityHandler "+msgJson.toString());
            String uid = msgJson.toString().replaceAll("\"", "");

            if(ccuHsApi.isEntityExisting(uid)){
                validMessage.set(true);
            }


        });
        return validMessage.get();
    }
}
