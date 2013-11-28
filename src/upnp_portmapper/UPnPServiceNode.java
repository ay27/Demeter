/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

import java.util.Vector;

/**
 *
 * @author éª?
 */
public class UPnPServiceNode {

    /**
     * The Device which Service is belong to.
     */
    public UPnPDeviceNode belongToDevice;
    /**
     * Ex: urn:upnp-org:serviceId:WANCommonIFC1 .
     */
    public String serviceID;
    /**
     * Ex: urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1 .
     */
    public String serviceType;
    /**
     * Ex: WANIPConnection .
     */
    public String serviceTemplate;
    /**
     * Ex: 1 .
     */
    public String tamplateVer;
    /**
     * Got from device describe XML<br/>
     * Ex: "/event"<br/>
     * ATTENTION:don't forget to keep "/" perfix.
     */
    public String controlURL;
    /**
     * Got from device describe XML<br/>
     * Ex: "/control"<br/>
     * ATTENTION:don't forget to keep "/" perfix.
     */
    public String eventSubURL;
    /**
     * Got from device describe XML<br/>
     * Ex: "/WFAWLANConfigSCPD.xml"<br/>
     * ATTENTION:don't forget to keep "/" perfix.
     */
    public String SCPDURL;
    /**
     * If no Actions,value should be null.
     */
    public Vector<UPnPActionNode> listAction;
    //public Vector<AUPnPstateVariable> listAction;     //Reserved for extention
}