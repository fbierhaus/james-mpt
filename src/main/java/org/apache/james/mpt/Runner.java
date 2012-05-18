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

/**
 * Runs protocol scripts.
 */
public class Runner {
    
    public Runner(){

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
    public void runSessions(ProtocolInteractor testScript) throws Exception {
    	
    	// start all the sessions
    	for (Iterator<Session> iterator = testScript.getSessions().values().iterator(); iterator.hasNext();) {
			Session session = (Session) iterator.next();
			session.start();
		}
    	
        try {
        	testScript.execute();
        } finally {
        	// stop all the sessions
        	for (Iterator<Session> iterator = testScript.getSessions().values().iterator(); iterator.hasNext();) {
    			Session session = (Session) iterator.next();
    			session.stop();
    		}
        }
    }
    
    
}
