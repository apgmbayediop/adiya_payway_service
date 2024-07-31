package sn.payway.partner.orangemoney.dto;

import java.math.BigDecimal;
import java.util.List;

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
public class OMPayResponse {

	
	private String message;
	private String currency;
	private BigDecimal amount;
	private BigDecimal fees;
	private String description;
	private String qrCode;
	private String reference;
	private String requestId;
	private String status;
	private String transactionId;
	private String code;
	private String otp;
	private OMAmountDto expire;
	private String detail;
	private String instance;
	private String deepLink;
	private String title;
	private String type;
	private String updatedAt;
	private List<String> violations;
	
	public OMPayResponse(String code, String message) {
       this.code = code;
       this.message = message;
	}

}
