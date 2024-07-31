package sn.adiya.transaction.exception;

public class TransactionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -487374556944281624L;

	
	private String code;
	public TransactionException() {
		super();
	}
	public TransactionException(String code,String message) {
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
