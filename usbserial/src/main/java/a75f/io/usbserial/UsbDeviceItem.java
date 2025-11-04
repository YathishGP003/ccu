package a75f.io.usbserial;


import java.util.Objects;

import javax.annotation.Nullable;

public class UsbDeviceItem {
    public String getSerial() {
        return serial;
    }

    public String getVendor() {
        return vendor;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPort() {
        return port;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setPort(String port) {
        this.port = port;
    }



    private String serial;
    private String vendor;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    private String productId;
    private String protocol;
    private String port;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;

    private BacnetConfig bacnetConfig;
    private ModbusConfig modbusConfig;

    public BacnetConfig getBacnetConfig() {
        return bacnetConfig;
    }

    public void setBacnetConfig(BacnetConfig bacnetConfig) {
        this.bacnetConfig = bacnetConfig;
    }

    public ModbusConfig getModbusConfig() {
        return modbusConfig;
    }

    public void setModbusConfig(ModbusConfig modbusConfig) {
        this.modbusConfig = modbusConfig;
    }

    public UsbDeviceItem(String serial, String vendor, String productId, String protocol, String port, String name) {
        this.serial = serial;
        this.vendor = vendor;
        this.productId = productId;
        this.protocol = protocol;
        this.port = port;
        this.name = name;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UsbDeviceItem other = (UsbDeviceItem) obj;
        return Objects.equals(this.serial, other.serial);
    }

    @Override
    public String toString() {
        return "UsbDeviceItem{" +
                "serial='" + serial + '\'' +
                ", vendor='" + vendor + '\'' +
                ", productId='" + productId + '\'' +
                ", protocol='" + protocol + '\'' +
                ", port='" + port + '\'' +
                ", bacnetConfig=" + bacnetConfig +
                ", modbusConfig=" + modbusConfig +
                '}';
    }

}

