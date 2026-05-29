package network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class LanAddressUtil {
    private LanAddressUtil() {
    }

    public static List<String> localIpv4Addresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        Collections.sort(addresses);
        return addresses;
    }
}
