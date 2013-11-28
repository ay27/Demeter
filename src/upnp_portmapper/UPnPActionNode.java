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
public class UPnPActionNode {

    /**
     * Ex: The Service which Action is belong to.
     */
    public UPnPServiceNode belongToService;
    /**
     * Ex: AddPortMapping.
     */
    public String name;
    /**
     * Ex: If no Arguments,value should be null.
     */
    public Vector<UPnPArgsNode> listArguments;
    //public Vector<UPnPServiceStateNode> serviceStateTable;
}