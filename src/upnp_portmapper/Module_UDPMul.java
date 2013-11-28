/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
//import java.net.StandardProtocolFamily;
import java.net.SocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import android.util.Log;

/**
 *
 * @author 楠�
 */
public class Module_UDPMul {

    private DatagramChannel channelUDP;
    private Selector selector;
    private ByteBuffer bufferThisChannel;
    private CharsetDecoder decoderThisChannel;
    private String ipDst;
    private int portDst;
    private InetSocketAddress addressDst;
    private InetSocketAddress addressLocal;
    private static final int RECV_TIMEOUT = 2000;

    public Module_UDPMul() throws IOException {
        ipDst = new String("239.255.255.250");
        portDst = 1900;
        addressDst = new InetSocketAddress(ipDst, portDst);
        addressLocal = new InetSocketAddress(0);
        selector = Selector.open();
        channelUDP = DatagramChannel.open();
        
        channelUDP.socket().bind(addressLocal);      //bind to local:randomPORT
        channelUDP.configureBlocking(false);
        channelUDP.register(selector, SelectionKey.OP_READ);
        decoderThisChannel = Charset.defaultCharset().newDecoder();
        bufferThisChannel = ByteBuffer.allocate(50000); //this capacity can buffer about 30 packet
        bufferThisChannel.clear();
    }

    /**
     * Use this method to send data over this channel.
     *
     * @param String dat: The data want to send
     * @return boolean isSendSuccessfully
     */
    public boolean Send(String dat) {
        bufferThisChannel.clear();
        bufferThisChannel.put(dat.getBytes());
        bufferThisChannel.flip();
        try {
            while (bufferThisChannel.hasRemaining()) {
                channelUDP.send(bufferThisChannel, addressDst);
            }
            return true;
        } catch (IOException ex) {
            Log.e("Send", ex.toString());
            return false;
        }
    }

    /**
     * This method is un-blocking and it will return a Vector contian all data
     * who time gap less then 1000ms
     *
     * @return Vector RecieveData
     */
    public Vector RecieveBundle() {
        Vector<String> rtnPtr = new Vector<String>();
        Set<SelectionKey> selectionKeysSet = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectionKeysSet.iterator();

        try {
            while (0 != selector.select(RECV_TIMEOUT)) {
                selectionKeysSet = selector.selectedKeys();
                keyIterator = selectionKeysSet.iterator();
                if (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isReadable()) {
                        bufferThisChannel.clear();
                        ((DatagramChannel) key.channel()).receive(bufferThisChannel);
                        bufferThisChannel.flip();
                        rtnPtr.add(decoderThisChannel.decode(bufferThisChannel).toString());
                    }
                    keyIterator.remove();
                }
            }
            return rtnPtr;
        } catch (IOException ex) {
            ex.printStackTrace();
            keyIterator.remove();
            return rtnPtr;
        }
    }

    /**
     * Use this method to release all resource this class use.
     */
    public void ReleaseRes() {
        try {
            this.selector.close();
            this.channelUDP.close();
        } catch (IOException ex) {
        }
        this.channelUDP = null;
        this.bufferThisChannel = null;
        this.decoderThisChannel = null;
        this.selector = null;
        this.ipDst = null;
    }
}
