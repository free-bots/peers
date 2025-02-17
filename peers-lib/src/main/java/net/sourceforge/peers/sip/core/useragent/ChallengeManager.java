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
    
    Copyright 2008-2013 Yohann Martineau 
*/

package net.sourceforge.peers.sip.core.useragent;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sip.RFC2617;
import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.syntaxencoding.*;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipMessage;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class ChallengeManager implements MessageInterceptor {

    public static final String ALGORITHM_MD5 = "MD5";

    private String username;
    private String password;
    private String realm;
    private String nonce;
    private String opaque;
    private String requestUri;
    private String digest;
    private String profileUri;
    private String qop;
    private String cnonce;
    private String authorizationUsername;

    private static volatile int nonceCount = 1;
    private String nonceCountHex;

    private final Config config;
    private final Logger logger;

    // FIXME what happens if a challenge is received for a register-refresh
    //       and another challenge is received in the mean time for an invite?
    private int statusCode;
    private SipHeaderFieldValue contact;

    private final InitialRequestManager initialRequestManager;
    private final MidDialogRequestManager midDialogRequestManager;
    private final DialogManager dialogManager;

    public ChallengeManager(Config config,
                            InitialRequestManager initialRequestManager,
                            MidDialogRequestManager midDialogRequestManager,
                            DialogManager dialogManager, Logger logger) {
        this.config = config;
        this.initialRequestManager = initialRequestManager;
        this.midDialogRequestManager = midDialogRequestManager;
        this.dialogManager = dialogManager;
        this.logger = logger;
        init();
    }

    private void init() {
        username = config.getUserPart();
        authorizationUsername = config.getAuthorizationUsername();
        if (authorizationUsername == null || authorizationUsername.isEmpty()) {
            authorizationUsername = username;
        }
        password = config.getPassword();
        profileUri = RFC3261.SIP_SCHEME + RFC3261.SCHEME_SEPARATOR
                + username + RFC3261.AT + config.getDomain();
    }

    private String md5hash(String message) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(ALGORITHM_MD5);
        } catch (NoSuchAlgorithmException e) {
            logger.error("no such algorithm " + ALGORITHM_MD5, e);
            return null;
        }
        byte[] messageBytes = message.getBytes();
        byte[] messageMd5 = messageDigest.digest(messageBytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        for (byte b : messageMd5) {
            int u_b = (b < 0) ? 256 + b : b;
            printStream.printf("%02x", u_b);
        }
        return out.toString();
    }

    public void handleChallenge(SipRequest sipRequest,
                                SipResponse sipResponse) {
        init();
        statusCode = sipResponse.getStatusCode();
        SipHeaders responseHeaders = sipResponse.getSipHeaders();
        SipHeaders requestHeaders = sipRequest.getSipHeaders();
        contact = requestHeaders.get(
                new SipHeaderFieldName(RFC3261.HDR_CONTACT));
        SipHeaderFieldValue authenticate;
        SipHeaderFieldName authenticateHeaderName;
        if (statusCode == RFC3261.CODE_401_UNAUTHORIZED) {
            authenticateHeaderName = new SipHeaderFieldName(
                    RFC3261.HDR_WWW_AUTHENTICATE);
        } else if (statusCode == RFC3261.CODE_407_PROXY_AUTHENTICATION_REQUIRED) {
            authenticateHeaderName = new SipHeaderFieldName(
                    RFC3261.HDR_PROXY_AUTHENTICATE);
        } else {
            return;
        }
        authenticate = responseHeaders.get(authenticateHeaderName);
        if (authenticate == null) {
            return;
        }
        if (!authenticate.getValue().startsWith(RFC2617.SCHEME_DIGEST)) {
            logger.info("unsupported challenge scheme in header: "
                    + authenticate);
            return;
        }
        String headerValue = authenticate.getValue();
        realm = getParameter(RFC2617.PARAM_REALM, headerValue);
        nonce = getParameter(RFC2617.PARAM_NONCE, headerValue);
        opaque = getParameter(RFC2617.PARAM_OPAQUE, headerValue);
        qop = getParameter(RFC2617.PARAM_QOP, headerValue);
        if ("auth".equals(qop)) {
            nonceCountHex = String.format("%08X", nonceCount++);
        }
        String method = sipRequest.getMethod();
        requestUri = sipRequest.getRequestUri().toString();
        cnonce = UUID.randomUUID().toString();
        digest = getRequestDigest(method);

        // FIXME message should be copied "as is" not created anew from scratch
        // and this technique is not clean
        String callId = responseHeaders.get(
                new SipHeaderFieldName(RFC3261.HDR_CALLID)).getValue();
        Dialog dialog = dialogManager.getDialog(callId);
        if (dialog != null) {
            midDialogRequestManager.generateMidDialogRequest(
                    dialog, RFC3261.METHOD_BYE, this);
        } else {
            SipHeaderFieldValue from = requestHeaders.get(
                    new SipHeaderFieldName(RFC3261.HDR_FROM));
            String fromTag = from.getParam(new SipHeaderParamName(
                    RFC3261.PARAM_TAG));
            try {
                initialRequestManager.createInitialRequest(
                        requestUri, method, profileUri, callId, fromTag, this);
            } catch (SipUriSyntaxException e) {
                logger.error("syntax error", e);
            }
        }
    }

    private String getRequestDigest(String method) {
        StringBuilder builder = new StringBuilder();
        builder.append(authorizationUsername);
        builder.append(RFC2617.DIGEST_SEPARATOR);
        builder.append(realm);
        builder.append(RFC2617.DIGEST_SEPARATOR);
        builder.append(password);
        String ha1 = md5hash(builder.toString());
        builder = new StringBuilder();
        builder.append(method);
        builder.append(RFC2617.DIGEST_SEPARATOR);
        builder.append(requestUri);
        String ha2 = md5hash(builder.toString());
        builder = new StringBuilder();
        builder.append(ha1);
        builder.append(RFC2617.DIGEST_SEPARATOR);
        builder.append(nonce);
        builder.append(RFC2617.DIGEST_SEPARATOR);
        if ("auth".equals(qop)) {
            builder.append(nonceCountHex);
            builder.append(RFC2617.DIGEST_SEPARATOR);
            builder.append(cnonce);
            builder.append(RFC2617.DIGEST_SEPARATOR);
            builder.append(qop);
            builder.append(RFC2617.DIGEST_SEPARATOR);
        }
        builder.append(ha2);
        return md5hash(builder.toString());
    }

    private String getParameter(String paramName, String header) {
        int paramPos = header.indexOf(paramName);
        if (paramPos < 0) {
            return null;
        }
        int paramNameLength = paramName.length();
        if (paramPos + paramNameLength + 3 > header.length()) {
            logMalformedHeader();
            return null;
        }
        if (header.charAt(paramPos + paramNameLength) !=
                RFC2617.PARAM_VALUE_SEPARATOR) {
            logMalformedHeader();
            return null;
        }
        if (header.charAt(paramPos + paramNameLength + 1) !=
                RFC2617.PARAM_VALUE_DELIMITER) {
            logMalformedHeader();
            return null;
        }
        header = header.substring(paramPos + paramNameLength + 2);
        int endDelimiter = header.indexOf(RFC2617.PARAM_VALUE_DELIMITER);
        if (endDelimiter < 0) {
            logMalformedHeader();
            return null;
        }
        return header.substring(0, endDelimiter);
    }

    private void logMalformedHeader() {
        logger.info("Malformed " + RFC3261.HDR_WWW_AUTHENTICATE + " header");
    }

    /**
     * add xxxAuthorization header
     */
    public void postProcess(SipMessage sipMessage) {
        if (realm == null || nonce == null || digest == null) {
            return;
        }
        SipHeaders sipHeaders = sipMessage.getSipHeaders();
        String cseq = sipHeaders.get(
                new SipHeaderFieldName(RFC3261.HDR_CSEQ)).getValue();
        String method = cseq.substring(cseq.trim().lastIndexOf(' ') + 1);
        digest = getRequestDigest(method);
        StringBuilder buf = new StringBuilder();
        buf.append(RFC2617.SCHEME_DIGEST).append(" ");
        appendParameter(buf, RFC2617.PARAM_USERNAME, authorizationUsername);
        buf.append(RFC2617.PARAM_SEPARATOR).append(" ");
        appendParameter(buf, RFC2617.PARAM_REALM, realm);
        buf.append(RFC2617.PARAM_SEPARATOR).append(" ");
        appendParameter(buf, RFC2617.PARAM_NONCE, nonce);
        buf.append(RFC2617.PARAM_SEPARATOR).append(" ");
        appendParameter(buf, RFC2617.PARAM_URI, requestUri);
        buf.append(RFC2617.PARAM_SEPARATOR).append(" ");
        appendParameter(buf, RFC2617.PARAM_RESPONSE, digest);
        if ("auth".equals(qop)) {
            buf.append(RFC2617.PARAM_SEPARATOR).append(" ");
            appendParameter(buf, RFC2617.PARAM_NC, nonceCountHex);
            buf.append(RFC2617.PARAM_SEPARATOR).append(" ");
            appendParameter(buf, RFC2617.PARAM_CNONCE, cnonce);
            buf.append(RFC2617.PARAM_SEPARATOR).append(" ");
            appendParameter(buf, RFC2617.PARAM_QOP, qop);
        }
        if (opaque != null) {
            buf.append(RFC2617.PARAM_SEPARATOR).append(" ");
            appendParameter(buf, RFC2617.PARAM_OPAQUE, opaque);
        }
        SipHeaderFieldName authorizationName;
        if (statusCode == RFC3261.CODE_401_UNAUTHORIZED) {
            authorizationName = new SipHeaderFieldName(
                    RFC3261.HDR_AUTHORIZATION);
        } else if (statusCode == RFC3261.CODE_407_PROXY_AUTHENTICATION_REQUIRED) {
            authorizationName = new SipHeaderFieldName(
                    RFC3261.HDR_PROXY_AUTHORIZATION);
        } else {
            return;
        }
        sipHeaders.add(authorizationName,
                new SipHeaderFieldValue(buf.toString()));
        // manage authentication on unregister challenge...
        if (contact != null) {
            SipHeaderParamName expiresName =
                    new SipHeaderParamName(RFC3261.PARAM_EXPIRES);
            String expiresString = contact.getParam(expiresName);
            if (expiresString != null && Integer.parseInt(expiresString) == 0) {
                SipHeaderFieldValue requestContact =
                        sipHeaders.get(new SipHeaderFieldName(RFC3261.HDR_CONTACT));
                requestContact.addParam(expiresName, expiresString);
            }
        }
    }

    private void appendParameter(StringBuilder builder, String name, String value) {
        builder.append(name);
        builder.append(RFC2617.PARAM_VALUE_SEPARATOR);
        builder.append(RFC2617.PARAM_VALUE_DELIMITER);
        builder.append(value);
        builder.append(RFC2617.PARAM_VALUE_DELIMITER);
    }

}
