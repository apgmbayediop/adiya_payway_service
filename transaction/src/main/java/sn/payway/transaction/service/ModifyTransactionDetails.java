package sn.payway.transaction.service;

import java.io.UnsupportedEncodingException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.exception.TransactionException;
import sn.apiapg.common.utils.APGCommonRequest;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.ChannelResponse;
import sn.apiapg.entities.Transaction;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class ModifyTransactionDetails {
	
	@Inject
	RenvoiCode renvoiCode;
	
	final static String TAG = ModifyTransactionDetails.class+"";
	public final static Logger LOG = Logger.getLogger(ModifyTransactionDetails.class);
	public Response Service(String flashcode,APGCommonRequest apgUtilisateurRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			Session session2 = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			LOG.info("####### Debut ModifyTransactionDetails from APG >>>>>>>");
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);			
			String[] Profil={BEConstantes.GROUPE_OPERATION_MANAGER,BEConstantes.GROUPE_SUPERVISEUR_SD,BEConstantes.GROUPE_SUPERVISEUR_P};
			utilVue.commonVerifyProfil(USER, TAG, Profil);
			String prenom = apgUtilisateurRequest.getBeneficiaryFirstName();
			String nom = apgUtilisateurRequest.getBeneficiaryLastName();
			String phone = apgUtilisateurRequest.getPhone();
			Long id = apgUtilisateurRequest.getId();
			utilVue.CommonObject(null, TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("id"), id);
			String[] param = {"id"};
			Object[] dat= {id};
			Transaction trans = (Transaction) session2.executeNamedQuerySingle(Transaction.class, "findTransactionById", param, dat);
			utilVue.CommonObject(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Transaction : "), trans);
			if(!trans.getStatus().equals(BEConstantes.STATUS_TRANSACTION_PENDING) && !(trans.getStatus().equals(BEConstantes.STATUS_TRANSACTION_VALIDATED) && ChannelResponse.C2C.getCode().equals(trans.getChannelType()))) {
				utilVue.CommonObject(null,TAG,ErrorResponse.POS_NOT_ALLOW_OPTIMA.getCode(),ErrorResponse.POS_NOT_ALLOW_OPTIMA.getMessage("Transaction déjà payée ou en instance de paiement"), null);
			}
			if(prenom != null && !prenom.equals(""))
				trans.setBeneficiaryFirstName(prenom);
			if(nom != null && !nom.equals(""))
				trans.setBeneficiaryLastName(nom);
			if(phone != null && !phone.equals(""))
				trans.setBeneficiaryMobileNumber(phone);
			session.updateObject(trans);
			if(phone != null && !phone.equals("")) {
				apgUtilisateurRequest.setOperationId(id+"");
				try {
					renvoiCode.Service(flashcode, apgUtilisateurRequest);
				} catch (UnsupportedEncodingException e) {
					LOG.info("================------- ERREUR RENVOI CODE ------===================");
				}
			}
			return Response.ok().entity(new AbstractResponse("0", "Transaction successfully modified"))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		} catch (TransactionException e) {
			return Response.ok().entity(new AbstractResponse(e.getCode(), e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		}	
	}

}