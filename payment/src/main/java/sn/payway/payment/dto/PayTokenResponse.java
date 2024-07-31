package sn.payway.payment.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sn.apiapg.common.utils.AbstractResponse;

@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_EMPTY)
public class PayTokenResponse extends AbstractResponse {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String PaymentMeansRef;
	private  String initalTransactionId;
	 private List<String> registrations;
	 private  List<JsonNode>savedCards;
	 
	 
	 public PayTokenResponse (String code,String message) {
		 this.code = code;
		 this.message = message;
	 }
}
