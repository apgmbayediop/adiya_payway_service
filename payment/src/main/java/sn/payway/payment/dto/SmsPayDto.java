package sn.payway.payment.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SmsPayDto {
	
	
	private String accountNumber;
	private BigDecimal balance;
	private BigDecimal amount;
	private String phone;
	private String currencyName;
	private String indicatif;
	private String transactionId;
	private String requestId;
	
}
