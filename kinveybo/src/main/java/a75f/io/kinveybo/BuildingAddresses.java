package a75f.io.kinveybo;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.kinvey.java.model.KinveyMetaData;

import java.io.Serializable;

/**
 * Created by Yinten on 10/14/2017.
 */

public class BuildingAddresses extends GenericJson implements Serializable
{
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getInstallerEmail() {
        return installerEmail;
    }

    public void setInstallerEmail(String installerEmail) {
        this.installerEmail = installerEmail;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

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
