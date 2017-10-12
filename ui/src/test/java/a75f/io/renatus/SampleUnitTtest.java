package a75f.io.renatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.MessageType;

/**
 * Example local unit test, which will execute isOn the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class SampleUnitTtest
{


/*
"/Applications/Android Studio.app/Contents/jre/jdk/Contents/Home/bin/java" -Didea.launcher.port=7532 "-Didea.launcher.bin.path=/Applications/Android Studio.app/Contents/bin" -Didea.junit.sm_runner -Dfile.encoding=UTF-8 -classpath "/Applications/Android Studio.app/Contents/lib/idea_rt.jar:/Applications/Android Studio.app/Contents/plugins/junit/lib/junit-rt.jar:/Users/ryanmattison/Library/Android/sdk/platforms/android-25/data/res:/Users/ryanmattison/Documents/reposv4/renatus/ui/build/intermediates/classes/test/debug:/Users/ryanmattison/Documents/reposv4/renatus/ui/build/intermediates/classes/debug:/Users/ryanmattison/.android/build-cache/f5eb42e7299b02bed11623971c2480362a209673/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/f5eb42e7299b02bed11623971c2480362a209673/output/res:/Users/ryanmattison/.android/build-cache/24eec64f9b96a6f7c395f77e94ac95ddce082f84/output/res:/Users/ryanmattison/.android/build-cache/24eec64f9b96a6f7c395f77e94ac95ddce082f84/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/2fc65da9d88d09a545ec2fb7fb2d9ba190438688/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/2fc65da9d88d09a545ec2fb7fb2d9ba190438688/output/res:/Users/ryanmattison/.android/build-cache/33f730744de0c261ebfdd8d8bc7b463967f9cadb/output/res:/Users/ryanmattison/.android/build-cache/33f730744de0c261ebfdd8d8bc7b463967f9cadb/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/e181888ec81c275ff6bd22bc7234762f23329c4a/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/e181888ec81c275ff6bd22bc7234762f23329c4a/output/res:/Users/ryanmattison/.android/build-cache/af51aad04a91fc3c9d3be9c78690b492b5d61701/output/res:/Users/ryanmattison/.android/build-cache/af51aad04a91fc3c9d3be9c78690b492b5d61701/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/2b6e99cabd557c9b85245afd8520c75f79219c06/output/res:/Users/ryanmattison/.android/build-cache/2b6e99cabd557c9b85245afd8520c75f79219c06/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/16ee2bd6733994d95e7436ab0212ceca86619565/output/res:/Users/ryanmattison/.android/build-cache/16ee2bd6733994d95e7436ab0212ceca86619565/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/881ad3e9e62c581629674e96e00c0382746f555f/output/res:/Users/ryanmattison/.android/build-cache/881ad3e9e62c581629674e96e00c0382746f555f/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/fa7f36805ce4273e5666fa3f9bd065956f87bca4/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/fa7f36805ce4273e5666fa3f9bd065956f87bca4/output/res:/Users/ryanmattison/.gradle/caches/modules-2/files-2.1/com.jakewharton/butterknife-annotations/8.7.0/2637055420a2da406e3ea9920c2ff4aafd38c970/butterknife-annotations-8.7.0.jar:/Users/ryanmattison/.gradle/caches/modules-2/files-2.1/org.greenrobot/eventbus/3.0.0/ddd99896e9569eaababbe81b35d80e1b91c4ad85/eventbus-3.0.0.jar:/Users/ryanmattison/.android/build-cache/8d60a0c710b9085393e8fb1c261737890b1b27d2/output/res:/Users/ryanmattison/.android/build-cache/8d60a0c710b9085393e8fb1c261737890b1b27d2/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/9382e7b0b0bc6753b31d03ccd3cfc1e91d353475/output/res:/Users/ryanmattison/.android/build-cache/9382e7b0b0bc6753b31d03ccd3cfc1e91d353475/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/ad4f3316f9b865a3d3ce00e7321b80382e8809ad/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/ad4f3316f9b865a3d3ce00e7321b80382e8809ad/output/res:/Users/ryanmattison/.android/build-cache/637e62b44fb0999aa6f9e106f8ef9bbab9049510/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/637e62b44fb0999aa6f9e106f8ef9bbab9049510/output/res:/Users/ryanmattison/.android/build-cache/fe6d47abdbc219c8223fcf81d9a6736572451794/output/res:/Users/ryanmattison/.android/build-cache/fe6d47abdbc219c8223fcf81d9a6736572451794/output/jars/classes.jar:/Users/ryanmattison/.gradle/caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/42a25dc3219429f0e5d060061f71acb49bf010a0/hamcrest-core-1.3.jar:/Users/ryanmattison/.android/build-cache/2bcd310c8fb7136a5bf4f2528cbf86cb76dce8e6/output/res:/Users/ryanmattison/.android/build-cache/2bcd310c8fb7136a5bf4f2528cbf86cb76dce8e6/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/bdbae27b840c248417cc5ce4e5a8c186554a0b60/output/res:/Users/ryanmattison/.android/build-cache/bdbae27b840c248417cc5ce4e5a8c186554a0b60/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/5934b8528551c97dbe5d0ebfc5cce1e5be34573a/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/5934b8528551c97dbe5d0ebfc5cce1e5be34573a/output/res:/Users/ryanmattison/.gradle/caches/modules-2/files-2.1/junit/junit/4.12/2973d150c0dc1fefe998f834810d68f278ea58ec/junit-4.12.jar:/Users/ryanmattison/.android/build-cache/6e2b988cd6eb745b19d283c43820906b473ed631/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/6e2b988cd6eb745b19d283c43820906b473ed631/output/res:/Users/ryanmattison/.android/build-cache/a92be714f70dc6a0741807a268d1368db898a081/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/a92be714f70dc6a0741807a268d1368db898a081/output/res:/Users/ryanmattison/Library/Android/sdk/extras/android/m2repository/com/android/support/support-annotations/25.3.1/support-annotations-25.3.1.jar:/Users/ryanmattison/.android/build-cache/a48163b88fcfc91f4906102aea8bb51e454bbac4/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/a48163b88fcfc91f4906102aea8bb51e454bbac4/output/res:/Users/ryanmattison/.android/build-cache/53bb72e5fb9f8f835ae8b63249a8017ebad8b9e5/output/res:/Users/ryanmattison/.android/build-cache/53bb72e5fb9f8f835ae8b63249a8017ebad8b9e5/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/4e144d8bb0a42bb09b71e50aa780c8a098a9fc54/output/res:/Users/ryanmattison/.android/build-cache/4e144d8bb0a42bb09b71e50aa780c8a098a9fc54/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/716ee58356ea1b719beb90f27c94658cafbfc026/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/716ee58356ea1b719beb90f27c94658cafbfc026/output/res:/Users/ryanmattison/Documents/reposv4/renatus/util/build/intermediates/classes/test/debug:/Users/ryanmattison/Documents/reposv4/renatus/util/build/intermediates/classes/debug:/Users/ryanmattison/Documents/reposv4/renatus/libs/google-http-client-1.19.0.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/gson-2.1.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/guava-18.0.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/realm-annotations-processor-3.2.1.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/google-http-client-gson-1.19.0.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/realm-annotations-3.2.1.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/jackson-core-2.1.3.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/rxjava-1.1.0.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/google-http-client-jackson2-1.19.0.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/java-api-core-3.0.1.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/google-http-client-android-1.19.0.jar:/Users/ryanmattison/Documents/reposv4/renatus/libs/realm-transformer-3.2.1.jar:/Users/ryanmattison/Documents/reposv4/renatus/bo/build/intermediates/classes/debug:/Users/ryanmattison/Documents/reposv4/renatus/usbserial/build/intermediates/classes/debug:/Users/ryanmattison/.android/build-cache/9431f42d7d330b58496f61087eb405c26a2863e9/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/9431f42d7d330b58496f61087eb405c26a2863e9/output/res:/Users/ryanmattison/.android/build-cache/9a474656ed22b51d1ed217f2cf65d3c38ef2cd8d/output/res:/Users/ryanmattison/.android/build-cache/9a474656ed22b51d1ed217f2cf65d3c38ef2cd8d/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/8d0ec977a3df5c88ec2faf018aae22af5b00653d/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/8d0ec977a3df5c88ec2faf018aae22af5b00653d/output/res:/Users/ryanmattison/.android/build-cache/4e1976501fbd4b526112e1e8e401205a3921989d/output/res:/Users/ryanmattison/.android/build-cache/4e1976501fbd4b526112e1e8e401205a3921989d/output/jars/classes.jar:/Users/ryanmattison/.android/build-cache/e8940e4555d4ca871d1400bc7ae6f5277eb0b0c9/output/res:/Users/ryanmattison/.android/build-cache/e8940e4555d4ca871d1400bc7ae6f5277eb0b0c9/output/jars/classes.jar:/Users/ryanmattison/Documents/reposv4/renatus/bluetooth/build/intermediates/classes/debug:/Users/ryanmattison/Documents/reposv4/renatus/logic/build/intermediates/classes/test/debug:/Users/ryanmattison/Documents/reposv4/renatus/logic/build/intermediates/classes/debug:/Users/ryanmattison/Library/Android/sdk/extras/android/m2repository/com/android/support/support-annotations/25.2.0/support-annotations-25.2.0.jar:/Users/ryanmattison/Documents/reposv4/renatus/ui/build/intermediates/sourceFolderJavaResources/test/debug:/Users/ryanmattison/Documents/reposv4/renatus/bo/build/intermediates/sourceFolderJavaResources/debug:/Users/ryanmattison/Documents/reposv4/renatus/logic/build/intermediates/sourceFolderJavaResources/debug:/Users/ryanmattison/Documents/reposv4/renatus/ui/build/intermediates/sourceFolderJavaResources/debug:/Users/ryanmattison/Documents/reposv4/renatus/util/build/intermediates/sourceFolderJavaResources/debug:/Users/ryanmattison/Documents/reposv4/renatus/usbserial/build/intermediates/sourceFolderJavaResources/debug:/Users/ryanmattison/Documents/reposv4/renatus/bluetooth/build/intermediates/sourceFolderJavaResources/debug:/Users/ryanmattison/Documents/reposv4/renatus/build/generated/mockable-android-25.jar" com.intellij.rt.execution.application.AppMain com.intellij.rt.execution.junit.JUnitStarter -ideVersion5 a75f.io.renatus.ExampleUnitTest,addition_isCorrect
23 00 07 D0
23 00 D0 07

Process finished with exit code 0

 */


	@Test
	public void test_LittleEndian() throws Exception
	{
		
		/*var dataG1 = [{
						type: "stepLine",
		                color: "red",
				        dataPoints :[
		{ x: 1, y: 0, indexLabel:"relay1",markerColor: "red" }, //dataPoint
		{ x: 2, y: 0},
		{ x: 3, y: 1},
		{ x: 4, y: 0}]
	},
		{
type: "stepLine",
		      dataPoints :[
			{ x: 1, y: 0, indexLabel:"room-temp",markerColor: "green"  }, //dataPoint
			{ x: 2, y: 7.3,indexLabel:"",markerColor: "green" },
			{ x: 3, y: 1, indexLabel:"",markerColor: "green" },
			{ x: 4, y: 0,indexLabel:"",markerColor: "green" }]
		}]*/
		JSONArray data = new JSONArray();
		JSONObject snType = new JSONObject();
		JSONArray dp = new JSONArray();
		try
		{
			snType.put("type", "stepLine");
			snType.put("color", "red");
			dp
		} catch (JSONException e) {
			e.printStackTrace();
		}
		System.out.print(snType.toString());
		/*CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =
				new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
		seedMessage.putEncrptionKey(new byte[16]);
		seedMessage.smartNodeAddress.set(2000);
		seedMessage.controls.digitalOut1.set((short) 1);
		System.out.println(seedMessage.toString());*/
		//        seedMessage.controls.time.day.set((short) 1);
		//        seedMessage.controls.time.hours.set((short) 1);
		//        seedMessage.controls.time.minutes.set((short) 1);
		//
		//        seedMessage.settings.ledBitmap.digitalOut1.set(1);
		//        seedMessage.controls.smartNodeControls_extras.digitalOut1.set(1);
		//        seedMessage.settings.ledBitmap.digitalOut2.set(1);
		//        System.out.println("Seed message: " + seedMessage.toString());
		//        System.out.println("Seed message size: " + seedMessage.size());
		//        TestStruct testStruct = new TestStruct();
		//
		//
		//        //testStruct.messageType.set((short) MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN.ordinal());
		//
		//        testStruct.messageType.set((short) 1);
		//        System.out.println(testStruct.toString());
		//
		//        LittleEndianTestStruct littleEndianTestStruct = new LittleEndianTestStruct();
		//        System.out.println("Size: " + testStruct.size());
		//       // littleEndianTestStruct.messageType.set((short) MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN.ordinal());
		//        littleEndianTestStruct.messageType.set((short) 1);
		//        littleEndianTestStruct.smartNodeAddress.set(2000);
		//
		//        System.out.println(littleEndianTestStruct.toString());
		//        System.out.println("Size: " + littleEndianTestStruct.size());
		//        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =  new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		//        seedMessage.encryptionKey.set(0);
		//
		//        seedMessage.smartNodeAddress.set(8000);
		//        seedMessage.controls.time.day.set((short) 1);
		//        seedMessage.controls.time.hours.set((short) 1);
		//        seedMessage.controls.time.minutes.set((short) 1);
		//
		//        seedMessage.settings.ledBitmap.digitalOut1.set(1);
		//
		//        System.out.println(seedMessage.toString());


        /*SmartNodeControls_t smartNodeControls_t = new SmartNodeControls_t();
        smartNodeControls_t.analogOut1.set((short)1);
        smartNodeControls_t.analogOut2.set((short)2);
        smartNodeControls_t.smartNodeControls_extras.smartNodeControlsBitExtras.digitalOut1.set(1);
        smartNodeControls_t.smartNodeControls_extras.smartNodeControlsBitExtras.digitalOut2.set(1);
        //Smart Node Controls: 00 00 00 00 00 01 02 00 00 00 60
        System.out.println("Smart Node Controls: " + smartNodeControls_t.toString());*/
	}
}