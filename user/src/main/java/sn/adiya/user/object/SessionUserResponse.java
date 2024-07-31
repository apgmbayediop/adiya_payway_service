package sn.adiya.user.object;

import lombok.Getter;
import lombok.Setter;
import sn.fig.common.utils.AbstractResponse;

@SuppressWarnings("serial")
@Getter @Setter  
public class SessionUserResponse extends AbstractResponse {
	protected String prenom;
	protected String nom;
	protected String email;
	protected String phone;
	protected String profil;
	protected String description;
	protected String id;
	protected String adresse;
	protected String genre;
	protected String login;
	protected Boolean first;
	protected String countryIsoCode;
	protected Long partnerId;
	protected String partnerCode;
	protected String matricule;
	protected String logo;
	protected String partnerType;
	protected String prefixeWallet;


	public SessionUserResponse(String code, String message,String prenom,String nom,String email,String phone,String profil,String description,String id,String adresse,String genre,String login,Boolean first,String countryIsoCode, Long partnerId, String partnerCode,String matricule,String logo, String partnerType, String prefixeWallet) {
		super();
		this.code = code;
		this.message = message;
		this.prenom = prenom;
		this.nom = nom;
		this.email = email;
		this.phone = phone;
		this.profil = profil;
		this.description = description;
		this.id = id;
		this.adresse = adresse;
		this.genre = genre;
		this.login = login;
		this.first = first;
		this.countryIsoCode = countryIsoCode;
		this.partnerId = partnerId;
		this.partnerCode = partnerCode;
		this.matricule = matricule;
		this.logo = logo;
		this.partnerType = partnerType;
		this.prefixeWallet = prefixeWallet;
	}
	
}
