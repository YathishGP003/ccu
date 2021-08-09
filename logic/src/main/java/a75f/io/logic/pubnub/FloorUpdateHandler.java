package a75f.io.logic.pubnub;

import android.util.Log;

import com.google.gson.JsonObject;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.sync.HttpUtil;

public class FloorUpdateHandler {
    public static final String CMD = "updateEntity";
    public static void updateFloor(JsonObject msgObject){
        String uid = msgObject.get("ids").getAsJsonArray().get(0).toString().replaceAll("\"", "");
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
        HDict[] dictArr  = {b.toDict()};
        String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "read",
                HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        Log.i("Jayatheertha",CCUHsApi.getInstance().getHSUrl());
        Log.i("Jayatheertha",uid);
        Log.i("Jayatheertha",response);
        Floor floor = null;
        if (response != null) {
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            while (it.hasNext()) {
                HRow row = (HRow) it.next();
                String id = row.get("id").toString();
                String dis = row.get("dis").toString();
                String floorStr = row.get("floor").toString();
                String siteRef = row.get("siteRef").toString();
                String orientation = row.get("orientation").toString();
                String floorNum = row.get("floorNum").toString();
                /*floor = new Floor.Builder()
                        .setDisplayName(row.get("dis").toString())
                        .setSiteRef(row.get("siteRef").toString())
                        .build();*/
                //floor.setId(row.get("id").toString());
                floor.setOrientation(Double.parseDouble(row.get("orientation").toString()));
                floor.setFloorNum(Double.parseDouble(row.get("floorNum").toString()));

                Log.i("Jayatheertha", id+">>>"+dis+">>>"+floorStr+">>>"+floorNum+">>>"+orientation+">>>"+siteRef);

                //CCUHsApi.getInstance().tagsDb.removeIdMap.values().remove(id);
            }

        }
    }
}
