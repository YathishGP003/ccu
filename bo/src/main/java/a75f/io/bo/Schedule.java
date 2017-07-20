package a75f.io.bo;


import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.kinvey.java.model.KinveyMetaData;

/**
 * Created by rmatt on 7/19/2017.
 */

public class Schedule extends GenericJson {

    @Key("_id")
    private String id;

    @Key("dayOfWeek")
    private int dayOfWeek;

    @Key("time0")
    private int time0;

    @Key("action0")
    private int action0;

    @Key("time1")
    private int time1;

    @Key("action1")
    private int action1;


    @Key("_kmd")
    private KinveyMetaData meta;
    @Key("_acl")
    private KinveyMetaData.AccessControlList acl;

    public Schedule() {
    }


}
