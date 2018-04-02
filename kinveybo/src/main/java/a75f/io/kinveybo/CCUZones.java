package a75f.io.kinveybo;
/**
 * Created by Yinten on 9/4/2017.
 */

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.kinvey.java.model.KinveyMetaData;

import java.io.Serializable;

public class CCUZones extends GenericJson implements Serializable
{ // For Serialization

	@Key("_id")
	private String                           id;
	@Key
	private String                           name;
	@Key
	private String                           ccu_id;
	@Key
	private Integer                          room_id;
	@Key
	private Integer                          floor_id;
	@Key
	private String                           floor_name;
	@Key
	private Integer                          cur_temp;
	@Key
	private Integer                          set_temp;
	@Key
	private Integer                          humidity;
	@Key
	private Integer                          damper_pos;
	@Key
	private Boolean                          occupied;
	@Key
	private Boolean                          has_paired_fsv;
	@Key
	private String                           zone_mode;
	@Key
	private Boolean                          isSetback;
	@Key
	private Boolean                          isZoneDead;
	@Key
	private String                           current_mode;
	@Key
	private String                           _zone;
	@Key
	private String                           updatetime;
	@Key("_kmd")
	private KinveyMetaData                   meta;
	@Key("_acl")
	private KinveyMetaData.AccessControlList acl;


	public String getId()
	{
		return id;
	}


	public void setId(String id)
	{
		this.id = id;
	}


	public String getName()
	{
		return name;
	}


	public void setName(String name)
	{
		this.name = name;
	}


	public String getCcu_id()
	{
		return ccu_id;
	}


	public void setCcu_id(String ccu_id)
	{
		this.ccu_id = ccu_id;
	}


	public Integer getRoom_id()
	{
		return room_id;
	}


	public void setRoom_id(Integer room_id)
	{
		this.room_id = room_id;
	}


	public Integer getFloor_id()
	{
		return floor_id;
	}


	public void setFloor_id(Integer floor_id)
	{
		this.floor_id = floor_id;
	}


	public String getFloor_name()
	{
		return floor_name;
	}


	public void setFloor_name(String floor_name)
	{
		this.floor_name = floor_name;
	}


	public Integer getCur_temp()
	{
		return cur_temp;
	}


	public void setCur_temp(Integer cur_temp)
	{
		this.cur_temp = cur_temp;
	}


	public Integer getSet_temp()
	{
		return set_temp;
	}


	public void setSet_temp(Integer set_temp)
	{
		this.set_temp = set_temp;
	}


	public Integer getHumidity()
	{
		return humidity;
	}


	public void setHumidity(Integer humidity)
	{
		this.humidity = humidity;
	}


	public Integer getDamper_pos()
	{
		return damper_pos;
	}


	public void setDamper_pos(Integer damper_pos)
	{
		this.damper_pos = damper_pos;
	}


	public Boolean getOccupied()
	{
		return occupied;
	}


	public void setOccupied(Boolean occupied)
	{
		this.occupied = occupied;
	}


	public Boolean getHas_paired_fsv()
	{
		return has_paired_fsv;
	}


	public void setHas_paired_fsv(Boolean has_paired_fsv)
	{
		this.has_paired_fsv = has_paired_fsv;
	}


	public String getZone_mode()
	{
		return zone_mode;
	}


	public void setZone_mode(String zone_mode)
	{
		this.zone_mode = zone_mode;
	}


	public Boolean getSetback()
	{
		return isSetback;
	}


	public void setSetback(Boolean setback)
	{
		isSetback = setback;
	}


	public Boolean getZoneDead()
	{
		return isZoneDead;
	}


	public void setZoneDead(Boolean zoneDead)
	{
		isZoneDead = zoneDead;
	}


	public String getCurrent_mode()
	{
		return current_mode;
	}


	public void setCurrent_mode(String current_mode)
	{
		this.current_mode = current_mode;
	}


	public String get_zone()
	{
		return _zone;
	}


	public void set_zone(String _zone)
	{
		this._zone = _zone;
	}


	public String getUpdatetime()
	{
		return updatetime;
	}


	public void setUpdatetime(String updatetime)
	{
		this.updatetime = updatetime;
	}


	public KinveyMetaData getMeta()
	{
		return meta;
	}


	public void setMeta(KinveyMetaData meta)
	{
		this.meta = meta;
	}


	public KinveyMetaData.AccessControlList getAcl()
	{
		return acl;
	}


	public void setAcl(KinveyMetaData.AccessControlList acl)
	{
		this.acl = acl;
	}


	public CCUZones()
	{
	}  //GenericJson classes must have a public empty constructor
}
