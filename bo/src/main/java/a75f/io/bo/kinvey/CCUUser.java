package a75f.io.bo.kinvey;

import com.google.api.client.util.Key;
import com.kinvey.android.model.User;

/**
 * Created by Yinten on 9/4/2017.
 */

public class CCUUser extends User
{
    

    
    @Key
    private String first_name;
    
    @Key
    private String last_name;
    
    @Key
    private String email;
    
    @Key
    private String user_type;
    
    @Key
    private String password;
    
    @Key
    private String ccuName;
    
    @Key
    private String buildingName;
    
    @Key
    private String address;
    
    @Key
    private String city;
    
    @Key
    private String zipCode;
    
    @Key
    private String state;
    @Key
    private String country;
    @Key
    private String cloudServer;
    @Key
    private String installedVersion;
    @Key
    private String installedTier;
    @Key
    private double lat;
    @Key
    private double lng;
    @Key
    private String macAddr;
    @Key
    private String phone;
    
    
    public String getCcuName()
    {
        return ccuName;
    }
    
    
    public void setCcuName(String ccuName)
    {
        this.ccuName = ccuName;
    }
    
    
    public String getBuildingName()
    {
        return buildingName;
    }
    
    
    public void setBuildingName(String buildingName)
    {
        this.buildingName = buildingName;
    }
    
    
    public String getAddress()
    {
        return address;
    }
    
    
    public void setAddress(String address)
    {
        this.address = address;
    }
    
    
    public String getCity()
    {
        return city;
    }
    
    
    public void setCity(String city)
    {
        this.city = city;
    }
    
    
    public String getZipCode()
    {
        return zipCode;
    }
    
    
    public void setZipCode(String zipCode)
    {
        this.zipCode = zipCode;
    }
    
    
    public String getState()
    {
        return state;
    }
    
    
    public void setState(String state)
    {
        this.state = state;
    }
    
    
    public String getCountry()
    {
        return country;
    }
    
    
    public void setCountry(String country)
    {
        this.country = country;
    }
    
    
    public String getCloudServer()
    {
        return cloudServer;
    }
    
    
    public void setCloudServer(String cloudServer)
    {
        this.cloudServer = cloudServer;
    }
    
    
    public String getInstalledVersion()
    {
        return installedVersion;
    }
    
    
    public void setInstalledVersion(String installedVersion)
    {
        this.installedVersion = installedVersion;
    }
    
    
    public String getInstalledTier()
    {
        return installedTier;
    }
    
    
    public void setInstalledTier(String installedTier)
    {
        this.installedTier = installedTier;
    }
    
    
    public double getLat()
    {
        return lat;
    }
    
    
    public void setLat(double lat)
    {
        this.lat = lat;
    }
    
    
    public double getLng()
    {
        return lng;
    }
    
    
    public void setLng(double lng)
    {
        this.lng = lng;
    }
    
    
    public String getMacAddr()
    {
        return macAddr;
    }
    
    
    public void setMacAddr(String macAddr)
    {
        this.macAddr = macAddr;
    }
    
    
    public String getFirst_name()
    {
        return first_name;
    }
    
    
    public void setFirst_name(String first_name)
    {
        this.first_name = first_name;
    }
    
    
    public String getLast_name()
    {
        return last_name;
    }
    
    
    public void setLast_name(String last_name)
    {
        this.last_name = last_name;
    }
    
    
    public String getEmail()
    {
        return email;
    }
    
    
    public void setEmail(String email)
    {
        this.email = email;
    }
    
    
    public String getUser_type()
    {
        return user_type;
    }
    
    
    public void setUser_type(String user_type)
    {
        this.user_type = user_type;
    }
    
    
    public String getPhone()
    {
        return phone;
    }
    
    
    public void setPhone(String phone)
    {
        this.phone = phone;
    }
    
    
    public String getPassword()
    {
        return password;
    }
    
    
    public void setPassword(String password)
    {
        this.password = password;
    }
}
