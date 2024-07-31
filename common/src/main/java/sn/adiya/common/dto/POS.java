package sn.adiya.common.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class POS implements Serializable {
	private String name;

	public POS(String name) {
		super();
		this.name = name;
	}
}
