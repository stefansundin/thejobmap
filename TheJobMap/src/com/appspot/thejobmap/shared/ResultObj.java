package com.appspot.thejobmap.shared;

public class ResultObj {
	public String result;
	public String error;
	
	public ResultObj() {}
	public ResultObj(String result) {
		this.result = result;
		this.error = null;
	}
	public ResultObj(String result, String error) {
		this.result = result;
		this.error = error;
	}
}
