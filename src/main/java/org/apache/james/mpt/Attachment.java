/**
 * 
 */
package org.apache.james.mpt;

/**
 * @author fred
 *
 */
public class Attachment {
	private byte[] data;
	private String filename;
	/**
	 * @param data
	 * @param filename
	 */
	public Attachment(byte[] data, String filename) {
		this.data = data;
		this.filename = filename;
	}
	public byte[] getData() {
		return data;
	}
	public String getFilename() {
		return filename;
	}
}
