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
package com.x75f.modbus4j.ip.encap;

import com.x75f.modbus4j.base.ModbusUtils;
import com.x75f.modbus4j.exception.ModbusTransportException;
import com.x75f.modbus4j.ip.IpMessageResponse;
import com.x75f.modbus4j.msg.ModbusResponse;
import com.x75f.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>EncapMessageResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class EncapMessageResponse extends EncapMessage implements IpMessageResponse {
    static EncapMessageResponse createEncapMessageResponse(ByteQueue queue) throws ModbusTransportException {
        // Create the modbus response.
        ModbusResponse response = ModbusResponse.createModbusResponse(queue);
        EncapMessageResponse encapResponse = new EncapMessageResponse(response);

        // Check the CRC
        ModbusUtils.checkCRC(encapResponse.modbusMessage, queue);

        return encapResponse;
    }

    /**
     * <p>Constructor for EncapMessageResponse.</p>
     *
     * @param modbusResponse a {@link com.x75f.modbus4j.msg.ModbusResponse} object.
     */
    public EncapMessageResponse(ModbusResponse modbusResponse) {
        super(modbusResponse);
    }

    /**
     * <p>getModbusResponse.</p>
     *
     * @return a {@link com.x75f.modbus4j.msg.ModbusResponse} object.
     */
    public ModbusResponse getModbusResponse() {
        return (ModbusResponse) modbusMessage;
    }
}
