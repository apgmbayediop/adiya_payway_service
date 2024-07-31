package sn.adiya.merchant.dto;

import java.util.Date;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaisseDto {

	private String numeroCaisse;
	private Long caisseSD;
	
	
	@NotNull(message = "nom")
	private String nom;
	
	@NotNull(message = "pointDeVente")
	private String pointDeVente;
	
	@NotNull(message = "terminal")
	private String terminal;

	private Date dateCreation;
	
	private Date dateLastModification;
	
	private String statut;
	private String secretKey;
	
	@JsonAlias("type")
	private String typeCaisse;
	private String telephone;
	private String email;
}
