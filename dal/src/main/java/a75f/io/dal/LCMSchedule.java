package a75f.io.dal;

/**
 * Created by Yinten on 9/4/2017.
 */

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.List;

public class LCMSchedule extends GenericJson
{
	@Key
	private String st;
	@Key
	private String et;
	@Key
	private int val;
	@Key
	private List<String> days = null;
	
	

	public String getSt() {
		return st;
	}
	

	public void setSt(String st) {
		this.st = st;
	}
	

	public String getEt() {
		return et;
	}
	

	public void setEt(String et) {
		this.et = et;
	}
	

	public int getVal() {
		return val;
	}
	

	public void setVal(int val) {
		this.val = val;
	}

	public List<String> getDays() {
		return days;
	}

	public void setDays(List<String> days) {
		this.days = days;
	}


	
}