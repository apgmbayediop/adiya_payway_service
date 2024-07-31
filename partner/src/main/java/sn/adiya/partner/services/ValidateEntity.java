package sn.adiya.partner.services;

import java.util.ArrayList;
import java.util.List;

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
import sn.fig.common.utils.StringOperation;
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerUtilisateur;
import sn.fig.mdb.EmailMessage;
import sn.fig.session.MessageSenderService;
import sn.fig.session.MessageSenderServiceBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * Moussa SENE
 * Cherif DIOUF
 * @version 1.0
 **/

@Stateless
public class ValidateEntity {
	final static String TAG = ValidateEntity.class+"";
	final static Logger LOG = Logger.getLogger(ValidateEntity.class);
	public Response Service(String flashcode, APGCommonRequest apgValidateRequest){
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
			String[] Profil = {BEConstantes.GROUPE_SUPER_ADMIN_EP,BEConstantes.GROUPE_ADMIN_CHECKER_EP,BEConstantes.GROUPE_SUPER_ADMIN_P,BEConstantes.GROUPE_ADMIN_CHECKER_P};
			utilVue.commonVerifyProfil(user, TAG, Profil);
			if(user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_EP) || user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_P) || user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_CHECKER_P)){
				String otp = apgValidateRequest.getOtp();
				LOG.info("### --- --- OTP SUPER ADMIN: "+otp);
				if(user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) || user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER_DIASPORA) || user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER)) {
					if(!Boolean.TRUE.equals(utilVue.checkOtp(flashcode, otp))) {
						utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("otp : "), null);
					}
				}else {
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("otp : "), otp);
					String[] parameters = {"flashCode","otp"};
					Object[] data = {flashcode,otp};
					user = session.executeNamedQuerySingle(Utilisateur.class,"findByOTP", parameters, data);
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), user);
				}
			}
			Long id = apgValidateRequest.getId();
			utilVue.CommonObject(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("idEntite : "), id);
			Partner p = (Partner) session.findObjectById(Partner.class, id, null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Entite : "), p);
			if(p.getIsValidated()){
				return Response.ok().entity(new AbstractResponse("1", "Entity already validated "))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}
			String action = apgValidateRequest.getAction();
			if(BEConstantes.ACTION_REJECT.equals(action)){
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("reason : "),apgValidateRequest.getReason() );
				session.deleteObject(Partner.class.getName(), "idPartner", p.getIdPartner(), "Rejet Création Partner "+p.getName());

				String mess = "La demande de validation de l'entité "+p.getName()+" n a pas abouti, elle a ete rejete par"+user.getPrenom()+" "+user.getNom()+". Raison : "+apgValidateRequest.getReason();
				String[] params = {"idPartner"};  
				Object[] dats = {p.getIdPartner()};
				PartnerUtilisateur pu =  session.executeNamedQuerySingle(PartnerUtilisateur.class,"findUtilisateurByEntiteValidated", params, dats);
				if(pu == null) {
					pu = new PartnerUtilisateur();
					pu.setIsValidated(Boolean.TRUE);
					pu.setMaker(user);
					pu.setPartner(p);
					pu.setUtilisateur(user);

					session.saveObject(pu);
				}
				if(user != null)
					messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, user.getEmail(), "Rejet Création Entite", mess, ""));
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

			p.setIsValidated(true);
			p.setIsActive(true);
			session.updateObject(p);
			if(user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_CHECKER_P))
				notifyUser(user,id);
			String subject = "ACTIVATION ENTITY",
					msg = " Dear "+p.getPrenomContact()+" "+p.getNomContact()+" , your entity has successfully activated",
					header = p.getEmailContact();
			messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, header, subject, msg, ""));
			List<APGPartner> lPtns = new ArrayList<APGPartner>();
			APGPartner ptn = new APGPartner();
			ptn.setId(p.getIdPartner());
			ptn.setName(p.getName());
			ptn.setTelephonePartner(p.getTelephonePartner());
			ptn.setTelephoneContact(p.getTelephoneContact());
			ptn.setEmail(p.getEmailContact());
			ptn.setCurrencyName(p.getCurrencyName());
			ptn.setCountryIsoCode(p.getCountryIsoCode());
			ptn.setNomContact(p.getNomContact());
			ptn.setPrenomContact(p.getPrenomContact());
			ptn.setPartnerType(p.getPType());
			//		ptn.setPrincipalAccountNumber(p.getPrincipalAccountNumber());
			ptn.setPartnerCode(p.getCode());
			ptn.setIsValidated(p.getIsValidated());
			lPtns.add(ptn);
			return Response.ok().entity(new APGPartnerResponse("0", "OK",lPtns))
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

	public static void notifyUser(Utilisateur user, Long idEntite) {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());

		if(user.getPartner().getLogo().equalsIgnoreCase("logo-cfp") || user.getPartner().getLogo().equalsIgnoreCase("logo-coopec"))
			return;

		String[] parameters = {"idPartner"};
		Object[] data = {idEntite};
		List<PartnerUtilisateur> lPU = session.executeNamedQueryList(PartnerUtilisateur.class, "findUtilisateurByEntite", parameters, data);
		for (PartnerUtilisateur pu : lPU) {
			Utilisateur utilisateur = pu.getUtilisateur();
			if(!Boolean.TRUE.equals(utilisateur.getIsActive())) {
				String smsPassword = StringOperation.getPasswwd();
				utilisateur.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
				utilisateur.setIsActive(true);
				session.updateObject(utilisateur);
				String msg="Bonjour, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" Votre identifiant : "+utilisateur.getLogin()+" votre mot de passe : "+smsPassword;
				APIUtilVue.getInstance().notifyUtilisateur(utilisateur, msg);
				String opt = user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) ? "\"<tr style=\"color:white;\">\n" + 
						"<th colspan=\"2\" bgcolor=\"black\">Merci de vous connecter sur https://pms.optima.world</th>\n" + 
						"</tr>\n" : "";
				msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
						"   <tr>\n" + 
						"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
						"   </tr>\n" + 
						"   <tr style=\"color:white;\">\n" + 
						"      <th bgcolor=\"black\">Votre identifiant</th>\n" + 
						"      <th bgcolor=\"black\">Votre mot de passe</th>\n" + 
						opt +
						"   </tr>\n" + 
						"   <tr style=\"color:black;\">\n" + 
						"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
						"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
						"   </tr>\n" + 
						"</table> ";
				String subject = utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" votre compte "+utilisateur.getPartner().getPType(),
						header = utilisateur.getEmail();
				String datas = "";
				messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null,header, subject, msg, datas));	
			}
		}
	}



}
