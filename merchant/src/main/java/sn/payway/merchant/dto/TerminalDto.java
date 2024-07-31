package sn.payway.merchant.dto;

import java.util.Date;

import javax.validation.constraints.NotNull;

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
public class TerminalDto {

	@NotNull(message = "numeroSerie")
	private String numeroSerie;
	
	@NotNull(message = "designation")
	private String designation;
	
	@NotNull(message = "versionApplication")
	private String versionApplication;
	
	private String heureTecollecte;
	
	private Date dateCreation;
	
	private Date dateLastModification;
	
	private Long partner;
	private String nomPartner;
	private boolean virtuel;
}
