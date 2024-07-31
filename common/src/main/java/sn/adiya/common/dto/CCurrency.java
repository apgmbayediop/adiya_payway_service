package sn.adiya.common.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class CCurrency implements Serializable {
	private String currency;

	public CCurrency(String currency) {
		super();
		this.currency = currency;
	}
}
