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

package net.sourceforge.peers.media;

import net.sourceforge.peers.Logger;
import net.sourceforge.peers.rtp.*;
import net.sourceforge.peers.sdp.Codec;

import java.io.IOException;

public class IncomingRtpReader implements RtpListener {

    private final RtpSession rtpSession;
    private final AbstractSoundManager soundManager;
    private final Decoder decoder;
    private final DtmfDecoder dtmfDecoder;


    public IncomingRtpReader(RtpSession rtpSession,
                             AbstractSoundManager soundManager, Codec codec, DtmfEventHandler dtmfHandler, Logger logger)
            throws IOException {
        logger.debug("playback codec:" + codec.toString().trim());
        this.rtpSession = rtpSession;
        this.soundManager = soundManager;
        switch (codec.getPayloadType()) {
            case RFC3551.PAYLOAD_TYPE_PCMU:
                decoder = new PcmuDecoder();
                break;
            case RFC3551.PAYLOAD_TYPE_PCMA:
                decoder = new PcmaDecoder();
                break;
            default:
                throw new RuntimeException("unsupported payload type");
        }
        dtmfDecoder = new DtmfDecoder(dtmfHandler);
        rtpSession.addRtpListener(this);
    }

    public void start() {
        rtpSession.start();
    }

    @Override
    public void receivedRtpPacket(RtpPacket rtpPacket) {
        if (rtpPacket.getPayloadType() == RFC4733.PAYLOAD_TYPE_TELEPHONE_EVENT) {
            dtmfDecoder.processPacket(rtpPacket);
        } else {
            byte[] rawBuf = decoder.process(rtpPacket.getData());
            if (soundManager != null) {
                soundManager.writeData(rawBuf, 0, rawBuf.length);
            }
        }
    }

}
