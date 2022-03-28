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

package net.sourceforge.peers;

import net.sourceforge.peers.sip.Utils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogger implements Logger {

    public static final String LOG_FILE = File.separator + "logs"
            + File.separator + "peers.log";
    public static final String NETWORK_FILE = File.separator + "logs"
            + File.separator + "transport.log";

    private PrintWriter logWriter;
    private PrintWriter networkWriter;
    private final Object logMutex;
    private final Object networkMutex;
    private final SimpleDateFormat logFormatter;
    private final SimpleDateFormat networkFormatter;

    public FileLogger(String peersHome) {
        if (peersHome == null) {
            peersHome = Utils.DEFAULT_PEERS_HOME;
        }
        try {
            logWriter = new PrintWriter(new BufferedWriter(
                    new FileWriter(peersHome + LOG_FILE)));
            networkWriter = new PrintWriter(new BufferedWriter(
                    new FileWriter(peersHome + NETWORK_FILE)));
        } catch (IOException e) {
            System.out.println("logging to stdout");
            logWriter = new PrintWriter(System.out);
            networkWriter = new PrintWriter(System.out);
        }
        logMutex = new Object();
        networkMutex = new Object();
        logFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        networkFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    }

    @Override
    public final void debug(String message) {
        synchronized (logMutex) {
            logWriter.write(genericLog(message, "DEBUG"));
            logWriter.flush();
        }
    }

    @Override
    public final void info(String message) {
        synchronized (logMutex) {
            logWriter.write(genericLog(message, "INFO "));
            logWriter.flush();
        }
    }

    @Override
    public final void error(String message) {
        synchronized (logMutex) {
            logWriter.write(genericLog(message, "ERROR"));
            logWriter.flush();
        }
    }

    @Override
    public final void error(String message, Exception exception) {
        synchronized (logMutex) {
            logWriter.write(genericLog(message, "ERROR"));
            exception.printStackTrace(logWriter);
            logWriter.flush();
        }
    }

    private String genericLog(String message, String level) {
        return logFormatter.format(new Date()) +
                " " +
                level +
                " [" +
                Thread.currentThread().getName() +
                "] " +
                message +
                "\n";
    }

    @Override
    public final void traceNetwork(String message, String direction) {
        synchronized (networkMutex) {
            String formattedMessage = networkFormatter.format(new Date()) +
                    " " +
                    direction +
                    " [" +
                    Thread.currentThread().getName() +
                    "]\n\n" +
                    message +
                    "\n";
            networkWriter.write(formattedMessage);
            networkWriter.flush();
        }
    }

}
