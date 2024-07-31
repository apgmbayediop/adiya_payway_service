package sn.adiya.payment.sms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.ChannelResponse;
import sn.fig.common.utils.MailUtils;
import sn.fig.entities.Card;
import sn.fig.entities.Country;
import sn.fig.entities.Partner;
import sn.fig.entities.Transaction;
import sn.fig.entities.Wallet;
import sn.fig.entities.aci.Caisse;
import sn.fig.session.MessageSenderService;
import sn.fig.session.MessageSenderServiceBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.payment.dto.DataResponse;
import sn.adiya.payment.dto.PaymentDto;
import sn.adiya.payment.dto.SmsPayDto;
import sn.adiya.payment.services.finalize.FinalizePayment;

@JBossLog
@Stateless
@Asynchronous
public class SMSSender {

	public static final String SN_INDICATIF="221";
	public void sendNotification(PaymentDto request,DataResponse resp, String network) {
		MessageSenderService sender = (MessageSenderService) BeanLocator
				.lookUp(MessageSenderServiceBean.class.getSimpleName());
		try {
			
			if(ChannelResponse.PAY_TRAVEL.getCode().equals(request.getTransactionType())||
					ChannelResponse.PAY_OPEN_ACCT.getCode().equals(request.getTransactionType())) {
				return;
			}
			String path = BEConstantes.NOTIF_TEMPLATE_TRX;
			String indicatif="";
			Card customerCard = resp.getCustomerCard();
			Card merchantCard = resp.getMerchantCard();
			Transaction trx = resp.getTrx();
			Partner commercant = trx.getPartner();
			Caisse caisse = resp.getCaisse();
			Wallet wallet = resp.getWallet();
			String customerPhone="";
			String customerCurrency="";
			String customer ="Client : ";
			String customerName  ="";
			String customerAcctNumber ="";
			boolean knownCustomer=false;
			BigDecimal customerBalance =BigDecimal.ZERO;
			boolean cardUsed= customerCard ==null?false:true;
			boolean walletUsed= wallet ==null?false:true;
			
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			if (cardUsed) {
				indicatif = getIndicatif(customerCard.getRegister().getCustomerIndicatif(),
						customerCard.getRegister().getCountry(),sess);
				customerName = customer+customerCard.getRegister().getFirstname()+" "+customerCard.getRegister().getLastname();
				knownCustomer =true;
				customerBalance = "FINAO".equals(network)?BigDecimal.ZERO:customerCard.getMontant();
				customerPhone = indicatif+customerCard.getRegister().getPhonenumber();
				customerCurrency = customerCard.getCurrency().getCurrencyName();
				customerAcctNumber = customerCard.getCin();
			}else if(walletUsed) {
				indicatif = getIndicatif(wallet.getCustomerIndicatif(),
						wallet.getCountryIsoCode(),sess);
				customerName = customer+wallet.getFirstname()+" "+wallet.getLastname();
				knownCustomer =true;
				customerBalance = new BigDecimal(resp.getDataCustomer().get(BEConstantes.PRM_HTML_BALANCE));
				customerPhone = indicatif+wallet.getPhonenumber();
				customerCurrency = wallet.getCurrencyName();
				customerAcctNumber = wallet.getPhonenumber();
			}
			String subject = "NOTIFICATION TRANSACTION";

			if(FinalizePayment.GENESYS_NETWORK.equals(network)) {
				log.info("genesys notification");
			}else {
				SmsPayDto merchantResp= new SmsPayDto();
				String phones =caisse.getTelephone()==null?caisse.getPointDeVente().getCommercant().getTelephoneContact():caisse.getTelephone();
	            merchantResp.setAccountNumber(resp.getDataPartner().get(BEConstantes.PRM_HTML_ACCOUNT));
				merchantResp.setBalance(new BigDecimal(resp.getDataPartner().get(BEConstantes.PRM_HTML_BALANCE)));
				merchantResp.setPhone(phones);
				merchantResp.setCurrencyName(commercant.getCurrencyName());
				merchantResp.setIndicatif(commercant.getCountryIndicatif());
				merchantResp.setTransactionId(request.getTransactionId());
				merchantResp.setRequestId(request.getRequestId());
				
				notifyMerchant(customerName, caisse, merchantCard, trx, merchantResp);
				if (resp.getMerchantEmail() != null) {
					MailUtils.sendMailHtml(resp.getMerchantEmail(), subject, path, resp.getDataPartner());
				}
				if(knownCustomer) {
					log.info("knownCustomer");
						subject = "NOTIFICATION OPERATION";
						if (resp.getCustomerEmail() != null) {
							MailUtils.sendMailHtml(resp.getCustomerEmail(), subject, path, resp.getDataCustomer());
						}
						SmsPayDto response = new SmsPayDto();
						response.setAccountNumber(customerAcctNumber);
						response.setBalance(customerBalance);
						response.setPhone(customerPhone);
						response.setCurrencyName(customerCurrency);
						response.setIndicatif(indicatif);
						response.setTransactionId(request.getTransactionId());
						response.setRequestId(request.getRequestId());
						notifyCustomer(response, trx);
					}
			}
	
		} catch (Exception e) {
			log.info("errorNotify",e);
			sender.notifysendSMS("APGSA",SN_INDICATIF, "221765503456", "erreur notification tpe");

		}
	}
	public void notifyReloadWallets(SmsPayDto response,Transaction trx) {
		try {
			StringBuilder message = new StringBuilder(150);
			String channelReload= "";
			Character space = ' ';
			//"Orange money,  wave, Free Money,  E-money, Carte Bancaire";
			String date = new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME,Locale.getDefault()).format(trx.getDate());
				message.append("Cher (e), client (e)  votre rechargement ").
				append(channelReload).append(" d'un montant de ").append(formatAmount(trx.getPayoutAmount())).
				append(space).append(trx.getPayoutCurrencyLabel()).append(" du ").append(date).
				append(space).
				append("a reussi . Votre nouveau solde est : ")
				.append(formatAmount(response.getBalance())).append(space).append(trx.getPayoutCurrencyLabel()).
				append("Reference :").append(response.getTransactionId()).append(". Optima vous remercie.");
				
				APIUtilVue.getInstance().sendSMS(trx.getPartner().getSenderSms(),
						response.getIndicatif(),response.getPhone(), message.toString());  
		}
		catch (Exception e) {
			log.error("notifyCustomerWallet", e);	
		}
	}
	
	public void notifyCustomer(SmsPayDto response,Transaction trx) {
		ChannelResponse channel = ChannelResponse.getChannel(trx.getChannelType());
		switch (channel) {
		case PAY_LOCATION: loyerNotifyCustomer(response, trx);break;
		case W2W:
		case CARD2W: notifyReloadWallets(response,trx);break;
		default:defaultNotifyCustomer(response, trx);break;
		}
	}
	
	private void defaultNotifyCustomer(SmsPayDto response,Transaction trx) {
		try {
			StringBuilder message = new StringBuilder(120);
			char space =' ';
			String date = new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME,Locale.getDefault()).format(trx.getDate());
			if(BEConstantes.CANCELED.equals(trx.getStatus())) {
				message.append("Rechargement de ").append(formatAmount(trx.getPayinAmount()))
				.append(space).append(trx.getPayinCurrencyLabel()).append(space).
				append("après annulation du marchand ").append(trx.getPartner().getName()).
				append(" est effectue avec succes le ")
				.append(date).append(". Solde ").append(formatAmount(response.getBalance())).
				append(space).append(trx.getPayinCurrencyLabel());
			}else {
			message.append("Votre paiement de ").append(formatAmount(trx.getPayinAmount()))
			.append(space).append(trx.getPayinCurrencyLabel()).append(space).
			append("au marchand ").append(trx.getPartner().getName()).
			append(" est effectue avec succes le ")
			.append(date).append(". Solde ").append(formatAmount(response.getBalance())).
			append(space).append(trx.getPayinCurrencyLabel());
			}
			APIUtilVue.getInstance().sendSMS(trx.getPartner().getSenderSms(), 
					response.getIndicatif(),response.getPhone(), message.toString());  
		}
		catch (Exception e) {
			log.error("notifyCustomerPayment", e);	
		}
	}
	
	private void loyerNotifyCustomer(SmsPayDto response,Transaction trx) {
		try {
			
			StringBuilder message = new StringBuilder(120);
			char space =' ';
			String date = new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME,Locale.getDefault()).format(trx.getDate());
			if(BEConstantes.CANCELED.equals(trx.getStatus())) {
				message.append("Rechargement de ").append(formatAmount(trx.getPayinAmount()))
				.append(space).append(trx.getPayinCurrencyLabel()).append(space).
				append("après annulation du marchand ").append(trx.getPartner().getName()).
				append(" est effectue avec succes le ")
				.append(date).append(". Solde ").append(formatAmount(response.getBalance())).
				append(space).append(trx.getPayinCurrencyLabel());
			}else {
			message.append("Paiement Location ").append(trx.getBeneficiaryAddress())
			.append(" Locataire : ").append(trx.getBeneficiaryFirstName()).append(space).
			append(trx.getBeneficiaryLastName()).append('(').append(trx.getBeneficiaryNumeroPiece()).
			append("), Montant ").append(formatAmount(trx.getPayinAmount()))
			.append(space).append(trx.getPayinCurrencyLabel()).append(space).append(". ")
			.append(trx.getPartner().getName())
			.append(". Votre nouveau solde  est de ").append(formatAmount(response.getBalance())).
			append(space).append(trx.getPayinCurrencyLabel()).
			append(". REF ").append(response.getTransactionId())
			.append(". Date ").append(date);
			}
			APIUtilVue.getInstance().sendSMS(trx.getPartner().getSenderSms(), 
					response.getIndicatif(),response.getPhone(), message.toString());  
		}
		catch (Exception e) {
			log.error("notifyCustomerPayment", e);	
		}
	}
	private void notifyMerchant(String customerName,Caisse caisse , Card merchantCard,Transaction trx,SmsPayDto response) {
		try {
			String date = new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME,Locale.getDefault()).format(trx.getDate());
			BigDecimal realAmount ;
			String currencyLabel;
			if(trx.getPayinCurrency().equals(trx.getPayoutCurrency())) {
				realAmount = trx.getPayinAmount();
				currencyLabel = trx.getPayinCurrencyLabel();
			}else {
				realAmount=new BigDecimal(trx.getRealAmount());
				currencyLabel =trx.getPayoutCurrencyLabel();
			}
			StringBuilder message =new StringBuilder(100);
			if(merchantCard==null) {
				
				message.append("Encaissement de ").append(formatAmount(realAmount)).append(' ').append(currencyLabel).
				append(" le ").append(date).append(" pour le marchand ").append(caisse.getPointDeVente().getCommercant().getName()).
				append('.').append(customerName).
				append(" REF ID : ").append(response.getTransactionId()).
				append(". Solde : ").append( formatAmount(trx.getNewBalance())).append(' ').append(trx.getPayoutCurrencyLabel());
				} else{
					message.append("Encaissement de ").append(realAmount).append(' ').append(currencyLabel).
					append(" le ").append(date).
					append('.').append(customerName).
					append(" REF ID : ").append(response.getTransactionId()).
					append(". Solde : ").append(formatAmount( merchantCard.getMontant())).append(' ').append(trx.getPayoutCurrencyLabel());
					
				}
			message.append(". Telechargez l'application Optima Business ici, https://bit.ly/3AN1yF0.");
			String phones =response.getPhone().replace(" ", "");
            Set<String> list = new HashSet<>();
            if(phones.contains(",")) {
			  list.addAll(Arrays.asList(phones.split(",")));
            	}else {
            	list.add(phones);
            }
            list.forEach(phone->{
            	APIUtilVue.getInstance().sendSMS(trx.getPartner().getSenderSms(), 
    					phone.substring(0,3),phone, message.toString());  
             });
		}
		catch (Exception e) {
			log.error("erroNotifyMerchant",e);
		}
	}
	private String formatAmount(BigDecimal amount) {

		BigDecimal amnt = amount ==null?BigDecimal.ZERO:amount;
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(' ');
		formatter.setDecimalFormatSymbols(symbols);
		return formatter.format(amnt.setScale(2, RoundingMode.DOWN));
	}
	private String getIndicatif(String indicatifCard,String countryCode, Session sess) {
		String indicatif;
		if(indicatifCard ==null||indicatifCard.isBlank()) {
			Country country = sess.executeNamedQuerySingle(Country.class, "findByCountryCode",
					new String[] { "countryCode" }, new String[] { countryCode });
			indicatif = country.getCountryIndicatif();
		}else {
			indicatif = indicatifCard;
		}
		return indicatif;
	}
	
}
