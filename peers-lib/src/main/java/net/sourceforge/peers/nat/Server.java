/*
    This file is part of Peers, a java SIP softphone.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2007, 2008, 2009, 2010 Yohann Martineau 
*/

package net.sourceforge.peers.nat;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;

public class Server {

    public static final String SERVER_HOST = "peers.sourceforge.net";
    public static final String PREFIX = "/peers";
    //public static final int SOCKET_TIMEOUT = 30000;//millis

    //private InetAddress localAddress;
    //private int localPort;
    private final InetAddress remoteAddress;
    private final int remotePort;

    private final Socket socket;

    //TODO constructor without parameters
    public Server(InetAddress localAddress, int localPort) throws IOException {
        super();
        //this.localAddress = localAddress;
        //this.localPort = localPort;
        this.remoteAddress = InetAddress.getByName(SERVER_HOST);
        this.remotePort = 80;
        socket = new Socket(remoteAddress, remotePort, localAddress, localPort);
        //socket.setSoTimeout(SOCKET_TIMEOUT);
    }

    /**
     * This method will update public address on the web server.
     *
     * @param email user identifier
     */
    public void update(String email) {
        String encodedEmail;
        try {
            encodedEmail = URLEncoder.encode(email, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        String urlEnd = "update2.php?email=" +
                encodedEmail;
        get(urlEnd);
        close();
    }

    public Document getPeers(String email) {
        String encodedEmail;
        try {
            encodedEmail = URLEncoder.encode(email, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://");
        urlBuilder.append(SERVER_HOST);
        urlBuilder.append(PREFIX);
        urlBuilder.append("/getassocasxml.php?email=");
        urlBuilder.append(encodedEmail);
        URL url;
        try {
            url = new URL(urlBuilder.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        System.out.println("retrieved peers");
        DocumentBuilderFactory documentBuilderFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
        try {
            URLConnection urlConnection = url.openConnection();
            InputStream inputStream = urlConnection.getInputStream();
            return documentBuilder.parse(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String get(String urlEnd) {
        StringBuilder get = new StringBuilder();
        get.append("GET ");
        get.append(PREFIX);
        get.append('/');
        get.append(urlEnd);
        get.append(" HTTP/1.1\r\n");
        get.append("Host: ");
        get.append(SERVER_HOST);
        get.append("\r\n");
        get.append("\r\n");

        try {
            socket.getOutputStream().write(get.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        System.out.println("> sent:\n" + get);

        StringBuilder result = new StringBuilder();
        try {
            byte[] buf = new byte[256];
            int read;
            while ((read = socket.getInputStream().read(buf)) > -1) {
                byte[] exactBuf = new byte[read];
                System.arraycopy(buf, 0, exactBuf, 0, read);
                result.append(new String(exactBuf));
            }
        } catch (SocketTimeoutException e) {
            System.out.println("socket timeout");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        System.out.println("< received:\n" + result);
        return result.toString();
    }

    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
