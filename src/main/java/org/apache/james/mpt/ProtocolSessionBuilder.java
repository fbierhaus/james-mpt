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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A builder which generates scripts from textual input.
 * 
 * @author Darrell DeBoer <darrell@apache.org>
 * 
 * @version $Revision: 776401 $
 */
public class ProtocolSessionBuilder {
    
    public static final String SERVER_CONTINUATION_TAG = "S: \\+";

    public static final String CLIENT_TAG = "C:";

    public static final String SERVER_TAG = "S:";
    
    public static final String SERVER_CAPTURE_TAG = "S<";

    public static final String OPEN_UNORDERED_BLOCK_TAG = "SUB {";

    public static final String CLOSE_UNORDERED_BLOCK_TAG = "}";

    public static final String COMMENT_TAG = "#";

    public static final String SESSION_TAG = "SESSION:";
    
    public static final String SLEEP_TAG = "SLEEP:";

    private final Properties variables;
    
    
    public ProtocolSessionBuilder() {
        variables = new Properties();
    }
    
    /**
     * Sets a substitution varaible.
     * The value of a variable will be substituted whereever
     * ${<code>NAME</code>} is found in the input
     * where <code>NAME</code> is the name of the variable.
     * @param name not null
     * @param value not null
     */
    public void setVariable(final String name, final String value) {
        variables.put(name, value);
    }
    
    /**
     * Builds a ProtocolSession by reading lines from the test file with the
     * supplied name.
     * 
     * @param scriptName
     *            The name of the protocol session file.
     * @return The ProtocolSession
     */
    public ProtocolInteractor buildProtocolSession(String scriptName, Map<String, Session> sessionMap)  throws Exception {
        ProtocolInteractor session = new ProtocolSession(sessionMap, variables);
        addTestFile(scriptName, session);
        return session;
    }
    
    public ProtocolInteractor buildProtocolSession(String scriptName, InputStream is, Map<String, Session> sessionMap)  throws Exception {
        ProtocolInteractor session = new ProtocolSession(sessionMap, variables);
        addProtocolLines(scriptName, is, session);
        return session;
    }    
    
    /**
     * Builds a ProtocolSession by reading lines from the reader.
     * 
     * @param scriptName not null
     * @param reader not null
     * @return The ProtocolSession
     */
    public ProtocolInteractor buildProtocolSession(final String scriptName, final Reader reader, Map<String, Session> sessionMap)  throws Exception {
        ProtocolInteractor session = new ProtocolSession(sessionMap, variables);
        addProtocolLines(scriptName, reader, session);
        return session;
    }


    /**
     * Adds all protocol elements from a test file to the ProtocolSession
     * supplied.
     * 
     * @param fileName
     *            The name of the protocol session file.
     * @param session
     *            The ProtocolSession to add the elements to.
     */
    public void addTestFile(String fileName, ProtocolInteractor session)  throws Exception {
        // Need to find local resource.
        InputStream is = this.getClass().getResourceAsStream(fileName);
        if (is == null) {
            throw new Exception("Test Resource '" + fileName + "' not found.");
        }

        addProtocolLines(fileName, is, session);
    }

    /**
     * Reads ProtocolElements from the supplied InputStream and adds them to the
     * ProtocolSession.
     * @param scriptName
     *            The name of the source file, for error messages.
     * @param is
     *            The input stream containing the protocol definition.
     * @param session
     *            The ProtocolSession to add elements to.
     */
    public void addProtocolLines(String scriptName, InputStream is, ProtocolInteractor session) throws Exception {
    	
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        doAddProtocolLines(session, scriptName, reader);
    }

    /**
     * Reads ProtocolElements from the supplied Reader and adds them to the
     * ProtocolSession.
     * @param scriptName
     *            The name of the source file, for error messages.
     * @param reader
     *            the reader containing the protocol definition.
     * @param session
     *            The ProtocolSession to add elements to.
     */
    public void addProtocolLines(String scriptName, Reader reader, ProtocolInteractor session) throws Exception {
        final BufferedReader bufferedReader;
        if (reader instanceof BufferedReader) {
            bufferedReader = (BufferedReader) reader;
        } else {
            bufferedReader = new BufferedReader(reader);
        }
        doAddProtocolLines(session, scriptName, bufferedReader);
    }
    
    /**
     * Reads ProtocolElements from the supplied Reader and adds them to the
     * ProtocolSession.
     * 
     * @param reader
     *            the reader containing the protocol definition.
     * @param session
     *            The ProtocolSession to add elements to.
     * @param scriptName
     *            The name of the source file, for error messages.
     */
    private void doAddProtocolLines(ProtocolInteractor session, String scriptName, BufferedReader reader) throws Exception {
        String line;
        int lineNumber = -1;
        String lastClientMsg = "";
        while ((line = reader.readLine()) != null) {
            String location = scriptName + ":" + lineNumber;
            if (SERVER_CONTINUATION_TAG.equals(line)) {
                session.CONT();
            } else if (line.startsWith(CLIENT_TAG)) {
                String clientMsg = "";
                if (line.length() > 3) {
                    clientMsg = line.substring(3);
                }
                session.CL(clientMsg);
                lastClientMsg = clientMsg;
            } else if (line.startsWith(SERVER_TAG)) {
                String serverMsg = "";
                if (line.length() > 3) {
                    serverMsg = line.substring(3);
                }
                session.SL(serverMsg, location, lastClientMsg);
            } else if (line.startsWith(SERVER_CAPTURE_TAG)){
            	List<String> variableNames = getVariableNames(line);
            	String serverMsg = getServerMessage(line);
                session.SL(serverMsg, location, lastClientMsg, variableNames.toArray(new String[variableNames.size()]));
            } else if (line.startsWith(OPEN_UNORDERED_BLOCK_TAG)) {
                List<String> unorderedLines = new ArrayList<String>(5);
                line = reader.readLine();

                while (!line.startsWith(CLOSE_UNORDERED_BLOCK_TAG)) {
                    if (!line.startsWith(SERVER_TAG)) {
                        throw new Exception(
                                "Only 'S: ' lines are permitted inside a 'SUB {' block.");
                    }
                    String serverMsg = line.substring(3);
                    unorderedLines.add(serverMsg);
                    line = reader.readLine();
                    lineNumber++;
                }

                session.SUB(unorderedLines, location, lastClientMsg);
            } else if (line.startsWith(COMMENT_TAG) || line.trim().length() == 0) {
                // ignore these lines.
            } else if (line.startsWith(SESSION_TAG)) {
            	String alias = getAlias(line);
            	if ((alias == null) || (alias.length() < 1)) {
					throw new Exception("No host alias specified");
				}
            	session.SS(alias);
            } else if (line.startsWith(SLEEP_TAG)){
            	long millis = getSleepTime(line);
            	session.SLEEP(millis);
            } else {
                String prefix = line;
                if (line.length() > 3) {
                    prefix = line.substring(0, 3);
                }
                throw new Exception("Invalid line prefix: " + prefix);
            }
            lineNumber++;
        }
    }

    protected List<String> getVariableNames(String line){
    	Matcher m = Pattern.compile("<([\\w]+?)>").matcher(line);
        List<String> variableNames = new ArrayList<String>();

    	int n = 0; 
    	while(m.find()){
    		variableNames.add(n, m.group(1));
    		n++;
    	}
    	return variableNames;
    }
    
    protected String getServerMessage(String line){
    	int colonPos = line.indexOf(':');
    	return line.substring(colonPos + 1).trim();
    }
    
    /**
     * Gets the number of seconds the system should sleep and converts to millis.
     * 
     * @param line
     * @return
     */
    protected long getSleepTime(String line){
    	int colonPos = line.indexOf(':');
    	String strVal = line.substring(colonPos + 1).trim();
    	long seconds = 0;
    	try {
			seconds = Integer.parseInt(strVal);
		} catch (NumberFormatException e) {
			return 0;
		}
    	
    	return seconds * 1000;
    }
    
    protected String getAlias(String line){
    	int colonPos = line.indexOf(':');
    	String alias = line.substring(colonPos + 1).trim();
    	return alias;
    	
    }
    

}
