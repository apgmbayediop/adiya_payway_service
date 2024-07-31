package sn.adiya.user.services;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
public class ModUserByMaker {
	final String TAG = ModUserByMaker.class+"";
	final Logger LOG = Logger.getLogger(ModUserByMaker.class);
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

			Long idUtilisateur = apgUtilisateurRequest.getId();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("idUtilisateur : "), idUtilisateur+"");

			String profile[] = {BEConstantes.GROUPE_ADMIN_MAKER_P};
			Utilisateur utilisateur = session.findObjectById(Utilisateur.class, idUtilisateur, null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), utilisateur);
			utilVue.commonVerifyProfil(user, TAG, profile);
			ObjectNode jo = new ObjectMapper().createObjectNode();
			if(apgUtilisateurRequest.getAdresse() != null && !apgUtilisateurRequest.getAdresse().isBlank()){
				jo.put("adresse", apgUtilisateurRequest.getAdresse());
			}
			if(apgUtilisateurRequest.getEmail() != null && !apgUtilisateurRequest.getEmail().isBlank()){
				jo.put("email", apgUtilisateurRequest.getEmail());
			}
			if(apgUtilisateurRequest.getNom() != null && !apgUtilisateurRequest.getNom().isBlank()){
				jo.put("nom", apgUtilisateurRequest.getNom());

			}
			if(apgUtilisateurRequest.getPrenom() != null && !apgUtilisateurRequest.getPrenom().isBlank()){
				jo.put("prenom", apgUtilisateurRequest.getPrenom());
			}
			if(apgUtilisateurRequest.getPhone() != null && !apgUtilisateurRequest.getPhone().isBlank()){
				jo.put("phone", apgUtilisateurRequest.getPhone());
			}

			if(Boolean.TRUE.equals(apgUtilisateurRequest.getIsMasterDealer())){
				jo.put("isMasterDealer", apgUtilisateurRequest.getIsMasterDealer());
			}
			if(utilisateur.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CAISSIER_P) && Boolean.TRUE.equals(apgUtilisateurRequest.getHasUssd())) {
				jo.put("hasUssd", apgUtilisateurRequest.getHasUssd());

				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("ussdLogin : "), apgUtilisateurRequest.getUssdLogin());
				if(apgUtilisateurRequest.getUssdLogin().length() != 12)
					utilVue.CommonLabel(null,TAG,ErrorResponse.USSD_LOGIN_INCORRECT_OPTIMA.getCode(),ErrorResponse.USSD_LOGIN_INCORRECT_OPTIMA.getMessage("ussdLogin : "), null);
				String pin = StringOperation.getTransactionCode(4);
				apgUtilisateurRequest.setUssdPassword(pin);
				String[] param = {"ussdLogin"};
				Object[] dt = {apgUtilisateurRequest.getUssdLogin()};
				List<Utilisateur> lU = session.executeNamedQueryList(Utilisateur.class, "findUserByUssdLogin", param, dt);
				if(!lU.isEmpty()) 
					utilVue.CommonLabel(null,TAG,ErrorResponse.USSD_DUPLICATA_OPTIMA.getCode(),ErrorResponse.USSD_DUPLICATA_OPTIMA.getMessage("ussdLogin"), null);
				jo.put("ussdLogin", apgUtilisateurRequest.getUssdLogin());
			}
			if(apgUtilisateurRequest.getIsActive() != null && !apgUtilisateurRequest.getIsActive().isBlank()) {
				jo.put("isActive", apgUtilisateurRequest.getIsActive());
			}

		UpdateToChecked modif = UpdateToChecked.builder()
		.dateStart(new Date())
		.entity("UTILISATEUR")
		.idEntity(String.valueOf(utilisateur.getIdUtilisateur()))
		.maker(user)
		.status(false)
		.value(jo.toString())
		.build();
		
		session.saveObject(modif);
		
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
