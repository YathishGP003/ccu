package ccu.x75fahrenheit.com.serial;

import org.greenrobot.eventbus.EventBus;

import a75f.io.bo.MessageEvent;
import a75f.io.bo.interfaces.ISerial;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class comm {

    public void sendOverWire(ISerial iSerial)
    {

    }

    public void recieveOverWire()
    { //update email


        //recieve bytes .. parse to event .. send on eventbus
        EventBus.getDefault().post(new MessageEvent());


    }



}
