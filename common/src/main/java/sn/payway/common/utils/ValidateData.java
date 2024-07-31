package sn.payway.common.utils;

import java.util.Set;

import javax.ejb.Stateless;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.AbstractResponse;

@Stateless
public class ValidateData {
	
	
	public static final String MERCHANT_NUMBER ="merchantNumber";
	public static final String PAN ="pan";
	public static final String AUTH ="auth";
	public static final String LOCAL_CURRENCY_AMOUNT ="localCurrencyAmount";
	public static final String LOCAL_CURRENCY_CODE ="localCurrencyCode";
	public static final String CANAL ="canal";
	public static final String MOYEN_RECEPTION ="moyenReception";
	public static final String TRANSACTION_ID ="transactionId";
	public static final String TIMESTAMP ="timestamp";
	public static final String TERMINAL_NUMBER ="terminalNumber";
	public static final String REQUEST_ID="requestId";
	public static final String FROM_WALLET_ID="fromWalletId";
	public static final String PIN="pin";
	public static final String TRANSACTION_CURRENCY_CODE="transactionCurrencyCode";
	public static final String FROM_COUNTRY="fromCountry";
	public static final String TO_COUNTRY="toCountry";
	public static final String REFERENCE_NUMBER="referenceNumber";
	public static final String MEANS_TYPE="meansType";
	public static final String AMOUNT ="amount";
	public static final String NUM_CAISSE="numeroCaisse";
	public static final String ID_PARTNER="idPartner";
	public static final String CHANNEL_TYPE="channelType";
	public static final String PRM_WALLET="wallet";
	public static final String FIND_CAISSE_BY_NUMCAISSE="Caisse.findByNumeroCaisse";
	public static final String ISO_ACQ_FIND_BY_REQ_ID="IsoAcquisition.findByRequestId";
	public static final String FIND_MY_WALLET="findMyWallet";
	public static final String NUM_POS="numeroPointDeVente";
	public static final String PRM_MCC ="mcc";
	
	public static final String  TRANSACTION_TYPE="transactionType";
	public static final String[] TRANSACTIONS_REQUEST ={TIMESTAMP,TERMINAL_NUMBER,"startDate","endDate"};
	public static final String[] INITIATE_ON_REQUEST ={"auth","requestId",TIMESTAMP,TERMINAL_NUMBER,"amount"};
	
	public static final String[] INITIATE_ON_PUSH_REQUEST ={"moyenReception","canal","requestId","timestamp","terminalNumber","transactionCurrencyCode","amount"};
	
	
	

	
	public AbstractResponse validataData(Object request, String ... listPro) {
		
		AbstractResponse resp = new AbstractResponse(ErrorResponse.REPONSE_SUCCESS.getCode(),"CHECKING SUCCESS");
		try(ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
		Validator validator = factory.getValidator();
		for(String property:listPro) {
		  Set<ConstraintViolation<Object>> violation = validator.validateProperty(request,property);
			 if(!violation.isEmpty()) {
				 ConstraintViolation<Object> constraint = violation.iterator().next();
				 resp.setCode(ErrorResponse.SYNTAXE_ERRORS_1802.getCode());
					resp.setMessage(ErrorResponse.SYNTAXE_ERRORS_1802.getMessage(constraint.getMessage()));
					break;
			 }
	  }
		}
		return resp;
	}
	
	
}
