package sn.payway.partner.services;

import java.util.List;

import javax.ejb.Stateless;
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
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.PartnerUtilisateur;
import sn.apiapg.mdb.EmailMessage;
import sn.apiapg.session.MessageSenderService;
import sn.apiapg.session.MessageSenderServiceBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * Moussa SENE
 * Cherif DIOUF
 * @version 1.0
 **/
@Stateless
public class LinkEntityUser {
	final static String TAG = LinkEntityUser.class+"";
	final static Logger LOG = Logger.getLogger(LinkEntityUser.class);
	public Response Service(String flashcode, APGCommonRequest apgLink){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());

			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), user);
			Partner partner = null;
			PartnerUtilisateur part = null;
			if(user.getPartner().getPType().equals(BEConstantes.PARTNER_SENDER_PAYER)) {
				String[] Profil = {BEConstantes.GROUPE_ADMIN_CHECKER_EP, BEConstantes.GROUPE_ADMIN_MAKER_EP, BEConstantes.GROUPE_ADMIN_MAKER_EP, BEConstantes.GROUPE_ADMIN_CHECKER_EP, BEConstantes.GROUPE_SUPER_ADMIN_EP};
				utilVue.commonVerifyProfil(user, "ListEntity", Profil);
			}else if(user.getPartner().getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
				String[] Profil = {BEConstantes.GROUPE_SUPERVISEUR_P,BEConstantes.GROUPE_SUPERVISEUR_SD, BEConstantes.GROUPE_ADMIN_MAKER_P, BEConstantes.GROUPE_ADMIN_CHECKER_P, BEConstantes.GROUPE_SUPER_ADMIN_P,BEConstantes.GROUPE_SUPER_ADMIN_DA,BEConstantes.GROUPE_SUPER_ADMIN_SD};
				utilVue.commonVerifyProfil(user, "ListEntity", Profil);
			}
			String userId = apgLink.getUserId(),entityId=apgLink.getEntityId();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("userId : "), userId);
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("entityId : "), entityId);
			try {
				Long.parseLong(entityId);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("entityId")))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
						.build();
			}
			try {
				Long.parseLong(userId);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("userId")))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
						.build();
			}

			/*
			 * Debut Validation Du CHECKER
			 */
			String[] parameters = {"idPartner","idUtilisateur"};
			if(user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_CHECKER_EP) || user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_CHECKER_P)){
				Object[] data = {Long.parseLong(entityId),Long.parseLong(userId)};
				part = (PartnerUtilisateur) session.executeNamedQuerySingle(PartnerUtilisateur.class,"findEntiteUtilisateur", parameters, data);
				utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Entite "+entityId+" Utilisateur "+userId+" : "), part);
				String action = apgLink.getAction();
				if(BEConstantes.ACTION_REJECT.equals(action)){
					utilVue.CommonLabel(null,"ValidateEntity",ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("reason : "),apgLink.getReason() );
					session.deleteObject(PartnerUtilisateur.class.getName(), "idPartnerUtilisateur", part.getIdPartnerUtilisateur(), "Rejet Assignation Entité "+part.getPartner().getName());

					String mess = "La demande d'assignation de l'entité "+part.getPartner().getName()+" avec l'utilisateur "+part.getUtilisateur().getPrenom()+" "+part.getUtilisateur().getNom()+" n a pas abouti, elle a ete rejete par"+user.getPrenom()+" "+user.getNom()+". Raison : "+apgLink.getReason();
					if(part.getMaker() != null)
						messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, part.getMaker().getEmail(), "Rejet Assignation Entite", mess, ""));
					return Response.ok().entity(new AbstractResponse("0", "Entity rejected "))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
							.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
							.build();	

				}else if(action != null && !"".equals(action)){
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("action : ")))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
							.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
							.build();
				}

				if(part.getIsValidated() == false){
					part.setIsValidated(true);
					session.updateObject(part);
					String mess = "Assignation entité "+part.getPartner().getName()+" avec l'utilisateur "+part.getUtilisateur().getPrenom()+" "+part.getUtilisateur().getNom()+" validé avec succès";
					if(part.getMaker() != null)
						messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, part.getMaker().getEmail(), "Assignation Entite", mess , ""));
					return Response.ok().entity(new AbstractResponse("0", "Entite successfully validated"))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
							.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
							.build();
				}
				if(part.getIsValidated() == null || part.getIsValidated() != false){
					return Response.ok().entity(new AbstractResponse("1", "The maker must validate before the checker"))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
							.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
							.build();
				}
			}
			/*
			 * Fin Validation Du CHECKER
			 */

			partner = (Partner) session.findObjectById(Partner.class,  Long.parseLong(entityId), null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Entite "+entityId+" : "), partner);
			Utilisateur us = (Utilisateur) session.findObjectById(Utilisateur.class, Long.parseLong(userId), null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur "+userId+" : "), us);

			String respLink = "";
			Object[] data = {Long.parseLong(entityId),Long.parseLong(userId)};
			String[] param = {"idPartner","idUtilisateur"};
			//	Object[] dt = {Long.parseLong(entityId),Long.parseLong(userId)};
			List<PartnerUtilisateur> lPartUser = session.executeNamedQueryList(PartnerUtilisateur.class, "findDuplicatedEntiteUtilisateur", param, data);
			if(!lPartUser.isEmpty() && apgLink.getAction() != null && !apgLink.getAction().equals("unlinked")) {
				if(!us.getPartner().getCode().equals(BEConstantes.CODE_CFP) && !us.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_SD) && !us.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CONTROLEUR_DSD))
					return Response.ok().entity(new AbstractResponse("1", "Entite already linked : DUPLICATA"))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
							.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
							.build();	
			}
			part = (PartnerUtilisateur) session.executeNamedQuerySingle(PartnerUtilisateur.class,"findEntiteUtilisateur", parameters, data);
			if(apgLink.getAction() != null && apgLink.getAction().equals("unlinked")) {
				/*
				 * DEBUT ACTION UNLINK
				 */
				if(part != null) {
					String details="Suppression DeleteSettlemment "+user.getPrenom()+" "+user.getNom();
					session.deleteObject("PartnerUtilisateur", "idPartnerUtilisateur", part.getIdPartnerUtilisateur(), details);
					respLink = "unlinked";
					return Response.ok().entity(new AbstractResponse("0", "Entite successfully "+respLink))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
							.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
							.build();
				}
			}
			/*
			 * FIN ACTION UNLINK
			 */
			part = new PartnerUtilisateur();
			String	otp ;
			if(user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_MAKER_EP) || user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_MAKER_P)) {
				part.setIsValidated(false);
				part.setMaker(user);
			}else if(user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_EP) || user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_P)){
				otp = apgLink.getOtp();
				LOG.info("### --- --- OTP SUPER ADMIN: "+otp);
				if(user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) || user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER_DIASPORA) || user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER)) {
					if(!Boolean.TRUE.equals(utilVue.checkOtp(flashcode, otp))) {
						utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("otp : "), null);
					}
				}else {
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("otp : "), otp);
					String[] parm = {"flashCode","otp"};
					Object[] dat = {flashcode,otp};
					user = session.executeNamedQuerySingle(Utilisateur.class,"findByOTP", parm, dat);
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), user);
				}
				part.setIsValidated(true);
			}
			else if(user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_DA) || user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_SD)) {
				part.setIsValidated(true);
			}
			part.setPartner(partner);
			part.setUtilisateur(us);

			session.saveObject(part);
			String mess = "Assignation entité "+part.getPartner().getName()+" avec l'utilisateur "+part.getUtilisateur().getPrenom()+" "+part.getUtilisateur().getNom()+" validé avec succès";
			if(part.getMaker() != null)
				messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, part.getMaker().getEmail(), "Assignation Entite", mess , ""));
			respLink = "linked";
			return Response.ok().entity(new AbstractResponse("0", "Entite successfully "+respLink))
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
