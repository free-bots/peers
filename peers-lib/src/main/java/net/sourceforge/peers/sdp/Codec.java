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
    
    Copyright 2010 Yohann Martineau 
*/

package net.sourceforge.peers.sdp;

import net.sourceforge.peers.media.MediaManager;

import java.util.Objects;

public class Codec {

    private int payloadType;
    private String name;

    public int getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(int payloadType) {
        this.payloadType = payloadType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(payloadType, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Codec)) {
            return false;
        }
        Codec codec = (Codec) obj;
        if (codec.getName() == null) {
            return name == null;
        }
        return codec.getName().equalsIgnoreCase(name);
    }

    @Override
    public String toString() {
        return String.valueOf(RFC4566.TYPE_ATTRIBUTE) + RFC4566.SEPARATOR +
                RFC4566.ATTR_RTPMAP + RFC4566.ATTR_SEPARATOR +
                payloadType + " " + name + "/" +
                MediaManager.DEFAULT_CLOCK + "\r\n";
    }

    public static Codec.Builder builder() {
        return new Codec.Builder();
    }

    public static final class Builder {
        private final Codec codec;

        public Builder() {
            this.codec = new Codec();
        }

        public Builder payloadType(int payloadType) {
            this.codec.setPayloadType(payloadType);
            return this;
        }

        public Builder name(String name) {
            this.codec.setName(name);
            return this;
        }

        public Codec build() {
            return this.codec;
        }
    }
}
