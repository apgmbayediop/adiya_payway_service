package sn.adiya.user.object;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter
public class APGUtilisateur implements Serializable {
	private Long id;
	private String prenom;
	private String nom;
	private String email;
	private String adresse;
	private String phone;
	private Boolean first;
	private String profil;
	private String libelleProfil;
	private String login;
	private String typedocument;
	private String numeroDocument;
	private String matricule;
	private String partnerId;
	private String partnerType;
	private String genre;
	private Boolean isActive;
	private Boolean isInit;
	private Boolean isDongle;
	private String reference;
	private String type;
	private String otp;
	private String value;
	private String agence;
	private String caisse;
	private String sousDistributeur;
	private String distributeur;
	private String partnerName;
	private String expirationDocDate;
	private Boolean isMasterDealer;
	private Boolean isValidated;
	private Boolean isConnected;
	private Long nbPassword;
	
	public APGUtilisateur() {
		super();
	}
	
	@Override
	public String toString() {
		return "APGUtilisateur [prenom=" + prenom + ", nom=" + nom + ", email=" + email + ", adresse="+ adresse + ", phone=" + phone 
				+ ", profil=" + profil + ", matricule=" + matricule + ", partnerId=" + partnerId + "]";
	}

	public APGUtilisateur(Long id,String prenom, String nom, String email, String phone, String login, String profil, String type,
			String partnerId, Boolean isDongle, String matricule, String reference, String adresse, String otp,
			String value, String genre, String agence, String caisse, String partnerType) {
		super();
		this.id = id;
		this.prenom = prenom;
		this.nom = nom;
		this.email = email;
		this.phone = phone;
		this.login = login;
		this.profil = profil;
		this.type = type;
		this.partnerId = partnerId;
		this.isDongle = isDongle;
		this.matricule = matricule;
		this.reference = reference;
		this.adresse = adresse;
		this.otp = otp;
		this.value = value;
		this.genre = genre;
		this.agence = agence;
		this.caisse = caisse;
		this.partnerType = partnerType;
	}

}
