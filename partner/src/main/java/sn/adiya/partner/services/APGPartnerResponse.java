package sn.adiya.partner.services;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import sn.fig.common.utils.AbstractResponse;

@SuppressWarnings("serial")
@XmlRootElement
public class APGPartnerResponse extends AbstractResponse{
	@XmlElement(name="apgPartner")
	private List<APGPartner> apgPartner;
	
	
	public APGPartnerResponse() {
		super();
	}

	public APGPartnerResponse(String code,String message,List<APGPartner> apgPartner) {
		super();
		this.code = code;
		this.message = message;
		this.apgPartner = apgPartner;
	}

	public List<APGPartner> getApgPartner() {
		return apgPartner;
	}
	public void setApgPartner(List<APGPartner> apgPartner) {
		this.apgPartner = apgPartner;
	}


}
