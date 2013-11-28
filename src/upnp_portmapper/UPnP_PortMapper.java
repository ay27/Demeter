/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author 楠�
 */
public class UPnP_PortMapper {

    private Bitman_IGD_ControlPoint controlPoint;
    private LinkedList<PortInfo> mappedList;
    //private UPnPActionNode actionNodeAddPortMapping;
    //private UPnPActionNode actionNodeDeletePortMapping;
    private SOAPDescriptor descAPM;
    private SOAPDescriptor descDPM;
    private SOAPDescriptor descEIP;

    public UPnP_PortMapper() throws IOException, IllegalStateException, InterruptedException {
        try {
            controlPoint = new Bitman_IGD_ControlPoint();
            controlPoint.Init();
            controlPoint.DiscoverDevice();
            controlPoint.ResloveDevice();
            controlPoint.ResloveService();
            if (controlPoint.getStatus() != UPnPControlPoint.Status.HighReady) {
                throw new IllegalStateException("Illegal State.");
            }
            mappedList = new LinkedList<PortInfo>();
            UPnPActionNode addPortMapping = controlPoint.GetActionNode("AddPortMapping");
            UPnPActionNode deletePortMapping = controlPoint.GetActionNode("DeletePortMapping");
            UPnPActionNode getExternalIPAddress = controlPoint.GetActionNode("GetExternalIPAddress");

            if ((addPortMapping == null) || (deletePortMapping == null) || (getExternalIPAddress == null)) {
                throw new InterruptedException("Fatal error:Vital action not found.");
            }
            descAPM = new SOAPDescriptor(addPortMapping);
            descDPM = new SOAPDescriptor(deletePortMapping);
            descEIP = new SOAPDescriptor(getExternalIPAddress);
        } catch (IOException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            throw ex;
        }
    }

    public boolean AddPortMapping(int internalPort, int externalPort, int leaseDurationSeconds, UPnPControlPoint.Protocol protocol) {
        //add to mapped list fist****

        descAPM.argsValueArray[0] = Utilities.getURL_IpAddress(descAPM.action.belongToService.belongToDevice.baseURL);
        //descAPM.argsValueArray[1] = new Integer(externalPort).toString();swich to below code \/
        descAPM.argsValueArray[1] = "" + externalPort;
        descAPM.argsValueArray[2] = protocol.toString();
        //descAPM.argsValueArray[3] = new Integer(internalPort).toString();swich to below code \/
        descAPM.argsValueArray[3] = "" + internalPort;
        descAPM.argsValueArray[4] = GetIP.getLocalIpAddress(true);
        descAPM.argsValueArray[5] = "1";
        descAPM.argsValueArray[6] = "id:" + UUID.randomUUID().toString();
        //descAPM.argsValueArray[7] = new Integer(leaseDurationSeconds).toString();swich to below code \/
        descAPM.argsValueArray[7] = "" + leaseDurationSeconds;
        try {
            if (controlPoint.SOAPCall(descAPM)) {
                mappedList.add(new PortInfo(descAPM.argsValueArray[0], externalPort, protocol, internalPort, descAPM.argsValueArray[4], descAPM.argsValueArray[6], leaseDurationSeconds));
                System.out.println("Request apply:" + descAPM.argsValueArray[0] + ":" + externalPort + "->" + descAPM.argsValueArray[4] + ":" + internalPort + " Protocol-" + protocol + "  Mapped,Description:" + descAPM.argsValueArray[6]);
                
            } else {
                
                System.out.println("Map port Request fail,reason:UPnP Device refused");
                return false;
            }
        } catch (IOException ex) {
            System.out.println("Map port request fail,reason:network");//debug
            
            ex.printStackTrace();
            return false;
        } catch (IllegalStateException ex) {
            System.out.println("Map port request fail,reason:class ControlPoint internal error");//debug
            
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean DeletePortMapping(int externalPort, UPnPControlPoint.Protocol protocol) {
        Iterator<PortInfo> portIterator = mappedList.iterator();
        PortInfo port = null;
        while (portIterator.hasNext()) {
            port = portIterator.next();
            if ((port.extPort == externalPort) && (port.protocol == protocol)) {
                descDPM.argsValueArray[0] = Utilities.getURL_IpAddress(descAPM.action.belongToService.belongToDevice.baseURL);
                descDPM.argsValueArray[1] = "" + externalPort;
                descDPM.argsValueArray[2] = protocol.toString();
                try {
                    if (controlPoint.SOAPCall(descDPM)) {
                        System.out.println("Request apply:" + port.remoteHost + ":" + port.extPort + "->" + port.localClient + ":" + port.intPort + " Protocol-" + port.protocol + "  Deleted,Description:" + port.description);
                        
                        portIterator.remove();
                    } else {
                        
                        System.out.println("Delete port request fail,reason:UPnP Device refused");
                        return false;
                    }
                } catch (IOException ex) {
                    System.out.println("Delete port request fail,reason:network");//debug
                    
                    ex.printStackTrace();
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    public int DeleteExistedPortMapping() {
        Iterator<PortInfo> portIterator = mappedList.iterator();
        PortInfo port = null;
        int failSum = 0;
        while (portIterator.hasNext()) {
            port = portIterator.next();
            descDPM.argsValueArray[0] = port.remoteHost;
            descDPM.argsValueArray[1] = "" + port.extPort;
            descDPM.argsValueArray[2] = port.protocol.toString();
            try {
                if (controlPoint.SOAPCall(descDPM)) {
                    System.out.println("Request apply:" + port.remoteHost + ":" + port.extPort + "->" + port.localClient + ":" + port.intPort + " Protocol-" + port.protocol + "  Deleted,Description:" + port.description);
                    
                    portIterator.remove();
                } else {
                    ++failSum;
                }
            } catch (IOException ex) {
                System.out.println("request fail,reason:network");//debug
                
                ex.printStackTrace();
                return -1;
            }
        }
        return failSum;
    }

    public String GetExternalIPAddress() {
        try {
            if (!controlPoint.SOAPCall(descEIP)) {
                return null;
            }
        } catch (IOException ex) {
            System.out.println("request fail,reason:network");//debug
            ex.printStackTrace();
            return null;
        }
        return descEIP.argsValueArray[0];
    }
}
