package net.sourceforge.peers;

import net.sourceforge.peers.media.MediaMode;
import net.sourceforge.peers.media.SoundSource;
import net.sourceforge.peers.rtp.RFC3551;
import net.sourceforge.peers.sdp.Codec;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * With this configuration only username, domain and password needs to be overwritten
 */
public abstract class SimpleConfig implements Config {
    private InetAddress publicIpAddress;

    @Override
    public void save() {

    }

    @Override
    public InetAddress getLocalInetAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public InetAddress getPublicInetAddress() {
        return publicIpAddress;
    }

    @Override
    public SipURI getOutboundProxy() {
        return null;
    }

    @Override
    public int getSipPort() {
        return 0;
    }

    @Override
    public MediaMode getMediaMode() {
        return MediaMode.captureAndPlayback;
    }

    @Override
    public boolean isMediaDebug() {
        return false;
    }

    @Override
    public SoundSource.DataFormat getMediaFileDataFormat() {
        return null;
    }

    @Override
    public String getMediaFile() {
        return null;
    }

    @Override
    public int getRtpPort() {
        return 0;
    }

    @Override
    public String getAuthorizationUsername() {
        return getUserPart();
    }

    @Override
    public List<Codec> getSupportedCodecs() {
        return Collections.singletonList(
                Codec.builder()
                        .name(RFC3551.PCMA)
                        .payloadType(RFC3551.PAYLOAD_TYPE_PCMA)
                        .build()
        );
    }

    @Override
    public void setLocalInetAddress(InetAddress inetAddress) {
    }

    @Override
    public void setPublicInetAddress(InetAddress inetAddress) {
        this.publicIpAddress = inetAddress;
    }

    @Override
    public void setUserPart(String userPart) {

    }

    @Override
    public void setDomain(String domain) {

    }

    @Override
    public void setPassword(String password) {

    }

    @Override
    public void setOutboundProxy(SipURI outboundProxy) {

    }

    @Override
    public void setSipPort(int sipPort) {

    }

    @Override
    public void setMediaMode(MediaMode mediaMode) {

    }

    @Override
    public void setMediaDebug(boolean mediaDebug) {

    }

    @Override
    public void setMediaFileDataFormat(SoundSource.DataFormat mediaFileDataFormat) {

    }

    @Override
    public void setMediaFile(String mediaFile) {

    }

    @Override
    public void setRtpPort(int rtpPort) {

    }

    @Override
    public void setAuthorizationUsername(String authorizationUsername) {

    }

    @Override
    public void setSupportedCodecs(List<Codec> supportedCodecs) {

    }
}
