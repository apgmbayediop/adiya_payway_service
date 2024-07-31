package sn.adiya.common.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class Channel implements Serializable{
	private String code;
	private String name;

	public Channel() {
		super();
	}
	
	@Override
	public String toString() {
		return "[code:" + code + ", name:" + name + "]";
	}
}
