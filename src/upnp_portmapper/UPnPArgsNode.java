/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

/**
 *
 * @author 楠�
 */
public class UPnPArgsNode {

    /**
     * Ex: The Action which Args is belong to.
     */
    public UPnPActionNode belongToAction;
    /**
     * Ex: NewRemoteHost .
     */
    public String nameArg;
    /**
     * Arguments transport direction,In:Action->PC;Out:PC->Action.
     */
    UPnPControlPoint.Direction direction;
    /**
     * related state variable.
     */
    public String relatedStateVariable;
}
//class AUPnPstateVariable{}                                //Reserved for extention