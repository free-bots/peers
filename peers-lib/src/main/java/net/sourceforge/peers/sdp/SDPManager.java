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
    
    Copyright 2007, 2008, 2009, 2010, 2012 Yohann Martineau 
*/

package net.sourceforge.peers.sdp;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.rtp.RFC3551;
import net.sourceforge.peers.sip.core.useragent.UserAgent;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

public class SDPManager {

    private final SdpParser sdpParser;
    private final UserAgent userAgent;
    private final List<Codec> supportedCodecs;
    private final Random random;

    private final Logger logger;

    public SDPManager(UserAgent userAgent, Logger logger) {
        this.userAgent = userAgent;
        this.logger = logger;
        sdpParser = new SdpParser();
        supportedCodecs = userAgent.getConfig().getSupportedCodecs();
        random = new Random();
    }

    public SessionDescription parse(byte[] sdp) {
        try {
            return sdpParser.parse(sdp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public MediaDestination getMediaDestination(
            SessionDescription sessionDescription) throws NoCodecException {
        InetAddress destAddress = sessionDescription.getIpAddress();
        List<MediaDescription> mediaDescriptions = sessionDescription.getMediaDescriptions();
        for (MediaDescription mediaDescription : mediaDescriptions) {
            if (RFC4566.MEDIA_AUDIO.equals(mediaDescription.getType())) {
                for (Codec offerCodec : mediaDescription.getCodecs()) {
                    if (supportedCodecs.contains(offerCodec)) {
                        String offerCodecName = offerCodec.getName();
                        if (offerCodecName.equalsIgnoreCase(RFC3551.PCMU) ||
                                offerCodecName.equalsIgnoreCase(RFC3551.PCMA)) {
                            int destPort = mediaDescription.getPort();
                            if (mediaDescription.getIpAddress() != null) {
                                destAddress = mediaDescription.getIpAddress();
                            }
                            MediaDestination mediaDestination =
                                    new MediaDestination();
                            mediaDestination.setDestination(
                                    destAddress.getHostAddress());
                            mediaDestination.setPort(destPort);
                            mediaDestination.setCodec(offerCodec);
                            return mediaDestination;
                        }
                    }
                }
            }
        }
        throw new NoCodecException();
    }

    public SessionDescription createSessionDescription(SessionDescription offer,
                                                       int localRtpPort)
            throws IOException {
        SessionDescription sessionDescription = new SessionDescription();
        sessionDescription.setUsername("user1");
        sessionDescription.setId(random.nextInt(Integer.MAX_VALUE));
        sessionDescription.setVersion(random.nextInt(Integer.MAX_VALUE));
        Config config = userAgent.getConfig();
        InetAddress inetAddress = config.getPublicInetAddress();
        if (inetAddress == null) {
            inetAddress = config.getLocalInetAddress();
        }
        sessionDescription.setIpAddress(inetAddress);
        sessionDescription.setName("-");
        sessionDescription.setAttributes(new Hashtable<>());
        List<Codec> codecs;
        if (offer == null) {
            codecs = supportedCodecs;
        } else {
            codecs = new ArrayList<>();
            for (MediaDescription mediaDescription :
                    offer.getMediaDescriptions()) {
                if (RFC4566.MEDIA_AUDIO.equals(mediaDescription.getType())) {
                    for (Codec codec : mediaDescription.getCodecs()) {
                        if (supportedCodecs.contains(codec)) {
                            codecs.add(codec);
                        }
                    }
                }
            }
        }
        MediaDescription mediaDescription = new MediaDescription();
        Hashtable<String, String> attributes = new Hashtable<>();
        attributes.put(RFC4566.ATTR_SENDRECV, "");
        mediaDescription.setAttributes(attributes);
        mediaDescription.setType(RFC4566.MEDIA_AUDIO);
        mediaDescription.setPort(localRtpPort);
        mediaDescription.setCodecs(codecs);
        List<MediaDescription> mediaDescriptions =
                new ArrayList<>();
        mediaDescriptions.add(mediaDescription);
        sessionDescription.setMediaDescriptions(mediaDescriptions);
        return sessionDescription;
    }

}
