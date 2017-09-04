package a75f.io.dal;

/**
 * Created by Yinten on 9/4/2017.
 */

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
		                   "st",
		                   "et",
		                   "val",
		                   "days"
})
public class LCMSchedule extends GenericJson
{
	@Key
	@JsonProperty("st")
	private String st;
	@Key
	@JsonProperty("et")
	private String et;
	@Key
	@JsonProperty("val")
	private int val;
	@Key
	@JsonProperty("days")
	private List<String> days = null;
	
	
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();
	
	@JsonProperty("st")
	public String getSt() {
		return st;
	}
	
	@JsonProperty("st")
	public void setSt(String st) {
		this.st = st;
	}
	
	@JsonProperty("et")
	public String getEt() {
		return et;
	}
	
	@JsonProperty("et")
	public void setEt(String et) {
		this.et = et;
	}
	
	@JsonProperty("val")
	public int getVal() {
		return val;
	}
	
	@JsonProperty("val")
	public void setVal(int val) {
		this.val = val;
	}
	
	@JsonProperty("days")
	public List<String> getDays() {
		return days;
	}
	
	@JsonProperty("days")
	public void setDays(List<String> days) {
		this.days = days;
	}
	
	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}
	
	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}
	
}