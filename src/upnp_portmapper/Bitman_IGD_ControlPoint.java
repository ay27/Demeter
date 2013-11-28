/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author 楠�
 */
public class Bitman_IGD_ControlPoint extends UPnPControlPoint {

    private String processingStringOriginal;
    private String processingString;
    private Vector<String> addressList;
    private LinkedList<UPnPDeviceNode> deviceSearchAcc;
    private LinkedList<UPnPServiceNode> serviceSearchAcc;
    private LinkedList<UPnPActionNode> actionSearchAcc;
    public static final String headMSEARCH =
            "M-SEARCH * HTTP/1.1\r\n"
            + "HOST: 239.255.255.250:1900\r\n"
            + "ST: UPnP:rootdevice\r\n"
            + "MAN: \"ssdp:discover\"\r\n"
            + "MX: 3\r\n\r\n";
    /**
     * Before send,Position symbol_XML_NAME and symbol_HOST_NAME_IP need
     * REPLACE.
     */
    public static final String headGET =
            "GET __XML_NAME__ HTTP/1.1\r\n"
            + "User-Agent: Bitman UPnP CP\r\n"
            + "Host: __HOST_NAME_IP__\r\n"
            + "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r\n"
            + "Connection: close\r\n\r\n";
    /**
     * Before send,Position symbol_CONTROL_URL, symbol_SERVICE_ID,
     * symbol_ACTION_NAME, symbol_HOST_NAME_IP, symbol_CONTENT_LENGTH need
     * REPLACE.
     *
     */
    public static final String headSOAP =
            "POST __CONTROL_URL__ HTTP/1.1\r\n"
            + "User-Agent: Bitman UPnP CP\r\n"
            + "Content-type: text/xml;charset=\"utf-8\"\r\n"
            + "Soapaction: \"__SERVICE_ID__#__ACTION_NAME__\"\r\n"
            + "Host: __HOST_NAME_IP__\r\n"
            + "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r\n"
            + "Connection: close\r\n"
            + "Content-Length: __CONTENT_LENGTH__\r\n\r\n";
    /**
     * Before send,Position symbol_SERVICE_ID, symbol_ACTION_NAME,
     * symbol_ACTION_NAME need REPLACE.
     *
     */
    public static final String paylodSOAPxml = "<?xml version=\"1.0\"?>"
            + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n"
            + "<SOAP-ENV:Body>"
            + "<u:__ACTION_NAME__ xmlns:u=\"__SERVICE_ID__\">"
            + "__ARGUMENT_LIST__"
            + "</u:__ACTION_NAME__>"
            + "</SOAP-ENV:Body>"
            + "</SOAP-ENV:Envelope>";
    /**
     * Tag for replacement.<br/>
     * Ex:string.replaceALL(symbol_XML_NAME, "/SCPD.xml");
     */
    public static final String symbol_XML_NAME = "__XML_NAME__";
    /**
     * Tag for replacement.<br/>
     * Ex:string.replaceALL(symbol_HOST_NAME_IP, "192.168.0.1:1900");
     */
    public static final String symbol_HOST_NAME_IP = "__HOST_NAME_IP__";
    /**
     * Tag for replacement.<br/>
     * Ex:string.replaceALL(symbol_CONTROL_URL, aUPnPServiceNode.controlURL);
     */
    public static final String symbol_CONTROL_URL = "__CONTROL_URL__";
    /**
     * Tag for replacement.<br/>
     * Ex:string.replaceALL(symbol_SERVICE_ID, aUPnPServiceNode.serviceType);
     */
    public static final String symbol_SERVICE_ID = "__SERVICE_ID__";
    /**
     * Tag for replacement.<br/>
     * Ex:string.replaceALL(symbol_ACTION_NAME, ActionName);
     */
    public static final String symbol_ACTION_NAME = "__ACTION_NAME__";
    /**
     * Tag for replacement.<br/><br/>
     * Ex:<br/>
     * string.replaceALL(symbol_ARGUMENT_LIST,
     * "&#60argname&#62value&#60/argname&#62");<br/>
     * string.replaceALL(symbol_ARGUMENT_LIST, "");
     */
    public static final String symbol_ARGUMENT_LIST = "__ARGUMENT_LIST__";
    /**
     * Tag for replacement.<br/>
     * Ex:string.replaceALL(symbol_CONTENT_LENGTH,
     * http_Paylod.length().toString());
     */
    public static final String symbol_CONTENT_LENGTH = "__CONTENT_LENGTH__";

    public Bitman_IGD_ControlPoint() throws IOException {
        ChannelUDPMul = new Module_UDPMul();
        rootDeviceList = new Vector<UPnPDeviceNode>();
        addressList = new Vector<String>();
        this.processingString = null;
        this.processingStringOriginal = null;
        this.status = Status.Uninit;
        deviceSearchAcc = new LinkedList<UPnPDeviceNode>();
        serviceSearchAcc = new LinkedList<UPnPServiceNode>();
        actionSearchAcc = new LinkedList<UPnPActionNode>();
    }

    @Override
    public synchronized void Init() {
        if (status == Status.Uninit) {
            status = Status.InitOk;
        }
    }

    @Override
    public synchronized void DiscoverDevice() throws IOException, IllegalStateException {
        if (status != Status.InitOk) {
            throw new IllegalStateException("Illegal State.Do Init() first.");
        }
        ChannelUDPMul.Send(headMSEARCH);
        Vector<String> deviceVector = ChannelUDPMul.RecieveBundle();
        Iterator<String> deviceIterator = deviceVector.iterator();
        String deviceReply = null;

        while (deviceIterator.hasNext()) {
            deviceReply = deviceIterator.next();
            String address = Utilities.SSDP_GetDescXMLAddress(deviceReply);
            if (address == null) {
                continue;
            } else if (addressList.contains(address)) {
                continue;
            }
            addressList.add(address);
        }
        status = Status.DiscovereOK;
    }

    @Override
    public synchronized void ResloveDevice() throws IOException, IllegalStateException {
        if (status != Status.DiscovereOK) {
            throw new IllegalStateException("Illegal State.Do DiscoverDevice() first.");
        } else if (addressList.isEmpty()) {
            throw new IllegalStateException("No UPnP Device found in current network.");
        }
        Module_TCPHTTPUni deviceChannel = null;
        Iterator<String> addressIterator = addressList.iterator();
        while (addressIterator.hasNext()) {
            String address = addressIterator.next();
            try {
                deviceChannel = new Module_TCPHTTPUni(Utilities.getURL_IpAddress(address), new Integer(Utilities.getURL_PortAddress(address)));
                String request = new String(this.headGET);
                //System.out.println(address);
                request = request.replaceAll(symbol_XML_NAME, Utilities.getURL_Resource(address));
                request = request.replaceAll(symbol_HOST_NAME_IP, Utilities.getURL_IpAddress(address) + ":" + Utilities.getURL_PortAddress(address));
                processingStringOriginal = deviceChannel.Session(request.getBytes("UTF-8"));
                processingString = new String(processingStringOriginal);
                //Start reslove XML data
                int[] i = NextTag("device", 2);
                if (i[0] == 1) {
                    doResloveDevice(i[1], true, null, deviceChannel);
                } else {
                    continue;
                }
                //XML Reslove OK
            } catch (IOException ex) {
                throw ex;
            }
        }
        status = Status.ResloveDeviceOk;
    }

    private void doResloveDevice(int startOffset, boolean isRoot, UPnPDeviceNode father, Module_TCPHTTPUni channelTCP) {
        UPnPDeviceNode deviceNode = new UPnPDeviceNode();
        deviceSearchAcc.addFirst(deviceNode);

        int[] i = NextTag("device", startOffset + 2);
        while (i[0] == 1) {//if next is <Tag>,recursion锛宨f is</Tag>,process and return
            doResloveDevice(i[1], false, deviceNode, channelTCP);
            i = NextTag("device", i[1]);
        }

        String xmlDeviceSection = processingString.substring(startOffset, i[1] + 9);
        deviceNode.baseURL = Utilities.MatchTagReluctant(xmlDeviceSection, "baseURL");
        if (deviceNode.baseURL == null) {
            deviceNode.baseURL = "http://" + channelTCP.getIpDst() + ":" + channelTCP.getPortDst();
        } else if (deviceNode.baseURL.endsWith("/")) {
            deviceNode.baseURL = deviceNode.baseURL.substring(0, deviceNode.baseURL.length() - 1);
        }

        deviceNode.deviceType = Utilities.MatchTagReluctant(xmlDeviceSection, "deviceType");
        deviceNode.tamplateVer = deviceNode.deviceType.split(":")[4];
        deviceNode.deviceTemplate = deviceNode.deviceType.split(":")[3];
        deviceNode.friendlyName = Utilities.MatchTagReluctant(xmlDeviceSection, "friendlyName");
        deviceNode.isRootDevice = isRoot;

        deviceNode.privateChannel = channelTCP;

        deviceNode.belongToDevice = father;
        if (deviceNode.belongToDevice != null) {
            if (deviceNode.belongToDevice.listLogicDevices == null) {
                deviceNode.belongToDevice.listLogicDevices = new Vector<UPnPDeviceNode>();
            }
            deviceNode.belongToDevice.listLogicDevices.add(deviceNode);
        }
        deviceNode.listServices = new Vector<UPnPServiceNode>();

        Pattern serviceTagRegex = Pattern.compile("<service>(.*?)</service>", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher serviceTagMatcher = serviceTagRegex.matcher(xmlDeviceSection);
        String xmlServiceSection = null;
        try {
            while (true) {
                serviceTagMatcher.find();
                xmlServiceSection = serviceTagMatcher.group(1);
                UPnPServiceNode serviceNode = new UPnPServiceNode();
                serviceSearchAcc.addFirst(serviceNode);
                serviceNode.belongToDevice = deviceNode;
                serviceNode.controlURL = Utilities.MatchTagReluctant(xmlServiceSection, "controlURL");
                serviceNode.eventSubURL = Utilities.MatchTagReluctant(xmlServiceSection, "eventSubURL");
                serviceNode.SCPDURL = Utilities.MatchTagReluctant(xmlServiceSection, "SCPDURL");
                if (!serviceNode.controlURL.startsWith("/")) {
                    serviceNode.controlURL = "/" + serviceNode.controlURL;
                }
                if (!serviceNode.eventSubURL.startsWith("/")) {
                    serviceNode.eventSubURL = "/" + serviceNode.eventSubURL;
                }
                if (!serviceNode.SCPDURL.startsWith("/")) {
                    serviceNode.SCPDURL = "/" + serviceNode.SCPDURL;
                }
                serviceNode.serviceID = Utilities.MatchTagReluctant(xmlServiceSection, "serviceID");
                serviceNode.serviceType = Utilities.MatchTagReluctant(xmlServiceSection, "serviceType");
                serviceNode.tamplateVer = serviceNode.serviceType.split(":")[4];
                serviceNode.serviceTemplate = serviceNode.serviceType.split(":")[3];
                deviceNode.listServices.add(serviceNode);
            }
        } catch (IllegalStateException ex) {
        }
        if (isRoot) {
            rootDeviceList.add(deviceNode);
        }
        processingString = processingString.substring(0, startOffset) + processingString.substring(i[1] + 9, processingString.length());
        return;
    }

    /**
     * Return a 2-item int array: <br/>
     * Item 0 -- Type: <br/>
     * -1 -&#62 &#60/tagName&#62 or &#60tagName&#62 NotFound, <br/>
     * 0 -&#62 &#60ttagName&#62, <br/>
     * 1 -&#62 &#60/tagName&#62. <br/>
     * <br/>
     * Item 1 -- Offset
     *
     * @param tagName - Tag's name
     * @param startOffset - Start Offset
     * @return [0] - type,[1] - offset
     */
    private int[] NextTag(String tagName, int startOffset) {
        int tagLength = tagName.length() + 2;
        int offset = SearchSvc.KMP_IgnoreCaption(processingString, tagName + ">", startOffset + 1);
        while (offset != -1) {
            if (processingString.substring(offset - 1, offset + tagName.length() + 1).equalsIgnoreCase("<" + tagName + ">")) {
                return new int[]{1, offset - 1};
            } else if (processingString.substring(offset - 2, offset + tagName.length() + 1).equalsIgnoreCase("</" + tagName + ">")) {
                return new int[]{0, offset - 2};
            }
            offset += tagLength;
            offset = SearchSvc.KMP_IgnoreCaption(processingString, "<" + tagName, offset);
        }
        return new int[]{-1, 0};
    }

    @Override
    public synchronized void ResloveService() throws IOException, IllegalStateException {
        if (status != Status.ResloveDeviceOk) {
            throw new IllegalStateException("Illegal State.Do ResloveDevice() first.");
        }
        doResloveService(rootDeviceList);
        status = Status.HighReady;
    }

    public void doResloveService(Vector<UPnPDeviceNode> operateDeviceSet) throws IOException {
        UPnPDeviceNode currentNode = null;
        Iterator<UPnPDeviceNode> deviceIterator = operateDeviceSet.iterator();
        while (deviceIterator.hasNext()) {
            currentNode = deviceIterator.next();
            if (currentNode.listLogicDevices != null) {//if has Logic Device,recursion,if no Logic Device,reocess and return
                doResloveService(currentNode.listLogicDevices);
            }

            //start process service here
            Iterator<UPnPServiceNode> serviceIterator;
            UPnPServiceNode currentService = null;
            if (currentNode.listServices != null) {
                serviceIterator = currentNode.listServices.iterator();
                while (serviceIterator.hasNext()) {
                    currentService = serviceIterator.next();
                    String request = new String(this.headGET);//prepare to get ActionDescXML
                    request = request.replaceAll(symbol_XML_NAME, currentService.SCPDURL);
                    request = request.replaceAll(symbol_HOST_NAME_IP, Utilities.getURL_IpAddress(currentNode.baseURL) + ":" + Utilities.getURL_PortAddress(currentNode.baseURL));
                    processingStringOriginal = currentNode.privateChannel.Session(request.getBytes());

                    if (processingStringOriginal == null) {
                        throw new IOException("Bitman Say:null http reply detected.");
                    } else if (!processingString.startsWith("HTTP/1.1 200 OK")) {
                        throw new IOException("Bitman Say:A error in http reply detected.");
                    }
                    processingString = new String(processingStringOriginal);

                    Pattern tagActionRegex = Pattern.compile("<action>(.*?)</action>", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                    Pattern tagArgumentRegex = Pattern.compile("<argument>(.*?)</argument>", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                    Matcher tagActionMatcher = tagActionRegex.matcher(processingString);
                    Matcher tagArgumentMatcher = null;
                    String xmlActionSection = null;
                    String xmlArgsSection = null;
                    UPnPActionNode currentAction = null;
                    UPnPArgsNode currentArgs = null;
                    try {
                        currentService.listAction = new Vector<UPnPActionNode>();
                        while (true) {
                            tagActionMatcher.find();
                            xmlActionSection = tagActionMatcher.group(1);
                            currentAction = new UPnPActionNode();
                            actionSearchAcc.addFirst(currentAction);
                            currentAction.belongToService = currentService;
                            currentAction.name = Utilities.MatchTagReluctant(xmlActionSection, "name");
                            tagArgumentMatcher = tagArgumentRegex.matcher(xmlActionSection);
                            try {
                                currentAction.listArguments = new Vector<UPnPArgsNode>();
                                while (true) {
                                    tagArgumentMatcher.find();
                                    xmlArgsSection = tagArgumentMatcher.group(1);
                                    currentArgs = new UPnPArgsNode();
                                    currentArgs.belongToAction = currentAction;
                                    String direction = Utilities.MatchTagReluctant(xmlArgsSection, "direction");
                                    if ("out".equalsIgnoreCase(direction)) {
                                        currentArgs.direction = UPnPControlPoint.Direction.out;
                                    } else {
                                        currentArgs.direction = UPnPControlPoint.Direction.in;
                                    }
                                    currentArgs.nameArg = Utilities.MatchTagReluctant(xmlArgsSection, "name");
                                    currentArgs.relatedStateVariable = Utilities.MatchTagReluctant(xmlArgsSection, "relatedStateVariable");
                                    currentAction.listArguments.add(currentArgs);
                                }
                            } catch (Exception e) {
                            }
                            currentService.listAction.add(currentAction);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }

    }

    @Override
    public UPnPDeviceNode GetDeviceNode(String name) throws IllegalStateException {
        if ((status != Status.HighReady) && (status != status.Busy)) {
            throw new IllegalStateException("Illegal State.Ready system first.");
        }
        if (name == null) {
            return null;
        }
        Iterator<UPnPDeviceNode> searchIterator = deviceSearchAcc.iterator();
        UPnPDeviceNode currentdDeviceNode;
        while (searchIterator.hasNext()) {
            currentdDeviceNode = searchIterator.next();
            if (currentdDeviceNode.deviceTemplate.equals(name)) {
                return currentdDeviceNode;
            }
        }
        return null;
    }

    @Override
    public UPnPServiceNode GetServiceNode(String name) {
        if ((status != Status.HighReady) && (status != status.Busy)) {
            throw new IllegalStateException("Illegal State.Ready system first.");
        }
        if (name == null) {
            return null;
        }
        Iterator<UPnPServiceNode> searchIterator = serviceSearchAcc.iterator();
        UPnPServiceNode currentServiceNode;
        while (searchIterator.hasNext()) {
            currentServiceNode = searchIterator.next();
            if (currentServiceNode.serviceTemplate.equals(name)) {
                return currentServiceNode;
            }
        }
        return null;
    }

    @Override
    public UPnPActionNode GetActionNode(String name) {
        if ((status != Status.HighReady) && (status != status.Busy)) {
            throw new IllegalStateException("Illegal State.Ready system first.");
        }
        if (name == null) {
            return null;
        }
        Iterator<UPnPActionNode> searchIterator = actionSearchAcc.iterator();
        UPnPActionNode currentActionNode;
        while (searchIterator.hasNext()) {
            currentActionNode = searchIterator.next();
            if (currentActionNode.name.equals(name)) {
                return currentActionNode;
            }
        }
        return null;
    }

    @Override
    public synchronized boolean SOAPCall(SOAPDescriptor descriptor) throws IOException {
        if (status == Status.Busy) {
            return false;
        } else if (status != Status.HighReady) {
            throw new IllegalStateException("Illegal State.Ready system first.");
        }
        status = Status.Busy;
        String requestHead = headSOAP;
        String requestPaylod = paylodSOAPxml;
        String argNameString = "";
        String reply = null;
        int i = 0;
        boolean isNeedResloveReply = false;
        for (i = 0; i < descriptor.argsSum; ++i) {
            if (descriptor.argsDirectionArray[i] == Direction.in) {
                argNameString += "<" + descriptor.argsNameArray[i] + ">" + descriptor.argsValueArray[i] + "</" + descriptor.argsNameArray[i] + ">";
            } else {
                isNeedResloveReply = true;
                break;
            }
        }

        requestPaylod = requestPaylod.replaceAll(symbol_ACTION_NAME, descriptor.actionName);
        requestPaylod = requestPaylod.replaceAll(symbol_SERVICE_ID, descriptor.serviceType);
        requestPaylod = requestPaylod.replaceAll(symbol_ARGUMENT_LIST, argNameString);

        requestHead = requestHead.replaceAll(symbol_CONTROL_URL, descriptor.action.belongToService.controlURL);
        requestHead = requestHead.replaceAll(symbol_SERVICE_ID, descriptor.serviceType);
        requestHead = requestHead.replaceAll(symbol_ACTION_NAME, descriptor.actionName);
        requestHead = requestHead.replaceAll(symbol_HOST_NAME_IP, Utilities.getURL_IpAddress(descriptor.action.belongToService.belongToDevice.baseURL) + ":" + Utilities.getURL_PortAddress(descriptor.action.belongToService.belongToDevice.baseURL));
        //requestHead = requestHead.replaceAll(symbol_CONTENT_LENGTH, new Integer(requestPaylod.length()).toString());switch to  below code \/
        requestHead = requestHead.replaceAll(symbol_CONTENT_LENGTH, "" + requestPaylod.length());
        try {
            reply = descriptor.action.belongToService.belongToDevice.privateChannel.Session((requestHead + requestPaylod).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            reply = descriptor.action.belongToService.belongToDevice.privateChannel.Session((requestHead + requestPaylod).getBytes());
        } catch (IOException ex) {
            throw ex;
        }

        if (!reply.startsWith("HTTP/1.1 200 OK")) {
            status = Status.HighReady;
            return false;
        }
        if (isNeedResloveReply && (i < descriptor.argsSum)) {
            descriptor.argsValueArray[i] = Utilities.MatchTagReluctant(reply, descriptor.argsNameArray[i]);
        }
        status = status.HighReady;
        return true;
    }

    @Override
    public boolean EventSubscribe(GENADescriptor descriptor) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Status getStatus() {
        return status;
    }
}