package sn.payway.partner.services;

import java.io.Serializable;
import java.util.List;

import sn.payway.common.dto.WOperator;

@SuppressWarnings("serial")
public class APGListWOperatorResponse implements Serializable {
	protected String code;
	protected String message;
	List<WOperator> lWOperators;

	public APGListWOperatorResponse(String code, String message, List<WOperator> lWOperators) {
		super();
		this.code = code;
		this.message = message;
		this.lWOperators = lWOperators;
	}

	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public List<WOperator> getlWOperators() {
		return lWOperators;
	}
	public void setlWOperators(List<WOperator> lWOperators) {
		this.lWOperators = lWOperators;
	}	

}
