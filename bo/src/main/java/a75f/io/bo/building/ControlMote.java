package a75f.io.bo.building;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Created by Yinten on 8/15/2017.
 */
@JsonSerialize
public class ControlMote
{
	//SerializationFeature.FAIL_ON_EMPTY_BEANS
	String controlMoteOutputInput = "Control Mote settings go here";
}
