package sn.payway.merchant.dto;

import java.util.Date;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sn.apiapg.entities.aci.PointDeVente;
import sn.apiapg.lis.utils.SysVar;


@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosDto {

	
private String numeroPointDeVente;
	
	@NotNull(message = "nom")
	private String nom;
	
	@NotNull(message = "adresse")
	private String adresse;
	
	private String telephone;
	
	@NotNull(message = "numeroCommercant")
	private Long numeroCommercant;
	
	private Date dateCreation;
	
	private Date dateLastModification;
	
	private String statut;
	private String raisonSocialeCommercant;
	private String marchandEcommercant;
	
	public PosDto(PointDeVente pointDeVente ) {
		super();
		this.numeroPointDeVente = pointDeVente.getNumeroPointDeVente();
		this.adresse = pointDeVente.getAdresse();
		this.nom = pointDeVente.getNom();
		this.telephone = pointDeVente.getTelephone();
		this.numeroCommercant  = pointDeVente.getCommercant().getIdPartner();
		this.raisonSocialeCommercant  = pointDeVente.getCommercant().getName();
		this.dateCreation = pointDeVente.getDateCreation();
		this.dateLastModification = pointDeVente.getDateLastModification();
		this.marchandEcommercant = pointDeVente.getMarchandEcommercant();
		this.statut = pointDeVente.getTypeOperation().equalsIgnoreCase(SysVar.TypeOperation.DELETE.name()) ? "DESACTIVER" : "ACTIF";
	}
}
