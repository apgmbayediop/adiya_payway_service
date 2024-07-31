package sn.adiya.user.object;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import sn.fig.common.utils.AbstractResponse;

@SuppressWarnings("serial")
public class APGProfilResponse extends AbstractResponse{

	@XmlElement(name="profil")
	private List<APGProfil> profil;

	/**
	 * @param profil
	 */
	public APGProfilResponse(String code, String message,List<APGProfil> profil) {
		super();
		this.code = code;
		this.message = message;
		this.profil = profil;
	}

	public List<APGProfil> getProfil() {
		return profil;
	}
	public void setProfil(List<APGProfil> profil) {
		this.profil = profil;
	}

}
