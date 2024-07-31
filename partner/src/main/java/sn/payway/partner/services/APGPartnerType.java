package sn.payway.partner.services;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

@SuppressWarnings("serial")
public class APGPartnerType implements Serializable{

	@XmlElement(name="id")
	private Long id;

	@XmlElement(name="partnerType")
	private String partnerType;
	@XmlElement(name="message")
	private String message;

	public APGPartnerType() {
		super();
	}

	public APGPartnerType(Long id, String partnerType) {
		super();
		this.id = id;
		this.partnerType = partnerType;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String getPartnerType() {
		return partnerType;
	}
	public void setPartnerType(String partnerType) {
		this.partnerType = partnerType;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

}
