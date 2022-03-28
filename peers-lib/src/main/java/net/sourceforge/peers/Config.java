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

package net.sourceforge.peers;

import java.net.InetAddress;
import java.util.List;

import net.sourceforge.peers.media.MediaMode;
import net.sourceforge.peers.media.SoundSource;
import net.sourceforge.peers.sdp.Codec;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;

public interface Config {

    void save();
    InetAddress getLocalInetAddress();
    InetAddress getPublicInetAddress();
    String getUserPart();
    String getDomain();
    String getPassword();
    SipURI getOutboundProxy();
    int getSipPort();
    MediaMode getMediaMode();
    boolean isMediaDebug();
    SoundSource.DataFormat getMediaFileDataFormat();
    String getMediaFile();
    int getRtpPort();
    String getAuthorizationUsername();
    List<Codec> getSupportedCodecs();
    void setLocalInetAddress(InetAddress inetAddress);
    void setPublicInetAddress(InetAddress inetAddress);
    void setUserPart(String userPart);
    void setDomain(String domain);
    void setPassword(String password);
    void setOutboundProxy(SipURI outboundProxy);
    void setSipPort(int sipPort);
    void setMediaMode(MediaMode mediaMode);
    void setMediaDebug(boolean mediaDebug);
    void setMediaFileDataFormat(SoundSource.DataFormat mediaFileDataFormat);
    void setMediaFile(String mediaFile);
    void setRtpPort(int rtpPort);
    void setAuthorizationUsername(String authorizationUsername);
    void setSupportedCodecs(List<Codec> supportedCodecs);

}
