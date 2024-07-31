package sn.adiya.common.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class Periodicite implements Serializable {
	private String code;
	private String libelle;
	
	public Periodicite(String code, String libelle) {
		super();
		this.code = code;
		this.libelle = libelle;
	}
}
