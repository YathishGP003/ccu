package a75f.io.dal;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.kinvey.java.model.KinveyMetaData;

/**
 * Created by Yinten on 9/26/2017.
 */

public class CCUPreconfiguration extends GenericJson
{
    
    @Key("_id")
    private String                           id;
    @Key
    private Object                           account_details;
    @Key
    private String                           intallation_type;
    @Key
    private Object                           product_options;
    @Key
    private Object                           sys_equip_config;
    @Key
    private Object                           floor_plan;
    @Key
    private Object                           floor_details;
    @Key
    private Object                           tuners;
    @Key
    private Object                           installer_options;
    @Key
    private Object                           system_devices;
    @Key
    private Object                           system_param;
    @Key
    private String                           configuration_type;
    @Key
    private String                           template_name;
    @Key
    private String                           template_desc;
    @Key
    private String                           otp;
    @Key
    private String                           config_type;
    @Key("_kmd")
    private KinveyMetaData                   meta;
    @Key("_acl")
    private KinveyMetaData.AccessControlList acl;
    
    public CCUPreconfiguration()
    {
    }  //GenericJson classes must have a public empty constructor
}
