package org.bitman.demeter;


import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 这个类主要用于获取手机的公网地址，包括IPv4和IPv6
 * @author ay27
 *
 */

public class Utilities {
	private static Pattern VALID_IPV4_PATTERN = null;
	private static Pattern VALID_IPV6_PATTERN = null;
	private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
	private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

	static {
		try {
			VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
			VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
		}
	}

	public static boolean isIpAddress(String ipAddress) {
		Matcher m1 = Utilities.VALID_IPV4_PATTERN.matcher(ipAddress);
		if (m1.matches()) {
			return true;
		}
		Matcher m2 = Utilities.VALID_IPV6_PATTERN.matcher(ipAddress);
		return m2.matches();
	}

	public static boolean isIpv4Address(String ipAddress) {
		Matcher m1 = Utilities.VALID_IPV4_PATTERN.matcher(ipAddress);
		return m1.matches();
	}

	public static boolean isIpv6Address(String ipAddress) {
		Matcher m1 = Utilities.VALID_IPV6_PATTERN.matcher(ipAddress);
		return m1.matches();
	}

	public static String getLocalIpAddress(boolean removeIPv6) {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() &&
                            !inetAddress.isAnyLocalAddress() &&
                            (!removeIPv6 || isIpv4Address(inetAddress.getHostAddress().toString())))
                            return inetAddress.getHostAddress().toString();
				}
			}
		} catch (SocketException ignore) {}
		return null;
	}


}
