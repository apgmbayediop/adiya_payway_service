package sn.adiya.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import sn.adiya.common.utils.Constantes;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PaymentMeans {

	OMSN("OMSN","Orange money",Constantes.WALLET),
	WVSN("WVSN","Wave",Constantes.WALLET);
	
	
	private String code;
	private  String description;
	private String type;
	
	private PaymentMeans(String code, String description,String type) {
		this.code = code;
		this.description =description;
		this.type = type;
	
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}
