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

package net.sourceforge.peers.sip.core.useragent;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.FileLogger;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.XmlConfig;
import net.sourceforge.peers.media.*;
import net.sourceforge.peers.rtp.RFC4733;
import net.sourceforge.peers.sdp.SDPManager;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.core.useragent.handlers.ByeHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.CancelHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.InviteHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.OptionsHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.RegisterHandler;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;
import net.sourceforge.peers.sip.syntaxencoding.SipUriSyntaxException;
import net.sourceforge.peers.sip.transaction.Transaction;
import net.sourceforge.peers.sip.transaction.TransactionManager;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipMessage;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import net.sourceforge.peers.sip.transport.TransportManager;

import java.io.File;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;


public class UserAgent implements DtmfEventHandler {

    public static final String CONFIG_FILE = "conf" + File.separator + "peers.xml";
    public static final int RTP_DEFAULT_PORT = 8000;

    private final String peersHome;
    private final Logger logger;
    private final Config config;

    private final List<String> peers;
    //private List<Dialog> dialogs;
    
    //TODO factorize echo and captureRtpSender
    private Echo echo;
    
    private final UAC uac;
    private final UAS uas;

    private final ChallengeManager challengeManager;
    
    private final DialogManager dialogManager;
    private final TransactionManager transactionManager;
    private final TransportManager transportManager;
    private final InviteHandler inviteHandler;

    private int cseqCounter;
    private AbstractSoundManagerFactory abstractSoundManagerFactory;
    private final SipListener sipListener;
    
    private final SDPManager sdpManager;
    private final MediaManager mediaManager;

    public UserAgent(SipListener sipListener, String peersHome, Logger logger)
                    throws SocketException {
        this(sipListener, null, peersHome, logger);
    }

    public UserAgent(SipListener sipListener, Config config, Logger logger)
                    throws SocketException {
        this(sipListener, config, null, logger);
    }

    private UserAgent(SipListener sipListener, Config config, String peersHome, Logger logger)
            throws SocketException {
        this(sipListener, null, config, peersHome, logger);
    }

    public UserAgent(SipListener sipListener, AbstractSoundManagerFactory abstractSoundManagerFactory, Config config, String peersHome, Logger logger)
                    throws SocketException {
        this.sipListener = sipListener;
        this.abstractSoundManagerFactory = abstractSoundManagerFactory;
        if (peersHome == null) {
            peersHome = Utils.DEFAULT_PEERS_HOME;
        }
        this.peersHome = peersHome;
        if (logger == null) {
            logger = new FileLogger(this.peersHome);
        }
        this.logger = logger;
        if (config == null) {
            config = new XmlConfig(this.peersHome + File.separator
                    + CONFIG_FILE, this.logger);
        }
        this.config = config;
        if (abstractSoundManagerFactory == null) {
            abstractSoundManagerFactory = new ConfigAbstractSoundManagerFactory(this.config, this.peersHome, this.logger);
        }
        this.abstractSoundManagerFactory = abstractSoundManagerFactory;

        cseqCounter = 1;

        String information = "starting user agent [" +
                "myAddress: " +
                config.getLocalInetAddress().getHostAddress() + ", " +
                "sipPort: " +
                config.getSipPort() + ", " +
                "userpart: " +
                config.getUserPart() + ", " +
                "domain: " +
                config.getDomain() + "]";
        logger.info(information);

        //transaction user
        
        dialogManager = new DialogManager(logger);
        
        //transaction
        
        transactionManager = new TransactionManager(logger);
        
        //transport
        
        transportManager = new TransportManager(transactionManager,
                config, logger);
        
        transactionManager.setTransportManager(transportManager);
        
        //core
        
        inviteHandler = new InviteHandler(this,
                dialogManager,
                transactionManager,
                transportManager,
                logger);
        CancelHandler cancelHandler = new CancelHandler(this,
                dialogManager,
                transactionManager,
                transportManager,
                logger);
        ByeHandler byeHandler = new ByeHandler(this,
                dialogManager,
                transactionManager,
                transportManager,
                logger);
        OptionsHandler optionsHandler = new OptionsHandler(this,
                transactionManager,
                transportManager,
                logger);
        RegisterHandler registerHandler = new RegisterHandler(this,
                transactionManager,
                transportManager,
                logger);
        
        InitialRequestManager initialRequestManager =
            new InitialRequestManager(
                this,
                inviteHandler,
                cancelHandler,
                byeHandler,
                optionsHandler,
                registerHandler,
                dialogManager,
                transactionManager,
                transportManager,
                logger);
        MidDialogRequestManager midDialogRequestManager =
            new MidDialogRequestManager(
                this,
                inviteHandler,
                cancelHandler,
                byeHandler,
                optionsHandler,
                registerHandler,
                dialogManager,
                transactionManager,
                transportManager,
                logger);
        
        uas = new UAS(this,
                initialRequestManager,
                midDialogRequestManager,
                dialogManager,
                transactionManager,
                transportManager);

        uac = new UAC(this,
                initialRequestManager,
                midDialogRequestManager,
                dialogManager,
                transactionManager,
                transportManager,
                logger);

        challengeManager = new ChallengeManager(config,
                initialRequestManager,
                midDialogRequestManager,
                dialogManager,
                logger);
        registerHandler.setChallengeManager(challengeManager);
        inviteHandler.setChallengeManager(challengeManager);
        byeHandler.setChallengeManager(challengeManager);

        peers = new ArrayList<>();
        //dialogs = new ArrayList<Dialog>();

        sdpManager = new SDPManager(this, logger);
        inviteHandler.setSdpManager(sdpManager);
        optionsHandler.setSdpManager(sdpManager);
        mediaManager = new MediaManager(this, this, logger);
    }
    
    // client methods

    public void close() {
        transportManager.closeTransports();
        transactionManager.closeTimers();
        inviteHandler.closeTimers();
        mediaManager.stopSession();
        config.setPublicInetAddress(null);
    }
    
    public SipRequest register() throws SipUriSyntaxException {
        return uac.register();
    }

    public void unregister() throws SipUriSyntaxException {
        uac.unregister();
    }
    
    public SipRequest invite(String requestUri, String callId)
            throws SipUriSyntaxException {
        return uac.invite(requestUri, callId);
    }
    
    public void terminate(SipRequest sipRequest) {
        uac.terminate(sipRequest);
    }
    
    public void acceptCall(SipRequest sipRequest, Dialog dialog) {
        uas.acceptCall(sipRequest, dialog);
    }
    
    public void rejectCall(SipRequest sipRequest) {
        uas.rejectCall(sipRequest);
    }
    
    
    /**
     * Gives the sipMessage if sipMessage is a SipRequest or 
     * the SipRequest corresponding to the SipResponse
     * if sipMessage is a SipResponse
     * @param sipMessage
     * @return null if sipMessage is neither a SipRequest neither a SipResponse
     */
    public SipRequest getSipRequest(SipMessage sipMessage) {
        if (sipMessage instanceof SipRequest) {
            return (SipRequest) sipMessage;
        } else if (sipMessage instanceof SipResponse) {
            SipResponse sipResponse = (SipResponse) sipMessage;
            Transaction transaction = (Transaction)transactionManager
                .getClientTransaction(sipResponse);
            if (transaction == null) {
                transaction = (Transaction)transactionManager
                    .getServerTransaction(sipResponse);
            }
            if (transaction == null) {
                return null;
            }
            return transaction.getRequest();
        } else {
            return null;
        }
    }
    
//    public List<Dialog> getDialogs() {
//        return dialogs;
//    }

    public List<String> getPeers() {
        return peers;
    }

//    public Dialog getDialog(String peer) {
//        for (Dialog dialog : dialogs) {
//            String remoteUri = dialog.getRemoteUri();
//            if (remoteUri != null) {
//                if (remoteUri.contains(peer)) {
//                    return dialog;
//                }
//            }
//        }
//        return null;
//    }

    public String generateCSeq(String method) {
        return String.valueOf(cseqCounter++) +
                ' ' +
                method;
    }
    
    public boolean isRegistered() {
        return uac.getInitialRequestManager().getRegisterHandler()
            .isRegistered();
    }

    public UAS getUas() {
        return uas;
    }

    public UAC getUac() {
        return uac;
    }

    public DialogManager getDialogManager() {
        return dialogManager;
    }
    
    public int getSipPort() {
        return transportManager.getSipPort();
    }

    public int getRtpPort() {
        return config.getRtpPort();
    }

    public String getDomain() {
        return config.getDomain();
    }

    public String getUserpart() {
        return config.getUserPart();
    }

    public MediaMode getMediaMode() {
        return config.getMediaMode();
    }

    public boolean isMediaDebug() {
        return config.isMediaDebug();
    }

    public SipURI getOutboundProxy() {
        return config.getOutboundProxy();
    }

    public Echo getEcho() {
        return echo;
    }

    public void setEcho(Echo echo) {
        this.echo = echo;
    }

    public AbstractSoundManagerFactory getAbstractSoundManagerFactory() {
        return abstractSoundManagerFactory;
    }

    public SipListener getSipListener() {
        return sipListener;
    }

    public MediaManager getMediaManager() {
        return mediaManager;
    }

    public Config getConfig() {
        return config;
    }

    public String getPeersHome() {
        return peersHome;
    }

    public TransportManager getTransportManager() {
        return transportManager;
    }

    @Override
    public void dtmfDetected(RFC4733.DTMFEvent dtmfEvent, int duration) {
        sipListener.dtmfEvent(dtmfEvent, duration);
    }
}
