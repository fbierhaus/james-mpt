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

import java.util.Iterator;
import java.util.Map;

/**
 * Runs protocol scripts.
 */
public class Runner {
    
    /** The Protocol session which is run before the testElements */
    private final ProtocolSession preElements;

    /** The Protocol session which contains the tests elements */
    private final ProtocolSession testElements;

    /** The Protocol session which is run after the testElements. */
    private final ProtocolSession postElements;
    
    public Runner(){
    	preElements = new ProtocolSession();
    	testElements = new ProtocolSession();
    	postElements = new ProtocolSession();
    }
    
    public void continueAfterFailure() {
        preElements.setContinueAfterFailure(true);
        testElements.setContinueAfterFailure(true);
        postElements.setContinueAfterFailure(true);
    }
    
    /**
     * Gets protocol session run after test.
     * @return not null
     */
    public ProtocolInteractor getPostElements() {
        return postElements;
    }

    /**
     * Gets protocol session run before test.
     * @return not null
     */
    public ProtocolInteractor getPreElements() {
        return preElements;
    }
    /**
     * Gets protocol session run on test.
     * @return not null
     */
    public ProtocolInteractor getTestElements() {
        return testElements;
    }



    /**
     * <p>Runs the pre,test and post protocol sessions against a local copy of the
     * server. This does not require that James be running, and is useful
     * for rapid development and debugging.
     * </p><p>
     * Instead of sending requests to a socket connected to a running instance
     * of James, this method uses the {@link HostSystem} to simplify
     * testing. One mock instance is required per protocol session/connection.
     */
    public void runSessions(Map<String, Session> sessionMap) throws Exception {
    	// first, ensure test elements have the session map
    	setSessionMap(sessionMap);
    	
    	// start all the sessions
    	for (Iterator<Session> iterator = sessionMap.values().iterator(); iterator.hasNext();) {
			Session session = (Session) iterator.next();
			session.start();
		}
    	
        try {
            testElements.execute();
        } finally {
        	// stop all the sessions
        	for (Iterator<Session> iterator = sessionMap.values().iterator(); iterator.hasNext();) {
    			Session session = (Session) iterator.next();
    			session.stop();
    		}
        }
    }
    
    protected void setSessionMap(Map<String, Session> sessionMap){
    	preElements.setSessionMap(sessionMap);
    	testElements.setSessionMap(sessionMap);
    	postElements.setSessionMap(sessionMap);
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
        
        String result  = "Runner ( "
            + "preElements = " + this.preElements + TAB
            + "testElements = " + this.testElements + TAB
            + "postElements = " + this.postElements + TAB
            + " )";
    
        return result;
    }

    
}