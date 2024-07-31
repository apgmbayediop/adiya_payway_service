package sn.payway.payment.services;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.jbosslog.JBossLog;
import sn.apiapg.common.config.entities.ToolsPartner;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.ChannelResponse;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.aci.Caisse;
import sn.apiapg.entities.aci.CommissionMonetique;
import sn.apiapg.entities.aci.IsoAcquisition;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.PaywayCommonTools;
import sn.payway.common.utils.PaywayException;
import sn.payway.partner.orangemoney.dto.OMAmountDto;
import sn.payway.partner.orangemoney.dto.OMClientDto;
import sn.payway.partner.orangemoney.dto.OMPayResponse;
import sn.payway.partner.orangemoney.dto.OMPaymentDto;
import sn.payway.partner.orangemoney.service.OrangeMoneyService;
import sn.payway.payment.dto.PaymentDetails;
import sn.payway.payment.dto.PaymentDto;
import sn.payway.payment.dto.ReloadWalletDto;
import sn.payway.payment.dto.ReloadWalletResponse;
import sn.payway.payment.services.finalize.FinalizePayment;
import sn.payway.payment.utils.PaymentHelper;
import sn.payway.transaction.service.CommissionService;
import sn.payway.wave.dto.WaveRequest;
import sn.payway.wave.services.WaveService;

@Stateless
@JBossLog
public class WalletPayService {

	private String waveKey;
	private String cancelUrl;
	private String returnUrl;
	private String baseHookUrl;

	@Inject
	private PaywayCommonTools tools;
	@Inject
	private CommissionService commissionService;
	@Inject
	private PaymentHelper payHelper;
	@Inject
	private WaveService wave;
	@Inject
	private OrangeMoneyService omService;
	@Inject
	private FinalizePayment finalizePay;

	@PostConstruct
	public void init() {
		initWave();
	}

	public void initWave() {
		try {

			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			ToolsPartner keyAPG = sess.findObjectById(ToolsPartner.class, null, "SESSION_PARTNER_PAYER_WAVE");

			String bearer = "Bearer ";
			waveKey = bearer + APIUtilVue.getInstance().apgDeCrypt(keyAPG.getValue());
			if (Constantes.PREPROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))) {
				baseHookUrl = "https://prepayway.afripayway.com";
			} else if (Constantes.PROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))) {
				baseHookUrl = "https://payway.afripayway.com";
			}  
			else {
				baseHookUrl = "https://wdev.afripayway.com";
			}
			cancelUrl = baseHookUrl + WaveService.ERROR_URL;
			returnUrl = baseHookUrl + WaveService.SUCCESS_URL;
		} catch (Exception e) {
			log.error("initWave", e);
		}
	}
	public PaymentDetails onlinePayment(IsoAcquisition acqui, PaymentDto request) {
		PaymentDetails resp =new PaymentDetails();
		BigDecimal amount = new BigDecimal(acqui.getField4());
		try {
			log.info("wallet onlinePayment");
			Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
			
			request.setAmount(amount);
			request.setCurrencyName(acqui.getField49());
			acqui.setField33(request.getBank());
			request.setMeansType(Constantes.WALLET);
			CommissionMonetique reqCom =new CommissionMonetique();
			reqCom.setMeansType(Constantes.WALLET);
			reqCom.setSubMeansType(request.getBank());
			reqCom.setChannelType(acqui.getChannelType());
			reqCom.setPartner(acqui.getCaisse().getPointDeVente().getCommercant());
			CommissionMonetique commMonetique =commissionService.findCommissionMonetique(reqCom);
			if (commMonetique == null) {
				log.info("commission not configured for channel " + acqui.getChannelType());
				resp.setCode(ErrorResponse.COMMISSION_NOT_FOUND.getCode());
				resp.setMessage(ErrorResponse.COMMISSION_NOT_FOUND.getMessage(""));
			}else {
			BigDecimal fees = BigDecimal.ZERO;
			ChannelResponse channel = ChannelResponse.getChannel(acqui.getChannelType());
			if(OnlinePaymentService.CUSTOMER_PAYED_CHANNELS.contains(channel)) {
				boolean isRegistration ="REGISTRATION".equals(request.getPurpose())||(ChannelResponse.W2W.getCode().equals(acqui.getChannelType())&&amount.equals(new BigDecimal(5)));
				fees = isRegistration?new BigDecimal(1):payHelper.roundingFees(acqui.getField49(),commissionService.getCommissionMonetique(amount, commMonetique));
			   acqui.setFees(fees);
			}
			request.setFees(fees);
			resp = sendPayment(request.getBank(), request, acqui.getCaisse());
			request.setCode(resp.getCode());
			String field53 = resp.getVersion()==null?acqui.getField53():resp.getVersion();
			acqui.setField53(field53);
			sess.updateObject(acqui);
			if(!Constantes.ISO_PENDING_STATUS.equals(resp.getCode())) {
				resp= finalizePay.updateTransaction(resp.getCode(), resp, acqui, null);
			}
			
			}
		}
		catch (Exception e) {
			log.error("errorWalletonlP", e);
			resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			resp.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		resp.setAmount(amount);
		resp.setRequestId(request.getRequestId());
		resp.setTransactionId(request.getTransactionId());
		resp.setCurrencyName(acqui.getField49());
		resp.setReturnUrl(acqui.getReturnUrl());
		resp.setBank(request.getBank());
		resp.setAuditNumber(request.getTransactionId());
		resp.setTransactionDate(new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME,Locale.getDefault()).format(acqui.getDateCreation()));
		log.info(resp.getCode());
		return resp;
		}

	public ReloadWalletResponse cardToWallet(ReloadWalletDto dto) {
		return null;
	}

	public PaymentDetails directPayment(Caisse caisse, PaymentDto dto) throws PaywayException {
		PaymentDetails resp = new PaymentDetails();
		resp.setAmount(dto.getAmount());
		resp.setRequestId(dto.getRequestId());
		resp.setTransactionDate(
				new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()).format(new Date()));
		try {
			log.info("wallet onlinePayment");
			String bank = getBankWallet(dto.getBank());
			CommissionMonetique reqCom = new CommissionMonetique();
			reqCom.setMeansType(Constantes.WALLET);
			reqCom.setSubMeansType(dto.getBank());
			reqCom.setChannelType(dto.getTransactionType());
			reqCom.setPartner(caisse.getPointDeVente().getCommercant());
			log.info(reqCom);
			CommissionMonetique commMonetique = commissionService.findCommissionMonetique(reqCom);
			if (commMonetique == null) {
				log.info("commission not configured for channel " + dto.getTransactionType());
				resp.setCode(ErrorResponse.COMMISSION_NOT_FOUND.getCode());
				resp.setMessage(ErrorResponse.COMMISSION_NOT_FOUND.getMessage(""));
			} else {
				log.info(commMonetique);
				BigDecimal fees = BigDecimal.ZERO;
				ChannelResponse channel = ChannelResponse.getChannel(dto.getTransactionType());
				if (OnlinePaymentService.CUSTOMER_PAYED_CHANNELS.contains(channel)) {
					boolean isRegistration = "REGISTRATION".equals(dto.getPurpose())
							|| (ChannelResponse.W2W.getCode().equals(dto.getTransactionType())
									&& dto.getAmount().equals(new BigDecimal(5)));
					fees = isRegistration ? new BigDecimal(1)
							: payHelper.roundingFees(dto.getCurrencyName(),
									commissionService.getCommissionMonetique(dto.getAmount(), commMonetique));
				}
				Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
				String transmettereur = "000000";
				IsoAcquisition acqui = payHelper.createIsoAcquisition(dto, transmettereur, transmettereur);
				String requestId = caisse.getNumeroCaisse() + "-" + dto.getRequestId();
				acqui.setField33(bank);
				acqui.setPaymentMeansType(dto.getMeansType());
				acqui.setFees(fees);
				acqui.setCaisse(caisse);
				acqui.setRequestId(requestId);
				acqui.setField63(dto.getRequestId());
				if(dto.getPhone()!=null) {
				  String phone = dto.getBank()+"-"+dto.getPhone();
				 acqui.setField2(phone);
				}
				acqui = (IsoAcquisition) sess.saveObject(acqui);
				dto.setTransactionId(acqui.getId().toString());
				dto.setFees(fees);
				resp = sendPayment(bank, dto, caisse);
				resp.setTransactionId(acqui.getId().toString());
				resp.setRequestId(dto.getRequestId());
				resp.setCurrencyName(dto.getCurrencyName());
				resp.setReturnUrl(dto.getReturnUrl());
				resp.setBank(bank);
				resp.setAmount(dto.getAmount());
				resp.setFees(fees);
				resp.setReturnUrl(acqui.getReturnUrl());
				String responseCode = Constantes.ISO_SUCCESS_STATUS.equals(resp.getCode())?ErrorResponse.REPONSE_SUCCESS.getCode():resp.getCode();
				resp.setCode(responseCode);
				log.info(resp.getCode());
				if (Constantes.ISO_PENDING_STATUS.equals(resp.getCode())) {
					acqui.setField56(resp.getReferencePayer());
					acqui.setField39(Constantes.ISO_PENDING_STATUS);
					String field53 = resp.getVersion()==null?acqui.getField53():resp.getVersion();
					acqui.setField53(field53);
				} else {
					
					String finalCode = resp.getCode();
					final  PaymentDetails finalResp =resp;
					final IsoAcquisition finalAcqui = acqui;
					Executors.newSingleThreadExecutor().execute(()->finalizePay.updateTransaction(finalCode, finalResp, finalAcqui, null));
				}
				sess.updateObject(acqui);

			}
		} catch (Exception e) {
			log.error("errorWalletonlP", e);
			resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			resp.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		log.info(resp.getCode());
		resp.setReferencePayer(null);
		resp.setResponsePayer(null);
		return resp;
	}

	public PaymentDetails sendPayment(String bank, PaymentDto dto, Caisse caisse) throws PaywayException {

		switch (bank) {
		case Constantes.CODE_OM_API: {
			return omPayment(dto,caisse);
		}
		case WaveService.WAVE_CODE: {
			return wavePayment(dto, caisse);
		}
		default:
			throw new PaywayException(ErrorResponse.UNKNOWN_ERROR.getCode(),
					ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}

		
	}

	public PaymentDetails wavePayment(PaymentDto dto, Caisse caisse) {
		PaymentDetails response = new PaymentDetails();
		try {
			WaveRequest reqPay = new WaveRequest();
			BigDecimal amount = dto.getAmount().add(dto.getFees()).setScale(0);
			reqPay.setAmount(amount);
			reqPay.setClientReference(dto.getTransactionId());
			reqPay.setCurrency("XOF");
			String completeUrl = dto.getRequestId() + "/" + dto.getTransactionId() + "/" + WaveService.WAVE_CODE;
			reqPay.setSuccessUrl(returnUrl + completeUrl);
			reqPay.setErrorUrl(cancelUrl + completeUrl);
			String commingReturUrl = dto.getReturnUrl() == null ? "" : dto.getReturnUrl();
			if (commingReturUrl.contains("intero.afripayway.com")) {
				reqPay.setSuccessUrl(commingReturUrl);
				reqPay.setErrorUrl(commingReturUrl);
			}
				WaveRequest merch = wave.getMerchant(waveKey, caisse.getPointDeVente().getCommercant());
				reqPay.setAggregatedMerchantId(merch.getId());
			
			WaveRequest wavResp = wave.payment(reqPay, waveKey);
			response.setResponsePayer(wavResp.getMessage());
			response.setReferencePayer(wavResp.getId());
			if (WaveService.PROCESSING_RESPONSE.equalsIgnoreCase(wavResp.getPaymentStatus())) {
				response.setPaymentUrl(wavResp.getWaveLaunchUrl());
				response.setCode(Constantes.ISO_PENDING_STATUS);
				response.setStatus(BEConstantes.STATUS_TRANSACTION_PENDING);
				response.setMessage(BEConstantes.STATUS_TRANSACTION_PENDING);

			} else {
				response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
				response.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
				response.setMessage(BEConstantes.STATUS_TRANSACTION_FAILED);
			}
		} catch (Exception e) {
			log.error("waveError", e);
			response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			response.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
			response.setMessage(BEConstantes.STATUS_TRANSACTION_FAILED);
		}
		response.setBank(WaveService.WAVE_CODE);
		return response;
	}

	public PaymentDetails omPayment(PaymentDto request, Caisse caisse) {
		log.info("paiement with OrangeMoney");
		log.info(request.getBank());
		PaymentDetails response;
		String paymentType;
		try {
			String phone = request.getPhone().replace(" ", "");
			phone = phone.substring(phone.length() - 9);
			OMPaymentDto dto = new OMPaymentDto();
			OMAmountDto amountDto = new OMAmountDto();
			BigDecimal totalAmount = request.getAmount().add(request.getFees()).setScale(0);
			Partner commercant = caisse.getPointDeVente().getCommercant();
			amountDto.setUnit(commercant.getCurrencyName());
			amountDto.setValue(totalAmount);
			dto.setAmount(amountDto);
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			HashMap<String, String> merchantCodes = new HashMap<>();
			String token;
			String otp = request.getOtp();
			paymentType = request.getPaymentType()==null||request.getPaymentType().isBlank()?OrangeMoneyService.PAYMENT_TYPE_CLASSIC:request.getPaymentType();
			if (Constantes.PREPROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))
					|| Constantes.PROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))) {
				merchantCodes.put(Constantes.ORANGE_CODE, Constantes.ORANGE_CODE);
				merchantCodes.put(Constantes.ORANGE_PIN, Constantes.ORANGE_PIN);
				merchantCodes.put(Constantes.ORANGE_RETAILER, Constantes.ORANGE_RETAILER);
				token = omService.getSavedToken(BEConstantes.APG_OM_TOKEN);
			} else {
				merchantCodes.put(Constantes.ORANGE_CODE, Constantes.OM_PAY_MERCH_CODE);
				merchantCodes.put(Constantes.ORANGE_PIN, Constantes.OM_PAY_PIN);
				merchantCodes.put(Constantes.ORANGE_RETAILER, Constantes.OM_PAY_PHONE);
				token = omService.getSavedToken(Constantes.OM_PAY_TOKEN);
				ToolsPartner omPin = sess.findObjectById(ToolsPartner.class, null, Constantes.OM_PAY_CUSTOMER_PIN);
				String pin = APIUtilVue.getInstance().apgDeCrypt(omPin.getValue());
				ToolsPartner omPhone = sess.findObjectById(ToolsPartner.class, null, Constantes.OM_PAY_CUSTOMER_PHONE);
				String phoneTest = APIUtilVue.getInstance().apgDeCrypt(omPhone.getValue());
				if (OrangeMoneyService.PAYMENT_TYPE_CODE.equals(request.getPaymentType())) {
				OMClientDto testCustomer = new OMClientDto();
				testCustomer.setId(phoneTest);
				testCustomer.setEncryptedPinCode(pin);
				testCustomer.setIdType(OrangeMoneyService.MSISDN);
				testCustomer.setWalletType(OrangeMoneyService.PRINCIPAL);
				otp = omService.generateOtp(testCustomer, token).getOtp();
				}
				phone = phoneTest;
			}
			ToolsPartner omCode = sess.findObjectById(ToolsPartner.class, null,
					merchantCodes.get(Constantes.ORANGE_CODE));
			String merchantCode = APIUtilVue.getInstance().apgDeCrypt(omCode.getValue());
			OMPayResponse omPayResp;
			if (OrangeMoneyService.PAYMENT_TYPE_QRCODE.equals(paymentType)) {
				String completeUrl = request.getRequestId() + "/" + request.getTransactionId() + "/"
						+ Constantes.CODE_OM_API;
				dto.setCallbackCancelUrl(cancelUrl + completeUrl);
				dto.setCallbackSuccessUrl(returnUrl + completeUrl);
				dto.setCode(merchantCode);
				dto.setName(commercant.getName());
				ObjectNode metadata = new ObjectMapper().createObjectNode();
				metadata.put("id", request.getTransactionId());
				metadata.put("msisdn", phone);
				metadata.put("requestId", request.getRequestId());
				dto.setMetadata(metadata);
				omPayResp = omService.qrCodePayment(dto, token);
			}else {
				OMClientDto customer = new OMClientDto();
				customer.setId(phone);
				customer.setIdType(OrangeMoneyService.MSISDN);
				customer.setWalletType(OrangeMoneyService.PRINCIPAL);
				customer.setOtp(otp);
				dto.setCustomer(customer);
				dto.setMethod(paymentType);
				dto.setReference(request.getTransactionId());
				OMClientDto omPartner = new OMClientDto();
				dto.setReceiveNotification(false);
				 if(OrangeMoneyService.PAYMENT_TYPE_CLASSIC.equals(paymentType)) {
					ToolsPartner omPin = sess.findObjectById(ToolsPartner.class, null, merchantCodes.get(BEConstantes.ORANGE_PIN));
					String pin=APIUtilVue.getInstance().apgDeCrypt(omPin.getValue());
					ToolsPartner omMercPhone = sess.findObjectById(ToolsPartner.class, null, merchantCodes.get(BEConstantes.ORANGE_RETAILER));
					String merchPhone = APIUtilVue.getInstance().apgDeCrypt(omMercPhone.getValue());
					omPartner.setEncryptedPinCode(pin);
					omPartner.setId(merchPhone);
					omPartner.setIdType(OrangeMoneyService.MSISDN);
					
				 }else {
					 omPartner.setId(merchantCode);
					omPartner.setIdType("CODE");
				 }
				dto.setPartner(omPartner);
				omPayResp =OrangeMoneyService.PAYMENT_TYPE_CLASSIC.equals(paymentType)? omService.classicPayment(dto, token):omService.oneStepPayment(dto, token);
			}
			response = new PaymentDetails();
			String responseCode = Constantes.ISO_SUCCESS_STATUS.equals(omPayResp.getCode())?ErrorResponse.REPONSE_SUCCESS.getCode():omPayResp.getCode();
			response.setCode(responseCode);
			response.setMessage(omPayResp.getMessage());
			response.setStatus(omPayResp.getStatus());
			response.setPaymentUrl(omPayResp.getDeepLink());
			response.setQrCode(omPayResp.getQrCode());
			response.setReferencePayer(omPayResp.getTransactionId());
			response.setPaymentType(request.getPaymentType());
			response.setVersion(OrangeMoneyService.VERSION2);
			
			
		} catch (Exception e) {
			log.error("omPay", e);
			response = new PaymentDetails(ErrorResponse.UNKNOWN_ERROR.getCode(),ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		response.setBank(Constantes.CODE_OM_API);
		return response;

	}
	

	public String getBankWallet(String bank) throws PaywayException {
		String codeBank;
		switch (bank.toUpperCase(Locale.getDefault())) {
		case "OMSN":
			codeBank = Constantes.CODE_OM_API;
			break;
		// case "FMSN": codeBank = FreeMoneyServices.CODE_FREEMONEY;break;
		case "WVSN":
			codeBank = WaveService.WAVE_CODE;
			break;
		default:
			throw new PaywayException(ErrorResponse.UNKNOWN_ERROR.getCode(),
					ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return codeBank;
	}

	public String getWaveKey() {
		return waveKey;
	}
}
