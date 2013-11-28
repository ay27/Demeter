/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

/**
 *
 * @author 楠�
 */
public class PortInfo {

    public String remoteHost = "";
    public int extPort = 0;
    public String localClient = "";
    public int intPort = 0;
    public int leaseDurationSeconds = 3600;
    public UPnPControlPoint.Protocol protocol = UPnPControlPoint.Protocol.TCP;
    public String description = "Default";

    public PortInfo(String remoteHost, int extPort, UPnPControlPoint.Protocol protocol, int intPort, String localClient, String description, int leaseDurationSeconds) {
        this.remoteHost = remoteHost;
        this.extPort = extPort;
        this.protocol = protocol;
        this.intPort = intPort;
        this.localClient = localClient;
        this.description = description;
        this.leaseDurationSeconds = leaseDurationSeconds;
    }
}