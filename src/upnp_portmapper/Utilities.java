/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author éª?
 */
public class Utilities {

    private static Pattern ipPattern = null;
    private static final String ipRegex = "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\\:([0-9]{1,5})(.*)";

    static {
        try {
            ipPattern = Pattern.compile(ipRegex);
        } catch (PatternSyntaxException e) {
        }
    }

    public static String SSDP_GetDescXMLAddress(String text) {
        if (!text.startsWith("HTTP/1.1 200 OK\r\n")) {
            return null;
        }
        Pattern tagRegex = Pattern.compile("^LOCATION: (.+?)$", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagRegex.matcher(text);
        tagMatcher.find();
        try {
            tagMatcher.group(1);
        } catch (Exception ex) {
            return null;
        }
        return tagMatcher.group(1);
    }

    public static String MatchTagGreedy(String text, String tag) {
        Pattern tagRegex = Pattern.compile("<" + tag + ">(.*)</" + tag + ">", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagRegex.matcher(text);
        tagMatcher.find();
        try {
            tagMatcher.group(1);
        } catch (Exception ex) {
            return null;
        }
        return tagMatcher.group(1);
    }

    public static String MatchTagReluctant(String text, String tag) {
        Pattern tagRegex = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagRegex.matcher(text);
        tagMatcher.find();
        try {
            tagMatcher.group(1);
        } catch (Exception ex) {
            return null;
        }
        return tagMatcher.group(1);
    }

    public static String getURL_IpAddress(String URLAddress) {
        Matcher ip_mat = ipPattern.matcher(URLAddress);
        ip_mat.find();
        try {
            return new String(ip_mat.group(1));
        } catch (Exception ex) {
            System.err.println("Wrong format of URL link.");
            return null;
        }
    }

    public static String getURL_PortAddress(String URLAddress) {
        Matcher ip_mat = ipPattern.matcher(URLAddress);
        ip_mat.find();
        try {
            return new String(ip_mat.group(2));
        } catch (Exception ex) {
            System.err.println("Wrong format of URL link.");
            return null;
        }
    }

    public static String getURL_Resource(String URLAddress) {
        Matcher ip_mat = ipPattern.matcher(URLAddress);
        ip_mat.find();
        try {
            return new String(ip_mat.group(3));
        } catch (Exception ex) {
            System.err.println("Wrong format of URL link.");
            return null;
        }
    }
}