package sn.payway.common.utils;

public class PaywayException extends Exception {

	
private static final long serialVersionUID = 1L;
	
	private String code;
	public PaywayException() {
		super();
	}
	public PaywayException(String code,String message) {
		super(message);
		this.code =code;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
}
