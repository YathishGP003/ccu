package a75f.io.bo.kinvey;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by Yinten on 3/26/2018.
 */

public class Domain extends GenericJson {

    @Key("_id")
    public String  id;

    @Key("domainId")
    public String domainId;

    @Key
    public ArrayList<String> buildingIds;

    @Key
    public HashMap<String, Object> tuners;

    @Key
    public ArrayList<CCUSchedules> schedules;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSchedules(ArrayList<CCUSchedules> schedules) {
        this.schedules = schedules;
    }

    public ArrayList<CCUSchedules> getSchedules() {
        return schedules;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public ArrayList<String> getBuildingIds() {
        return buildingIds;
    }

    public void setBuildingIds(ArrayList<String> buildingIds) {
        this.buildingIds = buildingIds;
    }

    public HashMap<String, Object> getTuners() {
        return tuners;
    }

    public void setTuners(HashMap<String, Object> tuners) {
        this.tuners = tuners;
    }

}
