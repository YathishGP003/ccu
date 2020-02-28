package a75f.io.renatus;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatDelegate;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import a75f.io.alerts.AlertManager;
import a75f.io.device.DeviceUpdateJob;
import a75f.io.device.mesh.LSerial;
import a75f.io.logic.Globals;
import a75f.io.logic.watchdog.Watchdog;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.UsbService;


/**
 * Created by rmatt isOn 7/19/2017.
 */

public abstract class UtilityApplication extends Application
{

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            switch (intent.getAction())
            {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED

                    NotificationHandler.setCMConnectionStatus(true);
                    LSerial.getInstance().setResetSeedMessage(true);
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    // DeviceUpdateJobOld.scheduleJob();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED

                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private UsbService               usbService;

    private DeviceUpdateJob          deviceUpdateJob;
    private final ServiceConnection usbConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1)
        {
            try {
                Log.d("USB Permisssion", "utility Application -" + arg1.isBinderAlive() + "," + arg1.toString() + "," + arg0.getClassName() + "," + arg1.getInterfaceDescriptor());
                if (arg1.isBinderAlive()) {
                    usbService = ((UsbService.UsbBinder) arg1).getService();
                    LSerial.getInstance().setUSBService(usbService);

                    //TODO: research what cts and dsr changes are.  For now no handler will be used, because I'm uncertain if the information is relevant.
                    usbService.setHandler(null);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }


        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            usbService = null;
        }
    };


    @Override
    public void onCreate()
    {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Globals.getInstance().setApplicationContext(this);
        AlertManager.getInstance(this).setApplicationContext(this);
        setUsbFilters();  // Start listening notifications from UsbService
        startService(new Intent(this, OTAUpdateHandlerService.class));  // Start OTA update event + timer handler service
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
        EventBus.getDefault().register(this);
        //disablePush();
//        new Thread()
//        {
//            @Override
//            public void run()
//            {
//                super.run();
//                openBot();
//            }
//        }.start();
    
        deviceUpdateJob = new DeviceUpdateJob();
        deviceUpdateJob.scheduleJob("DeviceUpdateJob", 60,
                15, TimeUnit.SECONDS);
        Watchdog.getInstance().addMonitor(deviceUpdateJob);
    }

    private void setUsbFilters()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }


    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras)
    {
        if (!UsbService.SERVICE_CONNECTED)
        {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty())
            {
                Set<String> keys = extras.keySet();
                for (String key : keys)
                {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            this.startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }





    /*public void openBot()
    {
        final String slackToken = "xoxb-255909669140-FdBDuw3yE0Syn2O7hLXQsjOO";
        mWebApiClient = SlackClientFactory.createWebApiClient(slackToken);
        String webSocketUrl = mWebApiClient.startRealTimeMessagingApi().findPath("url").asText();
        mRtmClient = new SlackRealTimeMessagingClient(webSocketUrl);
        mRtmClient.addListener(Event.HELLO, new EventListener()
        {

            @Override
            public void onMessage(JsonNode message)
            {
                Authentication authentication = mWebApiClient.auth();
                mBotId = authentication.getUser_id();
                System.out.println("User id: " + mBotId);
                System.out.println("Team name: " + authentication.getTeam());
                System.out.println("User name: " + authentication.getUser());
            }
        });
        final String selectedFloor = "";
        mRtmClient.addListener(Event.MESSAGE, new EventListener()
        {
            Zone selectedZone = null;


            @Override
            public void onMessage(JsonNode message)
            {
                String channelId = message.findPath("channel").asText();
                String userId = message.findPath("user").asText();
                String text = message.findPath("text").asText().trim();
                text = text.replace("<@U7HSRKP44> ", "");
                if (userId != null && !userId.equals(mBotId))
                {
                    Channel channel;
                    try
                    {
                        channel = mWebApiClient.getChannelInfo(channelId);
                    }
                    catch (SlackResponseErrorException e)
                    {
                        channel = null;
                    }
                    User user = mWebApiClient.getUserInfo(userId);
                    String userName = user.getName();
                    int min = 0;
                    int max = 10;
                    mWebApiClient.meMessage(channelId, "PING:" + text);
                    Log.i("CCU", "CCU state: " + text);
                    if (text.equals("state"))
                    {
                        mWebApiClient.meMessage(channelId,
                                LocalStorage.getApplicationSettingsAsString().length() > 999
                                        ? LocalStorage.getApplicationSettingsAsString()
                                                      .substring(0, 998)
                                        : LocalStorage.getApplicationSettingsAsString());
                    }
                    else if (text.equals("Clean up your grammer"))
                    {
                        mWebApiClient.meMessage(channelId,
                                "OK, I'll be more straight forward with you {state}, {zones}, " +
                                "{[zone selected] light, dark}");
                    }
                    else if (text.equals("zones"))
                    {
                        String zonesMessage = ":::: ";
                        for (Zone z : ccu().getFloors().get(0).mRoomList)
                        {
                            zonesMessage = z.roomName + " ::::";
                        }
                        mWebApiClient.meMessage(channelId, zonesMessage);
                    }
                    else if (text.startsWith("zones"))
                    {
                        String zoneSelected = text.replace("zones ", "");
                        for (Zone z : ccu().getFloors().get(0).mRoomList)
                        {
                            if (z.roomName.equalsIgnoreCase(zoneSelected))
                            {
                                selectedZone = z;
                                mWebApiClient.meMessage(channelId,
                                        selectedZone.roomName + " wise" + " choice");
                            }
                        }
                    }
                    else if (text.equals("light"))
                    {
                        if (selectedZone == null)
                        {
                            mWebApiClient
                                    .meMessage(channelId, " nub " + " must select zone first!");
                        }
                        else
                        {
                            selectedZone.findProfile(ProfileType.LIGHT)
                                        .setOverride(System.currentTimeMillis(), OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND, (short) 100);
                            mWebApiClient.meMessage(channelId,
                                    " TA DA, " + selectedZone.roomName + " is now" + " LIT!" +
                                    "--- [check " + "state for verification]");
                        }
                    }
                    else if (text.equals("dark"))
                    {
                        if (selectedZone == null)
                        {
                            mWebApiClient.meMessage(channelId,
                                    selectedZone.roomName + " nub " + " must select zone first!");
                        }
                        else
                        {
                            selectedZone.findProfile(ProfileType.LIGHT)
                                        .setOverride(System.currentTimeMillis(), OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND, (short) 0);
                            mWebApiClient.meMessage(channelId,
                                    " Hey, who" + " turn out the lights --- [check " +
                                    "state for verification]");
                        }
                    } int randomNum = rand.nextInt((max - min) + 1) + min;
                    if (randomNum == 0)
                    {
                        mWebApiClient.meMessage(channelId, "Ryan's CCU is sad : (");
                    }
                    else if (randomNum == 1)
                    {
                        mWebApiClient.meMessage(channelId, "Ryan's CCU is happy : )");
                    }
                    else if (randomNum == 2)
                    {
                        mWebApiClient.meMessage(channelId, "Ryan's CCU is confused :S");
                    }
                }
            }
        }); mRtmClient.connect();
    }*/


    @Override
    public void onTerminate()
    {
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
        super.onTerminate();
    }


    // Called in a separate thread
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSerialEvent(SerialEvent event)
    {
        LSerial.handleSerialEvent(this, event);
    }


}
