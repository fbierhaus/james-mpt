/**
 * 
 */
package org.apache.james.mpt;

import java.io.StringReader;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

/**
 * @author fred
 *
 */
public class TestVariableSubstitution extends MockObjectTestCase {

    ProtocolSessionBuilder builder;
    ProtocolSession protocolSession;
    Session session = mock(Session.class);

    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        builder = new ProtocolSessionBuilder();
        protocolSession = new ProtocolSession(new Properties());
    }
    

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testShouldSubstituteVariables() throws Exception{
    	// ServerResponse with capture
    	String serverResponseLine = "S<retention>: a004 OK \\[RETENTION ([0-9]+)\\] XCREATE \\[200\\] Command successful";
    	final String mockServerResponse = "a004 OK [RETENTION 90] XCREATE [200] Command successful";

    	// ClientRequest with variable set in capture above
    	String clientRequestLine = "C: a005 SELECT INBOX ${retention}";
    	final String expectedRequest = "a005 SELECT INBOX 90";
    	
    	// setup our expectations
        checking(new Expectations() {{
        	ignoring (session).start();
        	atLeast(1).of (session).readLine(); will(returnValue(mockServerResponse));
        	atLeast(1).of (session).writeLine(expectedRequest);
            ignoring (session).stop();
        }});        	
    	
        StringReader sr = new StringReader(serverResponseLine + "\n" + clientRequestLine);
    	builder.addProtocolLines("A Script", sr, protocolSession);
    	
    	Session[] mockSessions = {session};
    	protocolSession.runSessions(mockSessions);
    	
    }
    

}
