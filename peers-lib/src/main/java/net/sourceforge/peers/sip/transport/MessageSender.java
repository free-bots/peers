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
    
    Copyright 2007-2013 Yohann Martineau 
*/

package net.sourceforge.peers.sip.transport;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.Timer;
import net.sourceforge.peers.sip.RFC3261;

import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;


public abstract class MessageSender {

    public static final int KEEY_ALIVE_INTERVAL = 25; // seconds

    protected InetAddress inetAddress;
    protected int port;
    protected int localPort;
    private final Config config;
    private final String transportName;
    private final Timer timer;
    protected Logger logger;

    protected MessageSender(int localPort, InetAddress inetAddress,
                            int port, Config config,
                            String transportName, Logger logger) {
        super();
        this.localPort = localPort;
        this.inetAddress = inetAddress;
        this.port = port;
        this.config = config;
        this.transportName = transportName;
        timer = new Timer(getClass().getSimpleName() + " "
                + Timer.class.getSimpleName());
        this.logger = logger;
        //TODO check config
        timer.scheduleAtFixedRate(new KeepAlive(), 0,
                1000 * KEEY_ALIVE_INTERVAL);
    }

    public abstract void sendMessage(SipMessage sipMessage) throws IOException;

    public abstract void sendBytes(byte[] bytes) throws IOException;

    public String getContact() {
        StringBuilder builder = new StringBuilder();
        InetAddress myAddress = config.getPublicInetAddress();
        if (myAddress == null) {
            myAddress = config.getLocalInetAddress();
        }
        builder.append(myAddress.getHostAddress());
        builder.append(RFC3261.TRANSPORT_PORT_SEP);
        //builder.append(config.getSipPort());
        builder.append(localPort);
        builder.append(RFC3261.PARAM_SEPARATOR);
        builder.append(RFC3261.PARAM_TRANSPORT);
        builder.append(RFC3261.PARAM_ASSIGNMENT);
        builder.append(transportName);
        return builder.toString();
    }

    public int getLocalPort() {
        return localPort;
    }

    public void stopKeepAlives() {
        timer.cancel();
    }

    class KeepAlive extends TimerTask {

        @Override
        public void run() {
            byte[] bytes = (RFC3261.CRLF + RFC3261.CRLF).getBytes();
            try {
                sendBytes(bytes);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

    }

}
