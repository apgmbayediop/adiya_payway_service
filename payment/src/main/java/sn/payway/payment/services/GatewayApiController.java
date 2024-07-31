package sn.payway.payment.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sn.apiapg.common.config.entities.CoursDevise;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APGCommonRequest;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.ChannelResponse;
import sn.apiapg.common.utils.MailUtils;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.Wallet;
import sn.apiapg.session.AdministrationSession;
import sn.apiapg.session.AdministrationSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.PaywayCommonTools;
import sn.payway.payment.dto.PaymentDetails;
import sn.payway.payment.dto.PaymentDto;

@Stateless
public class GatewayApiController {

	private static final Logger LOG = Logger.getLogger(GatewayApiController.class);
	
	@Inject
	private PaywayCommonTools tools;
	private static final String MNC="mnc";
	private boolean isProd;
	public void init() {
		
	 isProd=Constantes.PREPROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))||Constantes.PROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY));
	}
	public PaymentDetails finalizeWithSendOperation(PaymentDto cashReq, Partner partner) {
		PaymentDetails res ;
		try {
			LOG.info("***************finalizeWithSendOperation**************");
				 res = updateOperation(cashReq, partner);
				LOG.info(res); 
				if (res != null && !ErrorResponse.REPONSE_SUCCESS.getCode().equals(res.getCode())) {
					Character newline = '\n';
					StringBuilder builder = new StringBuilder(100);
					builder.append("Details transaction\n id :").append(cashReq.getId())
							.append(newline).append("OperationId :").append(cashReq.getOperationId())
							.append(newline).append("status :").append(cashReq.getStatus()).append(newline)
							.append(newline).append("Erreur :").append(res.getMessage())
							.append(newline);
					String subject = partner.getName() + ":ERREUR FINALISATION OPERATION";
					String body = builder.toString();
					String toAdress = isProd?"operations@afripayway.com,team.it@afripayway.com":"team.it@afripayway.com";
					MailUtils.sendEmails(toAdress, subject, body, true, null, null);
				}
			
		} catch (Exception e) {
			LOG.error("finalizeWithSendOperation", e);
			res = new PaymentDetails(ErrorResponse.UNKNOWN_ERROR, "");
		}
		return res;
	}

	private String getChannelSendOperation(String channel) {
		ChannelResponse channelType = ChannelResponse.getChannel(channel);
		ChannelResponse selected;
		switch (channelType) {
		case CARD2W:
			selected = ChannelResponse.C2W;
			break;
		case CARD2C:
		case W2C:
			selected = ChannelResponse.C2C;
			break;
		case W2W:
			selected = ChannelResponse.C2W;
			break;
		case CARD2B:
		case W2B:
			selected = ChannelResponse.C2B;
			break;
		default:
			selected = channelType;
			break;
		}
		return selected.getCode();
	}

	private void completeRequest(APGCommonRequest cashReq, PaymentDto req) {
		ChannelResponse channelType = ChannelResponse.getChannel(cashReq.getTrChannel());
		String refs[]=(req.getReferenceNumber()==null)?new String[6]:req.getReferenceNumber().split("\\!");
		switch (channelType) {
		case C2W:
			cashReq.setToWalletId(req.getToWalletId());
			break;
		case C2B:
			cashReq.setToBankAccountNumber(req.getToBankAccountNumber());
			cashReq.setAchCode(req.getAchCode());
			cashReq.setToBankCode(req.getToBankCode());
			cashReq.setToBankSwift(req.getToBankSwift());
			cashReq.setToBankAccountType(req.getToBankAccountType());
			cashReq.setFromBankAccountType(req.getFromBankAccountType());
			cashReq.setTransitNumber(req.getTransitNumber());
			
			break;
		case BP006:{
			cashReq.setCarte(req.getCarte());
			cashReq.setChip(req.getChip());
			cashReq.setNumAbo(req.getNumAbo());
			cashReq.setDuree(req.getDuree());
			cashReq.setAmount(new BigDecimal(refs[0]));
			cashReq.setLocalCurrencyAmount(new BigDecimal(refs[5]));
			cashReq.setSessionId(req.getSessionId());
			break;
		}
		case BP001: 
			cashReq.setLocalCurrencyAmount(req.getLocalCurrencyAmount());
			cashReq.setLocalCurrencyCode(req.getLocalCurrencyCode());
			cashReq.setReferenceNumber(req.getReferenceNumber());
			cashReq.setSessionId(req.getSessionId());
			break;
		case BP002: 
			cashReq.setLocalCurrencyAmount(req.getLocalCurrencyAmount());
			cashReq.setLocalCurrencyCode(req.getLocalCurrencyCode());
			cashReq.setReferenceNumber(req.getReferenceNumber());
			cashReq.setReference(req.getReference());
			cashReq.setSessionId(req.getSessionId());
			break;
		case BP003: 
			cashReq.setLocalCurrencyAmount(req.getLocalCurrencyAmount());
			cashReq.setLocalCurrencyCode(req.getLocalCurrencyCode());
			cashReq.setReferenceNumber(refs[2]);
			cashReq.setSessionId(refs[5]);
			break;
		case BP004: 
			
			cashReq.setLocalCurrencyAmount(req.getLocalCurrencyAmount());
			cashReq.setLocalCurrencyCode(req.getLocalCurrencyCode());
			cashReq.setReferenceNumber(refs[2]);
			cashReq.setSessionId(refs[5]);
			break;
		case BP005: 
			cashReq.setCarte(refs[1]);
			cashReq.setNumAbo(refs[2]);
			cashReq.setDuree(Integer.parseInt(refs[3]));
			cashReq.setAmount(new BigDecimal(refs[0]));
			cashReq.setLocalCurrencyAmount(new BigDecimal(refs[4]));
			cashReq.setLocalCurrencyCode(refs[5]);
			break;
		case AIR001:{
			String mnc = "428";
			cashReq.setLocalCurrencyAmount(req.getLocalCurrencyAmount());
			cashReq.setLocalCurrencyCode(req.getLocalCurrencyCode());
			cashReq.setBeneficiaryPhone(req.getBeneficiary().getPhoneNumber());
			ObjectNode objet=new ObjectMapper().createObjectNode();
			objet.put(MNC, mnc);
			cashReq.setMnc(mnc);
			cashReq.setPaymentDetails(objet.toString());
			break;
		}
		case AIR002:{
			String mnc = "429";
			cashReq.setLocalCurrencyAmount(req.getLocalCurrencyAmount());
			cashReq.setLocalCurrencyCode(req.getLocalCurrencyCode());
			cashReq.setBeneficiaryPhone(req.getBeneficiary().getPhoneNumber());
			ObjectNode objet=new ObjectMapper().createObjectNode();
			objet.put(MNC, mnc);
			cashReq.setMnc(mnc);
			cashReq.setPaymentDetails(objet.toString());
			break;
		}
		case AIR003:{
			String mnc = "430";
			cashReq.setLocalCurrencyAmount(req.getLocalCurrencyAmount());
			cashReq.setLocalCurrencyCode(req.getLocalCurrencyCode());
			cashReq.setBeneficiaryPhone(req.getBeneficiary().getPhoneNumber());
			ObjectNode objet=new ObjectMapper().createObjectNode();
			objet.put(MNC, mnc);
			cashReq.setMnc(mnc);
			cashReq.setPaymentDetails(objet.toString());
			break;
		}
		case AIR004:{
			String mnc = "60804";
			cashReq.setLocalCurrencyAmount(req.getLocalCurrencyAmount());
			cashReq.setLocalCurrencyCode(req.getLocalCurrencyCode());
			cashReq.setBeneficiaryPhone(req.getBeneficiary().getPhoneNumber());
			ObjectNode objet=new ObjectMapper().createObjectNode();
			objet.put(MNC, mnc);
			cashReq.setMnc(mnc);
			cashReq.setPaymentDetails(objet.toString());
			break;
		}
		default:
			break;
		}
		
		
	}
	
	public PaymentDetails sendOperation(PaymentDto req,Wallet fromWallet) {
		PaymentDetails resp ;
		try {
			try (CloseableHttpClient http = HttpClients.createDefault()) {
				LOG.info("sendOperation "+req.getInTrRefNumber());
				Session sess =(Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
				Partner partner = fromWallet.getPartner();
				APGCommonRequest cashReq = createRequest(req,fromWallet);
				ObjectNode additonal = new ObjectMapper().createObjectNode();
				additonal.put("paymentMeans", req.getCardCin());
				cashReq.setAdditionalParameters1(additonal.asText());
				LOG.info(req.getBeneficiary().getAddress());
				BigDecimal amount = new BigDecimal(req.getAmount().toString());
				completeRequest(cashReq, req);
				
				if(!req.getCurrencyName().equals(partner.getCurrencyName())) {
					AdministrationSession adminSess =(AdministrationSession)BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());
					CoursDevise coursDevise = adminSess.findCourDevise(req.getCurrencyName(), partner.getCurrencyName(),null);
					amount = amount.multiply(BigDecimal.valueOf(coursDevise.getValeurAPG()));
					if("EUR".equals(partner.getCurrencyName())) {
						amount = amount.setScale(2, RoundingMode.HALF_DOWN);
					}else if(Constantes.CURRENCY_XOF.equals(partner.getCurrencyName())||Constantes.CURRENCY_XAF.equals(partner.getCurrencyName())) {
						amount = amount.setScale(0, RoundingMode.HALF_UP);
					}
				}
				cashReq.setAmount(amount);
				String url = tools.getProperty(Constantes.WILDFLY_LOCAL) + Constantes.SEND_OPERATION;
				LOG.info(url);
				HttpPost post = new HttpPost(url);
				LOG.info(partner.getIdPartner());
				String auth = APIUtilVue.getInstance().apgDeCrypt(partner.getToken());
				partner.setDHmacKey(new Date());
				sess.updateObject(partner);
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader("auth", auth);
				
				ObjectMapper mapper = new ObjectMapper();
				String json = mapper.writeValueAsString(cashReq);
				StringEntity entity = new StringEntity(json,StandardCharsets.UTF_8);
				post.setEntity(entity);
				try (CloseableHttpResponse response = http.execute(post)) {
					LOG.info(json);
					LOG.info(response.getStatusLine().getStatusCode());
					String textRsp = new String(response.getEntity().getContent().readAllBytes(),
							StandardCharsets.UTF_8);
					LOG.info("response "+textRsp);
					resp = mapper.readValue(textRsp, PaymentDetails.class);
					LOG.info(resp.getCode());
					LOG.info(resp.getMessage());
					LOG.info(resp.toString());
					if(!ErrorResponse.REPONSE_SUCCESS.getCode().equals(resp.getCode())) {
						Character newline = '\n';
						StringBuilder builder = new StringBuilder(100);
						builder.append("Details transaction\nNom envoyeur :").append(cashReq.getSenderLastName())
								.append(newline).append("Prénom envoyeur :").append(cashReq.getSenderFirstName())
								.append(newline).append("Pays envoyeur :").append(cashReq.getFromCountry()).append(newline)
								.append("Nom bénéficiaire :").append(cashReq.getBeneficiaryLastName()).append(newline)
								.append("Prénom beneficiaire :").append(cashReq.getBeneficiaryFirstName()).append(newline)
								.append("Pays destinataire :").append(cashReq.getToCountry()).append(newline)
								.append("Montant :").append(cashReq.getAmount()).append(newline).append("Devise :")
								.append(partner.getCurrencyName()).append(newline).append("Canal :")
								.append(cashReq.getTrChannel()).append(newline).append("IdTransaction paiement :")
								.append(cashReq.getId()).append(newline).append("Erreur :").append(resp.getMessage())
								.append(newline);
						String subject = partner.getName() + ":ERREUR INITIALISATION OPERATION";
						String body = builder.toString();
						String toAdress = isProd?"opeartions@afripayway.com,team.it@afripayway.com":"team.it@afripayway.com";
						MailUtils.sendEmails(toAdress, subject, body, true, null, null);
			           } 
				}
			}     
		
		} catch (Exception e) {
			LOG.info("sendOperErr", e);
			 resp = new PaymentDetails(ErrorResponse.UNKNOWN_ERROR, "");
		}
		return resp;
	}
	
	public PaymentDetails updateOperation(PaymentDto request, Partner partner) {
		PaymentDetails resp ;
		try {
			try (CloseableHttpClient http = HttpClients.createDefault()) {
				LOG.info("updateOperation");
				String url = tools.getProperty(Constantes.WILDFLY_LOCAL) + Constantes.UPDATE_OPERATION;
				LOG.info(url);
				HttpPost post = new HttpPost(url);
				String auth = APIUtilVue.getInstance().apgDeCrypt(partner.getToken());
				Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
				partner.setDHmacKey(new Date());
				sess.updateObject(partner);
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader("auth", auth);
				
				ObjectMapper mapper = new ObjectMapper();
				String json = mapper.writeValueAsString(request);
				StringEntity entity = new StringEntity(json,StandardCharsets.UTF_8);
				post.setEntity(entity);
				try (CloseableHttpResponse response = http.execute(post)) {
					LOG.info(json);
					LOG.info(response.getStatusLine().getStatusCode());
					String textRsp = new String(response.getEntity().getContent().readAllBytes(),
							StandardCharsets.UTF_8);
					LOG.info(textRsp);
					resp = mapper.readValue(textRsp, PaymentDetails.class);
					LOG.info(resp.getCode());
					LOG.info(resp.getMessage());
					LOG.info(resp.toString());
				}
			}
		} catch (Exception e) {
			LOG.info("updateOperErr", e);
			resp=new PaymentDetails(ErrorResponse.UNKNOWN_ERROR, "");
		}
		return resp;
	}
	
	private APGCommonRequest createRequest(PaymentDto req,Wallet fromWallet) {
		APGCommonRequest cashReq=new APGCommonRequest();
		String channel =getChannelSendOperation(req.getTransactionType());
		cashReq.setTrChannel(channel);
		cashReq.setPurpose(req.getPurpose());
		cashReq.setPurposeDetails(req.getPurposeDetails());
		cashReq.setToBankName(req.getToBankName());
		cashReq.setSourceOfIncome(req.getSourceOfIncome());
		cashReq.setInTrRefNumber(req.getRequestId());
		cashReq.setSenderAddress(fromWallet.getAddress());
		cashReq.setSenderDateDelivered(fromWallet.getIssueDate());
		cashReq.setSenderDateExpired(fromWallet.getExpiryDate());
		cashReq.setSenderDateOfBirth(fromWallet.getBirthdate());
		cashReq.setSenderFirstName(fromWallet.getFirstname());
		cashReq.setSenderId(fromWallet.getDocumentNumber());
		cashReq.setSenderLastName(fromWallet.getLastname());
		cashReq.setSenderNationality(fromWallet.getCountryIsoCode());
		cashReq.setSenderPhone(fromWallet.getPhonenumber());
		cashReq.setSenderTypePiece(fromWallet.getDocumentType());
		cashReq.setCodePostal(fromWallet.getCodePostal());
		cashReq.setFromCountry(req.getFromCountry());
		cashReq.setBeneficiaryFirstName(req.getBeneficiary().getFirstName());
		cashReq.setBeneficiaryLastName(req.getBeneficiary().getLastName());
		cashReq.setBeneficiaryPhone(req.getBeneficiary().getPhoneNumber());
		cashReq.setBeneficiaryAddress(req.getBeneficiary().getAddress());
		cashReq.setToCountry(req.getToCountry());
		cashReq.setCodeAgence(req.getCodeAgence());
		return cashReq;
	}
}
