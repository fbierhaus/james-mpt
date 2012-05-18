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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A protocol session which can be run against a reader and writer, which checks
 * the server response against the expected values. TODO make ProtocolSession
 * itself be a permissible ProtocolElement, so that we can nest and reuse
 * sessions.
 * 
 * @author Darrell DeBoer <darrell@apache.org>
 * 
 * @version $Revision: 776401 $
 */
public class ProtocolSession implements ProtocolInteractor {
    private boolean continued = false;

    private boolean continuationExpected = false;

    protected List<ProtocolElement> testElements = new ArrayList<ProtocolElement>();

    private Iterator<ProtocolElement> elementsIterator;

    private ProtocolElement nextTest;

    private boolean continueAfterFailure = false;
    
    private Properties variables;
    
    private Map<String, Session> sessionMap;
    
    private Session currentSession;
    
    public ProtocolSession(Map<String, Session> sessionMap,Properties variables){
    	this.sessionMap = sessionMap;
    	this.variables = variables;
    }

    public ProtocolSession(Map<String, Session> sessionMap){
    	this.sessionMap = sessionMap;
    	this.variables = new Properties();
    }
    
    public final boolean isContinueAfterFailure() {
        return continueAfterFailure;
    }

    public final void setContinueAfterFailure(boolean continueAfterFailure) {
        this.continueAfterFailure = continueAfterFailure;
    }


    public void setVariables(Properties variables){
    	this.variables.putAll(variables);
    }
    

	@Override
	public Map<String, Session> getSessions() {
		return sessionMap;
	}    
    
    /**
     * Returns the number of sessions required to run this ProtocolSession. If
     * the number of readers and writers provided is less than this number, an
     * exception will occur when running the tests.
     */
    public int getSessionCount() {
        return sessionMap.size();
    }

    /**
     * Executes the ProtocolSession in real time against the readers and writers
     * supplied, writing client requests and reading server responses in the
     * order that they appear in the test elements. The index of a reader/writer
     * in the array corresponds to the number of the session. If an exception
     * occurs, no more test elements are executed.
     * 
     * @param sessions not null
     */
    public void execute() throws Exception {
        elementsIterator = testElements.iterator();
        while (elementsIterator.hasNext()) {
            Object obj = elementsIterator.next();
            if (obj instanceof ProtocolElement) {
                ProtocolElement test = (ProtocolElement) obj;
                test.testProtocol(continueAfterFailure);
            }
        }
    }

    public void doContinue() {
        try {
            if (continuationExpected) {
                continued = true;
                while (elementsIterator.hasNext()) {
                    Object obj = elementsIterator.next();
                    if (obj instanceof ProtocolElement) {
                        nextTest = (ProtocolElement) obj;

                        if (!nextTest.isClient()) {
                            break;
                        }
                        nextTest.testProtocol(continueAfterFailure);
                    }
                }
                if (!elementsIterator.hasNext()) {
                    nextTest = null;
                }
            } else {
                throw new RuntimeException("Unexpected continuation");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#CL(java.lang.String)
     */
    public void CL(String clientLine) {
        testElements.add(new ClientRequest(clientLine));
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#SL(java.lang.String, java.lang.String)
     */
    public void SL(String serverLine, String location) {
        testElements.add(new ServerResponse(serverLine, location));
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#SUB(java.util.List, java.lang.String)
     */
    public void SUB(List<String> serverLines, String location) {
        testElements.add(new ServerUnorderedBlockResponse(serverLines, location));
    }


    /**
     * @see org.apache.james.mpt.ProtocolInteractor#CONT()
     */
    public void CONT() throws Exception {
        testElements.add(new ContinuationElement());
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#SL(int, java.lang.String, java.lang.String, java.lang.String)
     */
    public void SL(String serverLine, String location, String lastClientMessage, String[] variableNames) {
        testElements.add(new ServerResponse(serverLine, location, lastClientMessage, variableNames));
    }
    
    public void SL(String serverLine, String location, String lastClientMessage) {
        SL(serverLine, location, lastClientMessage, null);
    }    

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#SUB(int, java.util.List, java.lang.String, java.lang.String)
     */
    public void SUB(List<String> serverLines, String location, String lastClientMessage) {
        testElements.add(new ServerUnorderedBlockResponse(serverLines, location, lastClientMessage));
    }
    
    public void SLEEP(long millis){
    	testElements.add(new SleepElement(millis));
    }
    
    public void SS(String alias) throws Exception{
    	testElements.add(new SetSessionElement(alias));
    }

    /**
     * A client request, which write the specified message to a Writer.
     */
    public class ClientRequest implements ProtocolElement {

        private String message;

        /**
         * Initialises the ClientRequest, with a message and session number.
         * 
         * @param sessionNumber
         * @param message
         */
        public ClientRequest(String message) {
            this.message = message;
        }

        /**
         * Writes the request message to the PrintWriters. If the sessionNumber ==
         * -1, the request is written to *all* supplied writers, otherwise, only
         * the writer for this session is writted to.
         * 
         * @throws Exception
         */
        public void testProtocol(boolean continueAfterFailure) throws Exception {
            writeMessage(currentSession);
        }

        public void writeMessage(Session session) throws Exception {
            session.writeLine(substituteVariables(message));
        }

        /**
         * Replaces ${<code>NAME</code>} with variable value.
         * @param line not null
         * @return not null
         */
        protected String substituteVariables(String line) {
            if (variables.size() > 0) {
                final StringBuffer buffer = new StringBuffer(line);
                int start = 0;
                int end = 0;
                while (start >= 0 && end >= 0) { 
                    start = buffer.indexOf("${", end);
                    if (start < 0) {
                        break;
                    }
                    end = buffer.indexOf("}", start);
                    if (end < 0) {
                        break;
                    }
                    final String name = buffer.substring(start+2, end);
                    final String value = variables.getProperty(name);
                    if (value != null) {
                        buffer.replace(start, end + 1, value);
                        final int variableLength = (end - start + 2);
                        end = end + (value.length() - variableLength);
                    }
                }
                line = buffer.toString();
            }
            return line;
        }
        
        public boolean isClient() {
            return true;
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
            
            String retValue = "ClientRequest ( "
                + "message = " + this.message + TAB
                + " )";
        
            return retValue;
        }
        
        
    }

    /**
     * Represents a single-line server response, which reads a line from a
     * reader, and compares it with the defined regular expression definition of
     * this line.
     */
    private class ServerResponse implements ProtocolElement {
        private String lastClientMessage;

        private String expectedLine;

        protected String location;
        
        // The variable names to be captured for this line
        protected String[] variableNames;
        
        /**
         * Sets up a server response.
         * 
         * @param expectedPattern
         *            A Perl regular expression pattern used to test the line
         *            recieved.
         * @param location
         *            A descriptive value to use in error messages.
         */
        public ServerResponse(String expectedPattern, String location) {
            this(expectedPattern, location, null, null);
        }

        /**
         * Sets up a server response.
         * 
         * @param sessionNumber
         *            The number of session for a multi-session test
         * @param expectedPattern
         *            A Perl regular expression pattern used to test the line
         *            recieved.
         * @param location
         *            A descriptive value to use in error messages.
         */
        public ServerResponse(String expectedPattern, String location, String lastClientMessage, String[] variableNames) {
            this.expectedLine = expectedPattern;
            this.location = location;
            this.lastClientMessage = lastClientMessage;
            this.variableNames = variableNames;
        }

        /**
         * Reads a line from the supplied reader, and tests that it matches the
         * expected regular expression. If the sessionNumber == -1, then all
         * readers are tested, otherwise, only the reader for this session is
         * tested.
         * 
         * @param out
         *            Is ignored.
         * @param in
         *            The server response is read from here.
         * @throws InvalidServerResponseException
         *             If the actual server response didn't match the regular
         *             expression expected.
         */
        public void testProtocol(boolean continueAfterFailure) throws Exception {
                checkResponse(currentSession, continueAfterFailure);
        }

        protected void checkResponse(Session session, boolean continueAfterFailure) throws Exception {
            String testLine = readLine(session);
            if (!match(expectedLine, testLine)) {
                String errMsg = "\nLocation: " + location + "\nLastClientMsg: "
                        + lastClientMessage + "\nExpected: '" + expectedLine
                        + "'\nActual   : '" + testLine + "'";
                if (continueAfterFailure) {
                    System.out.println(errMsg);
                } else {
                    throw new InvalidServerResponseException(errMsg);
                }
            }
        }

        /**
         * A convenience method which returns true if the actual string matches
         * the expected regular expression.
         * 
         * @param expected
         *            The regular expression used for matching.
         * @param actual
         *            The actual message to match.
         * @return <code>true</code> if the actual matches the expected.
         */
        protected boolean match(String expected, String actual) {
        	boolean result = false;
        	
        	if (variableNames != null){
        		// need to assign some variables from server response
        		Matcher m = Pattern.compile(expected).matcher(actual);
        		int n = 0;
        		while (m.find()){
        			variables.setProperty(variableNames[n], m.group(1));
        			n++;
        		}
        	} 
    		// do the straight match
            result = Pattern.matches(expected, actual);
                
            return result;
        }

        /**
         * Grabs a line from the server and throws an error message if it
         * doesn't work out
         * 
         * @return String of the line from the server
         */
        protected String readLine(Session session) throws Exception {
            try {
                return session.readLine();
            } catch (IOException e) {
                String errMsg = "\nLocation: " + location + "\nExpected: "
                        + expectedLine + "\nReason: Server Timeout.";
                throw new InvalidServerResponseException(errMsg);
            }
        }

        public boolean isClient() {
            return false;
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
            
            String result = "ServerResponse ( "
                + "lastClientMessage = " + this.lastClientMessage + TAB
                + "expectedLine = " + this.expectedLine + TAB
                + "location = " + this.location + TAB
                + " )";
        
            return result;
        }
        
        
    }

    /**
     * Represents a set of lines which must be recieved from the server, in a
     * non-specified order.
     */
    private class ServerUnorderedBlockResponse extends ServerResponse {
        private List<String> expectedLines = new ArrayList<String>();

        /**
         * Sets up a ServerUnorderedBlockResponse with the list of expected
         * lines.
         * 
         * @param expectedLines
         *            A list containing a reqular expression for each expected
         *            line.
         * @param location
         *            A descriptive location string for error messages.
         */
        public ServerUnorderedBlockResponse(List<String> expectedLines, String location) {
            this(expectedLines, location, null);
        }

        /**
         * Sets up a ServerUnorderedBlockResponse with the list of expected
         * lines.
         * 
         * @param sessionNumber
         *            The number of the session to expect this block, for a
         *            multi-session test.
         * @param expectedLines
         *            A list containing a reqular expression for each expected
         *            line.
         * @param location
         *            A descriptive location string for error messages.
         */
        public ServerUnorderedBlockResponse(List<String> expectedLines, String location, String lastClientMessage) {
            super("<Unordered Block>", location, lastClientMessage, null);
            this.expectedLines = expectedLines;
        }

        /**
         * Reads lines from the server response and matches them against the
         * list of expected regular expressions. Each regular expression in the
         * expected list must be matched by only one server response line.
         * 
         * @param reader
         *            Server responses are read from here.
         * @throws InvalidServerResponseException
         *             If a line is encountered which doesn't match one of the
         *             expected lines.
         */
        protected void checkResponse(Session session, boolean continueAfterFailure) throws Exception {
            List<String> testLines = new ArrayList<String>(expectedLines);
            while (testLines.size() > 0) {
                String actualLine = readLine(session);

                boolean foundMatch = false;
                for (int i = 0; i < testLines.size(); i++) {
                    String expected = (String) testLines.get(i);
                    if (match(expected, actualLine)) {
                        foundMatch = true;
                        testLines.remove(expected);
                        break;
                    }
                }

                if (!foundMatch) {
                    StringBuffer errMsg = new StringBuffer().append(
                            "\nLocation: ").append(location).append(
                            "\nExpected one of: ");
                    Iterator<String> iter = expectedLines.iterator();
                    while (iter.hasNext()) {
                        errMsg.append("\n    ");
                        errMsg.append(iter.next());
                    }
                    errMsg.append("\nActual: ").append(actualLine);
                    if (continueAfterFailure) {
                        System.out.println(errMsg.toString());
                    } else {
                        throw new InvalidServerResponseException(errMsg
                                .toString());
                    }
                }
            }
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
            
            String result = "ServerUnorderedBlockResponse ( "
                + "expectedLines = " + this.expectedLines + TAB
                + " )";
        
            return result;
        }
        
        
    }

    private class ContinuationElement implements ProtocolElement {


        public void testProtocol(boolean continueAfterFailure) throws Exception {
            continuationExpected = true;
            continued = false;
            String testLine = currentSession.readLine();
            if (!"+".equals(testLine) || !continued) {
                final String message = "Expected continuation";
                if (continueAfterFailure) {
                    System.out.print(message);
                } else {
                    throw new InvalidServerResponseException(message);
                }
            }
            continuationExpected = false;
            continued = false;

            if (nextTest != null) {
                nextTest.testProtocol(continueAfterFailure);
            }
        }

        public boolean isClient() {
            return false;
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
            String result = "ContinuationElement ()";
        
            return result;
        }
        
        
    }
    
    
    private class SleepElement implements ProtocolElement{
    	
    	private final long millis;
    	
		/**
		 * @param millis
		 */
		public SleepElement(long millis) {
			this.millis = millis;
		}

		/* (non-Javadoc)
		 * @see org.apache.james.mpt.ProtocolSession.ProtocolElement#testProtocol(org.apache.james.mpt.Session[], boolean)
		 */
		@Override
		public void testProtocol(boolean continueAfterFailure) throws Exception {
			Thread.sleep(millis);
		}

		/* (non-Javadoc)
		 * @see org.apache.james.mpt.ProtocolSession.ProtocolElement#isClient()
		 */
		@Override
		public boolean isClient() {
			return false;
		}
    	
	
    }

    private class SetSessionElement implements ProtocolElement{
    	
    	private final String alias;
    	
		/**
		 * @param millis
		 */
		public SetSessionElement(String alias) {
			this.alias = alias;
		}

		/* (non-Javadoc)
		 * @see org.apache.james.mpt.ProtocolSession.ProtocolElement#testProtocol(org.apache.james.mpt.Session[], boolean)
		 */
		@Override
		public void testProtocol(boolean continueAfterFailure) throws Exception {
	    	Session session = sessionMap.get(alias);
	    	if (session == null) {
				throw new Exception("No session found for alias " + alias);
			}
	    	currentSession = session;
		}

		/* (non-Javadoc)
		 * @see org.apache.james.mpt.ProtocolSession.ProtocolElement#isClient()
		 */
		@Override
		public boolean isClient() {
			return false;
		}	
    }
    
    /**
     * Represents a generic protocol element, which may write requests to the
     * server, read responses from the server, or both. Implementations should
     * test the server response against an expected response, and throw an
     * exception on mismatch.
     */
    interface ProtocolElement {
        /**
         * Executes the ProtocolElement against the supplied session.
         * 
         * @param continueAfterFailure true when the execution should continue,
         * false otherwise
         * @throws Exception
         */
        void testProtocol(boolean continueAfterFailure) throws Exception;

        boolean isClient();
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
        StringBuffer sessions = new StringBuffer("(");
        for (Iterator<String> iterator = sessionMap.keySet().iterator(); iterator.hasNext();) {
			String alias = (String) iterator.next();
			sessions.append(alias).append(",");
		}
        sessions.append(")");
        
        String result  = "ProtocolSession ( "
            + "continued = " + this.continued + TAB
            + "continuationExpected = " + this.continuationExpected + TAB
            + "testElements = " + this.testElements + TAB
            + "elementsIterator = " + this.elementsIterator + TAB
            + "sessions = " + sessions.toString() + TAB
            + "nextTest = " + this.nextTest + TAB
            + "continueAfterFailure = " + this.continueAfterFailure + TAB
            + " )";
    
        return result;
    }


    
    
}
