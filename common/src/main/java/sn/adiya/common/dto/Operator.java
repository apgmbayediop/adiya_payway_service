package sn.adiya.common.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class Operator implements Serializable {
	private String name;
	private String mnc;

	public Operator(String name, String mnc) {
		super();
		this.name = name;
		this.mnc = mnc;
	}
}
