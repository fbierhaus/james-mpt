/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mpt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.apache.tools.ant.BuildException;

final class ExternalSession implements Session {

    /** Number of milliseconds to sleep after empty read */
    private static final int SHORT_WAIT_FOR_INPUT = 10;

    private static final byte[] CRLF = { '\r', '\n' };

    private final Monitor monitor;

    private final ByteBuffer readBuffer;

    private final Charset ascii;

    private final ByteBuffer lineEndBuffer;

    private boolean first = true;

    private final String shabang;
    
    private SocketChannel channel;
    
    private final String alias;
    
    private final String host;
    
    private final int port;

    public ExternalSession(final String alias, final String host, final int port, final Monitor monitor, String shabang) {
        this(alias, host, port, monitor, shabang, false);
    }

    public ExternalSession(final String alias, final String host, final int port, final Monitor monitor, String shabang, boolean debug) {
        super();
        this.alias = alias;
        this.host = host;
        this.port = port;
        this.monitor = monitor;
        readBuffer = ByteBuffer.allocateDirect(2048);
        ascii = Charset.forName("US-ASCII");
        lineEndBuffer = ByteBuffer.wrap(CRLF);
        this.shabang = shabang;
    }

    public String readLine() throws Exception {
        StringBuffer buffer = new StringBuffer();
        readlineInto(buffer);
        final String result;
        if (first && shabang != null) {
            // fake shabang
            monitor.note("<-" + buffer.toString());
            result = shabang;
            first = false;
        } else {
            result = buffer.toString();
            monitor.note("<-" + result);
        }
        return result;
    }

    private void readlineInto(StringBuffer buffer) throws Exception {
        monitor.debug("[Reading line]");
        readBuffer.flip();
        while (oneFromLine(buffer))
            ;
//      May have partial read
        readBuffer.compact();
        monitor.debug("[Done]");
    }

    private boolean oneFromLine(StringBuffer buffer) throws Exception {
        final boolean result;
        if (readBuffer.hasRemaining()) {
            char next = (char) readBuffer.get();
            if (next == '\n') {
                monitor.debug("[LF]");
//              Reached end of the line
                result = false;
            } else if (next == '\r') {
//              CRLF line endings so drop
                monitor.debug("[CR]");
                result = true;
            } else {
//              Load buffer
                monitor.debug(next);
                buffer.append(next);
                result = true;
            }
        } else {
            monitor.debug("[Reading into buffer]");
            readBuffer.clear();
            while (channel.read(readBuffer) == 0) {
//              No response yet
//              Wait a little while
                Thread.sleep(SHORT_WAIT_FOR_INPUT);
            }
//          Reset for transfer into string buffer
            readBuffer.flip();
            monitor.debug("[Done]");
            result = true;
        }
        return result;
    }

    public void start() throws Exception {
    	
    	InetSocketAddress address = new InetSocketAddress(host, port);
		try {
			monitor.note("Connecting " + alias + "....");
			channel = SocketChannel.open(address);
		} catch (IOException e) {
			throw new BuildException("Error opening channel", e);
		}
		
		while (!channel.finishConnect()) {
            monitor.note("connecting " + alias + "...");
            Thread.sleep(2);
        }
    }

    public void stop() throws Exception {
        monitor.note("closing " + alias);
        channel.close();
    }

    public void writeLine(String line) throws Exception {
        monitor.note("-> " + line);
        monitor.debug("[Writing line]");
        ByteBuffer writeBuffer = ascii.encode(line);
        while (writeBuffer.hasRemaining()) {
        	channel.write(writeBuffer);
        }
        lineEndBuffer.rewind();
        while (lineEndBuffer.hasRemaining()) {
        	channel.write(lineEndBuffer);
        }
        monitor.debug("[Done]");
    }
    
    /**
     * Writes binary data to the channel.
     * 
     * @param data
     * @param fileName
     * @throws IOException
     */
    public void writeData(Attachment attachment) throws IOException{
    	monitor.note("-> binary file (" + attachment.getData().length + " bytes): " + attachment.getFilename());
    	monitor.debug("[Writing binary data]");
    	ByteBuffer writeBuffer = ByteBuffer.wrap(attachment.getData());
        while (writeBuffer.hasRemaining()) {
        	channel.write(writeBuffer);
        }    	
        lineEndBuffer.rewind();
        while (lineEndBuffer.hasRemaining()) {
        	channel.write(lineEndBuffer);
        }
        monitor.debug("[Done]");
    }

    /**
     * Constructs a <code>String</code> with all attributes
     * in name = value format.
     *
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        final String TAB = " ";
        
        String result =  "External ( "
            + "alias = " + alias + TAB
            + "monitor = " + this.monitor + TAB
            + "readBuffer = " + this.readBuffer + TAB
            + "ascii = " + this.ascii + TAB
            + "lineEndBuffer = " + this.lineEndBuffer + TAB
            + "first = " + this.first + TAB
            + "shabang = " + this.shabang + TAB
            + " )";
    
        return result;
    }


    
    
}