package sn.payway.payment.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sn.apiapg.common.exception.ErrorResponse;

@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_EMPTY)
public class PaymentDetails extends PaymentDto {

	private String code;
	private String message;
	private BigDecimal fees;
	private BigDecimal balance;
	private String status;
	private String merchantName;
	private String merchantNumber;
	private String merchantMcc;
	private String merchantCountry;
	private String merchantCity;
	private String merchantAddress;
	private String merchantOrabId;
	private String paymentUrl;
	private String paymentCountry;
	private String fromCountryIndicatif;
	
	private BigDecimal senderAmount;
	private String senderCurrency;
	private String token;
	private String qrCode;
	private String deepLink;
	private String version;
	
	private String orgId;
	
	private Person beneficiary;
	
	private String description;
	
	
	public PaymentDetails(String code, String message) {
		this.code=code;
		this.message = message;
	}
	
	public PaymentDetails(ErrorResponse resp, String message) {
		this.code=resp.getCode();
		this.message = resp.getMessage(message);
	}
	
	
	
}
