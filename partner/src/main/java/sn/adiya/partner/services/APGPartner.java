package sn.adiya.partner.services;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@SuppressWarnings("serial")
@JsonInclude(value = Include.NON_EMPTY)
@Data
public class APGPartner implements Serializable {
	private Long id;
	private String name;
	private String email;
	private String telephonePartner;
	private String telephoneContact;
	private String prenomContact;
	private String nomContact;
	private String partnerTypeId;
	private String partnerType;
	private String consumerId;
	//	private String secretKey;
	private String auth;
	//	private String principalAccountNumber;
	//	private String feesAccountNumber;
	private String countryId;
	private String countryIsoCode;
	private String countryIndicatif;
	private String balance;
	private String partnerCode;
	private String currencyName;
	//private Boolean isNotify;
	private Boolean isWebHook;
	private Boolean isValidated;
	private Boolean withOperationCode;
	private String adresse;
	private String region;
	private Boolean isOpened;
	private Boolean isAssigned;
	private String nbChild;
	private String plafondTrx;
	private String volumeTrx;
	private Long assignedUserId;
	private String assignedUserFirstName;
	private String assignedUserLastName;
	private String assignedUserPhone;
	private String codeAgence;
	private String prefixeWallet;
	private String logo;
	private String type;
	private Boolean active;
	private Boolean isB2B;
	private BigDecimal alertBalance;
	private String emailBalance ;
	private String emailCode ;
	private String emailFX ;
	private String emailAlert;
	private String mcc; 
	private String modeReglement;
	private String numeroCompte; 
	private Boolean isDecouvert;
	BigDecimal balanceDecouvert;
	private Boolean isActive;
	private Boolean sdPayTrans;

}
