package com.appspot.thejobmap.shared;

/**
 * This is the object about operation result status that is sent by the server in return to operations.
 * It is used pretty much everywhere where the server wants to say OK or FAIL.
 * The id field is used when an add operation is made so the client can make future updates to that entity.
 * 
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */
public class ResultObj {
	public String result;
	public String info;
	public Long id;

	/**
	 * Constructors.
	 */
	public ResultObj() {}
	public ResultObj(String result) {
		this.result = result;
		this.info = null;
	}
	public ResultObj(String result, String error) {
		this.result = result;
		this.info = error;
	}
	public ResultObj(String result, Long id) {
		this.result = result;
		this.id = id;
	}
}
