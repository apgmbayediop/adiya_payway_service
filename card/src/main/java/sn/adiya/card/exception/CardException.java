package sn.adiya.card.exception;

public class CardException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String code;
	public CardException() {
		super();
	}
	public CardException(String code,String message) {
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
