package sn.payway.wave.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class WaveRequest {

	
	private String code;
	private String id;
	private String message;
	private BigDecimal amount;
	@JsonProperty("checkout_status")
	private String checkoutStatus;
	@JsonProperty("client_reference")
	private String clientReference;
	private String currency;
	@JsonProperty("error_url")
	private String errorUrl;
	@JsonProperty("last_payment_error")
	private WaveRequest lastPaymentError;
	@JsonProperty("payment_status")
	private String paymentStatus;
	@JsonProperty("success_url")
	private String successUrl;
	@JsonProperty("wave_launch_url")
	private String waveLaunchUrl;
	@JsonProperty("when_completed")
	private String whenCompleted;
	@JsonProperty("when_created")
	private String whenCreated;
	@JsonProperty("when_expires")
	private String whenExpires;
	@JsonProperty("business_name")
	private String businessName;
	@JsonProperty("override_business_name")
	private String overrideBusinessName;
	
	@JsonProperty("business_type")
	private String businessType;
	@JsonProperty("msg")
	private String msg;
	@JsonProperty("business_registration_identifier")
	private String businessRegistrationIdentifier;
	private String name;
	@JsonProperty("aggregated_merchant_id")
	private String aggregatedMerchantId;
	private List<WaveRequest>details;
	
	
	
}
