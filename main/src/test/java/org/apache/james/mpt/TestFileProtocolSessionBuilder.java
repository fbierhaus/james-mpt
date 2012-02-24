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

import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.apache.james.mpt.ProtocolSession.ClientRequest;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class TestFileProtocolSessionBuilder extends MockObjectTestCase {

    private static final String SCRIPT_WITH_VARIABLES = "HELLO ${not} ${foo} WORLD ${bar}";
    private static final String SCRIPT_WITH_FOO_REPLACED_BY_WHATEVER = "HELLO ${not} whatever WORLD ${bar}";
    private static final String SCRIPT_WITH_VARIABLES_INLINED = "HELLO not foo WORLD bar";

    ProtocolSessionBuilder builder = new ProtocolSessionBuilder();
    ProtocolSession protocolSession ;
    Session mockSession = mock(Session.class);
    
    
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }


    
    public void testShouldPreserveContentsWhenNoVariablesSet() throws Exception {
        // expectations
        checking(new Expectations() {{
            one (mockSession).writeLine(SCRIPT_WITH_VARIABLES);
        }});    	
        
        // execute
        protocolSession = new ProtocolSession(new Properties());
        ClientRequest clientRequest = protocolSession.new ClientRequest(SCRIPT_WITH_VARIABLES);
        clientRequest.writeMessage(mockSession);
        
    }

    public void testShouldReplaceVariableWhenSet() throws Exception {
        // expectations
        checking(new Expectations() {{
            one (mockSession).writeLine(SCRIPT_WITH_FOO_REPLACED_BY_WHATEVER);
        }});    	
        
        // execute
        Properties props = new Properties();
        props.setProperty("foo", "whatever");
        protocolSession = new ProtocolSession(props);
        ClientRequest clientRequest = protocolSession.new ClientRequest(SCRIPT_WITH_VARIABLES);
        clientRequest.writeMessage(mockSession);
    }
    
    public void testShouldReplaceAllVariablesWhenSet() throws Exception {
        // expectations
        checking(new Expectations() {{
            one (mockSession).writeLine(SCRIPT_WITH_VARIABLES_INLINED);
        }});    	
        
        // execute
        Properties props = new Properties();
        props.setProperty("bar", "bar");
        props.setProperty("foo", "foo");
        props.setProperty("not", "not");
        protocolSession = new ProtocolSession(props);
        ClientRequest clientRequest = protocolSession.new ClientRequest(SCRIPT_WITH_VARIABLES);
        clientRequest.writeMessage(mockSession);
    }
    
    public void testShouldReplaceVariableAtBeginningAndEnd() throws Exception {
        // expectations
        checking(new Expectations() {{
            one (mockSession).writeLine("whatever Some Other Scriptwhateverwhatever");
        }});    	
        
        // execute
        Properties props = new Properties();
        props.setProperty("foo", "whatever");
        protocolSession = new ProtocolSession(props);
        ClientRequest clientRequest = protocolSession.new ClientRequest("${foo} Some Other Script${foo}${foo}");
        clientRequest.writeMessage(mockSession);
    }
    
    public void testShouldIgnoreNotQuiteVariables() throws Exception {
        final String NEARLY = "{foo}${}${foo Some Other Script${foo}";
        // expectations
        checking(new Expectations() {{
            one (mockSession).writeLine(NEARLY);
        }});    	
        
        // execute
        Properties props = new Properties();
        props.setProperty("foo", "whatever");
        protocolSession = new ProtocolSession(props);
        ClientRequest clientRequest = protocolSession.new ClientRequest(NEARLY);
        clientRequest.writeMessage(mockSession);
    }
    
    public void testOneVaribleName(){
    	String line = "S<foo>: a001 LOGIN";
    	String expected = "foo";
    	List<String> variableNames = builder.getVariableNames(line);
    	assertEquals(expected, variableNames.get(0));
    }
    
    public void testTwoVaribleNames(){
    	String line = "S<foo><bar>: a001 LOGIN";
    	String expected1 = "foo";
    	String expected2 = "bar";
    	List<String> variableNames = builder.getVariableNames(line);
    	assertEquals(expected1, variableNames.get(0));
    	assertEquals(expected2, variableNames.get(1));
    }    
    
    public void testGetServerMessage(){
    	String line = "S<foo><bar>: a001 LOGIN";
    	String expected = "a001 LOGIN";
    	String actual = builder.getServerMessage(line);
    	assertEquals(expected, actual);
    }
    
}
