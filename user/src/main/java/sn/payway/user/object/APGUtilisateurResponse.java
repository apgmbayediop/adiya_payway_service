package sn.payway.user.object;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import sn.apiapg.common.utils.AbstractResponse;

@SuppressWarnings("serial")
@XmlRootElement
public class APGUtilisateurResponse extends AbstractResponse{
	@XmlElement(name="apgUtilisateur")
	private List<APGUtilisateur> apgUtilisateur;
	
	public APGUtilisateurResponse() {
		super();
	}

	public APGUtilisateurResponse(String code,String message, List<APGUtilisateur> apgUtilisateur) {
		super();
		this.code = code;
		this.message = message;
		this.apgUtilisateur = apgUtilisateur;
	}

	public List<APGUtilisateur> getApgUtilisateur() {
		return apgUtilisateur;
	}
	public void setApgUtilisateur(List<APGUtilisateur> apgUtilisateur) {
		this.apgUtilisateur = apgUtilisateur;
	}

}
