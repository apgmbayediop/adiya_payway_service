package sn.adiya.partner.services;

import java.io.Serializable;
import java.util.List;

import sn.adiya.common.dto.Operator;

@SuppressWarnings("serial")
public class APGListOperatorResponse implements Serializable {
	protected String code;
	protected String message;
	List<Operator> lOperators;

	public APGListOperatorResponse(String code, String message, List<Operator> lOperators) {
		super();
		this.code = code;
		this.message = message;
		this.lOperators = lOperators;
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

	public List<Operator> getlOperators() {
		return lOperators;
	}
	public void setlOperators(List<Operator> lOperators) {
		this.lOperators = lOperators;
	}	

}
