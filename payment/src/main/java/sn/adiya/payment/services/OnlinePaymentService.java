package sn.adiya.payment.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import sn.fig.common.config.entities.CoursDevise;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.ChannelResponse;
import sn.fig.common.utils.MailUtils;
import sn.fig.entities.Account;
import sn.fig.entities.Country;
import sn.fig.entities.Partner;
import sn.fig.entities.Wallet;
import sn.fig.entities.aci.Caisse;
import sn.fig.entities.aci.CommissionMonetique;
import sn.fig.entities.aci.IsoAcquisition;
import sn.fig.session.AdministrationSession;
import sn.fig.session.AdministrationSessionBean;
import sn.fig.session.MessageSenderService;
import sn.fig.session.MessageSenderServiceBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.dto.ListResponse;
import sn.adiya.common.utils.Constantes;
import sn.adiya.common.utils.AdiyaCommonTools;
import sn.adiya.common.utils.AdiyaException;
import sn.adiya.common.utils.ValidateData;
import sn.adiya.payment.dto.CommissionDto;
import sn.adiya.payment.dto.PaymentDetails;
import sn.adiya.payment.dto.PaymentDto;
import sn.adiya.payment.dto.PaymentMeans;
import sn.adiya.payment.dto.Person;
import sn.adiya.payment.utils.PaymentHelper;
import sn.adiya.transaction.account.service.PartnerAccountService;
import sn.adiya.transaction.service.CommissionService;
import sn.adiya.user.services.UserManager;

@Stateless
public class OnlinePaymentService {

	private static final Logger LOG = Logger.getLogger(OnlinePaymentService.class);
	public static final List<ChannelResponse> CUSTOMER_PAYED_CHANNELS = List.of(ChannelResponse.CARD2W,
			ChannelResponse.CARD2CASHME, ChannelResponse.W2W, ChannelResponse.PAY_SCHOOL, ChannelResponse.AIR000,
			ChannelResponse.AIR001, ChannelResponse.AIR002, ChannelResponse.AIR003, ChannelResponse.AIR004,
			ChannelResponse.BP001, ChannelResponse.BP002, ChannelResponse.BP003, ChannelResponse.BP004,
			ChannelResponse.BP005, ChannelResponse.BP006, ChannelResponse.BP007, ChannelResponse.BP008,
			ChannelResponse.W2B, ChannelResponse.CARD2B, ChannelResponse.W2C,
			ChannelResponse.PAY_TRAVEL);

	@Inject
	private AdiyaCommonTools config;
	@Inject
	private CommissionService commissionService;
	@Inject
	private ValidateData validation;
	@Inject
	private UserManager userManager;
	@Inject
	private PartnerAccountService partnerAcctService;
	@Inject
	private PaymentHelper payHelper;

	public boolean autorizeServer(PaymentDto dto) {
		String calAuth = new StringBuilder().append(dto.getTimestamp()).append(dto.getTransactionId())
				.append(config.getProperty("adiya.key")).toString();
		String result = DigestUtils.md5Hex(calAuth.getBytes(StandardCharsets.UTF_8));
		LOG.info(result);
		LOG.info(dto);
		return result.equals(dto.getAuth());
	}

	public PaymentDetails details(PaymentDto dto) {
		Long transactionId = Long.parseLong(dto.getTransactionId());
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		IsoAcquisition acqui = session.findObjectById(IsoAcquisition.class, transactionId, null);
		PaymentDetails response = new PaymentDetails();
		response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		if (Constantes.ISO_PENDING_STATUS.equals(acqui.getField39())) {
			BigDecimal merchantAmount = new BigDecimal(acqui.getField4());
			response.setRequestId(acqui.getField63());
			response.setStatus(acqui.getField39());
			response.setTransactionId(transactionId.toString());
			response.setMerchantName(acqui.getField43());
			response.setPaymentCountry(acqui.getCaisse().getPointDeVente().getCommercant().getCountryIsoCode());
			response.setFromCountryIndicatif(acqui.getCaisse().getPointDeVente().getCommercant().getCountryIndicatif());
			response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			response.setBank(dto.getBank());
			LOG.info(dto.getBank());
			if (dto.getBank() == null) {
				response.setAmount(merchantAmount);
				response.setCurrencyName(acqui.getField49());
				response.setFees(acqui.getFees());
			} else {
				String subMeansType = payHelper.getSubMeansTypeFromBank(dto.getBank());
				CommissionDto commDto = new CommissionDto();
				commDto.setAmount(merchantAmount);
				commDto.setChannelType(acqui.getChannelType());
				commDto.setIdPartner(acqui.getCaisse().getPointDeVente().getCommercant().getIdPartner());
				commDto.setSubMeansType(subMeansType);
				commDto.setMeansType(dto.getMeansType());
				BigDecimal fees = getTransactionFees(commDto);
				PaymentDetails senderAmnt = senderAmount(merchantAmount, dto.getBank(), fees, acqui.getField49());
				response.setAmount(senderAmnt.getAmount());
				response.setFees(senderAmnt.getFees());
				response.setCurrencyName(senderAmnt.getCurrencyName());
				acqui.setField33(dto.getBank());
				acqui.setFees(senderAmnt.getFees());
				acqui.setPaymentMeansType(dto.getMeansType());
				acqui.setField56(UUID.randomUUID().toString());
				session.updateObject(acqui);
			}
		}
		return response;
	}

	public PaymentDetails senderAmount(BigDecimal merchantAmount, String bank, BigDecimal fees, String currency) {
		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		String[] params = new String[] { "code" };
		String[] data = new String[] { bank };
		Partner partner = sess.executeNamedQuerySingle(Partner.class, "findPartnerByCode", params, data);
		BigDecimal bankAmount;
		BigDecimal bankFees;
		if (currency.equals(partner.getCurrencyName())) {
			bankAmount = merchantAmount;
			bankFees = fees;
		} else {
			AdministrationSession adminSess = (AdministrationSession) BeanLocator
					.lookUp(AdministrationSessionBean.class.getSimpleName());
			CoursDevise coDevise = adminSess.findCourDevise(currency, partner.getCurrencyName(), null);
			bankAmount = merchantAmount.multiply(BigDecimal.valueOf(coDevise.getValeurAPG()));
			bankFees = fees.multiply(BigDecimal.valueOf(coDevise.getValeurAPG()));
			bankAmount = bankAmount.setScale(2, RoundingMode.HALF_UP);
			bankFees = bankFees.setScale(2, RoundingMode.HALF_UP);
		}
		if (Constantes.CURRENCY_XOF.equals(partner.getCurrencyName())
				|| Constantes.CURRENCY_XAF.equals(partner.getCurrencyName())) {
			bankAmount = bankAmount.setScale(0, RoundingMode.HALF_UP);
			bankFees = bankFees.setScale(0, RoundingMode.HALF_UP);
		}
		PaymentDetails details = new PaymentDetails();
		details.setAmount(bankAmount);
		details.setFees(bankFees);
		details.setCurrencyName(partner.getCurrencyName());
		return details;
	}

	public BigDecimal getTransactionFees(CommissionDto commDto) {
		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		LOG.info("getTransactionFees");
		String[] params = new String[] { Constantes.ID_PARTNER, Constantes.CHANNEL_TYPE, Constantes.MEANS_TYPE,
				"subMeansType" };
		Object[] data = new Object[] { commDto.getIdPartner(), commDto.getChannelType(), commDto.getMeansType(),
				commDto.getSubMeansType() };
		Object[] defaultData = new Object[] { commDto.getIdPartner(), commDto.getChannelType(), commDto.getMeansType(),
				"ALL" };
		String query = "Comm.FindByPartenaireChannelAndSubMeans";
		CommissionMonetique comm = sess.executeNamedQuerySingle(CommissionMonetique.class, query, params, data);
		CommissionMonetique commission = comm == null
				? sess.executeNamedQuerySingle(CommissionMonetique.class, query, params, defaultData)
				: comm;
		BigDecimal fees;
		if (commission == null) {
			LOG.info("commission not found");
			LOG.info(Arrays.toString(data));

			fees = null;
		} else {
			ChannelResponse channel = ChannelResponse.getChannel(commDto.getChannelType());
			fees = CUSTOMER_PAYED_CHANNELS.contains(channel)
					? commissionService.getCommissionMonetique(commDto.getAmount(), commission)
					: BigDecimal.ZERO;
		}
		return fees;
	}

	

	public PaymentDetails getBank(PaymentDto dto) {
		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		IsoAcquisition acqui = sess.findObjectById(IsoAcquisition.class, Long.parseLong(dto.getTransactionId()), null);
		PaymentDetails details = new PaymentDetails();
		Partner commercant = acqui.getCaisse().getPointDeVente().getCommercant();
		if (Constantes.ISO_PENDING_STATUS.equals(acqui.getField39())) {
			String bank;
			String code = ErrorResponse.REPONSE_SUCCESS.getCode();
			switch (dto.getMeansType()) {
			case Constantes.CARTE:
				bank = commercant.getCodeBanque() == null ? Constantes.CODE_ORABANK : commercant.getCodeBanque();
				break;
			case Constantes.CRYPTO:
				bank = Constantes.CODE_TRIPLEA;
				break;
			default:
				bank = null;
				code = ErrorResponse.UNKNOWN_ERROR.getCode();
				break;
			}
			details.setBank(bank);
			details.setCode(code);
		} else {
			details.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		}
		return details;
	}

	public PaymentDetails initiateOnlinePayment(PaymentDto request) throws AdiyaException{
		PaymentDetails response = new PaymentDetails(ErrorResponse.UNKNOWN_ERROR.getCode(), ErrorResponse.UNKNOWN_ERROR.getMessage(""));;
			LOG.info("initiateOnlinePayment");
			AbstractResponse check = validation.validataData(request, ValidateData.INITIATE_ON_REQUEST);
			if (check.getCode().equals(ErrorResponse.REPONSE_SUCCESS.getCode())) {

				if (request.getTransactionType() == null) {
					request.setTransactionType(ChannelResponse.OLCRG01.getCode());
				}
				Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
				String internWallet = " " + request.getFromWalletId();
				if ((ChannelResponse.CARD2W.getCode().equals(request.getTransactionType())
						|| ChannelResponse.W2W.getCode().equals(request.getTransactionType()))
						&& internWallet.contains("OPT")) {

					response = new PaymentDetails(ErrorResponse.WALLET_INTROUVABLE_OPTIMA,
							ErrorResponse.WALLET_INTROUVABLE_OPTIMA.getMessage(""));
				} else {
					Caisse caisse = session.executeNamedQuerySingle(Caisse.class, ValidateData.FIND_CAISSE_BY_NUMCAISSE,
							new String[] { ValidateData.NUM_CAISSE }, new String[] { request.getTerminalNumber() });

					if(!verifyInitiatePay(request, null, caisse.getTerminalKey())) {
						throw new AdiyaException(ErrorResponse.PARTNER_NOT_ALLOWED.getCode(),ErrorResponse.PARTNER_NOT_ALLOWED.getMessage(""));
					}
					request.setPan("0000000000000000");

					Partner commercant = caisse.getPointDeVente().getCommercant();
					if (commercant.getCurrencyName().equals(request.getCurrencyName())) {
						Account account = partnerAcctService.findOrCreate(commercant,
								BEConstantes.DEFAULT_COMPTE_PRINCIPAL, commercant.getCurrencyName());
						String params[] = { "status", "commercant" };
						Object data[] = { Constantes.ISO_PENDING_STATUS, commercant };
						List<String> pendingAmount = session.executeNamedQueryList(String.class,
								"sumTrxByStatusAndCommercant", params, data);
						BigDecimal sumPending = pendingAmount.stream().map(p -> new BigDecimal(p))
								.reduce(BigDecimal.ZERO, BigDecimal::add);
						BigDecimal nextBalance = account.getBalance().add(request.getAmount()).add(sumPending);
						LOG.info(nextBalance);
						LOG.info(sumPending);
						if (nextBalance.compareTo(account.getMaxBalance()) > 0) {
							throw new AdiyaException(ErrorResponse.TRANSACTION_NOT_ALLOW_OPTIMA.getCode(),
									ErrorResponse.TRANSACTION_NOT_ALLOW_OPTIMA.getMessage(""));
						}
						List<CommissionMonetique> commMonetique = session.executeNamedQueryList(
								CommissionMonetique.class, "CommissionMonetique.FindByPartenaireChannelOnly",
								new String[] { ValidateData.ID_PARTNER, "channelType" },
								new Object[] { commercant.getIdPartner(), request.getTransactionType() });
						if (commMonetique == null || commMonetique.isEmpty()) {
							LOG.info("commission not configured for channel " + request.getTransactionType());
							throw new AdiyaException(ErrorResponse.TRANSACTION_CHANNEL_ERROR.getCode(),
									ErrorResponse.TRANSACTION_CHANNEL_ERROR.getMessage(""));
						}
						String requestId = request.getTerminalNumber() + "-" + request.getRequestId();
						IsoAcquisition findAcqui = session.executeNamedQuerySingle(IsoAcquisition.class,
								ValidateData.ISO_ACQ_FIND_BY_REQ_ID, new String[] { ValidateData.REQUEST_ID },
								new String[] { requestId });
						if (findAcqui != null) {
							throw new AdiyaException(ErrorResponse.TRANSACTION_DOUBLOON_ERROR.getCode(),
									ErrorResponse.TRANSACTION_DOUBLOON_ERROR.getMessage(""));
						}
						request.setMerchantAddress(commercant.getName());
						request.setMerchantNumber(commercant.getIdPartner().toString());
						request.setPosNumber(caisse.getPointDeVente().getNumeroPointDeVente());
						Wallet fromWallet = null;
						String acquerreur = "000003";
						String transmetteur = acquerreur;
						if (Constantes.WALLET.equalsIgnoreCase(request.getMeansType())) {
							if (request.getFromWalletId() != null) {
								fromWallet = session.executeNamedQuerySingle(Wallet.class, ValidateData.FIND_MY_WALLET,
										new String[] { ValidateData.PRM_WALLET },
										new String[] { request.getFromWalletId() });
								if (fromWallet != null) {
									request.setCustomer(new Person(fromWallet));
								}
							}
						}

						if (!Constantes.SEND_OPERATION.equals(request.getAlias())
								&& !Constantes.WALLET.equalsIgnoreCase(request.getMeansType())) {
							request.setFromWalletId(null);
						}
						IsoAcquisition acquisition =payHelper.createIsoAcquisition(request, acquerreur, transmetteur);
						acquisition.setField39(Constantes.ISO_PENDING_STATUS);
						acquisition.setCaisse(caisse);
						acquisition.setField63(request.getRequestId());
						acquisition.setField64(request.getTerminalNumber());
						acquisition.setReturnUrl(request.getReturnUrl());
						acquisition.setRequestId(requestId);
						acquisition.setField128(null);
						acquisition.setField44(request.getToWalletId());
						acquisition.setField48(request.getReferenceNumber());
						acquisition.setCardId(request.getWalletId());
						if (ChannelResponse.CARD2B.getCode().equals(request.getTransactionType())) {
							acquisition.setField44(request.getToBankAccountNumber());
						}

						acquisition = (IsoAcquisition) session.saveObject(acquisition);

						 response = new PaymentDetails();
						 response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
						 response.setMessage(ErrorResponse.REPONSE_SUCCESS.getMessage(""));
						 response.setTransactionId(Long.toString(acquisition.getId()));
						StringBuilder payUrl = new StringBuilder(100);
						payUrl.append(config.getProperty("adiya.baseUrl")).append("?transactionId=")
								.append(acquisition.getId()).append("&merchantNumber=")
								.append(commercant.getIdPartner()).append("&requestId=").append(request.getRequestId());
						if (request.getMeansType() != null && !request.getMeansType().isBlank()
								&& !request.getMeansType().equalsIgnoreCase("null")) {
							String meansType = request.getMeansType().replace(" ", "").replace("'", "").replace("\"",
									"");
							payUrl.append("&meansType=").append(meansType);
						}

						if (Constantes.WALLET.equals(request.getMeansType()) && request.getFromCountry() != null
								&& request.getFromWalletId() != null) {
							LOG.info("complete url");
							if (request.getFromWalletId().startsWith("INT")
									|| request.getFromWalletId().startsWith("OPD")) {
								
							} else {
								Country country = session.executeNamedQuerySingle(Country.class, "findByCountryCode",
										new String[] { "countryCode" },
										new String[] { request.getFromCountry().toUpperCase(Locale.getDefault()) });
								String walletContent[] = request.getFromWalletId().split("-");
								if (country != null) {
									String sender = walletContent[0] + country.getCountryIsoCode();
									if (!"optsn".equalsIgnoreCase(sender)) {
										payUrl.append("&countryIndicatif=").append(country.getCountryIndicatif())
												.append("&phone=").append(walletContent[1]).append("&sender=")
												.append(sender.toLowerCase(Locale.getDefault()));
									}
								}
							}
						}
						String paymentUrl = payUrl.toString();
						response.setPaymentUrl(paymentUrl);
						LOG.info(paymentUrl);
						request.setTransactionId(acquisition.getId().toString());
						sendUrl(request, paymentUrl);
					} else {
						LOG.info("currency is not the currency of the merchant");
						response = new PaymentDetails(ErrorResponse.CURRENCY_ERROR, "");
					}
				}
			} else {
				response = new PaymentDetails(check.getCode(), check.getMessage());
			}
		return response;

	}

	@Asynchronous
	public void sendPendingDebitNotify(Wallet wallet, IsoAcquisition acqui) {
		/*
		 * try { if(wallet == null) { return; }else
		 * if(wallet.getWallet().startsWith("OPT")){ LOG.info("sendPendingDebitNotify");
		 * StringBuilder builder =new
		 * StringBuilder(100).append("Vous avez recu une demande de paiement de ").
		 * append(acqui.getField5()).append(' ').append(acqui.getField49()).
		 * append(" venant de ").append(acqui.getCaisse().getPointDeVente().
		 * getCommercant().getName()).
		 * append(". Veuillez composer #2828# pour valider."); String message =
		 * builder.toString(); String
		 * sender=wallet.getPartner().getSenderSms()==null?"OPTIMA":wallet.getPartner().
		 * getSenderSms(); String phone
		 * =wallet.getCustomerIndicatif()+wallet.getPhonenumber();
		 * APIUtilVue.getInstance().sendSMS(sender, wallet.getCustomerIndicatif(),phone
		 * , message);
		 * 
		 * } } catch (Exception e) { LOG.info("sendPending "+e.getMessage()); }
		 */
	}

	private boolean verifyInitiatePay(PaymentDto request, String flashcode, String terminalKey) throws AdiyaException {
		boolean response = true;
		if (flashcode == null || flashcode.isEmpty()) {
			String calAuth = request.getTimestamp() + request.getAmount() + request.getRequestId()
					+ request.getCurrencyName();
			String calAuth1 = request.getTimestamp() + request.getAmount() + request.getRequestId()
					+ request.getCurrencyName() + request.getSubMeansType();

			calAuth = calAuth + terminalKey;
			String result = DigestUtils.md5Hex(calAuth.getBytes(StandardCharsets.UTF_8));
			String result1 = DigestUtils.md5Hex(calAuth1.getBytes(StandardCharsets.UTF_8));

			LOG.info(calAuth);
			LOG.info(request.getAuth());
			LOG.info("has result " + result);
			LOG.info("has result1 " + result1);
			if (!(request.getAuth().equalsIgnoreCase(result) || request.getAuth().equalsIgnoreCase(result1))) {
				response = false;
			}
		} else {
			Utilisateur user = userManager.verifyFlashcode(flashcode);
			LOG.info("globalResponse................." + user.getNom());

		}
		return response;
	}

	private void sendUrl(PaymentDto request, String payUrl) {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

		StringBuilder message = new StringBuilder(100);
		String commingMessage = request.getMessage() == null || request.getMessage().isBlank() ? ""
				: request.getMessage();
		message.append(commingMessage);
		if (ChannelResponse.PAY_SCHOOL.getCode().equals(request.getTransactionType())) {
			message.append("Cher Parent, veuillez cliquer sur ce lien pour payer la scolarite de votre")
					.append(" enfant  Nom : ").append(request.getBeneficiary().getLastName()).append(" Prenom : ")
					.append(request.getBeneficiary().getFirstName()).append(" ref : ")
					.append(request.getTransactionId()).append(" Montant : ").append(request.getAmount()).append(" ")
					.append(payUrl).append(". Merci de votre confiance");

		} else if (ChannelResponse.PAY_LOCATION.getCode().equals(request.getTransactionType())) {
			message.append("Cher locataire, ").append("veuillez cliquer sur ce lien pour payer")
					.append("votre loyer  Nom : ").append(request.getBeneficiary().getLastName()).append(".Prenom : ")
					.append(request.getBeneficiary().getFirstName()).append(" ref :").append(request.getTransactionId())
					.append(". Montant :").append(request.getAmount()).append(" ").append(payUrl);
		} else {
			message.append("Veuillez cliquer sur ce lien ").append(payUrl).append(" pour confirmer votre operation.");
		}

		if ("MAIL".equalsIgnoreCase(request.getCanal())) {
			LOG.info("PUSH PAYMENT : MAIL");
			MailUtils.sendEmails(request.getMoyenReception(), "PUSH PAYMENT", message.toString(), Boolean.TRUE, null,
					null);
		}
		if ("SMS".equalsIgnoreCase(request.getCanal())) {
			LOG.info("PUSH PAYMENT : sms");
			MessageSenderService senderM = (MessageSenderService) BeanLocator
					.lookUp(MessageSenderServiceBean.class.getSimpleName());

			String sender = "APGSA";
			if (request.getToWalletId() != null) {
				Wallet wallet = session.executeNamedQuerySingle(Wallet.class, ValidateData.FIND_MY_WALLET,
						new String[] { ValidateData.PRM_WALLET }, new String[] { request.getToWalletId() });
				if (wallet != null) {
					if (wallet.getEmail() != null) {
						MailUtils.sendEmails(wallet.getEmail(), "PUSH PAYMENT", message.toString(), Boolean.TRUE, null,
								null);
					}
					if (wallet.getPartner().getSenderSms() != null) {
						sender = wallet.getPartner().getSenderSms();
					}
				}
			}
			String indicatif = request.getMoyenReception().substring(0, 3);
			senderM.notifysendSMS(sender, indicatif, request.getMoyenReception(), message.toString());
		} else if ("WHATSAPP".equalsIgnoreCase(request.getCanal())) {
			LOG.info("PUSH PAYMENT : whatsapp");
		}
	}



	public String initatePaymentAuth(String terminalKey,PaymentDto request) {
		String calAuth = request.getTimestamp() + request.getAmount() + request.getRequestId()
		+ request.getCurrencyName();
        calAuth = calAuth + terminalKey;
   return DigestUtils.md5Hex(calAuth.getBytes(StandardCharsets.UTF_8));

	}

	public ListResponse<PaymentMeans> paymentMeans(PaymentDto dto) {
		
		  ListResponse<PaymentMeans> response = new ListResponse<>();
		response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		response.setData(List.of(PaymentMeans.values()).stream().filter(m->m.getType().equals(dto.getMeansType())).collect(Collectors.toList()));
		return response;
	}
	public PaymentDetails statusPayment(Long transactionId) {
		Session session = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		IsoAcquisition isoAcqu = session.findObjectById(IsoAcquisition.class, transactionId, null);
		PaymentDetails resp = new PaymentDetails();
		resp.setAmount(new BigDecimal(isoAcqu.getField4()));
		resp.setTransactionType(isoAcqu.getChannelType());
		resp.setFees(isoAcqu.getFees());
		resp.setRequestId(isoAcqu.getField63());
		resp.setBank(isoAcqu.getField33());
		resp.setCurrencyName(isoAcqu.getCaisse().getPointDeVente().getCommercant().getCurrencyName());
		resp.setTransactionId(StringUtils.leftPad(isoAcqu.getId().toString(), 8, '0'));
		resp.setMeansType(isoAcqu.getPaymentMeansType());
		resp.setTransactionDate(
				new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()).format(isoAcqu.getDateCreation()));
		resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		resp.setTransactionId(isoAcqu.getId().toString());
		resp.setReturnUrl(isoAcqu.getReturnUrl());
		resp.setPhone(isoAcqu.getField2());
		resp.setMerchantNumber(isoAcqu.getCaisse().getPointDeVente().getCommercant().getIdPartner().toString());
		resp.setMerchantName(isoAcqu.getCaisse().getPointDeVente().getCommercant().getName());
		String code = ErrorResponse.UNKNOWN_ERROR.getCode();
		if(Constantes.ISO_SUCCESS_STATUS.equals(isoAcqu.getField39())) {
		  code = ErrorResponse.REPONSE_SUCCESS.getCode();	
		}else if(Constantes.ISO_PENDING_STATUS.equals(isoAcqu.getField39())) {
			code = Constantes.ISO_PENDING_STATUS;
		}
		resp.setCode(code);
		return resp;
	}

	
}
