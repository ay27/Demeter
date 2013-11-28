/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

import java.io.IOException;
import java.util.Vector;

/**
 *
 * @author 楠�
 */


public abstract class UPnPControlPoint {
	enum Direction {

	    in, out
	};

	enum Status {

	    Uninit, InitOk, DiscovereOK, ResloveDeviceOk, HighReady, Busy
	};

	enum Protocol {

	    TCP, UDP
	};

    protected Module_UDPMul ChannelUDPMul = null;
    protected Vector<UPnPDeviceNode> rootDeviceList = null;
    protected Status status = Status.Uninit;

    public abstract void Init();

    public abstract void DiscoverDevice() throws IOException;

    public abstract void ResloveDevice() throws IOException;

    //public abstract boolean ResloveDevice(String deviceName);
    //public abstract void ResloveDeviceAll();
    public abstract void ResloveService() throws IOException;

    //public abstract boolean ResloveService(String serviceName);
    //public abstract void ResloveServiceAll();
    public abstract UPnPDeviceNode GetDeviceNode(String name);

    public abstract UPnPServiceNode GetServiceNode(String name);

    public abstract UPnPActionNode GetActionNode(String name);

    public abstract boolean SOAPCall(SOAPDescriptor descriptor) throws IOException;

    public abstract boolean EventSubscribe(GENADescriptor descriptor);
}