package sn.adiya.payment.tokenization;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.ChannelResponse;
import sn.fig.entities.Partner;
import sn.fig.entities.PaymentMeansToken;
import sn.fig.entities.Wallet;
import sn.fig.entities.aci.Caisse;
import sn.fig.entities.aci.CommissionMonetique;
import sn.fig.entities.aci.IsoAcquisition;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.Constantes;
import sn.adiya.common.utils.AdiyaException;
import sn.adiya.payment.dto.PayTokenResponse;
import sn.adiya.payment.dto.PaymentDetails;
import sn.adiya.payment.dto.PaymentDto;
import sn.adiya.payment.dto.Person;
import sn.adiya.payment.dto.SearchPayTokenDto;
import sn.adiya.payment.services.OnlinePaymentService;

@Stateless
@JBossLog
public class CardTokenization {

	@Inject
	private OnlinePaymentService payService;
	
	   public PayTokenResponse  getToken(SearchPayTokenDto dto)  {
		   PayTokenResponse response = new PayTokenResponse();
		   try {
		   Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
				List<PaymentMeansToken> tokens = sess.executeNamedQueryList(PaymentMeansToken.class,
						"tokensByWalletAndBankAndMerchant",new String[] {"walletId","bank","merchant"}, new Object[] {dto.getWalletId(),dto.getBankId(),dto.getMerchantId()});
				
					  List<String> registrations = new ArrayList<>();
						 List<JsonNode>savedCards =new ArrayList<>();
						 log.info(tokens);
				    ObjectMapper mapper = new ObjectMapper();
					for(PaymentMeansToken token:tokens) {
						try {
						if(!registrations.contains(token.getPaymentMeansRef())) {
					   registrations.add(token.getPaymentMeansRef());
					   savedCards.add(mapper.readTree(token.getSavedData()));
					   }
						}
						catch (JsonProcessingException e) {
							log.info(e.getMessage());
						}
					   if("INITIAL".equals(token.getRecurringType())) {
						   response.setPaymentMeansRef(token.getPaymentMeansRef());
						   response.setInitalTransactionId(token.getBankTransactionId());
					   }
					
				   }
					   response.setSavedCards(savedCards);
					   response.setRegistrations(registrations);
	   }
		   catch (IOException e) {
			   log.error("getToken", e);
		}
			return response;
	   }

	public PaymentDetails tokenizeByRandomPay(PaymentDto dto) {
		PaymentDetails response;
		try {
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			String pin = APIUtilVue.getInstance().apgSha(dto.getWalletId() + dto.getPin());
			Wallet wallet = sess.executeNamedQuerySingle(Wallet.class, "findLoginWallet",
					new String[] { "wallet", "pin" }, new String[] { dto.getWalletId(), pin });
			if (wallet == null) {
				response = new PaymentDetails(ErrorResponse.WALLET_ERROR_5001, "");
			} else {
				Caisse caisse = sess.executeNamedQuerySingle(Caisse.class, "Caisse.findByNumeroCaisse",
						new String[] { "numeroCaisse" }, new String[] { dto.getTerminalNumber() });
					Partner commercant = caisse.getPointDeVente().getCommercant();
					BigDecimal amount = generateTokenAmount(caisse.getPointDeVente().getCommercant().getCurrencyName());
					PaymentDto request = new PaymentDto();
					String returnUrl = dto.getWalletId().startsWith("INT-")?"https://intero.afriadiya.com/home/tabs/confirmation/"+request.getRequestId():dto.getReturnUrl();
							request.setAmount(amount);
					request.setTransactionType(ChannelResponse.OLCRG01.getCode());
					request.setCurrencyName(commercant.getCurrencyName());
					request.setRequestId(RandomStringUtils.randomNumeric(5) + wallet.getPhonenumber());
					request.setTerminalNumber(dto.getTerminalNumber());
					request.setTimestamp(Long.toString(System.currentTimeMillis()));
					request.setWalletId(dto.getWalletId());
					request.setReturnUrl(returnUrl);
					request.setMeansType(Constantes.CARTE);
					Person customer = new Person(wallet);
					Person beneficiary = new Person();
					request.setCustomer(customer);
					request.setBeneficiary(beneficiary);
					beneficiary.setFirstName(
							Normalizer.normalize(wallet.getFirstname(), Normalizer.Form.NFKD).replaceAll("\\p{M}", ""));
					beneficiary.setLastName(
							Normalizer.normalize(wallet.getLastname(), Normalizer.Form.NFKD).replaceAll("\\p{M}", ""));
					String auth= payService.initatePaymentAuth(caisse.getTerminalKey(),request);
					request.setAuth(auth);
					final CommissionMonetique commMonetique = sess.executeNamedQuerySingle(CommissionMonetique.class,
							"CommissionMonetique.FindByPartenaireChannel",
							new String[] { "idPartner", "channelType", "meansType" },
							new Object[] { commercant.getIdPartner(), request.getTransactionType(), Constantes.CARTE });
					if (commMonetique == null) {
						log.info("commission not configured for channel " + request.getTransactionType());
						response = new PaymentDetails(ErrorResponse.COMMISSION_NOT_FOUND, "");
					}else {
						response =payService.initiateOnlinePayment(request);
					}
				
			}
		}
		catch (AdiyaException e) {
			response = new PaymentDetails(e.getCode(), e.getMessage());
		}
		catch (Exception e) {
			log.error(e);
			response = new PaymentDetails(ErrorResponse.UNKNOWN_ERROR.getCode(), ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}

		return response;
	}

	private BigDecimal generateTokenAmount(String currencyName) {
		 BigDecimal amount;
		 
		 if(Constantes.CURRENCY_XOF.equals(currencyName)||Constantes.CURRENCY_XAF.equals(currencyName)) {
			  int val = new SecureRandom().nextInt(1000)+100;
			  log.info(val);
			  while(val%5!=0) {
				  val =val+1; 
			  }
			  amount = BigDecimal.valueOf(val);
		 }else {
			 double val = new SecureRandom().nextInt(5)+1;
			  amount = BigDecimal.valueOf(val).setScale(2);
		 }
		return amount;
	}

	public PayTokenResponse getListToken(PaymentDto dto) {
		
		   Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		   Caisse caisse = sess.executeNamedQuerySingle(Caisse.class, "Caisse.findByNumeroCaisse", new String[] {"numeroCaisse"}, new String[] {dto.getTerminalNumber()});
		   String calAuth= DigestUtils.sha256Hex(dto.getWalletId()+dto.getTimestamp()+dto.getTerminalNumber()+caisse.getTerminalKey());
		   PayTokenResponse response;
		   if(calAuth.equals(dto.getAuth())) {
			   SearchPayTokenDto search = new SearchPayTokenDto();
			   search.setBankId(Long.parseLong(Constantes.CODE_ORABANK));
			   search.setMerchantId(caisse.getPointDeVente().getCommercant().getIdPartner());
			   search.setWalletId(dto.getWalletId());
			   response = getToken(search);
			   response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		   }else {
			   response = new PayTokenResponse();
			   response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		   }
		   return response;
	}
	public void addToken(Session sess, PaymentDto request,IsoAcquisition acquisition) {
		log.info("registration"+request.getRegistrationId());
		
		boolean tosave = acquisition.getCardId()!=null&&request.getRegistrationId()!=null&&!request.getRegistrationId().isBlank();
		if(tosave)
		{
			log.info("save token");
			List<PaymentMeansToken> tokens = sess.executeNamedQueryList(PaymentMeansToken.class,
					"tokensByWalletAndRefAndMerchant",new String[] {"walletId","paymentMeansRef","merchant"}, new Object[] {acquisition.getCardId(),request.getRegistrationId(),acquisition.getCaisse().getPointDeVente().getCommercant().getIdPartner()});
			if(tokens==null||tokens.isEmpty()) {	
			PaymentMeansToken token = new PaymentMeansToken();
			token.setBank(Long.parseLong(request.getBank()));
			token.setBankTransactionId(request.getReferencePayer());
			token.setDate(Instant.now());
			token.setMerchant(acquisition.getCaisse().getPointDeVente().getCommercant().getIdPartner());
			token.setPaymentMeansRef(request.getRegistrationId());
			token.setRecurringType(request.getRecurringType());
			token.setWalletId(acquisition.getCardId());
			token.setSavedData(request.getSavedCards().toString());
			sess.saveObject(token);
		}
		}
		else {
			log.info("no registration");
		}
	}
}
