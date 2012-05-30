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

package org.apache.james.mpt.antlib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.james.mpt.ExternalHostSystem;
import org.apache.james.mpt.Monitor;
import org.apache.james.mpt.ProtocolInteractor;
import org.apache.james.mpt.ProtocolSessionBuilder;
import org.apache.james.mpt.RemoteHost;
import org.apache.james.mpt.Runner;
import org.apache.james.mpt.Session;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;

/**
 * Task executes MPT scripts against a server
 * running on a given port and host.
 */
public class MailProtocolTestTask extends Task implements Monitor{

	private static final String TIMESTAMP = "timestamp";
	
    private boolean quiet = false;
    private File script;
    private Union scripts;
    private boolean skip = false;
    private String shabang = null;
    private List<RemoteHost> remoteHosts = new ArrayList<RemoteHost>();
    private String errorProperty;
    
    /**
     * Gets the error property.
     * 
     * @return name of the ant property to be set on error,
     * null if the script should terminate on error
     */
    public String getErrorProperty() {
        return errorProperty;
    }

    /**
     * Sets the error property.
     * @param errorProperty name of the ant property to be set on error,
     * nul if the script should terminate on error
     */
    public void setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
    }

    /**
     * Should progress output be suppressed?
     * @return true if progress information should be suppressed,
     * false otherwise
     */
    public boolean isQuiet() {
        return quiet;
    }

    /**
     * Sets whether progress output should be suppressed/
     * @param quiet true if progress information should be suppressed,
     * false otherwise
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    /**
     * Should the execution be skipped?
     * @return true if exection should be skipped, 
     * otherwise false
     */
    public boolean isSkip() {
        return skip;
    }

    /**
     * Sets execution skipping.
     * @param skip true to skip excution
     */
    public void setSkip(boolean skip) {
        this.skip = skip;
    }


    /**
     * Gets the script to execute.
     * @return file containing test script
     */
    public File getScript() {
        return script;
    }

    /**
     * Sets the script to execute.
     * @param script not null
     */
    public void setScript(File script) {
        this.script = script;
    }

    /**
     * Gets script shabang.
     * This will be substituted for the first server response.
     * @return script shabang, 
     * or null for no shabang
     */
    public String getShabang() {
        return shabang;
    }
    
    /**
     * Sets the script shabang.
     * When not null, this value will be used to be substituted for the 
     * first server response.
     * @param shabang script shabang, 
     * or null for no shabang.
     */
    public void setShabang(String shabang) {
        this.shabang = shabang;
    }

    public void addRemoteHost(RemoteHost rh){
    	remoteHosts.add(rh);
    }
    
    @Override
    public void execute() throws BuildException {

        
        if (scripts == null && script == null) {
            throw new BuildException("Scripts must be specified as an embedded resource collection"); 
        }
        
        if (scripts != null && script != null) {
            throw new BuildException("Scripts can be specified either by the script attribute or as resource collections but not both."); 
        }
        
        
        if(skip) {
            log("Skipping excution");
        } else if (errorProperty == null) {
            doExecute();
        } else {
            try {
                doExecute();
            } catch (BuildException e) {
                final Project project = getProject();
                project.setProperty(errorProperty, e.getMessage());
                log(e, Project.MSG_DEBUG);
            }
        }
    }

    public void add(ResourceCollection resources) {
        if (scripts == null) {
            scripts = new Union();
        }
        scripts.add(resources);
    }
    
    private void doExecute() throws BuildException {
        
    	Map<String, Session> sessionMap = new HashMap<String, Session>();
    	for (RemoteHost remoteHost : remoteHosts) {
            ExternalHostSystem factory = new ExternalHostSystem(remoteHost.getAlias(), remoteHost.getHost(), remoteHost.getPort(), this, getShabang(), null);
            debug("creating new session for "+ remoteHost.getAlias());
            Session session = factory.newSession();
            sessionMap.put(remoteHost.getAlias(), session);
		}
    	
        final ProtocolSessionBuilder builder = new ProtocolSessionBuilder();
        Date current = new Date();
        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
        
        if (scripts == null) {
            scripts = new Union();
            scripts.add(new FileResource(script));
        }
        
        for (final Iterator<Resource> it=scripts.iterator();it.hasNext();) {
            final Resource resource = it.next();
            note(" --- Running script: " + resource.getName() + " --- ");
            try {
                final Runner runner = new Runner();

                try {
                    
                    final InputStream inputStream = resource.getInputStream();
                    builder.setVariable(TIMESTAMP, df.format(current));
                    ProtocolInteractor testScript = builder.buildProtocolSession(resource.getName(), inputStream, sessionMap);
                    runner.runSessions(testScript);
                    
                } catch (UnsupportedOperationException e) {
                    log("Resource cannot be read: " + resource.getName(), Project.MSG_WARN);
                }
            } catch (IOException e) {
                throw new BuildException("Cannot load script " + resource.getName(), e);
            } catch (Exception e) {
                log(e.getMessage(), Project.MSG_ERR);
                throw new BuildException("[FAILURE] in script " + resource.getName() + "\n" + e.getMessage(), e);
            }
            
        }
    
    }

    public void note(String message) {
        if (quiet) {
            log(message, Project.MSG_DEBUG);
        } else {
            log(message, Project.MSG_INFO);
        }
    }

    public void debug(char character) {
        log("'" + character + "'", Project.MSG_DEBUG);
    }

    public void debug(String message) {
        log(message, Project.MSG_DEBUG);
    }
    
}
