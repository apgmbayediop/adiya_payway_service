package sn.adiya.user.services;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sn.fig.common.entities.UpdateToChecked;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.StringOperation;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class ModUserByChecker {
	final String TAG = ModUserByChecker.class+"";
	final Logger LOG = Logger.getLogger(ModUserByChecker.class);
	public Response Service(String flashcode,APGCommonRequest apgUtilisateurRequest) throws Exception{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			LOG.info("####### Start >>>>>>> ");
			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), user);

			Long id = apgUtilisateurRequest.getId();
			utilVue.CommonObject(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("id : "), id);

			String profile[] = {BEConstantes.GROUPE_ADMIN_CHECKER_P};
			UpdateToChecked uptc = session.findObjectById(UpdateToChecked.class, id, null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Update"), uptc);
			Utilisateur utilisateur = session.findObjectById(Utilisateur.class, Long.parseLong(uptc.getIdEntity()), null);
			utilVue.commonVerifyProfil(user, TAG, profile);
			uptc.setChecker(user);
			uptc.setDateEnd(new Date());
			JsonNode jo;
			if(Boolean.TRUE.equals(apgUtilisateurRequest.getIsValidated())) {
			try {
				jo = new ObjectMapper().readTree(uptc.getValue());
				
				if(jo.hasNonNull("email") && !jo.get("email").asText().isBlank()) {
					utilisateur.setEmail(jo.get("email").asText());
				}
				if(jo.hasNonNull("adresse") && !jo.get("adresse").asText().isBlank()) {
					utilisateur.setAdresse(jo.get("adresse").asText());
				}
				if(jo.hasNonNull("nom")  && !jo.get("nom").asText().isBlank()) {
					utilisateur.setNom(jo.get("nom").asText());
				}
				if(jo.hasNonNull("prenom") && !jo.get("prenom").asText().isBlank()) {
					utilisateur.setPrenom(jo.get("prenom").asText());
				}
				if(jo.hasNonNull("phone")  && !jo.get("phone").asText().isBlank()) {
					utilisateur.setPhone(jo.get("phone").asText());
				}
				try {
					utilisateur.setIsMasterDealer(jo.get("isMasterDealer").asBoolean());
				} catch (Exception e) {
				}
				
				try {
					utilisateur.setIsActive(jo.get("isActive").asBoolean());
				} catch (Exception e) {
				}
				
				if(utilisateur.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CAISSIER_P)) {
					if(jo.hasNonNull("ussdLogin")  && !jo.get("ussdLogin").asText().isBlank()) {
						utilisateur.setUssdLogin(jo.get("ussdLogin").asText());
						if(jo.get("ussdLogin").asText().length() != 12)
							utilVue.CommonLabel(null,TAG,ErrorResponse.USSD_LOGIN_INCORRECT_OPTIMA.getCode(),ErrorResponse.USSD_LOGIN_INCORRECT_OPTIMA.getMessage("ussdLogin : "), null);
						String pin = StringOperation.getTransactionCode(4);
						String[] param = {"ussdLogin"};
						Object[] dt = {jo.get("ussdLogin").asText()};
						List<Utilisateur> lU = session.executeNamedQueryList(Utilisateur.class, "findUserByUssdLogin", param, dt);
						if(!lU.isEmpty()) 
							utilVue.CommonLabel(null,TAG,ErrorResponse.USSD_DUPLICATA_OPTIMA.getCode(),ErrorResponse.USSD_DUPLICATA_OPTIMA.getMessage("ussdLogin"), null);
						utilisateur.setUssdPassword(utilVue.apgSha(jo.get("ussdLogin").asText()+pin));

					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			uptc.setStatus(true);
			}else {
				uptc.setStatus(false);
			}
			session.updateObject(utilisateur);
			session.updateObject(uptc);

			LOG.info("####### End >>>>>>>");
			return Response.ok().entity(new AbstractResponse("0", "OK"))
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
