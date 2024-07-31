package sn.adiya.partner.orangemoney.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OMPaymentDto {

	
	
	private String method;
	private OMClientDto partner;
	private OMClientDto customer;
	private OMAmountDto amount;
	private String reference;
	private boolean receiveNotification;
	private String otp;
	private String callbackSuccessUrl;
	private String callbackCancelUrl;
	private JsonNode metadata;
	private String code;
	private String name;
	private int validity = 900;
}
