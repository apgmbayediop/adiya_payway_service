package sn.adiya.common.utils;

public class AdiyaException extends Exception {

	
private static final long serialVersionUID = 1L;
	
	private String code;
	public AdiyaException() {
		super();
	}
	public AdiyaException(String code,String message) {
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
