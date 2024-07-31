package sn.payway.common.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class APGCountry implements Serializable{
	private String code;
	private String name;

	public APGCountry() {
		super();
	}

}
