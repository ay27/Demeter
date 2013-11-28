/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

import java.nio.channels.SocketChannel;
import java.util.Vector;

/**
 *
 * @author éª?
 */
public class UPnPDeviceNode {

    /**
     * The father Device which include this Device instance.
     */
    public UPnPDeviceNode belongToDevice;
    /**
     * Is a Root Device.
     */
    public boolean isRootDevice;
    /**
     * Ex: urn:schemas-upnp-org:device:InternetGatewayDevice:1 .
     */
    public String deviceType;
    /**
     * Ex: Tenda Wireless-N Router .
     */
    public String friendlyName;
    /**
     * Ex: InternetGatewayDevice .
     */
    public String deviceTemplate;
    /**
     * Ex: 1 .
     */
    public String tamplateVer;
    /**
     * Ex: http://192.168.0.1:49152/ <br/>
     * ATTENTION:don't forget to keep "/" suffix.
     */
    public String baseURL;
    /**
     * If no LogicDevice,value should be null.
     */
    public Vector<UPnPDeviceNode> listLogicDevices;
    /**
     * If no Service,value should be null.
     */
    public Vector<UPnPServiceNode> listServices;
    /**
     * The SocketChannel which was only for this device.
     */
    public Module_TCPHTTPUni privateChannel;
}
