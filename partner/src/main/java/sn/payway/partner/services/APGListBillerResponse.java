package sn.payway.partner.services;

import java.io.Serializable;
import java.util.List;
import sn.payway.common.dto.Billers;

@SuppressWarnings("serial")
public class APGListBillerResponse implements Serializable {
	protected String code;
	protected String message;
	List<Billers> lBillers;

	public APGListBillerResponse(String code, String message, List<Billers> lBillers) {
		super();
		this.code = code;
		this.message = message;
		this.lBillers = lBillers;
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

	public List<Billers> getlBillers() {
		return lBillers;
	}
	public void setlBillers(List<Billers> lBillers) {
		this.lBillers = lBillers;
	}

}
