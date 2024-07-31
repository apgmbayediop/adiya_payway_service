package sn.payway.payment.services.finalize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lombok.extern.jbosslog.JBossLog;
import sn.apiapg.account.entities.PartnerAccount;
import sn.apiapg.commission.entities.PayerCommission;
import sn.apiapg.commission.entities.SenderCommission;
import sn.apiapg.common.config.entities.CoursDevise;
import sn.apiapg.common.config.entities.ToolsPartner;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AccountManager;
import sn.apiapg.common.utils.AccountManagerBean;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.ChannelResponse;
import sn.apiapg.common.utils.MailUtils;
import sn.apiapg.entities.Account;
import sn.apiapg.entities.AccountTransaction;
import sn.apiapg.entities.AccountWallet;
import sn.apiapg.entities.BulkPaymentFile;
import sn.apiapg.entities.Card;
import sn.apiapg.entities.CardAcctTransaction;
import sn.apiapg.entities.Country;
import sn.apiapg.entities.Currency;
import sn.apiapg.entities.EventBookedTicket;
import sn.apiapg.entities.EventTicket;
import sn.apiapg.entities.LinkedCard;
import sn.apiapg.entities.OptimaFavorites;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.Transaction;
import sn.apiapg.entities.Wallet;
import sn.apiapg.entities.WalletAcctTransaction;
import sn.apiapg.entities.aci.Caisse;
import sn.apiapg.entities.aci.CommissionMonetique;
import sn.apiapg.entities.aci.IsoAcquisition;
import sn.apiapg.session.AdministrationSession;
import sn.apiapg.session.AdministrationSessionBean;
import sn.apiapg.session.MessageSenderService;
import sn.apiapg.session.MessageSenderServiceBean;
import sn.apiapg.session.OperationSession;
import sn.apiapg.session.OperationSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.ValidateData;
import sn.payway.payment.dto.DataResponse;
import sn.payway.payment.dto.DispatchPayCommission;
import sn.payway.payment.dto.PaymentDetails;
import sn.payway.payment.dto.PaymentDto;
import sn.payway.payment.dto.Person;
import sn.payway.payment.dto.SmsPayDto;
import sn.payway.payment.sd.SDManager;
import sn.payway.payment.services.GatewayApiController;
import sn.payway.payment.services.OnlinePaymentService;
import sn.payway.payment.sms.SMSSender;
import sn.payway.payment.tokenization.CardTokenization;
import sn.payway.payment.utils.PaymentHelper;
import sn.payway.transaction.account.service.PartnerAccountService;
import sn.payway.transaction.service.CommissionService;
import sn.payway.transaction.wallet.service.WalletAcctService;

@Stateless
@JBossLog
public class FinalizePayment {

	public static final String PAYWAY_CODE_BANK = "PAYWAY";
	public static final BigDecimal HUNDRED = new BigDecimal("100");
	public static final String OPTIMA_CODE="87262";
	public static final String GENESYS_NETWORK ="GENESYS";
	@Inject
	private CommissionService commissionService;
	@Inject
	private CardTokenization tokenization;
	@Inject
	private PaymentHelper payHelper;
	@Inject
	private PartnerAccountService pAcctService;
	@Inject
	private WalletAcctService walletAcctService;
	@Inject
	private SMSSender smsSender;
	@Inject
	private SDManager sdManager;
	@Inject
	private GatewayApiController gatwayApi;
	
	
	
	/*
	 * @Inject private GenesysServices genesys;
	 */

	public PaymentDetails updateTransaction(String statusCode, PaymentDto request, IsoAcquisition acquisition,
			Card customerCard) {
		PaymentDetails resp = new PaymentDetails(ErrorResponse.UNKNOWN_ERROR, "");
		try {
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			tokenization.addToken(sess, request, acquisition);
			Caisse caisse = acquisition.getCaisse();
			BigDecimal amount = new BigDecimal(acquisition.getField4());
			request.setAmount(amount);
			request.setPan(acquisition.getField2());
			request.setCurrencyName(acquisition.getField49());
			request.setTerminalNumber(caisse.getNumeroCaisse());
			request.setPosNumber(caisse.getPointDeVente().getNumeroPointDeVente());
			request.setMerchantNumber(Long.toString(caisse.getPointDeVente().getCommercant().getIdPartner()));
			request.setMerchantAddress(caisse.getPointDeVente().getCommercant().getName());
			request.setTransactionType(acquisition.getChannelType());
			request.setMeansType(acquisition.getPaymentMeansType());
			resp.setReturnUrl(acquisition.getReturnUrl());
			resp.setAmount(amount);
			resp.setAutorisationCode(acquisition.getField38());
			resp.setAuditNumber(StringUtils.leftPad(acquisition.getId().toString(), 8, '0'));
			resp.setTransactionDate(new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault())
					.format(acquisition.getDateCreation()));
			resp.setCurrencyName(acquisition.getField49());
			resp.setTransactionId(acquisition.getId().toString());
			resp.setRequestId(acquisition.getField63());
			resp.setBank(request.getBank());

			if (acquisition.getField56() == null) {
				acquisition.setField56(request.getReferencePayer());
			}
			if (Constantes.ISO_PENDING_STATUS.equals(statusCode)) {
				resp.setCode(ErrorResponse.PENDING_PAYMENT.getCode());
				resp.setStatus("PENDING");
				resp.setMessage(resp.getStatus());
				return resp;
			}
			acquisition.setRecon(0);
			acquisition.setStatus(0);
			request.setCommissionSender(BigDecimal.ZERO);
			String codeBank = request.getBank()==null?acquisition.getField33():request.getBank();
			String codeBanque = "banque = " + codeBank;
			log.info(codeBanque);
			Partner sender = caisse.getPointDeVente().getCommercant();
			// Wallet wallet =null;
			String referencePayer = acquisition.getField56()==null?request.getReferencePayer():acquisition.getField56();
			acquisition.setField56(referencePayer);
			if (!PAYWAY_CODE_BANK.equals(codeBank)) {
				sender = sess.executeNamedQuerySingle(Partner.class, "findPartnerByCode", new String[] { "code" },
						new String[] { codeBank });
				acquisition.setField33(codeBank);
				if (BEConstantes.ORABANK_CODE.equalsIgnoreCase(request.getBank())) {
					request.setMeansType(Constantes.CARTE);
					acquisition.setField46(request.getCardType());
					
				}
			}

			CommissionMonetique reqComm = new CommissionMonetique();
			reqComm.setMeansType(acquisition.getPaymentMeansType());
			reqComm.setChannelType(acquisition.getChannelType());
			reqComm.setPartner(caisse.getPointDeVente().getCommercant());
			reqComm.setMeansType(acquisition.getPaymentMeansType());
			reqComm.setSubMeansType(payHelper.getSubMeansTypeFromBank(codeBank));
			CommissionMonetique commission = commissionService.findCommissionMonetique(reqComm);

			if (ErrorResponse.REPONSE_SUCCESS.getCode().equals(statusCode)||Constantes.ISO_SUCCESS_STATUS.equals(statusCode)) {
				acquisition.setField39(Constantes.ISO_SUCCESS_STATUS);
				sess.updateObject(acquisition);
				resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
				resp.setMessage("SUCCESS");
				resp.setStatus(resp.getMessage());

			} else {
				resp.setMessage(BEConstantes.STATUS_TRANSACTION_FAILED);
				resp.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
				acquisition.setField39(Constantes.ISO_UNKNOWN_ERROR_STATUS);
			}

			sess.updateObject(acquisition);
			request.setTransactionId(acquisition.getId().toString());
			request.setRequestId(acquisition.getField63());
			saveTransaction(request, acquisition, commission, customerCard, request.getCardType(), sender);
			return resp;
		} catch (Exception e) {
			log.error("error", e);
			resp.setMessage(BEConstantes.STATUS_TRANSACTION_FAILED);
			resp.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
			return resp;
		}

	}

	@Asynchronous
	public void saveTransaction(PaymentDto request, IsoAcquisition acqui, CommissionMonetique comm, Card customerCard,
			String network, Partner payer) {
		DataResponse resp = saveOperation(request, acqui, comm, customerCard, network, payer);
		if (resp != null) {
			request.setTransactionType(acqui.getChannelType());
			smsSender.sendNotification(request, resp, network);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	private DataResponse saveOperation(PaymentDto request, IsoAcquisition acqui, CommissionMonetique comm,
			Card customerCard, String network, Partner sender) {
		MessageSenderService senderM = (MessageSenderService) BeanLocator
				.lookUp(MessageSenderServiceBean.class.getSimpleName());
		DataResponse resp = null;
		String customerEmail = null;
		String merchantEmail;
		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		Transaction trx = null;
		Wallet wallet =null;
		try {
			log.info("********saveTransaction*************");
			log.info("result iso " + acqui.getField39());
			log.info("cardType " + network);
			trx = createPaymentTransaction(sess, acqui, request, comm, sender, network);
			trx.setResponsePayeur(request.getResponsePayer());
			Map<String, String> dataCustomer = new ConcurrentHashMap<>();
			trx.setFromCard(customerCard);
			trx.setTag(network);
			if (trx.getId() == null) {
				trx = (Transaction) sess.saveObject(trx);
			}
			acqui.setField52(trx.getId().toString());
			BigDecimal totalAmount = new BigDecimal(acqui.getField4());
			if (Constantes.ISO_SUCCESS_STATUS.equals(acqui.getField39())) {
				trx.setStatus(BEConstantes.STATUS_TRANSACTION_VALIDATED);

				acqui.setStatus(1);
				acqui.setRecon(0);
				acqui.setField52(trx.getId().toString());
				sess.updateObject(acqui);
				log.info("trxtId " + trx.getId());
				Partner commercant = acqui.getCaisse().getPointDeVente().getCommercant();
				String apgNetwork = "APG";
				if (apgNetwork.equals(network) || OPTIMA_CODE.equals(sender.getCode())) {
					BigDecimal commissionAPG = trx.getCommissionAPG().add(trx.getCommissionPayerSA());
					trx.setCommissionAPG(commissionAPG);
					trx.setCommissionPayerSA(BigDecimal.ZERO);
					if(Constantes.CARTE.equals(request.getMeansType())) {
						dataCustomer = updateCardBalance(customerCard, false, totalAmount, trx, request);
						customerEmail = customerCard.getRegister().getEmail();
					}else {
						 wallet =sess.executeNamedQuerySingle(Wallet.class, "findMyWallet", new String[] {"wallet"}, new String[] {acqui.getCardId()});
						 dataCustomer = updateWalletBalance(wallet, false, totalAmount, trx, sess, request);
						customerEmail = wallet.getEmail();
					}
							
				}
				Map<String, String> dataPartner;
				dataPartner = updateMerchantBalance(commercant, true, trx.getPayoutAmount(), trx, acqui);
				if (acqui.getCaisse().getTelephone() == null) {
					merchantEmail = commercant.getEmailContact();
					Caisse caisse = acqui.getCaisse();
					caisse.setEmail(merchantEmail);
					caisse.setTelephone(commercant.getCountryIndicatif() + commercant.getTelephonePartner());
					sess.updateObject(caisse);
				} else {
					merchantEmail = acqui.getCaisse().getEmail() == null ? commercant.getEmailContact()
							: acqui.getCaisse().getEmail();
				}

				if (!sender.equals(trx.getPartner()) && !BEConstantes.ESPACE_PAYEUR.equals(sender.getPType())) {
					updateSenderBalance(sender, acqui.getField49(), new BigDecimal(acqui.getField4()), trx, true);
				}
				sess.updateObject(trx);
				resp = new DataResponse();
				resp.setCustomerCard(customerCard);
				resp.setMerchantCard(trx.getToCard());
				resp.setDataCustomer(dataCustomer);
				resp.setDataPartner(dataPartner);
				resp.setCustomerEmail(customerEmail);
				resp.setMerchantEmail(merchantEmail);
				resp.setWallet(wallet);
				resp.setTrx(trx);
				resp.setCaisse(acqui.getCaisse());
				log.info(acqui.getCardId());
				sess.updateObject(trx);
				finaliseOperationByChannel(trx, acqui, sess, request);
			} else {
				sess.updateObject(trx);
				sess.updateObject(acqui);
				log.info("transaction failed " + trx.getId());
			}
			sess.flush();
			log.info("********finalize*************");

			return resp;
		} catch (Exception e) {
			log.error("finalizee", e);
			senderM.notifysendSMS("APGSA", SMSSender.SN_INDICATIF, "221773790423",
					"erreur save payment trx " + request.getTerminalNumber() + " " + e.getMessage());
			return null;
		} finally {
			updateCorrespondantOperation(sess, acqui, trx);
		}
	}

	private Transaction createPaymentTransaction(Session sess, IsoAcquisition acqui, PaymentDto request,
			CommissionMonetique comm, Partner sender, String network) {

		Transaction trx = null;
		try {
			if (acqui.getField52() == null) {
				trx = new Transaction();
			} else {

				trx = sess.executeNamedQuerySingle(Transaction.class, "findTransactionById", new String[] { "id" },
						new Long[] { Long.parseLong(acqui.getField52()) });
			}
			Caisse caisse = acqui.getCaisse();
			Partner commercant = caisse.getPointDeVente().getCommercant();
			boolean withBanque = sender != null && !sender.equals(trx.getPartner())
					&& !BEConstantes.ESPACE_PAYEUR.equals(sender.getPType());
			ChannelResponse channel = ChannelResponse.getChannel(acqui.getChannelType());
			DispatchPayCommission commResp = getCommissionPayment(request, channel, comm, acqui, sender, withBanque);
			BigDecimal payinAmount = new BigDecimal(acqui.getField4());
			BigDecimal payoutAmount = new BigDecimal(acqui.getField4());
			String senderCurrency = sender == null ? commercant.getCurrencyName() : sender.getCurrencyName();
			Currency currencyIn = sess.executeNamedQuerySingle(Currency.class, "Currency.findByName",
					new String[] { "currencyName" }, new String[] { commercant.getCurrencyName() });

			BigDecimal realAmount = payinAmount.add(commResp.getPayinCommission());
			if (!OnlinePaymentService.CUSTOMER_PAYED_CHANNELS.contains(channel)) {
				payoutAmount = payoutAmount.subtract(commResp.getPayoutCommission());
			}
			trx.setAccountNumber(commercant.getPrincipalAccountNumber());
			trx.setChannelType(request.getTransactionType());
			trx.setConsumerId(commercant.getConsumerId());
			trx.setCoursAPG(1f);
			trx.setSenderCurrency(senderCurrency);
			trx.setPayoutAmount(payoutAmount);
			trx.setPayinCommission(commResp.getPayinCommission());
			trx.setPayoutCommission(commResp.getPayoutCommission());
			trx.setCommissionSenderSA(commResp.getCommissionEmetteur());
			trx.setCommissionSender(commResp.getCommissionAccepteur());
			trx.setCommissionAPG(commResp.getCommissionSupportTechnique());
			trx.setCommissionSponsor(commResp.getCommissionDistributeur());
			trx.setCommissionPayerSA(commResp.getCommissionBanque());
			trx.setCommissionPayerP(commResp.getCommissionGim());
			trx.setTaxe(commResp.getTaxe());
			trx.setRealAmount(realAmount.toString());
			trx.setPayoutCurrency(commercant.getCurrencyName());
			trx.setPayoutCurrencyLabel(currencyIn.getCurrencyLabel());
			trx.setDate(new Date());
			trx.setToCountry(commercant.getCountry());
			trx.setFromCountry(commercant.getCountry());
			trx.setInTrRefNumber(request.getTerminalNumber() + "-" + acqui.getId());
			trx.setOperationId("acq-" + acqui.getField11() + "-" + acqui.getField12());
			trx.setDescription(channel.getMessage());
			if (senderCurrency.equals(commercant.getCurrencyName())) {
				trx.setPayinAmount(request.getAmount());
				trx.setPayinCurrency(commercant.getCurrencyName());
				trx.setPayinCurrencyLabel(currencyIn.getCurrencyLabel());

			} else {
				AdministrationSession adminSess = (AdministrationSession) BeanLocator
						.lookUp(AdministrationSessionBean.class.getSimpleName());
				CoursDevise coDevise = adminSess.findCourDevise(commercant.getCurrencyName(), senderCurrency, null);
				payinAmount = realAmount.multiply(BigDecimal.valueOf(coDevise.getValeurAPG())).setScale(2,
						RoundingMode.HALF_UP);
				BigDecimal payinCommission = commResp.getPayinCommission()
						.multiply(BigDecimal.valueOf(coDevise.getValeurAPG())).setScale(2, RoundingMode.HALF_UP);
				Currency currencySender = sess.executeNamedQuerySingle(Currency.class, "Currency.findByName",
						new String[] { "currencyName" }, new String[] { senderCurrency });

				trx.setPayinAmount(payinAmount);
				trx.setPayinCommission(payinCommission);
				trx.setPayinCurrency(senderCurrency);
				trx.setPayinCurrencyLabel(currencySender.getCurrencyLabel());
				trx.setCoursAPG(coDevise.getValeurAPG());
				trx.setSenderCurrency(commercant.getCurrencyName());
			}
			trx.setCoursPayerAPGLocal(trx.getCoursAPG());
			if ("FINAO".equals(network)) {
				trx.setInTrRefNumber(request.getReferencePayer());

			}
			trx.setMerchantName(acqui.getCaisse().getPointDeVente().getCommercant().getName());
			trx.setPartner(commercant);
			trx.setPartnerPayer(sender);
			trx.setOutTrRefNumber(request.getReferencePayer());
			trx.setSenderCardNumber(acqui.getCardId());
			trx.setIsActive(true);
			trx.setIdtWallet(acqui.getField44());
			ObjectNode otherDetails = new ObjectMapper().createObjectNode();
			if (acqui.getBeneficiaryData() != null) {
				ObjectMapper mapper = new ObjectMapper();

				Person beneficiary = mapper.readValue(acqui.getBeneficiaryData(), Person.class);
				trx.setBeneficiaryFirstName(beneficiary.getFirstName());
				trx.setBeneficiaryLastName(beneficiary.getLastName());
				trx.setBeneficiaryMobileNumber(beneficiary.getPhoneNumber());
				trx.setBeneficiaryNumeroPiece(beneficiary.getDocumentNumber());
				trx.setBeneficiaryAddress(beneficiary.getAddress());
				if (ChannelResponse.PAY_SCHOOL.getCode().equals(acqui.getChannelType())) {
					JsonNode details = mapper.readTree(acqui.getBeneficiaryData());
					otherDetails.set("level", details.get("level"));
					otherDetails.set("month", new TextNode("month"));
				}
			}

			if (ChannelResponse.W2W.getCode().equals(acqui.getChannelType())) {
				otherDetails.put("fromWalletId", acqui.getField2());
			}
			trx.setOtherDetails(otherDetails.toString());
			String cardId = acqui.getCardId() == null ? "card" : acqui.getCardId();
			if (cardId.startsWith("OPD")) {
				trx.setIdfWallet(acqui.getCardId());
				trx.setOtherDetails(null);
			}
			if (acqui.getField45() != null) {
				ObjectMapper mapper = new ObjectMapper();
				Person customer = mapper.readValue(acqui.getField45(), Person.class);
				trx.setSenderFirstName(customer.getFirstName());
				trx.setSenderLastName(customer.getLastName());
			}
			trx.setResponsePayeur(request.getResponsePayer());
			trx.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
		} catch (Exception e) {
			log.error("createTrx", e);
		}
		return trx;
	}

	public DispatchPayCommission getCommissionPayment(PaymentDto request, ChannelResponse channel,
			CommissionMonetique comm, IsoAcquisition acqui, Partner banque, boolean withBanque) {
		BigDecimal payinAmount = new BigDecimal(acqui.getField4());
		BigDecimal payinCommission = acqui.getFees();
		BigDecimal totalAmount = payinAmount.add(payinCommission);
		BigDecimal payoutCommission = commissionService.getCommissionMonetique(payinAmount, comm);
		DispatchPayCommission resp = new DispatchPayCommission();
		if (BigDecimal.ZERO.equals(payoutCommission)) {
			resp.setCommissionAccepteur(BigDecimal.ZERO);
			resp.setCommissionDistributeur(BigDecimal.ZERO);
			resp.setCommissionEmetteur(BigDecimal.ZERO);
			resp.setCommissionGim(BigDecimal.ZERO);
			resp.setCommissionSupportTechnique(BigDecimal.ZERO);
			resp.setPayinCommission(BigDecimal.ZERO);
			resp.setPayoutCommission(BigDecimal.ZERO);
			resp.setTaxe(BigDecimal.ZERO);
			resp.setCommissionBanque(BigDecimal.ZERO);
		} else {
			BigDecimal commissionBanque = withBanque ? getCommissionBanque(banque, totalAmount, acqui.getChannelType())
					: BigDecimal.ZERO;
			BigDecimal taxe;
			BigDecimal tva = new BigDecimal("1.17");
			if (OnlinePaymentService.CUSTOMER_PAYED_CHANNELS.contains(channel)) {
				log.info("payedChannel");
				payoutCommission = new BigDecimal(payinCommission.toString());
			} else if (BEConstantes.ORABANK_CODE.equalsIgnoreCase(request.getBank())) {
				payoutCommission = payoutCommission.multiply(tva);
			}
			if (BEConstantes.ORABANK_CODE.equalsIgnoreCase(request.getBank())) {
				commissionBanque = commissionBanque.multiply(tva);
			}
			BigDecimal commAccepteur = payoutCommission.multiply(comm.getCommissionAccepteur()).divide(HUNDRED);
			BigDecimal commDistributeur = payoutCommission.multiply(comm.getCommissionDistributeur()).divide(HUNDRED);
			BigDecimal commEmetteur = payoutCommission.multiply(comm.getCommissionEmetteur()).divide(HUNDRED);
			BigDecimal commGim = payoutCommission.multiply(comm.getCommissionGim()).divide(HUNDRED);
			BigDecimal commSupTechn = payoutCommission
					.subtract(commAccepteur.add(commDistributeur).add(commEmetteur).add(commissionBanque.add(commGim)));
			taxe = commSupTechn.multiply(new BigDecimal("17")).divide(HUNDRED).abs();

			resp.setCommissionAccepteur(commAccepteur);
			resp.setCommissionDistributeur(commDistributeur);
			resp.setCommissionEmetteur(commEmetteur);
			resp.setCommissionGim(commGim);
			resp.setCommissionSupportTechnique(commSupTechn);
			resp.setPayinCommission(payinCommission);
			resp.setPayoutCommission(payoutCommission);
			resp.setTaxe(taxe);
			resp.setCommissionBanque(commissionBanque);
		}
		return resp;
	}

	private BigDecimal getCommissionBanque(Partner sender, BigDecimal amount, String channelType) {

		BigDecimal commission = BigDecimal.ZERO;
		try {
			if(Constantes.CODE_RESTAU.equals(sender.getCode())) {
			 return BigDecimal.ZERO;	
			}
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			if (BEConstantes.PARTNER_PAYER.equals(sender.getPType())) {
				String[] prms = { ValidateData.CHANNEL_TYPE, ValidateData.ID_PARTNER };
				Object[] data = { channelType, sender.getIdPartner() };
				List<PayerCommission> payerComm = sess.executeNamedQueryList(PayerCommission.class,
						"findPayerCommissionByCp", prms, data);
				if (payerComm == null || payerComm.isEmpty()) {
					data = new Object[] { ChannelResponse.OLCRG01.getCode(), sender.getIdPartner() };
					payerComm = sess.executeNamedQueryList(PayerCommission.class, "findPayerCommissionByCp", prms,
							data);
				}
				commission = payHelper.getCommissionPartner(amount, payerComm.get(0));

			} else {
				String[] prms = { ValidateData.CHANNEL_TYPE, ValidateData.ID_PARTNER };
				List<SenderCommission> lSendComms = sess.executeNamedQueryList(SenderCommission.class,
						"senderCommission", prms, new Object[] { channelType, sender.getIdPartner() });
				if (lSendComms == null || lSendComms.isEmpty()) {
					lSendComms = sess.executeNamedQueryList(SenderCommission.class, "senderCommission", prms,
							new Object[] { ChannelResponse.OLCRG01.getCode(), sender.getIdPartner() });
				}
				SenderCommission comm = lSendComms.get(0);
				commission = payHelper.getCommissionPartner(amount, comm);
			}
		} catch (Exception e) {
		}
		return commission;
	}

	private Map<String, String> updateCardBalance(Card customerCard, boolean isCredit, BigDecimal totalAmount,
			Transaction trx, PaymentDto req) throws Exception {
		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		Map<String, String> dataCustomer = new ConcurrentHashMap<>();
		log.info("updateCardBalance");
		//String transactionId = req == null || req.getTransactionId() == null ? trx.getId().toString()
			//	: req.getTransactionId();

		trx.setFromCard(customerCard);
		/*if (genesys.isGenesys(customerCard.getPartner())) {
			dataCustomer.put(BEConstantes.PRM_HTML_AMOUNT, totalAmount.toString());
			dataCustomer.put(BEConstantes.PRM_HTML_ACCOUNT, customerCard.getCin());
			dataCustomer.put(BEConstantes.PRM_HTML_TRX_TYPE,
					isCredit ? BEConstantes.SENS_CREDIT : BEConstantes.SENS_DEBIT);
			dataCustomer.put(BEConstantes.PRM_HTML_TRX_ID, transactionId);
			dataCustomer.put(BEConstantes.PRM_HTML_BALANCE, BigDecimal.ZERO.toString());
			dataCustomer.put(BEConstantes.PRM_HTML_MERCH_NAME, trx.getPartner().getName());
			dataCustomer.put(BEConstantes.PRM_HTML_TRX_DESC, trx.getDescription());
			dataCustomer.put(BEConstantes.PRM_HTML_CURRENCY, customerCard.getCurrency().getCurrencyName());
			dataCustomer.put(BEConstantes.PRM_HTML_DATE,
					new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()).format(trx.getDate()));

		} else*/ if (customerCard.isLinked()) {
			LinkedCard lnkCad = sess.executeNamedQuerySingle(LinkedCard.class, "LC.findByCin", new String[] { "cin" },
					new String[] { customerCard.getCin() });
			if (Constantes.WALLET.equalsIgnoreCase(lnkCad.getLinkedType())) {
				Wallet wallet = sess.findObjectById(Wallet.class, Long.parseLong(lnkCad.getReference()), null);
				dataCustomer = updateWalletBalance(wallet, isCredit, totalAmount, trx, sess, req);
			} else {
				Partner partner = sess.findObjectById(Partner.class, Long.parseLong(lnkCad.getReference()), null);

				dataCustomer = updatePartnerBalance(partner, totalAmount, isCredit, trx);
			}
		}
		CardAcctTransaction cardAcct = new CardAcctTransaction();
		BigDecimal amount = isCredit ? totalAmount : totalAmount.negate();
		cardAcct.setAmount(amount);
		cardAcct.setCard(customerCard);
		cardAcct.setDate(new Date());
		cardAcct.setDescription(trx.getId() + "|" + trx.getDescription());
		cardAcct.setTransaction(trx);
		sess.saveObject(cardAcct);

		return dataCustomer;
	}

	private Map<String, String> updateWalletBalance(Wallet wallet, boolean isCredit, BigDecimal totalAmount,
			Transaction trx, Session sess, PaymentDto req) {
		log.info("updateWalletBalance");
		Map<String, String> dataCustomer = new ConcurrentHashMap<>();
		AccountWallet account = walletAcctService.findOrCreateAccount(wallet, BEConstantes.DEFAULT_COMPTE_PRINCIPAL);
		BigDecimal amount = isCredit?totalAmount:totalAmount.negate();
		
		BigDecimal newBalance = account.getBalance().add(amount);
		
		WalletAcctTransaction walletTrx = new WalletAcctTransaction();
		walletTrx.setAmount(amount);
		walletTrx.setAccount(account);
		walletTrx.setDate(new Date());
		walletTrx.setDescription(trx.getDescription() + "|" + trx.getId());
		walletTrx.setTransaction(trx);
		walletTrx = (WalletAcctTransaction) sess.saveObject(walletTrx);
		trx.setPrefixeWallet(wallet.getWallet().substring(0,3));
		log.info("walletTrx " + walletTrx.getId());
		if (isCredit) {
			trx.setIdtWallet(wallet.getWallet());
			AccountManager accountManager = (AccountManager) BeanLocator
					.lookUp(AccountManagerBean.class.getSimpleName());
			try {
				accountManager.regulCreanceTontine(wallet);
			} catch (InterruptedException e) {
				log.error("tontineError", e);

			}
		} else {
			trx.setIdfWallet(wallet.getWallet());
		}
		sess.updateObject(trx);
		String transactionId = req == null || req.getTransactionId() == null ? trx.getId().toString()
				: req.getTransactionId();
		dataCustomer.put(BEConstantes.PRM_HTML_AMOUNT, totalAmount.toString());
		dataCustomer.put(BEConstantes.PRM_HTML_ACCOUNT, wallet.getPhonenumber());
		dataCustomer.put(BEConstantes.PRM_HTML_TRX_TYPE, isCredit ? BEConstantes.SENS_CREDIT : BEConstantes.SENS_DEBIT);
		dataCustomer.put(BEConstantes.PRM_HTML_TRX_ID, transactionId);
		dataCustomer.put(BEConstantes.PRM_HTML_BALANCE,newBalance.toString());
		dataCustomer.put(BEConstantes.PRM_HTML_MERCH_NAME, trx.getPartner().getName());
		dataCustomer.put(BEConstantes.PRM_HTML_TRX_DESC, trx.getDescription());
		dataCustomer.put(BEConstantes.PRM_HTML_CURRENCY, account.getWallet().getPartner().getCurrencyName());
		dataCustomer.put(BEConstantes.PRM_HTML_DATE,
				new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()).format(trx.getDate()));

		return dataCustomer;
	}

	private Map<String, String> updatePartnerBalance(Partner partner, BigDecimal total, boolean isCredit,
			Transaction trx) throws Exception {
		
		BigDecimal amount=isCredit ? total:total.negate();
		BigDecimal newBalance =addAccountTransaction(partner, trx, amount, isCredit);
		
		trx.setNewBalance(newBalance);
		Map<String, String> dataPartner = new ConcurrentHashMap<>();
		dataPartner.put(BEConstantes.PRM_HTML_AMOUNT, amount.toString());
		dataPartner.put(BEConstantes.PRM_HTML_ACCOUNT, partner.getPrincipalAccountNumber());
		dataPartner.put(BEConstantes.PRM_HTML_TRX_TYPE, BEConstantes.SENS_CREDIT);
		dataPartner.put(BEConstantes.PRM_HTML_TRX_ID, trx.getId().toString());
		dataPartner.put(BEConstantes.PRM_HTML_BALANCE, newBalance.toString());
		dataPartner.put(BEConstantes.PRM_HTML_MERCH_NAME, partner.getName());
		dataPartner.put(BEConstantes.PRM_HTML_TRX_DESC, trx.getDescription());
		dataPartner.put(BEConstantes.PRM_HTML_CURRENCY, partner.getCurrencyName());
		dataPartner.put(BEConstantes.PRM_HTML_DATE,
				new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()).format(trx.getDate()));
		return dataPartner;
	}

	private BigDecimal addAccountTransaction(Partner partner, Transaction trx, BigDecimal principal, boolean isCredit) {

		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		String accountType= BEConstantes.DEFAULT_COMPTE_PRINCIPAL;
		if(Constantes.CODE_RESTAU.equals(partner.getCode())) {
			accountType = BEConstantes.DEFAULT_COMPTE_COMPENSE;
		}
		Account account = pAcctService.findOrCreate(partner,accountType ,
				partner.getCurrencyName());
		AccountTransaction acctTrx = new AccountTransaction();
		BigDecimal amount = isCredit ? principal : principal.negate();
		acctTrx.setAccount(account);
		acctTrx.setAmount(amount);
		acctTrx.setDate(new Date());
		acctTrx.setDescription(trx.getDescription() + "|" + trx.getId());
		acctTrx.setTransaction(trx);
		session.saveObject(acctTrx);
		return account.getBalance().add(amount);
	}

	private void updateCorrespondantOperation(Session sess, IsoAcquisition acqui, Transaction trx) {
		try {
			log.error("update Correspond");
			BulkPaymentFile bpf = sess.executeNamedQuerySingle(BulkPaymentFile.class, "findOperationByAcqui",
					new String[] { "acqui" }, new IsoAcquisition[] { acqui });

			if (bpf == null) {
				EventBookedTicket bet = sess.executeNamedQuerySingle(EventBookedTicket.class, "findBookedByAcquisition",
						new String[] { "acqui" }, new IsoAcquisition[] { acqui });
				if (bet == null) {
					log.info("not ticket");
				} else {
					updateTicketOperation(sess, acqui, trx, bet);
				}
			} else {
				updateBulkOperation(sess, acqui, trx, bpf);
			}

		} catch (Exception e) {
			log.error("update Correspond err");
		}
	}

	private void updateBulkOperation(Session sess, IsoAcquisition acqui, Transaction trx, BulkPaymentFile bpf) {
		try {
			log.info(acqui.getField39());
			if (Constantes.ISO_SUCCESS_STATUS.equals(acqui.getField39())) {
				bpf.setStatus(BEConstantes.STATUS_TRANSACTION_PAYED);
				
				Map<String, String> dataMail = new ConcurrentHashMap<>();

				dataMail.put(BEConstantes.PRM_HTML_TRX_ID, acqui.getId().toString());
				dataMail.put(BEConstantes.PRM_HTML_CUSTOMER_NAME, bpf.getCustomerFirstName()+" "+bpf.getCustomerLastName());
				dataMail.put(BEConstantes.PRM_HTML_BENEFICIARY_NAME, bpf.getBeneficiaryName());
				dataMail.put(BEConstantes.PRM_HTML_BENEFICIARY_ID, bpf.getBeneficiaryId());
				dataMail.put(BEConstantes.PRM_HTML_AMOUNT, trx.getPayinAmount().toString());
				dataMail.put(BEConstantes.PRM_HTML_CURRENCY, trx.getPayinCurrency());
				dataMail.put(BEConstantes.PRM_HTML_DETAILS, "");
				dataMail.put(BEConstantes.PRM_HTML_PARTNER_NAME, trx.getPartner().getName());
				dataMail.put(BEConstantes.PRM_HTML_DATE,
						new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()).format(trx.getDate()));
				String templatePath = BEConstantes.NOTIF_TEMPLATE_SCHOOL;
				MailUtils.sendMailHtml(trx.getPartner().getEmailContact(), "Reçu de Paiement : " +  bpf.getCustomerFirstName()+" "+bpf.getCustomerLastName(),
						templatePath, dataMail);
			} else {
				bpf.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
				bpf.setFailedReason(trx.getResponsePayeur());
			}
			sess.updateObject(bpf);
			if (trx.getId() != null) {
				trx.setSenderFirstName(bpf.getCustomerFirstName());
				trx.setSenderLastName(bpf.getCustomerLastName());
				trx.setSenderMobileNumber(bpf.getCustomerPhone());
				trx.setBeneficiaryFirstName(bpf.getBeneficiaryName());
				trx.setBeneficiaryNumeroPiece(bpf.getBeneficiaryId());
				trx.setBeneficiaryTypePiece(bpf.getCodeInstitution());
				sess.updateObject(trx);
			}
		} catch (Exception e) {
			log.error("update Bulk",e);
		}
	}

	private void updateTicketOperation(Session sess, IsoAcquisition acqui, Transaction trx, EventBookedTicket bet) {
		try {
			if (Constantes.ISO_SUCCESS_STATUS.equals(acqui.getField39())) {
				bet.setStatus(BEConstantes.STATUS_TRANSACTION_PAYED);
				EventTicket ticket = bet.getTicket();
				ticket.setNbSold(ticket.getNbSold() + 1);
				sess.updateObject(ticket);
			} else {
				bet.setStatus(BEConstantes.STATUS_TRANSACTION_FAILED);
			}
			sess.updateObject(bet);
			if (trx.getId() != null) {
				trx.setSenderFirstName(bet.getFirstName());
				trx.setSenderLastName(bet.getLastName());
				trx.setSenderMobileNumber(bet.getPhoneNumber());
				trx.setBeneficiaryFirstName(bet.getFirstName());
				trx.setBeneficiaryLastName(bet.getLastName());
				trx.setBeneficiaryMobileNumber(bet.getPhoneNumber());
				sess.updateObject(trx);
			}
		} catch (Exception e) {
			log.error("update ticket");
		}
	}
	private void finaliseOperationByChannel(Transaction trx, IsoAcquisition acqui, Session sess,PaymentDto req) {

		log.info("*****finaliseOperationByChannel**********");
		String []benData = acqui.getBeneficiaryData()==null?new String[]{"NO_ACTION"}:acqui.getBeneficiaryData().split("\\!");
		String alias=benData[benData.length-1];
		log.info(alias);
		if(Constantes.SEND_OPERATION.equals(alias)) {
			Wallet fromWallet = sess.executeNamedQuerySingle(Wallet.class, "findMyWallet", new String[] { "wallet" },
					new String[] { acqui.getCardId() });
			PaymentDto cashReq = new PaymentDto();
			trx.setIdfWallet(fromWallet.getWallet());
			String status = BEConstantes.STATUS_TRANSACTION_CANCELED;
			if(Constantes.ISO_SUCCESS_STATUS.equals(acqui.getField39())) {
				status = BEConstantes.STATUS_TRANSACTION_VALIDATED;
			}
			cashReq.setStatus(status);
			cashReq.setId(benData[9]);
			cashReq.setOperationId(benData[10]);
			
			ObjectNode otherDetails = new ObjectMapper().createObjectNode();
			otherDetails.put("fromWalletId" ,acqui.getField2());
			otherDetails.put("id" ,acqui.getId());
			otherDetails.put("amount" ,new BigDecimal(acqui.getField4()).toPlainString());
			otherDetails.put("fees" ,acqui.getFees().toString());
			otherDetails.put("status" ,trx.getStatus());
			otherDetails.put("trxRef" ,trx.getId());
			cashReq.setOtherDetails(otherDetails.toString());
			cashReq.setFromWalletId(acqui.getCardId());
			Partner partner = fromWallet.getPartner();
			PaymentDetails resp = gatwayApi.finalizeWithSendOperation(cashReq,partner);
			log.info(resp.getCode());
			trx.setIdfWallet(null);
			sess.updateObject(trx);
			if(ErrorResponse.REPONSE_SUCCESS.getCode().equals(resp.getCode())) {
				/*Executors.newSingleThreadExecutor().execute(()->{
				try {
					Thread.sleep(50_000);
				} catch (InterruptedException e) {
				  logger.error("ErrorInterr",e);
				}
				Transaction trx1 = sess.executeNamedQuerySingle(Transaction.class, "findTransactionById",
						new String[] {"id"}, new Long[] {Long.parseLong(benData[9])});
				trx.setOtherDetails(trx1.getOtherDetails());
				trx1.setIdfWallet(acqui.getCardId());
				if(ChannelResponse.W2W.getCode().equals(acqui.getChannelType())) {
					ObjectNode otherDetails = new ObjectMapper().createObjectNode();
					otherDetails.put("fromWalletId" ,acqui.getField2());
					logger.info(otherDetails.asText());
					logger.info(otherDetails.toString());
					trx1.setOtherDetails(otherDetails.toString());
					trx.setOtherDetails(otherDetails.toString());
				}
				trx.setIdfWallet(null);
				sess.updateObject(trx);
				sess.updateObject(trx1);
				
				});*/
				if(BEConstantes.STATUS_TRANSACTION_VALIDATED.equals(trx.getStatus())) {
				 changeFavoriteStatus(acqui,status);
				}
			}
		}else if (ChannelResponse.W2W.getCode().equals(acqui.getChannelType())
				|| ChannelResponse.CARD2W.getCode().equals(acqui.getChannelType())) {
			if (acqui.getCardId()!=null&&acqui.getCardId().startsWith(Constantes.OPTI_DIASPORA_PREFIX)) {
			
			} else {
				log.info("credit wallet");
				Wallet toWallet = sess.executeNamedQuerySingle(Wallet.class, "findMyWallet", new String[] { "wallet" },
						new String[] { acqui.getField44() });
				Map<String, String>data=updateWalletBalance(toWallet, true, trx.getPayinAmount(), trx,sess,req);
				SmsPayDto response = new SmsPayDto();
				response.setAccountNumber(toWallet.getPhonenumber());
				response.setAmount(new BigDecimal(data.get(BEConstantes.PRM_HTML_AMOUNT)));
				response.setBalance(new BigDecimal(data.get(BEConstantes.PRM_HTML_BALANCE)));
				response.setPhone(toWallet.getPhonenumber());
				response.setCurrencyName(toWallet.getCurrencyName());
				response.setIndicatif(toWallet.getCustomerIndicatif());
				response.setTransactionId(data.get(BEConstantes.PRM_HTML_TRX_ID));
				response.setRequestId(req.getRequestId());
				smsSender.notifyCustomer(response, trx);
				
				
			}
		} else {
			log.info("finalize payment operation");
		}
	}
	private void changeFavoriteStatus(IsoAcquisition acqui, String status) {
		if(BEConstantes.STATUS_TRANSACTION_VALIDATED.equals(status)) {
			Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
			if(acqui.getField2().contains("-")) {
			String payId[] =acqui.getField2().split("-");
			OptimaFavorites fav = sess.executeNamedQuerySingle(OptimaFavorites.class, "findFavByStatus",
					new String[] {"walletId","channelType","nom","fav","status"}, 
					new String[] {acqui.getCardId(),"accountWallet",payId[0],payId[1],"PENDING"});
			if(fav ==null) {
				log.info("no fav found");
			}else {
				fav.setStatus(BEConstantes.STATUS_TRANSACTION_VALIDATED);
				sess.updateObject(fav);
			}
			}
		}	
	}
	private void updateSenderBalance(Partner sender, String trxCurrency,BigDecimal amount, Transaction trx,boolean isCredit) {
		try {
			log.info("pType " + sender.getPType());
			
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			OperationSession opSession = (OperationSession) BeanLocator
					.lookUp(OperationSessionBean.class.getSimpleName());
			BigDecimal lastBalPartner = opSession.balancePartner(sender);
			BigDecimal principal = new BigDecimal(amount.toString());
			BigDecimal commission = trx.getCommissionPayerSA();
			ChannelResponse channel = ChannelResponse.getChannel(trx.getChannelType());
			if (OnlinePaymentService.CUSTOMER_PAYED_CHANNELS.contains(channel)) {
				log.info("addCommission " + trx.getPayinCommission());
				principal = principal.add(trx.getPayinCommission());
			}
			if (!trxCurrency.equals(sender.getCurrencyName())) {
				AdministrationSession adminSession = (AdministrationSession) BeanLocator
						.lookUp(AdministrationSessionBean.class.getSimpleName());
				CoursDevise cours = adminSession.findCourDevise(trxCurrency,
						sender.getCurrencyName(), null);
				principal = principal.multiply(BigDecimal.valueOf(cours.getValeurAPG())).setScale(4,
						RoundingMode.HALF_UP);
				commission = commission.multiply(BigDecimal.valueOf(cours.getValeurAPG())).setScale(4,
						RoundingMode.HALF_UP);
				trx.setCoursPayerAPGLocal(cours.getValeurAPG());

			}
			BigDecimal partnerAmount = principal.subtract(commission);
			BigDecimal newBalance = isCredit?lastBalPartner.add(partnerAmount):lastBalPartner.subtract(partnerAmount);
			log.info("lastBalanceSender " + lastBalPartner);
			PartnerAccount partnerAccCrP = new PartnerAccount();
			partnerAccCrP.setAccountNumber(sender.getPrincipalAccountNumber());
			partnerAccCrP.setCurrencyName(sender.getCurrencyName());
			partnerAccCrP.setAmount(newBalance);
			partnerAccCrP.setDate(new Date());
			partnerAccCrP.setDescription(trx.getDescription()+"|"+trx.getId()+"|"+trx.getStatus());
			partnerAccCrP.setIdTransaction(trx.getId().toString());
			partnerAccCrP.setPartnerCode(sender.getCode());
			partnerAccCrP.setPrincipal(partnerAmount);
			partnerAccCrP.setIsCredit(isCredit);
			partnerAccCrP.setCommissionSender(commission);
			partnerAccCrP.setOperationPartner(BEConstantes.OPERATION_PARTNER_IN);
			partnerAccCrP = (PartnerAccount) sess.saveObject(partnerAccCrP);
			log.info("partnerAccCrPId " + partnerAccCrP.getId());
			addAccountTransaction(sender, trx, partnerAmount, isCredit);
			updateCorrespondantAccount(sender,trx,partnerAmount,!isCredit);
		} catch (Exception e) {
			log.error("exceptionCreditSender", e);
		}
	}
	private void updateCorrespondantAccount(Partner merchant,Transaction trx, BigDecimal amount,boolean isCredit) {
		try {
			String payerAcctId="PAYER_ACCOUNT_ID_"+merchant.getIdPartner();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			ToolsPartner payeracctID = session.findObjectById(ToolsPartner.class, null,
					payerAcctId);
			log.info(payerAcctId);
			if(payeracctID ==null) {
				log.info("no payeur");
			}else {
				String payerID =APIUtilVue.getInstance().apgDeCrypt(payeracctID.getValue());
				Partner payer = session.findObjectById(Partner.class, Long.parseLong(payerID), null);
				
				addAccountTransaction(payer, trx, amount, isCredit);
			}
	} catch (Exception e) {
		log.error("exceptionCreditSender", e);
	}
	}
	private Map<String, String> updateMerchantBalance(Partner commercant, boolean isCredit, BigDecimal amount,
			Transaction trx, IsoAcquisition acqui) throws Exception {
		if(acqui.getCaisse().getCaisseSD() ==null) 
		{
		OperationSession opSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());

		return updateMerchantByPartnerAccount(commercant, acqui, opSession, amount, isCredit, trx);
		
		}else {
        	return sdManager.finalizeSDOperation(commercant, isCredit, amount, trx, acqui);
        }
	}
	private Map<String, String>updateMerchantByPartnerAccount(Partner commercant,IsoAcquisition acqui,OperationSession opSession,
			BigDecimal amount,boolean isCredit,Transaction trx) throws Exception  {
		BigDecimal lastBalPartner = opSession.balancePartner(commercant);
		BigDecimal newBalance = isCredit ? lastBalPartner.add(amount) : lastBalPartner.subtract(amount);
		log.info("lastBalance " + lastBalPartner);
		trx.setPreviousBalance(lastBalPartner);
		trx.setNewBalance(newBalance);
		PartnerAccount partnerAccCr = new PartnerAccount();
		partnerAccCr.setAccountNumber(commercant.getPrincipalAccountNumber());
		partnerAccCr.setCurrencyName(commercant.getCurrencyName());
		partnerAccCr.setAmount(newBalance);
		partnerAccCr.setDate(new Date());
		partnerAccCr.setDescription(trx.getDescription()+"|"+trx.getId());
		partnerAccCr.setIdTransaction(trx.getId().toString());
		partnerAccCr.setPartnerCode(commercant.getCode());
		partnerAccCr.setPrincipal(amount);
		partnerAccCr.setIsCredit(isCredit);
		partnerAccCr.setOperationPartner(BEConstantes.OPERATION_PARTNER_IN);
		partnerAccCr.setFees(BigDecimal.ZERO);
		partnerAccCr.setTotal(amount);
		Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		partnerAccCr = (PartnerAccount) sess.saveObject(partnerAccCr);
		log.info("partner update balance = " + partnerAccCr.getAmount());
		addAccountTransaction(commercant, trx, amount, isCredit);
		updateCorrespondantAccount(commercant, trx,amount, isCredit );
        Map<String, String>dataPartner = new ConcurrentHashMap<>();
		dataPartner.put(BEConstantes.PRM_HTML_AMOUNT, partnerAccCr.getPrincipal().toString());
		dataPartner.put(BEConstantes.PRM_HTML_ACCOUNT, commercant.getCode());
		dataPartner.put(BEConstantes.PRM_HTML_TRX_TYPE, BEConstantes.SENS_CREDIT);
		dataPartner.put(BEConstantes.PRM_HTML_TRX_ID, acqui.getId().toString());
		dataPartner.put(BEConstantes.PRM_HTML_BALANCE, partnerAccCr.getAmount().toString());
		dataPartner.put(BEConstantes.PRM_HTML_MERCH_NAME, commercant.getName());
		dataPartner.put(BEConstantes.PRM_HTML_TRX_DESC, trx.getDescription());
		dataPartner.put(BEConstantes.PRM_HTML_CURRENCY, commercant.getCurrencyName());
		dataPartner.put(BEConstantes.PRM_HTML_DATE,
				new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()).format(trx.getDate()));	
	return dataPartner;
	}
	public PaymentDetails saveCancellation(IsoAcquisition acqui, Transaction trx, Card merchantCard,
			Partner commercant, String network, BigDecimal customerBalance) {
		MessageSenderService sender = (MessageSenderService) BeanLocator
				.lookUp(MessageSenderServiceBean.class.getSimpleName());
		String path = BEConstantes.path + "/alldocs/MONETIQUE/SPECS/notify_operation.html";
		try {
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			Map<String, String> dataPartner = updateMerchantBalance(commercant, false, trx.getPayoutAmount(), trx,acqui);
			String indicatif = SMSSender.SN_INDICATIF;
			Card customerCard = trx.getFromCard();
			//Map<String, String> dataCustomer;
			String subject = "NOTIFICATION TRANSACTION";
			trx.setStatus(BEConstantes.STATUS_TRANSACTION_CANCELED);
			trx.setDateBatch(new Date());
			acqui.setRecon(1);
			sess.updateObject(trx);
			sess.updateObject(acqui);
			if (customerCard!=null&&customerCard.getRegister().getCountry() != null) {
				Country country = sess.executeNamedQuerySingle(Country.class, "findByCountryCode",
						new String[] { "countryCode" }, new String[] { customerCard.getRegister().getCountry() });
				if (country == null) {
					log.info("country customer is null " + customerCard.getRegister().getCountry());
				} else {
					indicatif = country.getCountryIndicatif();
				}
			}
			if ("APG".equals(network)) {
				//BigDecimal totalAmount = trx.getPayinAmount().add(acqui.getFees());
				//dataCustomer = updateCardBalance(customerCard, true, totalAmount, trx,null);
			    sender.notifyCancelPayment(customerCard, commercant, merchantCard, trx, indicatif);
				/*if (customerCard != null && customerCard.getRegister().getEmail() != null
						&& !genesys.isGenesys(customerCard.getPartner())) {
					log.info("mail");
					String toAddress = customerCard.getRegister().getEmail();
					MailUtils.sendMailHtml(toAddress, subject, path, dataCustomer);
				}*/
			}else if("FINAO".equals(network)) {
				SmsPayDto notiyResp= new SmsPayDto();
				notiyResp.setPhone(indicatif+customerCard.getRegister().getPhonenumber());
				notiyResp.setIndicatif(indicatif);
				notiyResp.setBalance(customerBalance);
				smsSender.notifyCustomer(notiyResp, trx);
			}
			if (commercant.getEmailContact() != null) {
				String toAddress = commercant.getEmailContact();
				MailUtils.sendMailHtml(toAddress, subject, path, dataPartner);

			}
			PaymentDetails resp = new PaymentDetails();
			resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			resp.setMessage("Annulation réussie");

			return resp;
		} catch (Exception e) {
			log.error("error", e);
			sender.notifysendSMS(BEConstantes.ENTETE_SMS_APG,SMSSender.SN_INDICATIF, "221773790423",
					"erreur save cancel trx idAcqui " + acqui.getId() + " " + e.getMessage());
			return new PaymentDetails(ErrorResponse.UNKNOWN_ERROR, "");
		}
	}
}
