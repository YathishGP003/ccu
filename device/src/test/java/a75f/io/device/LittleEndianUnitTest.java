package a75f.io.device;

/**
 * Determine the feasibility of using Struct library from Javolution for Renatus.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class LittleEndianUnitTest
{
	/*//region vars
	private static final char[] HEXA = {'0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	
	MessageType messageType = MessageType.NUM_PROTOCOL_MESSAGE_TYPES.FSV_PAIRING_REQ; //0
	short   mSensorAddress      = 0;
	byte[]  encryptionKey       = new byte[16];
	short   dayOfWeek           = 0;
	short   hourOfDay           = 0;
	short   minute              = 0;
	int     damperPos           = 0;
	int     controlSignal       = 0;
	float   analog2OutValues    = 0;
	int     analog_out3         = 0;
	int     analog_out4         = 0;
	boolean isHeating           = false;
	int     getExhaustFanStatus = 0;
	int     digital_out2        = 0;
	int     digital_out3        = 0;
	boolean restartDevice = false;
	int     digital_out4        = 0;
	//operationalMode = (byte) (oaoOnly & 0xff);
	int getOAODamperPosMax = 0;
	int OAODamperPosMin    = 0;
	float propConstant               = 0.0f;
	float integConstant              = 0.0f;
	float calculatedPropControlSpeed = 0.0f;
	int   integControlTimeout        = 0;//0x0f;
	int heatingAirFlowTemperature = 0;
	int coolingAirFlowTemperature = 0;
	byte oaoOnly = 0x04;
	String roomName = "roomName";
	private int  power_in        = 0;
	private int  power_out       = 0;
	private int  analogIn1       = 0;
	private int  analogIn2       = 0;
	private int  thermister1     = 0;
	private int  thermister2     = 0;
	private int  relay1          = 0;
	private int  relay2          = 0;
	private int  analogOut1      = 0;
	private int  analogOut2      = 0;
	private int  power24v1       = 0;
	private int  power24v2       = 0;
	private int  logo            = 0;
	private byte operationalMode = 0x0;
	//endregion vars
	
	@Test
	public void testPower24v1()
	{
		analogIn1 = 0x04;
		restartDevice = true;
		CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		
		
		seedMessage.settings.roomName.set(roomName);
		seedMessage.settings.ledBitmap.analogIn1.set((short) 1);
		if(restartDevice)
			seedMessage.controls.reset.set((short)1);
//		seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
		//seedMessage.putEncrptionKey(encryptionKey);
		
		//seedMessage.smartNodeAddress.set(mSensorAddress);
		
		//seedMessage.settings.roomName.set(roomName);
		//seedMessage.controls.smartNodeControls_extras.smartNodeControlsBitExtras.digitalOut1.set(1);
		//seedMessage.controls.smartNodeControls_extras.smartNodeControlsBitExtras.padding.set(1);
		
		byte[] seedMessageOld = new byte[77];
		byte[] seedMessageStruct = seedMessage.getOrderedBuffer();
		seedMessageOld = getPairingByteArray();
		System.out.println("SeedMessageOld: " + toString(seedMessageOld));
		System.out.println("seedMessageStr: " + toString(seedMessageStruct));
		
		System.out.println("SeedMessageOld.length: " + seedMessageOld.length);
		System.out.println("seedMessageStr.length: " + seedMessageStruct.length);
		analogIn1 = 0;
		restartDevice = false;
		*//*****
		 SeedMessageOld: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
		 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
		 04 00 00 00 00 72 6F 6F 6D 4E 61 6D 65 00 00 00
		 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
		 00 00 00 00 00 00 00 00 00 20 00 00 00
		 seedMessageStr: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
		 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
		 04 00 00 00 00 72 6F 6F 6D 4E 61 6D 65 00 00 00
		 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
		 00 00 00 00 00 00 00 00 00 20
		 *
		 *
		 *//*
		
	}
	
	@Test
	public void test_SensorAddress_EncryptionKey_MessageType_RoomName() throws Exception
	{
		
		CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		mSensorAddress      = 2000;
		encryptionKey       = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
				0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16};
		messageType = MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN;
		seedMessage.messageType.set(messageType);
		seedMessage.putEncrptionKey(encryptionKey);
		
		seedMessage.smartNodeAddress.set(mSensorAddress);
		
		seedMessage.settings.roomName.set(roomName);
		//seedMessage.controls.smartNodeControls_extras.smartNodeControlsBitExtras.digitalOut1.set(1);
		//seedMessage.controls.smartNodeControls_extras.smartNodeControlsBitExtras.padding.set(1);
		
		byte[] seedMessageOld = new byte[77];
		byte[] seedMessageStruct = seedMessage.getOrderedBuffer();
		seedMessageOld = getPairingByteArray();
		System.out.println("SeedMessageOld: " + toString(seedMessageOld));
		System.out.println("seedMessageStr: " + toString(seedMessageStruct));
		
		System.out.println("SeedMessageOld.length: " + seedMessageOld.length);
		System.out.println("seedMessageStr.length: " + seedMessageStruct.length);
		
		
		//Clean up
		messageType = MessageType.FSV_PAIRING_REQ;
		encryptionKey = new byte[]{};
		mSensorAddress = 0;
		*//****
		 * SeedMessageOld: 23 D0 07 01 02 03 04 05 06 07 08 09 10 11 12 13
		 14 15 16 00 00 00 00 00 00 00 00 00 00 00 00 00
		 00 00 00 00 00 72 6F 6F 6D 4E 61 6D 65 00 00 00
		 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
		 00 00 00 00 00 00 00 00 00 00 00 00 00
		 seedMessageStr: 23 D0 07 01 02 03 04 05 06 07 08 09 10 11 12 13
		 14 15 16 00 00 00 00 00 00 00 00 00 00 00 00 00
		 00 00 00 00 00 72 6F 6F 6D 4E 61 6D 65 00 00 00
		 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
		 00 00 00 00 00 00 00 00 00 00
		 *//*
	}
	
	
	//region old code
	
	byte[] getPairingByteArray()
	{
		
		int nCounter = 0;
		byte msg = (byte) messageType.ordinal();
		byte byteArray[] = new byte[77];//change
		
		byteArray[nCounter++] = msg;
		nCounter = fillDeviceAddress(nCounter, byteArray);
		nCounter = fillEncryptionKey(nCounter, byteArray);
		nCounter = fillSmartNodeSettings(nCounter, byteArray);
		nCounter = fillSmartNodeControls(nCounter, byteArray);
		return byteArray;
		
	}
	
	public String toString(byte[] bytes)
	{
		
		TextBuilder tmp = new TextBuilder();
		final int size = bytes.length;
		final ByteBuffer buffer = ByteBuffer.wrap(bytes);
		final int start = buffer.position();
		for (int i = 0; i < size; i++)
		{
			int b = buffer.get(start + i) & 0xFF;
			tmp.append(HEXA[b >> 4]);
			tmp.append(HEXA[b & 0xF]);
			tmp.append(((i & 0xF) == 0xF) ? '\n' : ' ');
		}
		return tmp.toString();
	}
	
	private int fillDeviceAddress(int nCounter, byte[] byteArray)
	{
		
		byteArray[nCounter++] = (byte) (mSensorAddress & 0xff);
		byteArray[nCounter++] = (byte) ((mSensorAddress >> 8) & 0xff);
		return nCounter;
	}
	
	private int fillEncryptionKey(int nCounter, byte[] byteArray)
	{
		
		for (int nCount = 0; nCount < 16; nCount++)
		{
			byteArray[nCounter++] = (byte) (encryptionKey[nCount] & 0xff);
		}
		return nCounter;
	}
	
	private int fillSmartNodeSettings(int nCounter, byte[] byteArray)
	{
		
		byteArray[nCounter++] = (byte) (0 & 0xff);
		byteArray[nCounter++] = (byte) (0 & 0xff);
		byteArray[nCounter++] = (byte) (getOAODamperPosMax & 0xff);
		byteArray[nCounter++] = (byte) (OAODamperPosMin & 0xff);
		byteArray[nCounter++] = (byte) (0 & 0xff);
		byteArray[nCounter++] = (byte) (0 & 0xff);
		byteArray[nCounter++] = (byte) (0 & 0xff);
		
		int k1 = (int) (propConstant * 100);
		int k2 = (int) (integConstant * 100);
		int spread = (int) (calculatedPropControlSpeed * 10);
		int timeout = integControlTimeout;
		
		byteArray[nCounter++] = (byte) (k1 & 0xff);
		byteArray[nCounter++] = (byte) (k2 & 0xff);
		byteArray[nCounter++] = (byte) (spread & 0xff);
		byteArray[nCounter++] = (byte) (timeout & 0xff);
		byteArray[nCounter++] = (byte) (heatingAirFlowTemperature & 0xff);
		byteArray[nCounter++] = (byte) (coolingAirFlowTemperature & 0xff);
		
		//led
		
		byteArray[nCounter++] = (byte) (power_in
		                                | power_out
		                                | analogIn1
		                                | analogIn2
		                                | thermister1
		                                | thermister2
		                                | analogOut1
		                                | analogOut2);
		
		byteArray[nCounter++] = (byte) (relay1
		                                | relay2
		                                | (power24v1 > 0 ? 0x400 : 0)
		                                | power24v2
		                                | logo);
		
		byteArray[nCounter++] = operationalMode; //OAO Profile only
//        byteArray[nCounter++] = (byte) (0x00 &  0xff);
		byteArray[nCounter++] = (byte) (0x00 & 0xff);//Lighting intensity (%) to use when occupants are detected
		byteArray[nCounter++] = (byte) (0x00 & 0xff);//Minimum time that a lighting control override will stay in effect
		
		byte[] value = nullTerminateRoomName(roomName.getBytes(StandardCharsets.UTF_8));
		for (int i = 0; i < value.length; i++)
		{
			byteArray[nCounter++] = value[i];
		}
		byteArray[nCounter++] = (byte) (0x00);
		
		return nCounter;
	}
	
	private int fillSmartNodeControls(int nCounter, byte[] byteArray)
	{
		
		byteArray[nCounter++] = (byte) (dayOfWeek & 0xff);
		byteArray[nCounter++] = (byte) (hourOfDay & 0xff);
		byteArray[nCounter++] = (byte) (minute & 0xff);
		byteArray[nCounter++] = (byte) (0 & 0xff);
		byteArray[nCounter++] = (byte) (damperPos & 0xff);
		byteArray[nCounter++] = (byte) (controlSignal & 0xff);
		
		int retDamper = (int) (analog2OutValues * 10);
		byteArray[nCounter++] = (byte) (retDamper & 0xff);
		byteArray[nCounter++] = (byte) (analog_out3 & 0xff);
		byteArray[nCounter++] = (byte) (analog_out4 & 0xff);
		
		byteArray[nCounter++] = (byte) (00 & 0xff);// Command for the infrared transmitter
		
		byteArray[nCounter++] = (byte) (isHeating ? 0x01 : 0x00
		                                                   | (getExhaustFanStatus > 0 ? 0x02 : 0x00)
		                                                   | (digital_out2 & 0x04)
		                                                   | (digital_out3 & 0x08)
		                                                   | (digital_out4 & 0x10)
		                                                   | (restartDevice ? 0x20 : 0x00)
		);
		return nCounter;
	}
	
	private byte[] nullTerminateRoomName(byte[] inputBytes)
	{
		
		byte[] value = new byte[25];
//
//        System.arraycopy(inputBytes, 0, value, 0, inputBytes.length);
//        System.arraycopy(nullTerminator, 0, value, inputBytes.length, nullTerminator.length);
		
		System.arraycopy(inputBytes, 0, value, 0, inputBytes.length);
		return value;
	}
	
	//endregion old code*/
}