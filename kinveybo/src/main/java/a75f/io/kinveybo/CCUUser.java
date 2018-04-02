    package a75f.io.kinveybo;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.kinvey.android.model.User;
import com.kinvey.java.model.KinveyMetaData;

import java.util.Collection;

/**
 * Created by Yinten on 9/4/2017.
 */

public class CCUUser extends User
{
    @Key("_id")
    private String id;

    @Key
    private String firstname;

    @Key
    private String lastname;

    @Key
    private String domain;

    @Key
    private String email;

    @Key
    private String password;

    @Key
    private CAddress addressInformation;

    @Key("_geoloc")
    private float[] _geoloc; //Deprecates lat, lang

    @Key
    private Collection<AccessLog> accessLogs;

    @Key("_kmd")
    private KinveyMetaData meta;

    @Key("_acl")
    private KinveyMetaData.AccessControlList acl;

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



    public CAddress getAddressInformation() {
        return addressInformation;
    }

    public void setAddressInformation(CAddress addressInformation) {
        this.addressInformation = addressInformation;
    }

    public float[] get_geoloc() {
        return _geoloc;
    }

    public void set_geoloc(float[] _geoloc) {
        this._geoloc = _geoloc;
    }

    public Collection<AccessLog> getAccessLogs() {
        return accessLogs;
    }

    public void setAccessLogs(Collection<AccessLog> accessLogs) {
        this.accessLogs = accessLogs;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }




}
