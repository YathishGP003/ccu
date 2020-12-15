/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2006-2011 Serotonin Software Technologies Inc. http://serotoninsoftware.com
 * @author Matthew Lohbihler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.x75f.modbus4j.ip.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import com.x75f.modbus4j.ModbusMaster;
import com.x75f.modbus4j.base.BaseMessageParser;
import com.x75f.modbus4j.exception.ModbusInitException;
import com.x75f.modbus4j.exception.ModbusTransportException;
import com.x75f.modbus4j.ip.IpMessageResponse;
import com.x75f.modbus4j.ip.IpParameters;
import com.x75f.modbus4j.ip.encap.EncapMessageParser;
import com.x75f.modbus4j.ip.encap.EncapMessageRequest;
import com.x75f.modbus4j.ip.encap.EncapWaitingRoomKeyFactory;
import com.x75f.modbus4j.ip.xa.XaMessageParser;
import com.x75f.modbus4j.ip.xa.XaMessageRequest;
import com.x75f.modbus4j.ip.xa.XaWaitingRoomKeyFactory;
import com.x75f.modbus4j.msg.ModbusRequest;
import com.x75f.modbus4j.msg.ModbusResponse;
import com.x75f.modbus4j.sero.messaging.EpollStreamTransport;
import com.x75f.modbus4j.sero.messaging.MessageControl;
import com.x75f.modbus4j.sero.messaging.OutgoingRequestMessage;
import com.x75f.modbus4j.sero.messaging.StreamTransport;
import com.x75f.modbus4j.sero.messaging.Transport;
import com.x75f.modbus4j.sero.messaging.WaitingRoomKeyFactory;

/**
 * <p>TcpMaster class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class TcpMaster extends ModbusMaster {
    private static final int RETRY_PAUSE_START = 50;
    private static final int RETRY_PAUSE_MAX = 1000;

    // Configuration fields.
    private short nextTransactionId = 0;
    private final IpParameters ipParameters;
    private final boolean keepAlive;
    private final boolean autoIncrementTransactionId;

    // Runtime fields.
    private Socket socket;
    private Transport transport;
    private MessageControl conn;

    /**
     * <p>Constructor for TcpMaster.</p>
     *
     * @param params
     * @param keepAlive
     * @param autoIncrementTransactionId
     * @param validateResponse - confirm that requested slave id is the same in the response
     */
    public TcpMaster(IpParameters params, boolean keepAlive, boolean autoIncrementTransactionId, boolean validateResponse) {
        this.ipParameters = params;
        this.keepAlive = keepAlive;
        this.autoIncrementTransactionId = autoIncrementTransactionId;
    }

    /**
     * <p>Constructor for TcpMaster.</p>
     * Default to not validating the slave id in responses
     *
     * @param params a {@link com.x75f.modbus4j.ip.IpParameters} object.
     * @param keepAlive a boolean.
     * @param autoIncrementTransactionId a boolean.
     */
    public TcpMaster(IpParameters params, boolean keepAlive, boolean autoIncrementTransactionId) {
        this(params, keepAlive, autoIncrementTransactionId, false);
    }


    /**
     * <p>Constructor for TcpMaster.</p>
     *
     * Default to auto increment transaction id
     * Default to not validating the slave id in responses
     *
     * @param params a {@link com.x75f.modbus4j.ip.IpParameters} object.
     * @param keepAlive a boolean.
     */
    public TcpMaster(IpParameters params, boolean keepAlive) {
        this(params, keepAlive, true, false);
    }

    /**
     * <p>Setter for the field <code>nextTransactionId</code>.</p>
     *
     * @param id a short.
     */
    public void setNextTransactionId(short id) {
        this.nextTransactionId = id;
    }

    /**
     * <p>Getter for the field <code>nextTransactionId</code>.</p>
     *
     * @return a short.
     */
    protected short getNextTransactionId() {
        return nextTransactionId;
    }

    /** {@inheritDoc} */
    @Override
    synchronized public void init() throws ModbusInitException {
        try {
            if (keepAlive)
                openConnection();
        }
        catch (Exception e) {
            throw new ModbusInitException(e);
        }
        initialized = true;
    }

    /** {@inheritDoc} */
    @Override
    synchronized public void destroy() {
        closeConnection();
        initialized = false;
    }

    /** {@inheritDoc} */
    @Override
    synchronized public ModbusResponse sendImpl(ModbusRequest request) throws ModbusTransportException {
        try {
            // Check if we need to open the connection.
            if (!keepAlive)
                openConnection();



        }
        catch (Exception e) {
            closeConnection();
            throw new ModbusTransportException(e, request.getSlaveId());
        }

        // Wrap the modbus request in a ip request.
        OutgoingRequestMessage ipRequest;
        if (ipParameters.isEncapsulated())
            ipRequest = new EncapMessageRequest(request);
        else {
            if(autoIncrementTransactionId)
                this.nextTransactionId++;
            ipRequest = new XaMessageRequest(request, getNextTransactionId());
        }

        /*if(LOG.isDebugEnabled()){
            StringBuilder sb = new StringBuilder();
            for (byte b : Arrays.copyOfRange(ipRequest.getMessageData(),0,ipRequest.getMessageData().length)) {
                sb.append(String.format("%02X ", b));
            }
            LOG.debug("Encap Request: " + sb.toString());
        }*/

        // Send the request to get the response.
        IpMessageResponse ipResponse;
        try {
            ipResponse = (IpMessageResponse) conn.send(ipRequest);
            if (ipResponse == null)
                return null;

            /*if(LOG.isDebugEnabled()){
                StringBuilder sb = new StringBuilder();
                for (byte b : Arrays.copyOfRange(ipResponse.getMessageData(),0,ipResponse.getMessageData().length)) {
                    sb.append(String.format("%02X ", b));
                }
                LOG.debug("Response: " + sb.toString());
            }*/
            return ipResponse.getModbusResponse();
        }
        catch (Exception e) {
            if (keepAlive) {
                // The connection may have been reset, so try to reopen it and attempt the message again.
                try {
                    openConnection();
                    ipResponse = (IpMessageResponse) conn.send(ipRequest);
                    if (ipResponse == null)
                        return null;
                    /*if(LOG.isDebugEnabled()){
                        StringBuilder sb = new StringBuilder();
                        for (byte b : Arrays.copyOfRange(ipResponse.getMessageData(),0,ipResponse.getMessageData().length)) {
                            sb.append(String.format("%02X ", b));
                        }
                        LOG.debug("Response: " + sb.toString());
                    }*/
                    return ipResponse.getModbusResponse();
                }
                catch (Exception e2) {
                    closeConnection();
                    throw new ModbusTransportException(e2, request.getSlaveId());
                }
            }

            throw new ModbusTransportException(e, request.getSlaveId());
        }
        finally {
            // Check if we should close the connection.
            if (!keepAlive)
                closeConnection();
        }
    }

    //
    //
    // Private methods
    //
    private void openConnection() throws IOException {
        // Make sure any existing connection is closed.
        closeConnection();

        // Try 'retries' times to get the socket open.
        int retries = getRetries();
        int retryPause = RETRY_PAUSE_START;
        while (true) {
            try {
                socket = new Socket();
                socket.setSoTimeout(getTimeout());
                socket.connect(new InetSocketAddress(ipParameters.getHost(), ipParameters.getPort()), getTimeout());
                if (getePoll() != null)
                    transport = new EpollStreamTransport(socket.getInputStream(), socket.getOutputStream(), getePoll());
                else
                    transport = new StreamTransport(socket.getInputStream(), socket.getOutputStream());
                break;
            }
            catch (IOException e) {
                closeConnection();

                if (retries <= 0)
                    throw e;

                retries--;

                // Pause for a bit.
                try {
                    Thread.sleep(retryPause);
                }
                catch (InterruptedException e1) {
                    // ignore
                }
                retryPause *= 2;
                if (retryPause > RETRY_PAUSE_MAX)
                    retryPause = RETRY_PAUSE_MAX;
            }
        }

        BaseMessageParser ipMessageParser;
        WaitingRoomKeyFactory waitingRoomKeyFactory;
        if (ipParameters.isEncapsulated()) {
            ipMessageParser = new EncapMessageParser(true);
            waitingRoomKeyFactory = new EncapWaitingRoomKeyFactory();
        }
        else {
            ipMessageParser = new XaMessageParser(true);
            waitingRoomKeyFactory = new XaWaitingRoomKeyFactory();
        }

        conn = getMessageControl();
        conn.start(transport, ipMessageParser, null, waitingRoomKeyFactory);
        if (getePoll() == null)
            ((StreamTransport) transport).start("Modbus4J TcpMaster");
    }

    private void closeConnection() {
        closeMessageControl(conn);
        try {
            if (socket != null)
                socket.close();
        }
        catch (IOException e) {
            getExceptionHandler().receivedException(e);
        }

        conn = null;
        socket = null;
    }
}
