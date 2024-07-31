package sn.adiya.user.services;

import java.io.UnsupportedEncodingException;
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
import sn.fig.mdb.EmailMessage;
import sn.fig.session.MessageSenderService;
import sn.fig.session.MessageSenderServiceBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.user.object.SessionUserResponse;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationChangePassword {

	final static String TAG = AdministrationChangePassword.class+"";
	static final Logger LOG = Logger.getLogger(AdministrationChangePassword.class);
	public Response Service(String flashcode,String token,APGCommonRequest apgPasswordRequest) throws UnsupportedEncodingException{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());

			LOG.info("####### Start Change Password from APG >>>>>>> ");
			Utilisateur USER = null;
			Utilisateur user = null;
			String newPassword = null;
			if(!BEConstantes.CANAL_USSD.equals(apgPasswordRequest.getCanal())) {
				Response globalResponse = utilVue.CommonFlashCode(flashcode);
				if (globalResponse.getEntity() instanceof Utilisateur) {
					USER = (Utilisateur) globalResponse.getEntity();
				}
				utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
				String oldPassword = apgPasswordRequest.getOldPassword();
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("oldPassword : "), oldPassword);
				newPassword = apgPasswordRequest.getNewPassword();
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("newPassword : "), newPassword);
				if(!Boolean.TRUE.equals(utilVue.checkPassword(newPassword))) {
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("password : "), null);
				}
				String confPassword = apgPasswordRequest.getConfPassword();
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("confPassword : "), confPassword);
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("token : "), token);
				if(!newPassword.equals(confPassword)) {
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("confPassword : "), null);
				}
				String[] parameters = {"flashCode","token"}; 
				Object[] data = {flashcode, utilVue.getTag(utilVue.decodeToken(token))};
				Utilisateur usr = session.executeNamedQuerySingle(Utilisateur.class,"findByToken", parameters, data);
				utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Token Utilisateur "), usr);

				LOG.info("####### End Session User >>>>>>>");
				LOG.info("------- Token : "+token);
				LOG.info("####### Start Login ChangePassword >>>>>>>");

				String[] params = {"flashCode","token","password"};
				Object[] dats = {flashcode, utilVue.getTag(utilVue.decodeToken(token)), utilVue.apgSha(oldPassword)};
				user = session.executeNamedQuerySingle(Utilisateur.class,"findByTokenPasswd", params, dats);
				utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Token Utilisateur oldPassword "), user);
				user.setPassword(APIUtilVue.getInstance().apgSha(newPassword));
				user.setNbPassword(0L);

				if(user.getFirst())
					user.setFirst(false);

				session.updateObject(user);

				String subject = "YOUR PASSWORD HAS BEEN CHANGED",
						header = user.getEmail();
				String datas = "";
				String opt = user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) ? "\"<tr style=\"color:white;\">\n" + 
						 "<th colspan=\"2\" bgcolor=\"black\">Merci de vous connecter sur https://pms.optima.world</th>\n" + 
						 "</tr>\n" : "";
				String msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
						"   <tr>\n" + 
						"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
						"   </tr>\n" + 
						"   <tr style=\"color:white;\">\n" + 
						"      <th bgcolor=\"black\">Votre identifiant</th>\n" + 
						"      <th bgcolor=\"black\">Votre mot de passe</th>\n" + 
						opt +
						"   </tr>\n" + 
						"   <tr style=\"color:black;\">\n" + 
						"      <th bgcolor=\"white\"> "+user.getLogin()+" </th>\n" + 
						"      <th bgcolor=\"white\"> "+newPassword+" </th>\n" + 
						"   </tr>\n" + 
						"</table> ";

					messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, header, subject, msg, datas));
			}else { 
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("ussdLogin"),apgPasswordRequest.getUssdLogin());
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("ussdPassword"),apgPasswordRequest.getUssdPassword());
				String[] parameters = {"ussdLogin","ussdPassword"};
				Object[] data = {apgPasswordRequest.getUssdLogin(),utilVue.apgSha(apgPasswordRequest.getUssdLogin()+apgPasswordRequest.getUssdPassword())};
				List<Utilisateur> users = (List<Utilisateur>) session.executeNamedQueryList(Utilisateur.class,"findUserByUssd", parameters, data);
				if(!users.isEmpty()) {
					user = users.get(0);
				}else
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), null);
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("newUssdPassword"),apgPasswordRequest.getNewUssdPassword());
				if(apgPasswordRequest.getNewUssdPassword().length() != 4)				
					utilVue.CommonLabel(null,TAG,ErrorResponse.USSD_PIN_INCORRECT_OPTIMA.getCode(),ErrorResponse.USSD_PIN_INCORRECT_OPTIMA.getMessage("ussdPassword"),null);
				user.setUssdPassword(utilVue.apgSha(apgPasswordRequest.getUssdLogin()+apgPasswordRequest.getNewUssdPassword()));

				session.updateObject(user);

				String subject = "YOUR USSD PASSWORD HAS BEEN CHANGED",
						header = user.getEmail();
				String datas = "";
				String msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
						"   <tr>\n" + 
						"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
						"   </tr>\n" + 
						"   <tr style=\"color:white;\">\n" + 
						"      <th bgcolor=\"black\">USSD LOGIN</th>\n" + 
						"      <th bgcolor=\"black\">USSD PASSWORD</th>\n" + 
						"   </tr>\n" + 
						"   <tr style=\"color:black;\">\n" + 
						"      <th bgcolor=\"white\"> "+user.getUssdLogin()+" </th>\n" + 
						"      <th bgcolor=\"white\"> "+apgPasswordRequest.getNewUssdPassword()+" </th>\n" + 
						"   </tr>\n" + 
						"</table> ";

					messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, header, subject, msg, datas));
			}

			LOG.info("####### Fin Change Password from APG >>>>>>>");

			SessionUserResponse response = new SessionUserResponse(ErrorResponse.REPONSE_SUCCESS.getCode(), "OK",
					user.getPrenom(), 
					user.getNom(), 
					user.getEmail(), 
					user.getPhone(), 
					user.getIdGroupeUtilisateur(), 
					user.getGroupeUtilisateur().getLibelle(), 
					user.getIdUtilisateur()+"", 
					user.getAdresse(), 
					user.getGenre(), 
					user.getLogin(),
					user.getFirst(),
					user.getPartner().getCountryIsoCode(),
					user.getPartner().getIdPartner(),
					user.getPartner().getCode(),
					user.getMatricule(),
					user.getPartner().getLogo(),
					user.getPartner().getPType(),
					user.getPartner().getPrefixeWallet());
			return Response.ok().entity(response)
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

}
