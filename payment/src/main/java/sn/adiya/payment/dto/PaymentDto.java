package sn.adiya.payment.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_EMPTY)
public class PaymentDto {

	private String code;
	private String requestId;
	private String transactionId;
	private String auth;
	private String timestamp;
	private String bank;
	private String meansType;
	private String subMeansType;
	private String terminalNumber;
	private String terminalSN;
	private String clientIp;
	private String clientCountry;
	@JsonAlias({"channelType","trChannel"})
	private String transactionType;
	private String inTrRefNumber;
	private String senderId;
	private String cardCin;
	private String walletId;
	private String fromWalletId;
	private String toWalletId;
	private String issuerData;
	private String fallBack;
	private String pin;
	private String pinBlock;
	private String pan;
	private String expirationDate;
	private String panSeq;
	private String track2;
	private String cardType;
	private String alias;
	private BigDecimal amount;
	private BigDecimal fees;
	private String phone;
	@JsonAlias({"transactionCurrencyCode"})
	private String currencyName;
	private Person customer;
	private Person beneficiary;
	
	private String returnUrl;
	private String referenceNumber;
	private String reference;
	private String toBankAccountNumber;
	private String merchantAddress;
	private String merchantNumber;
	private String posNumber;
	
	private String canal;
	private String moyenReception;
	private String message;
	
	private String infosCaisseProvider;
	
	private String registrationId;
	private String recurringType;
	private List<String> registrations;
	private List<JsonNode> savedCards;
	private String initialRegistrationId;
	@JsonAlias("InitialTransactionId")
	private String initialTransactionId;
	
	private String purpose;
	private String purposeDetails;
	private String toBankName;
	private String achCode;
	private String toBankCode;
	private String toBankSwift;
	private String toBankAccountType;
	private String fromBankAccountType;
	private String transitNumber;
	
	private String sourceOfIncome;
	private String transactionDate;
	
	private String referencePayer;
	private String responsePayer;
	
	private String autorisationCode;
	private String auditNumber;
	private BigDecimal commissionSender;
	
	private String id;
	private String operationId;
	private String status;
	private String otherDetails;
	private String codeAgence;
	private BigDecimal localCurrencyAmount;
	private String localCurrencyCode;
	private String numAbo;
	private String sessionId;
	private String formule;
	private String carte;
	private String chip;
	private Integer duree;
	
	private String otp;
	private String paymentType;
	private Boolean push;
	private String fromCountry;
	private String toCountry;
	private String savedCard;
	
	public String toStringInitiate() {
		return "{amount=" + amount + ", currency=" + currencyName + ", terminalNumber=" + terminalNumber
				+ ", requestId=" + requestId + ",transactionType ="+transactionType+
				"transactionId="+transactionId+", bank="+bank+",timestamp="+timestamp+"}";

	}
	
}
