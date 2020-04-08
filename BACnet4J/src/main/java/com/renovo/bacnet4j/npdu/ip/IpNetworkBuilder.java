
package com.renovo.bacnet4j.npdu.ip;

import static com.renovo.bacnet4j.npdu.ip.IpNetworkUtils.toIpAddrString;

import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.util.BACnetUtils;

public class IpNetworkBuilder {
    private String localBindAddress = IpNetwork.DEFAULT_BIND_IP;
    private String broadcastAddress;
    private String subnetMask;
    private int port = IpNetwork.DEFAULT_PORT;
    private int localNetworkNumber = Address.LOCAL_NETWORK;
    private boolean reuseAddress = false;

    public IpNetworkBuilder withLocalBindAddress(final String localBindAddress) {
        this.localBindAddress = localBindAddress;
        return this;
    }

    /**
     * Either this method or withSubnet must be called.
     *
     * @param broadcastAddress
     *            the broadcast address for the network
     * @param networkPrefix
     *            the number of bits in the local subnet.
     * @return this
     */
    public IpNetworkBuilder withBroadcast(final String broadcastAddress, final int networkPrefixLength) {
        this.broadcastAddress = broadcastAddress;
        this.subnetMask = toIpAddrString(IpNetworkUtils.createMask(networkPrefixLength));

        return this;
    }

    /**
     * Either this method or withBroadcast must be called.
     *
     * @param subnetAddress
     *            the address of the local subnet, NOT the subnet mask., e.g. 192.168.0.0. The subnet address is
     *            required because the given local bind address could be the wildcard address, i.e. 0.0.0.0, from
     *            which the broadcast address cannot be calculated.
     * @param networkPrefix
     *            the number of bits in the local subnet.
     * @return this
     */
    public IpNetworkBuilder withSubnet(final String subnetAddress, final int networkPrefixLength) {
        final long subnetMask = IpNetworkUtils.createMask(networkPrefixLength);
        this.subnetMask = toIpAddrString(subnetMask);

        final long negMask = ~subnetMask & 0xFFFFFFFFL;
        final long subnet = IpNetworkUtils.bytesToLong(BACnetUtils.dottedStringToBytes(subnetAddress));

        this.broadcastAddress = toIpAddrString(subnet | negMask);

        return this;
    }

    public IpNetworkBuilder withPort(final int port) {
        this.port = port;
        return this;
    }

    public IpNetworkBuilder withLocalNetworkNumber(final int localNetworkNumber) {
        this.localNetworkNumber = localNetworkNumber;
        return this;
    }

    public IpNetworkBuilder withReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
        return this;
    }

    public String getLocalBindAddress() {
        return localBindAddress;
    }

    public String getBroadcastAddress() {
        return broadcastAddress;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public int getPort() {
        return port;
    }

    public int getLocalNetworkNumber() {
        return localNetworkNumber;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public IpNetwork build() {
        if (broadcastAddress == null || subnetMask == null) {
            throw new IllegalArgumentException("Either withBroadcast or withSubnet must be called.");
        }

        return new IpNetwork(port, localBindAddress, broadcastAddress, subnetMask, localNetworkNumber, reuseAddress);
    }
}
