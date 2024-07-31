package sn.adiya.partner.services;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("serial")
@XmlRootElement
public class APGPartnerTypeResponse implements Serializable{
	protected String code;
	@XmlElement(name="lPartnerType")
	List<APGPartnerType> lPartnerType;

	public APGPartnerTypeResponse() {
		super();
	}
	
	public APGPartnerTypeResponse(String code) {
		super();
		this.code = code;
	}

	public APGPartnerTypeResponse(String code, List<APGPartnerType> lPartnerType) {
		super();
		this.code = code;
		this.lPartnerType = lPartnerType;
	}

	public List<APGPartnerType> getlPartnerType() {
		return lPartnerType;
	}
	public void setlPartnerType(List<APGPartnerType> lPartnerType) {
		this.lPartnerType = lPartnerType;
	}

}
