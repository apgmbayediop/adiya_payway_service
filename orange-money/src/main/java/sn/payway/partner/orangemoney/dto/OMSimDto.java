package sn.payway.partner.orangemoney.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OMSimDto {

	private String msisdn;
	private String pinCode;
	private String merchantCode;
	private String type;
	private String grade;
	private String expiresAt;
	
	
}
