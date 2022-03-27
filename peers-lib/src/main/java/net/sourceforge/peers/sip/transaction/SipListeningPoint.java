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

package net.sourceforge.peers.sip.transaction;


import java.util.Objects;

public class SipListeningPoint {

    private final int localPort;
    private final String localTransport;

    public SipListeningPoint(int localPort, String localTransport) {
        super();
        this.localPort = localPort;
        this.localTransport = localTransport;
    }

    @Override
    public boolean equals(Object obj) {
        if (Objects.isNull(obj)) {
            return false;
        }
        if (obj.getClass() != SipListeningPoint.class) {
            return false;
        }
        SipListeningPoint other = (SipListeningPoint) obj;
        return localPort == other.localPort &&
                localTransport.equals(other.localTransport);
    }

    @Override
    public String toString() {
        return ":" + localPort + '/' + localTransport;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public int getlocalPort() {
        return localPort;
    }

    public String getlocalTransport() {
        return localTransport;
    }

}
