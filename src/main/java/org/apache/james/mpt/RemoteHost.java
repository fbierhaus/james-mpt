/**
 * 
 */
package org.apache.james.mpt;

/**
 * Simple wrapper for info about a remote host.
 * 
 * @author fred
 *
 */
public class RemoteHost {

	private int port;
	private String host;
	private String alias;

	public RemoteHost(){}
	
	public RemoteHost(String host, int port, String alias){
		this.host = host;
		this.port = port;
		this.setAlias(alias);
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @param alias the alias to set
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}
}
