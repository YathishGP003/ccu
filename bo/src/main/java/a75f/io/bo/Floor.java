package a75f.io.bo;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 8/8/17.
 */

public class Floor
{
	
	private int             mFloorId;
	private String          mFloorName;
	private String          mKinveyWebId;
	private ArrayList<Room> mRoomList;
	
	public Floor(int floorId ,String webId, String floor ) {
		mFloorId = floorId;
		mKinveyWebId = webId;
		mFloorName = floor;
		mRoomList = new ArrayList<Room>();
	}
	
}
