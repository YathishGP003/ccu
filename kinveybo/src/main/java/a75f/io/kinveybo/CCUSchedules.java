package a75f.io.kinveybo;
/**
 * Created by Yinten on 9/4/2017.
 */

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.ArrayList;


public class CCUSchedules extends GenericJson
{ // For Serialization

	@Key
	public String type;

	@Key
	public ArrayList<InternalSchedule> internalSchedules;


	public CCUSchedules()
	{
	}  //GenericJson classes must have a public empty constructor


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ArrayList<InternalSchedule> getInternalSchedules() {
		return internalSchedules;
	}

	public void setInternalSchedules(ArrayList<InternalSchedule> internalSchedules) {
		this.internalSchedules = internalSchedules;
	}
}

