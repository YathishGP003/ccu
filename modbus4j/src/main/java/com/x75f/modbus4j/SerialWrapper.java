package com.x75f.modbus4j;


import com.x75f.modbus4j.serial.SerialPortWrapper;

import java.io.InputStream;
import java.io.OutputStream;

public class SerialWrapper implements SerialPortWrapper {

    private int baudRate;
    private int flowControlIn;
    private int flowControlOut;
    private int dataBits;
    private int stopBits;
    private int parity;
    private InputStream serialInputStream = null;
    private OutputStream serialOutputStream = null;

    public SerialWrapper(int baudRate, int flowControlIn,
                         int flowControlOut, int dataBits, int stopBits, int parity) {

        this.baudRate = baudRate;
        this.flowControlIn = flowControlIn;
        this.flowControlOut = flowControlOut;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }


    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#close()
     */
    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#open()
     */
    @Override
    public void open() throws Exception {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getInputStream()
     */
    @Override
    public InputStream getInputStream() {
        // TODO Auto-generated method stub
        return serialInputStream;
    }


    public void setInputStream(InputStream inputStream) {
        this.serialInputStream = inputStream;
    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getOutputStream()
     */
    @Override
    public OutputStream getOutputStream() {
        // TODO Auto-generated method stub
        return serialOutputStream;
    }


    public void setOutputStream(OutputStream outputStream) {
        this.serialOutputStream = outputStream;
    }
    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getBaudRate()
     */
    @Override
    public int getBaudRate() {
        // TODO Auto-generated method stub
        return baudRate;
    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getStopBits()
     */
    @Override
    public int getStopBits() {
        // TODO Auto-generated method stub
        return stopBits;
    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getParity()
     */
    @Override
    public int getParity() {
        // TODO Auto-generated method stub
        return parity;
    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getDataBits()
     */
    @Override
    public int getDataBits() {
        // TODO Auto-generated method stub
        return dataBits;
    }

}