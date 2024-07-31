package sn.adiya.user.object;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class APGProfil implements Serializable{
	private String code;
	private String libelle;
	private String parent;
	private String rang;

	public APGProfil() {
		super();
	}


}
