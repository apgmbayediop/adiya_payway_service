package sn.payway.partner.orangemoney.service;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import sn.apiapg.common.config.entities.CoursDevise;
import sn.apiapg.common.config.entities.ParametresGeneraux;
import sn.apiapg.common.config.entities.ToolsPartner;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.MailUtils;
import sn.apiapg.entities.aci.IsoAcquisition;
import sn.apiapg.session.AdministrationSession;
import sn.apiapg.session.AdministrationSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.dto.OAuthDto;
import sn.payway.common.dto.OAuthResponse;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.PaywayCommonTools;
import sn.payway.partner.orangemoney.dto.OMClientDto;
import sn.payway.partner.orangemoney.dto.OMPayResponse;
import sn.payway.partner.orangemoney.dto.OMPaymentDto;
import sn.payway.partner.orangemoney.dto.OMSimDto;

@Stateless
public class OrangeMoneyService {

	private static final Logger logger = Logger.getLogger(OrangeMoneyService.class);

	
	
	public static final String CODE = "36381";
	public static final String OM_PAY_STATUS_SUCCESS="SUCCESS";
	public static final String OM_PAY_STATUS_ACCEPTED="ACCEPTED";
	
	public static final Set<String> FINAL_STATUS = Set.of(OM_PAY_STATUS_ACCEPTED, "CANCELLED", "FAILED", "REJECTED", OM_PAY_STATUS_SUCCESS);
	public static final Set<String> CANCELED_STATUS = Set.of("CANCELLED", "FAILED", "REJECTED");
	
	public static final String MSISDN = "MSISDN";
	public static final String PRINCIPAL = "PRINCIPAL";
	public static final String VERSION2 = "V2";
	public static final String ONE_STEP = "OM_ONE_STEP";


	public static final String PAYMENT_TYPE_QRCODE = "QRCODE";
	public static final String PAYMENT_TYPE_CODE = "CODE";
	public static final String PAYMENT_TYPE_CLASSIC = "CLASSIC";
	
	private String email;
	@Inject
	private PaywayCommonTools tools;
	private String baseUrl;
	
	@PostConstruct
	public void init() {
		try {
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			ToolsPartner tokenUrlTool = session.findObjectById(ToolsPartner.class, null,
					Constantes.OM_PAY_BASE_URL);
			
			baseUrl =APIUtilVue.getInstance().apgDeCrypt(tokenUrlTool.getValue());
			ParametresGeneraux param = session.findObjectById(ParametresGeneraux.class, null, Constantes.OM_CANCEL_MAILING);
			email = param ==null?"":param.getText().replaceAll(" ", "");
			} catch (Exception e) {
			logger.error("initOMError", e);
		}
	}
	public OAuthResponse accessToken(OAuthDto dto) {
		OAuthResponse response =null;
		try {
			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
			HttpPost post = new HttpPost(baseUrl + "/oauth/token");
			post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
			
			List<NameValuePair> form = new ArrayList<>();
			form.add(new BasicNameValuePair("grant_type", dto.getGrantType()));
			form.add(new BasicNameValuePair("client_id", dto.getClientId()));
			form.add(new BasicNameValuePair("client_secret", dto.getClientSecret()));

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form);
			post.setEntity(entity);
			try (CloseableHttpResponse resp = http.execute(post)) {
				String respStr = EntityUtils.toString(resp.getEntity());
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					
					response = new ObjectMapper().readValue(respStr, OAuthResponse.class);
				}

			}
			}
		} catch (IOException e) {
		      logger.error("errorTokenOM",e);
		}
		return response;
	}
	public OMClientDto getMerchantDetails(HashMap<String, String> merchantCodes) throws UnsupportedEncodingException {
		OMClientDto omPartner = null;
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			String retailerCode = merchantCodes.get(BEConstantes.ORANGE_RETAILER);
			String pinCode = merchantCodes.get(BEConstantes.ORANGE_PIN);
				ToolsPartner omPhone = session.findObjectById(ToolsPartner.class, null, retailerCode);
				String phone = APIUtilVue.getInstance().apgDeCrypt(omPhone.getValue());
				ToolsPartner omPinCode = session.findObjectById(ToolsPartner.class, null, pinCode);
				String pin = APIUtilVue.getInstance().apgDeCrypt(omPinCode.getValue());
				omPartner = new OMClientDto();
				omPartner.setId(phone);
				omPartner.setIdType(MSISDN);
				omPartner.setEncryptedPinCode(pin);
				omPartner.setWalletType(PRINCIPAL);
		return omPartner;
}

	/*public OMPayResponse payment(PaymentRequest request, IsoAcquisition acqui, BigDecimal fees) {

		logger.info("paiement with OrangeMoney");
		logger.info(request.getBank());
		acqui.setField56(Long.toString(System.currentTimeMillis()));
		acqui.setField32(Constantes.CODE_OM_API);
		OMPayResponse resp = new OMPayResponse();
		String phone = request.getPhone().replace(" ", "");
		phone = phone.substring(phone.length()-9);
		String paymentMeans = "orange-"+phone;
		acqui.setField2(paymentMeans);
		
			OMPaymentDto dto = new OMPaymentDto();
			OMAmountDto amountDto = new OMAmountDto();
			BigDecimal totalAmount = new BigDecimal(acqui.getField4()).add(fees).setScale(0);
			amountDto.setUnit(acqui.getField49());
			amountDto.setValue(totalAmount);
			logger.info(amountDto);
			OMClientDto customer = new OMClientDto();
			customer.setId(phone);
			customer.setIdType(MSISDN);
			customer.setWalletType(PRINCIPAL);
			dto.setAmount(amountDto);
			dto.setCustomer(customer);
			dto.setMethod("CLASSIC");
			dto.setPartner(getMerchantDetails(acqui.getChannelType()));
			dto.setReceiveNotification(false);
			dto.setReference(acqui.getId().toString());
			logger.info(dto.toString());
			resp = classicPayment(dto,acqui.getChannelType());
			acqui.setField54(resp.getResponsePayer());
			acqui.setField53(VERSION2);
			acqui.setFees(fees);
			if(Constantes.ISO_PENDING_STATUS.equals(resp.getCode())) {
			acqui.setField56(resp.getReferencePayer());
			}

		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		sess.updateObject(acqui);
		return resp;
	}*/

	public OMPayResponse classicPayment(OMPaymentDto paymentDto,String token) {

		logger.info("paiement with classic payment");
		OMPayResponse response;
		try {
			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpPost post = new HttpPost(baseUrl + "/api/eWallet/v1/payments");
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(HttpHeaders.AUTHORIZATION, token);
				paymentDto.setOtp(null);
				paymentDto.setReceiveNotification(true);
				String request = new ObjectMapper().writeValueAsString(paymentDto);
				logger.info("request classic "+request);
			
				StringEntity entity = new StringEntity(request);
				post.setEntity(entity);
				try (CloseableHttpResponse cHResp = http.execute(post)) {
					logger.info(cHResp.getStatusLine().getStatusCode());
					int statusCode = cHResp.getStatusLine().getStatusCode();
					String respStr = EntityUtils.toString(cHResp.getEntity());
					logger.info("response "+respStr);
					 response = new ObjectMapper().readValue(respStr, OMPayResponse.class);
					if(statusCode ==HttpStatus.SC_ACCEPTED||statusCode ==HttpStatus.SC_CREATED||
							statusCode ==HttpStatus.SC_OK) {
					response.setCode(Constantes.ISO_PENDING_STATUS);
					response.setStatus(BEConstantes.STATUS_TRANSACTION_PENDING);
					response.setMessage(BEConstantes.STATUS_TRANSACTION_PENDING);
					}else {
						response.setCode(Constantes.ISO_UNKNOWN_ERROR_STATUS);
						response.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
						response.setMessage(BEConstantes.STATUS_TRANSACTION_FAILED);
						
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("initPayOMError",e);
			response = new OMPayResponse(ErrorResponse.UNKNOWN_ERROR.getCode(),ErrorResponse.UNKNOWN_ERROR.getMessage(""));
			
		}

		return response;
	}
	public OMPayResponse oneStepPayment(OMPaymentDto paymentDto,String token) {

		logger.info("paiement one step");
		OMPayResponse resp;
		try {
			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpPost post = new HttpPost(baseUrl + "/api/eWallet/v1/payments/onestep");
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(HttpHeaders.AUTHORIZATION, token);
				String request = new ObjectMapper().writeValueAsString(paymentDto);
				StringEntity entity = new StringEntity(request);
				post.setEntity(entity);
				try (CloseableHttpResponse cHResp = http.execute(post)) {
					logger.info(cHResp.getStatusLine().getStatusCode());
					String respStr = EntityUtils.toString(cHResp.getEntity());
					logger.info("response "+respStr);
					resp = new ObjectMapper().readValue(respStr, OMPayResponse.class);
					String code = OM_PAY_STATUS_SUCCESS.equals(resp.getStatus())?Constantes.ISO_SUCCESS_STATUS:Constantes.ISO_UNKNOWN_ERROR_STATUS;
					if (Constantes.PREPROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))
							|| Constantes.PROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))) {
						logger.info("env "+tools.getProperty(Constantes.ENV_PRPOPERTY) );
					}else {
						logger.info("env test always success");
						code = Constantes.ISO_SUCCESS_STATUS;
					}
					resp.setCode(code);
				}
			}
		}
		catch (Exception e) {
			logger.error("oneStepOMError",e);
			resp = new OMPayResponse(ErrorResponse.UNKNOWN_ERROR.getCode(),ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}

		return resp;
	}
	
	public OMPayResponse generateOtp(OMClientDto client,String token) {

		logger.info("generate otp");
		OMPayResponse response;
		try {
			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpPost post = new HttpPost(baseUrl + "/api/eWallet/v1/payments/otp");
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(HttpHeaders.AUTHORIZATION, token);
				String request = new ObjectMapper().writeValueAsString(client);
				logger.info(" request otp"+request);
				StringEntity entity = new StringEntity(request);
				post.setEntity(entity);
				try (CloseableHttpResponse cHResp = http.execute(post)) {
					Integer statusCode = cHResp.getStatusLine().getStatusCode();
					String respStr = EntityUtils.toString(cHResp.getEntity());
					logger.info("response otp"+respStr);
					response = new ObjectMapper().readValue(respStr, OMPayResponse.class);
					response.setCode(statusCode.toString());
				}
			}
		}
		catch (Exception e) {
			logger.error("otpOMError",e);
			response = new OMPayResponse();
			response.setCode("500");
		}

		return response;
	}
	
	public OMPayResponse getTransactionStatus(String transactionId,String token) {

		OMPayResponse resp;
		try {
			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpGet post = new HttpGet(baseUrl + "/api/eWallet/v1/transactions/"+transactionId+"/status");
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(HttpHeaders.AUTHORIZATION, token);
				try (CloseableHttpResponse cHResp = http.execute(post)) {
					String respStr = EntityUtils.toString(cHResp.getEntity());
					resp = new ObjectMapper().readValue(respStr, OMPayResponse.class);
					if("400".equals(resp.getStatus())) {
						resp.setStatus("FAILED");
					}
					
				}
			}
		}
		catch (Exception e) {
			logger.error("statusOMError",e);
			resp = new OMPayResponse();
			resp.setStatus(BEConstantes.STATUS_TRANSACTION_PENDING);
		}

		return resp;
	}
	public void testNumbers(String token) {

		logger.info("getTestNumbers");
		try {
			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpGet get = new HttpGet("https://api.sandbox.orange-sonatel.com/api/assignments/v1/partner/sim-cards?nbMerchants=1&nbCustomers=1");
				get.addHeader(HttpHeaders.AUTHORIZATION, token);
				try (CloseableHttpResponse cHResp = http.execute(get)) {
					logger.info(cHResp.getStatusLine().getStatusCode());
					String respStr = EntityUtils.toString(cHResp.getEntity());
					if(cHResp.getStatusLine().getStatusCode() == 200) {
					List<OMSimDto>numbers = new ObjectMapper().readValue(respStr, 
							new TypeReference<List<OMSimDto>>() {});
					if(cHResp.getStatusLine().getStatusCode()==HttpStatus.SC_OK) {
						for(OMSimDto client :numbers) {
						    saveNumber(client);
						}
						init();
					}
					}
					
				}
			}
		}
		catch (Exception e) {
			logger.error("statusOMError",e);
		}
	}
	
	private void saveNumber(OMSimDto client) {
		try {
		String codePhone ="MERCHANT".equals(client.getType())?Constantes.OM_PAY_PHONE:Constantes.OM_PAY_CUSTOMER_PHONE;
		String codePIN ="MERCHANT".equals(client.getType())?Constantes.OM_PAY_PIN:Constantes.OM_PAY_CUSTOMER_PIN;
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		
		logger.info(client);
		ToolsPartner omPhone = session.findObjectById(ToolsPartner.class, null, codePhone);
		String phone = APIUtilVue.getInstance().encod(client.getMsisdn());
		
		ToolsPartner omPublicKey = session.findObjectById(ToolsPartner.class, null, Constantes.OM_PAY_PUBLIC_KEY);
		ToolsPartner omPinCode = session.findObjectById(ToolsPartner.class, null, codePIN);
		ToolsPartner omMerchCode = session.findObjectById(ToolsPartner.class, null, Constantes.OM_PAY_MERCH_CODE);
		Instant expiresAt = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(client.getExpiresAt()));
		Long validity = Duration.between(Instant.now(),expiresAt).toSeconds();
		String publicKey = omPublicKey.getValue();
		if("MERCHANT".equals(client.getType())) {
			if(omMerchCode ==null) {
				omMerchCode = new ToolsPartner();
				omMerchCode.setCode(Constantes.OM_PAY_MERCH_CODE);
				omMerchCode.setDate(new Date());
				omMerchCode.setKey(codePhone);
				omMerchCode.setValue(APIUtilVue.getInstance().encod(client.getMerchantCode()));
				omMerchCode.setFiltre(validity.toString());	
				session.saveObject(omMerchCode);
			}else {
				omMerchCode.setDate(new Date());
				omMerchCode.setKey(Constantes.OM_PAY_MERCH_CODE);
				omMerchCode.setValue(APIUtilVue.getInstance().encod(client.getMerchantCode()));
				omMerchCode.setFiltre(validity.toString());
				session.updateObject(omMerchCode);
			}	
		}
		
		String pin =  APIUtilVue.getInstance().encod(encryptedPin(client.getPinCode(), publicKey));
		if(omPhone ==null) {
			omPhone = new ToolsPartner();
			omPhone.setCode(codePhone);
			omPhone.setDate(new Date());
			omPhone.setKey(codePhone);
			omPhone.setValue(phone);
			omPhone.setFiltre(validity.toString());	
			session.saveObject(omPhone);
		}else {
			omPhone.setDate(new Date());
			omPhone.setKey(codePhone);
			omPhone.setValue(phone);
			omPhone.setFiltre(validity.toString());
			session.updateObject(omPhone);
		}
			if(omPinCode ==null) {
				omPinCode = new ToolsPartner();
				omPinCode.setCode(codePIN);
				omPinCode.setDate(new Date());
				omPinCode.setKey(codePIN);
				omPinCode.setValue(pin);
				omPinCode.setFiltre(validity.toString());
				session.saveObject(omPinCode);
			}else {
				omPinCode.setDate(new Date());
				omPinCode.setValue(pin);
				omPinCode.setFiltre(validity.toString());
				session.updateObject(omPinCode);
			}
		}
		catch (Exception e) {
			logger.error("saveNumber",e);
		}
	}

	/*public OMPayResponse paymentWithPayer(PaymentRequest request, IsoAcquisition acqui, BigDecimal fees,
			String payerCode) {
		OMPayResponse resp;
		try {
			logger.info("paiement OM with payer");
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			Partner partnerPayer = sess.executeNamedQuerySingle(Partner.class, "findPartnerByCode",
					new String[] { "code" }, new String[] { payerCode });

			String[] prms = { ValidateData.CHANNEL_TYPE, ValidateData.ID_PARTNER };
			Object[] data = { acqui.getChannelType(), partnerPayer.getIdPartner() };
			List<PayerCommission> payerComm = sess.executeNamedQueryList(PayerCommission.class,
					"findPayerCommissionByCp", prms, data);
			if (payerComm == null || payerComm.isEmpty()) {
				logger.info("commission payer not configured");
				resp = new OMPayResponse();
				resp.setBank(payerCode);
				resp.setAmount(new BigDecimal(acqui.getField4()));
				resp.setCurrencyName(acqui.getCaisse().getPointDeVente().getCommercant().getCurrencyName());
				resp.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
				resp.setTransactionId(acqui.getId().toString());
				resp.setMeansType(PaymentUtils.TRX_TYPE_WALLET);
				resp.setMessage(resp.getStatus());
				resp.setRequestId(acqui.getField63());
				acqui.setField39(Constantes.ISO_UNKNOWN_ERROR_STATUS);
				sess.updateObject(acqui);
			} else {
				String phone = request.getPhone().replace(" ", "");
				phone = phone.substring(phone.length() - 9);
				String idfWallet = "OM-" + phone;
				APIUtilVue utilVue = APIUtilVue.getInstance();
				Transaction trx = new Transaction();
				String inTrRefNumber = phone + acqui.getId();
				BigDecimal amount = fees.add(new BigDecimal(acqui.getField4())).setScale(0, RoundingMode.HALF_UP);
				trx.setDate(new Date());
				trx.setInTrRefNumber(inTrRefNumber);
				trx.setAccountNumber(acqui.getCaisse().getPointDeVente().getCommercant().getPrincipalAccountNumber());
				trx.setChannelType(acqui.getChannelType());
				trx.setPayinAmount(amount);
				trx.setPayoutAmount(amount);
				trx.setPayinCommission(fees);
				trx.setRealAmount(amount.toString());
				trx.setPartner(acqui.getCaisse().getPointDeVente().getCommercant());
				trx.setPartnerPayer(partnerPayer);
				trx.setPayinCurrency(partnerPayer.getCurrencyName());
				trx.setToCountry(partnerPayer.getCountry());
				trx.setFromCountry(partnerPayer.getCountry());
				trx.setIdtWallet(acqui.getField44());
				trx.setIdfWallet(idfWallet);
				trx.setSenderMobileNumber(phone);
				trx.setPrefixeWallet("OM");
				trx.setPayoutCurrency(acqui.getCaisse().getPointDeVente().getCommercant().getCurrencyName());
				trx.setStatus(BEConstantes.STATUS_TRANSACTION_PENDING);
				trx.setBillReferenceNumber(acqui.getId().toString());

				trx = (Transaction) sess.saveObject(trx);
				acqui.setField52(trx.getId().toString());
				acqui.setField56(Long.toString(trx.getId()));
				utilVue.callPayer(trx);
				resp = new OMPayResponse();
				resp.setAmount(new BigDecimal(acqui.getField4()));
				resp.setCurrencyName(acqui.getCaisse().getPointDeVente().getCommercant().getCurrencyName());
				resp.setTransactionId(acqui.getId().toString());
				resp.setRequestId(acqui.getField63());
				resp.setBank(payerCode);
				resp.setCode(Constantes.ISO_PENDING_STATUS);
				resp.setStatus(BEConstantes.STATUS_TRANSACTION_PENDING);
				resp.setMessage(BEConstantes.STATUS_TRANSACTION_PENDING);

				sess.updateObject(acqui);
				sess.updateObject(trx);
			}

		} catch (Exception e) {
			logger.error("ErrorOM", e);
			resp = new OMPayResponse(ErrorResponse.UNKNOWN_ERROR, "");
		}
		return resp;
	}*/

	/*public OMPayResponse sendOperation(PaymentRequest request, String auth) {
		OMPayResponse resp;
		try {
			try (CloseableHttpClient http = HttpClients.createDefault()) {
				String url = tools.getProperty(Constantes.WILDFLY_LOCAL) + Constantes.SEND_OPERATION;
				logger.info(url);
				HttpPost post = new HttpPost(url);
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(ValidateData.AUTH, auth);

				ObjectMapper mapper = new ObjectMapper();
				String json = mapper.writeValueAsString(request);
				StringEntity entity = new StringEntity(json);
				post.setEntity(entity);
				try (CloseableHttpResponse response = http.execute(post)) {
					logger.info(json);
					logger.info(response.getStatusLine().getStatusCode());
					String textRsp = new String(response.getEntity().getContent().readAllBytes(),
							StandardCharsets.UTF_8);
					logger.info(textRsp);
					resp = mapper.readValue(textRsp, OMPayResponse.class);
					logger.info(resp.getCode());
					logger.info(resp.getMessage());
					logger.info(resp.toString());
				}
			}
		} catch (Exception e) {
			logger.error("ErrorSend", e);
			resp = new OMPayResponse(ErrorResponse.UNKNOWN_ERROR, "");

		}
		return resp;
	}*/

	/*public OMPayResponse getStatusOMPayer(IsoAcquisition acqui) {
		logger.info("status om");
		OMPayResponse resp = new OMPayResponse();
		resp.setBank(acqui.getField33());
		resp.setRequestId(acqui.getField63());
		resp.setTransactionId(acqui.getId().toString());
		resp.setAmount(new BigDecimal(acqui.getField4()));
		try {
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			String params[] = { "id" };
			Long[] id = { Long.parseLong(acqui.getField52()) };

			Transaction trx = sess.executeNamedQuerySingle(Transaction.class, "findTransactionById", params, id);
			logger.info(trx.getStatus());
			if (BEConstantes.STATUS_TRANSACTION_PENDING.equals(trx.getStatus())) {
				long diffTime = getTimeDifference(trx.getDate());
				logger.info(diffTime);
				int timeout = 5 * 60;
				if (diffTime > timeout) {
					logger.info("timeout " + timeout);
					resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
					resp.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
				} else {
					resp.setStatus(BEConstantes.STATUS_TRANSACTION_PENDING);
					resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
				}
			} else {
				String code = BEConstantes.STATUS_TRANSACTION_PAYED.equals(trx.getStatus())
						? ErrorResponse.REPONSE_SUCCESS.getCode()
						: ErrorResponse.UNKNOWN_ERROR.getCode();
				String status = BEConstantes.STATUS_TRANSACTION_PAYED.equals(trx.getStatus()) ? "SUCCESS" : "FAILED";
				resp.setCode(code);
				resp.setStatus(status);
				resp.setMessage(status);
			}
		} catch (Exception e) {
			logger.error("errOm " + e.getMessage());
			resp.setReturnUrl(acqui.getReturnUrl());

		}
		logger.info(resp.getStatus());
		return resp;
	}*/

	public OMPayResponse details(String orderCurrency, BigDecimal totalAmount, String partnerCode,BigDecimal orderFees) {
		OMPayResponse response;
		try {
			logger.info("orange money details");
			BigDecimal omAmount = new BigDecimal(totalAmount.toString());
			BigDecimal omFees = new BigDecimal(orderFees.toString());
			String omCurrency = "XOF";
			if (omCurrency.equals(orderCurrency)) {
				logger.info("freemoney same currency");
			} else {
				AdministrationSession adminSess = (AdministrationSession) BeanLocator
						.lookUp(AdministrationSessionBean.class.getSimpleName());
				CoursDevise coDevise = adminSess.findCourDevise(orderCurrency, orderCurrency,
						null);
				omAmount = omAmount.multiply(BigDecimal.valueOf(coDevise.getValeurAPG()));
				omFees = omFees.multiply(BigDecimal.valueOf(coDevise.getValeurAPG()));
				omAmount = omAmount.setScale(2, RoundingMode.HALF_UP);
				omFees = omFees.setScale(2, RoundingMode.HALF_UP);
			}
			response = new OMPayResponse();
			response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			response.setCurrency(omCurrency);
			response.setAmount(omAmount.add(omFees));
			response.setFees(omFees);
			
		} catch (Exception e) {
			response = new OMPayResponse(ErrorResponse.UNKNOWN_ERROR.getCode(), ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		logger.info("finish om payment");
		return response;
	}
	

	public long getTimeDifference(Date dateC) {
		String date = new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()).format(dateC);
		LocalDateTime dateLast = LocalDateTime.parse(date,
				DateTimeFormatter.ofPattern(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()));
		return ChronoUnit.SECONDS.between(dateLast, LocalDateTime.now(Clock.systemUTC()));

	}
	public OMClientDto getClient(String customerPhone) {
		OMClientDto client;
		try {
			String phone;
			String pin="";
			if(Constantes.PREPROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))||Constantes.PROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))) {
				phone =customerPhone.substring(customerPhone.length()-9);
			}else {
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			
			ToolsPartner omPhone = session.findObjectById(ToolsPartner.class, null,Constantes.OM_PAY_CUSTOMER_PHONE);
			phone = APIUtilVue.getInstance().apgDeCrypt(omPhone.getValue());
			ToolsPartner omPinCode = session.findObjectById(ToolsPartner.class, null, Constantes.OM_PAY_CUSTOMER_PIN);
			 pin = APIUtilVue.getInstance().apgDeCrypt(omPinCode.getValue());
			}
			client = new OMClientDto();
			client.setId(phone);
			client.setIdType(MSISDN);
			client.setEncryptedPinCode(pin);
			
		} catch (Exception e) {

			logger.error("initOMError", e);
			client =null;
		}
		return client;
	
	}
	
	public OMPayResponse prepCancel(Session sess,IsoAcquisition acqui) {
		logger.info("cancelOM");
		
		OMPayResponse response = new OMPayResponse();
		response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		//response.setPush(false);
		String object="ANNULATION PAIEMENT : "+acqui.getField56();
		String line ="\n";
		String phone =acqui.getField2().contains("-")?acqui.getField2().split("-")[1]:acqui.getField2();
		StringBuilder message =new StringBuilder();
		message.append("Cher Partenaire,")
		    .append(line)
		   .append("Nous vous prions de bien vouloir annuler la transaction avec les informations ci-dessous")
		   .append(line)
		   .append("Date : ")
		   .append(new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIM, Locale.FRANCE).format(acqui.getDateCreation()))
		   .append(line)
		   .append("Montant : ")
		   .append(acqui.getFees().add(new BigDecimal(acqui.getField4()).setScale(0)))
		   .append(line)
		   .append("Référence : ")
		   .append(acqui.getField56())
		   .append(line)
		   .append("Numéro téléphone : ")
		   .append(phone)
		   .append(line)
		   .append("Code Marchand : 490132")
		   .append(line)
		   .append("Veuillez nous confirmer par mail une fois la requête traitée")
		   .append(line)
		   .append("Cordialement");
		acqui.setField39(Constantes.ISO_TO_CANCELED_STATUS);
		sess.updateObject(acqui);
		//mailSender.sendSimpleMail(object, message.toString(), email);
	    MailUtils.sendEmails(email, object, message.toString(), true, null, null);
		return response;
	}
	public OMPayResponse confirmPayment(IsoAcquisition acqui) {

		logger.info("confirm classic payment");
		OMPayResponse resp;
		try {
			String token  = getSavedToken(Constantes.OM_PAY_TOKEN);
			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpPost post = new HttpPost("https://api.sandbox.orange-sonatel.com/api/eWallet/v1/transactions/"+acqui.getField56()+"/confirm");
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(HttpHeaders.AUTHORIZATION, token);
				OMClientDto client = getClient(acqui.getCardId());
				String request = new ObjectMapper().writeValueAsString(client);
				logger.info("request confirm classic "+request);
				StringEntity entity = new StringEntity(request);
				post.setEntity(entity);
				try (CloseableHttpResponse cHResp = http.execute(post)) {
					logger.info(cHResp.getStatusLine().getStatusCode());
					String respStr = EntityUtils.toString(cHResp.getEntity());
					logger.info("response "+respStr);
					 resp = new ObjectMapper().readValue(respStr, OMPayResponse.class);
				}
			}
		}
		catch (Exception e) {
			logger.error("classOMError",e);
			 resp = new OMPayResponse("FAILED","ereur");
		}
		return resp;
	}
	public OMPayResponse qrCodePayment(OMPaymentDto paymentDto,String token) {

		logger.info("paiement with qrcode payment");
		OMPayResponse resp;
		try {
			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpPost post = new HttpPost(baseUrl + "/api/eWallet/v4/qrcode");
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(HttpHeaders.AUTHORIZATION, token);
				paymentDto.setOtp(null);
				String request = new ObjectMapper().writeValueAsString(paymentDto);
				logger.info(request);		
				StringEntity entity = new StringEntity(request);
				post.setEntity(entity);
				try (CloseableHttpResponse cHResp = http.execute(post)) {
					logger.info(cHResp.getStatusLine().getStatusCode());
					int statusCode = cHResp.getStatusLine().getStatusCode();
					String respStr = EntityUtils.toString(cHResp.getEntity());
					logger.info("response "+respStr);
					resp = new ObjectMapper().readValue(respStr, OMPayResponse.class);
					if(statusCode ==HttpStatus.SC_ACCEPTED||statusCode ==HttpStatus.SC_CREATED||
							statusCode ==HttpStatus.SC_OK) {
					resp.setCode(Constantes.ISO_PENDING_STATUS);
					resp.setStatus(BEConstantes.STATUS_TRANSACTION_PENDING);
					resp.setMessage(BEConstantes.STATUS_TRANSACTION_PENDING);
					}else {
						resp.setCode(Constantes.ISO_UNKNOWN_ERROR_STATUS);
						resp.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
						resp.setMessage(BEConstantes.STATUS_TRANSACTION_FAILED);
						
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("initPayOMError",e);
			resp = new OMPayResponse(ErrorResponse.UNKNOWN_ERROR.getCode(),ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}

		return resp;
	}
	
	public String getSavedToken(String code) {
		
		logger.info(code);	
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		ToolsPartner omTOken = session.findObjectById(ToolsPartner.class, null, code);
		return "Bearer "+omTOken.getValue();
	}
	
	public String encryptedPin(String data, String publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
		return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
	}
	
	private PublicKey getPublicKey(String base64PublicKey){
		PublicKey publicKey = null;
		try{
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey.getBytes()));
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			publicKey = keyFactory.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException e) {
			
			logger.error("AlogEx",e);
		} catch (InvalidKeySpecException e) {
			logger.error("InvalidKey",e);
			
		}
		return publicKey;
	}
	
}

