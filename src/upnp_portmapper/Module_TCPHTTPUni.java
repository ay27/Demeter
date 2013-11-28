/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author éª?
 */
public class Module_TCPHTTPUni {

    private SocketChannel channelTCP;
    private Selector selector;
    private ByteBuffer bufferThisChannel;
    private CharsetDecoder decoderThisChannel;
    private String ipDst;
    private int portDst;
    private InetSocketAddress addressDst;

    public Module_TCPHTTPUni(String TargetAddress, int TargetPort) throws IOException {
        ipDst = TargetAddress;
        portDst = TargetPort;
        decoderThisChannel = Charset.defaultCharset().newDecoder();
        addressDst = new InetSocketAddress(ipDst, portDst);
        bufferThisChannel = ByteBuffer.allocate(50000); //this capacity can buffer about 30 packet

    }

    /**
     * This method is blocking,and it will return a String which contian the
     * reply of request.
     *
     * when this method exit,the connection to endpoint will drop,and it will be
     * automatic established at next time call.
     *
     * @param byte[] dat: Request.
     * @return String Reply
     */
    public String Session(byte[] dat) throws IOException {
        while (!EstablishConnection());

        String rtn = new String("");
        Set<SelectionKey> selectionKeysSet = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectionKeysSet.iterator();

        bufferThisChannel.clear();
        bufferThisChannel.put(dat);
        bufferThisChannel.flip();
        try {
            while (bufferThisChannel.hasRemaining()) {
                channelTCP.write(bufferThisChannel);
            }
        } catch (IOException ex) {
            DropConnection();
            throw ex;
        }

        try {
            if(0 == selector.select(30000))
                throw new IllegalStateException("Http request time out.");
            selectionKeysSet = selector.selectedKeys();
            keyIterator = selectionKeysSet.iterator();
            if (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                while (key.isReadable()) {
                    bufferThisChannel.clear();
                    switch (((SocketChannel) key.channel()).read(bufferThisChannel)) {
                        case -1:
                            keyIterator.remove();
                            DropConnection();
                            return rtn;
                        case 0:
                            break;
                        default:
                            bufferThisChannel.flip();
                            rtn = rtn + decoderThisChannel.decode(bufferThisChannel).toString();  //uncertain,debug
                            break;
                    }
                }
                keyIterator.remove();
            }
            DropConnection();
            return null;

        } catch (IOException ex) {
            keyIterator.remove();
            DropConnection();
            throw ex;
        } catch(IllegalStateException ex){
            keyIterator.remove();
            DropConnection();
            throw ex;
        }
    }

    private boolean EstablishConnection() throws IOException {
        try {
            channelTCP = SocketChannel.open();
            channelTCP.socket().bind(new InetSocketAddress(0));
            channelTCP.configureBlocking(false);
            selector = Selector.open();
            channelTCP.connect(addressDst);
            while (!channelTCP.finishConnect());
            channelTCP.register(selector, SelectionKey.OP_READ, bufferThisChannel);
            return true;
        } catch (IOException ex) {
            throw ex;
        }
    }

    private boolean DropConnection() {
        try {
            selector.close();
            channelTCP.close();
            selector = null;
            channelTCP = null;
            return true;
        } catch (IOException ex) {
            ex.printStackTrace(); //exception_deal  this position will hold the exception 
            //because this function may be called when exception has happend
            return false;
        }
    }

    public String getIpDst() {
        return ipDst;
    }

    public int getPortDst() {
        return portDst;
    }
}
