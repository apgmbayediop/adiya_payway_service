package sn.adiya.common.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class Bank implements Serializable {
	private String name;
	private String code;

	public Bank(String name, String code) {
		super();
		this.name = name;
		this.code = code;
	}
}
