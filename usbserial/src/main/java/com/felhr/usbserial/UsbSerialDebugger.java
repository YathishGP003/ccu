package com.felhr.usbserial;

import com.felhr.utils.HexData;

import a75f.io.logger.CcuLog;

public class UsbSerialDebugger
{
    private static final String CLASS_ID = UsbSerialDebugger.class.getSimpleName();
    public static final String ENCODING = "UTF-8";

    private UsbSerialDebugger()
    {

    }

    public static void printLogGet(byte[] src, boolean verbose)
    {
        if(!verbose)
        {
            CcuLog.i(CLASS_ID, "Data obtained from write buffer: " + new String(src));
        }else
        {
            CcuLog.i(CLASS_ID, "Data obtained from write buffer: " + new String(src));
            CcuLog.i(CLASS_ID, "Raw data from write buffer: " + HexData.hexToString(src));
            CcuLog.i(CLASS_ID, "Number of bytes obtained from write buffer: " + src.length);
        }
    }

    public static void printLogPut(byte[] src, boolean verbose)
    {
        if(!verbose)
        {
            CcuLog.i(CLASS_ID, "Data obtained pushed to write buffer: " + new String(src));
        }else
        {
            CcuLog.i(CLASS_ID, "Data obtained pushed to write buffer: " + new String(src));
            CcuLog.i(CLASS_ID, "Raw data pushed to write buffer: " + HexData.hexToString(src));
            CcuLog.i(CLASS_ID, "Number of bytes pushed from write buffer: " + src.length);
        }
    }


}
