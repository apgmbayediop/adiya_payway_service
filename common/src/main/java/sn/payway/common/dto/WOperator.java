package sn.payway.common.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class WOperator implements Serializable {
	private String mnc;
	private String name;
	
	public WOperator(String mnc, String name) {
		super();
		this.mnc = mnc;
		this.name = name;
	}
}
