package com.appspot.thejobmap.shared;

public class ResultObj {
	public String result;
	public String info;
	
	public ResultObj() {}
	public ResultObj(String result) {
		this.result = result;
		this.info = null;
	}
	public ResultObj(String result, String error) {
		this.result = result;
		this.info = error;
	}
}
