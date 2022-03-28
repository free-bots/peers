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
    
    Copyright 2008, 2009, 2010, 2011 Yohann Martineau 
*/

package net.sourceforge.peers.media;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

import net.sourceforge.peers.Logger;


public class Capture implements Runnable {
    
    public static final int SAMPLE_SIZE = 16;
    public static final int BUFFER_SIZE = SAMPLE_SIZE * 20;
    
    private final PipedOutputStream rawData;
    private boolean isStopped;
    private final SoundSource soundSource;
    private final Logger logger;
    private final CountDownLatch latch;
    
    public Capture(PipedOutputStream rawData, SoundSource soundSource,
            Logger logger, CountDownLatch latch) {
        this.rawData = rawData;
        this.soundSource = soundSource;
        this.logger = logger;
        this.latch = latch;
        isStopped = false;
    }

    public void run() {
        byte[] buffer;

        try {
            while (!isStopped) {
                buffer = soundSource.readData();
                if (buffer == null) {
                    break;
                }
                int maxWaitForWriteMS = (buffer.length / RtpSender.CONSUMING_BYTES_PER_MS) + 1000; // plus 1 sec to be on the safe size
                long startWrite = System.currentTimeMillis();
                // TODO solve problem about never being able to write the data provided by the SoundSource
                // if the provided byte-array is bigger than the max-capacity of rawData (CaptureRtpSender.PIPE_SIZE)
                while (true) {
                    try {
                        rawData.write(buffer);
                        break;
                    } catch (InterruptedIOException e) {
                        // PipedOutputStream only has 1 sec (hardcoded) patience to be able to write, so if the byte-arrays
                        // provided by soundSource is big enough we may get in trouble. Lets way at least for the time a well
                        // running RtpSender could use consuming the bytes we want to write. Only after that, throw the exception
                        if ((System.currentTimeMillis() - startWrite) > maxWaitForWriteMS) throw e;
                    }
                }
                rawData.flush();
            }
        } catch (IOException e) {
            logger.error("Error writing raw data", e);
        } finally {
            try {
                rawData.close();
            } catch (IOException e) {
                logger.error("Error closing raw data output pipe", e);
            }
            latch.countDown();
            if (latch.getCount() != 0) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    logger.error("interrupt exception", e);
                }
            }
        }
    }

    public synchronized void setStopped(boolean isStopped) {
        this.isStopped = isStopped;
    }

}
