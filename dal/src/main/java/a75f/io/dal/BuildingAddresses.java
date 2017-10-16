package a75f.io.dal;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.kinvey.java.model.KinveyMetaData;

/**
 * Created by Yinten on 10/14/2017.
 */

public class BuildingAddresses extends GenericJson
{
    
    @Key("_id")
    private String                           id;
    @Key
    private String                           user;
    @Key
    private String                           installerEmail;
    @Key
    private String                           buildingName;
    @Key
    private String                           address;
    @Key
    private String                           city;
    @Key
    private String                           zipcode;
    @Key
    private String                           state;
    @Key
    private String                           country;
    @Key
    private String                           timeZone;
    @Key
    private Integer                          count;
    @Key
    private String                           lat;
    @Key
    private String                           lng;
    @Key("_kmd")
    private KinveyMetaData                   meta;
    @Key("_acl")
    private KinveyMetaData.AccessControlList acl;
    
    
    
}
