package sn.adiya.transaction.service;

import java.io.UnsupportedEncodingException;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.Transaction;
import sn.fig.session.OperationSession;
import sn.fig.session.OperationSessionBean;

@Stateless
public class RenvoiCode {
	final static String TAG = ModifyTransactionDetails.class+"";
	public final static Logger LOG = Logger.getLogger(ModifyTransactionDetails.class);
	public Response Service(String flashcode, APGCommonRequest apgRenvoiCodeRequest) throws UnsupportedEncodingException{
		try {
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
			APIUtilVue utilVue = APIUtilVue.getInstance();

			LOG.info("####### Debut Renvoi Code from APG >>>>>>>");
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();   
			}
			
			if(!USER.getPartner().getCode().equals(BEConstantes.CODE_APG)) {
				String message = "Profile not authorized : "+USER.getIdGroupeUtilisateur()+ " ";
				return Response.ok().entity(new AbstractResponse(ErrorResponse.REPONSE_UNSUCCESS.getCode(), message))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
						.build();
			}
			LOG.info("####### partnerId : "+USER.getPartner().getIdPartner()+"");
			LOG.info("####### partner name : "+USER.getPartner().getName());
			
			String operationId = apgRenvoiCodeRequest.getOperationId();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("operationId : "), operationId);

			Transaction transaction = operationSession.findTransactions(operationId);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("transaction"), transaction);

			String reseaux = "";
			if(transaction.getPartnerPayer().getCode() != null && transaction.getPartnerPayer().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER)){
				notifySMS(transaction);			
			}else{
				String messageTo="Bonjour, vous avez recu un transfert de "+transaction.getPayoutAmount()+" "+transaction.getPayoutCurrency()+" de "+transaction.getSenderFirstName()+" "+transaction.getSenderLastName()+" Code de retrait "+utilVue.getCodeCustomer(utilVue.apgDeCrypt(transaction.getOperationId()))+reseaux;
				utilVue.sendSMS("APGSA", transaction.getToCountry().getCountryIndicatif(),transaction.getBeneficiaryMobileNumber(), messageTo); 
			}
			return Response.ok().entity(new AbstractResponse(ErrorResponse.REPONSE_SUCCESS.getCode(), ErrorResponse.REPONSE_SUCCESS.getMessage("")))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.build();
		} catch (TransactionException e) {
			return Response.ok().entity(new AbstractResponse(e.getCode(), e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		}
	}

	private static void notifySMS(Transaction transaction) throws UnsupportedEncodingException {
		APIUtilVue utilVue = APIUtilVue.getInstance();
		String reseaux = " Disponible dans le reseau "+transaction.getPartner().getSenderSms();
		String messageTo="Vous avez recu un transfert de "+transaction.getPayoutAmount()+" "+transaction.getPayoutCurrencyLabel()+" de "+transaction.getSenderFirstName()+" "+transaction.getSenderLastName()+" le "+utilVue.commonDateToString((transaction.getDate()),BEConstantes.FORMAT_DATE_TIME)+" . Code de retrait "+utilVue.getCodeCustomer(utilVue.apgDeCrypt(transaction.getOperationId()))+" "+reseaux+". REF ID: "+transaction.getId()+". Telechargez Optima App ici https://bit.ly/3q52ksp Contact 338606067";
		utilVue.sendSMS(transaction.getPartner().getSenderSms(), transaction.getToCountry().getCountryIndicatif(),transaction.getBeneficiaryMobileNumber(), messageTo); 

		String prefixe="SD";
		if(transaction.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER_DIASPORA)) prefixe="D";
		else if(transaction.getPartner().getEspace().equals(BEConstantes.ESPACE_PROVIDER)) prefixe="SD";
		else if(transaction.getPartner().getEspace().equals(BEConstantes.ESPACE_ENVOYEUR)) prefixe="S";
		if(transaction.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER)) prefixe="IC";

		String messageToR="("+prefixe+" )Client "+transaction.getBeneficiaryMobileNumber()+" Vous avez recu un transfert de "+transaction.getPayoutAmount()+" "+transaction.getPayoutCurrencyLabel()+" de "+transaction.getSenderFirstName()+" "+transaction.getSenderLastName()+" le "+utilVue.commonDateToString((transaction.getDate()),BEConstantes.FORMAT_DATE_TIME)+" . Code de retrait "+utilVue.getCodeCustomer(utilVue.apgDeCrypt(transaction.getOperationId()))+" "+reseaux+". REF ID: "+transaction.getId()+". Telechargez Optima App ici https://bit.ly/3q52ksp Contact 338606067";
		if(utilVue.isPROD() || utilVue.isPREPROD()) {
			utilVue.sendSMS(transaction.getPartner().getSenderSms(), transaction.getToCountry().getCountryIndicatif(),"771233088", messageToR); 
			utilVue.sendSMS(transaction.getPartner().getSenderSms(), transaction.getToCountry().getCountryIndicatif(),"775790048", messageToR); 
			utilVue.sendSMS(transaction.getPartner().getSenderSms(), transaction.getToCountry().getCountryIndicatif(),"706391532", messageToR); 
			utilVue.sendSMS(transaction.getPartner().getSenderSms(), transaction.getToCountry().getCountryIndicatif(),"776357917", messageToR); 
			utilVue.sendSMS(transaction.getPartner().getSenderSms(), transaction.getToCountry().getCountryIndicatif(),"776358213", messageToR); 
		}
	} 

}
