package sn.payway.user.services;

import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sn.apiapg.common.entities.UpdateToChecked;
import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.exception.TransactionException;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.session.OperationSession;
import sn.apiapg.session.OperationSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class ListPendingUpdateUser {
	final String TAG = ListPendingUpdateUser.class+"";
	final Logger LOG = Logger.getLogger(ListPendingUpdateUser.class);
	public Response Service(String flashcode) throws Exception{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());

			LOG.info("####### Start >>>>>>> ");
			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), user);

			String profile[] = {BEConstantes.GROUPE_ADMIN_CHECKER_P};
			utilVue.commonVerifyProfil(user, TAG, profile);
			List<Utilisateur> users = session.executeNamedQueryList(Utilisateur.class, "findUtoRemove", new String[] {"idPartner"}, new Object[] {user.getPartner().getIdPartner()});
			List<Long> lIDUser =  users.stream().map(Utilisateur::getIdUtilisateur).collect(Collectors.toList());
			List<String> lID = lIDUser.stream().map(id -> id.toString()).collect(Collectors.toList());
			//		List<UpdateToChecked> pendingList = session.executeNamedQueryList(UpdateToChecked.class, "findByEntityAndIdEntityIn", new String[] {"entity","listIdEntity","status"}, new Object[] {"UTILISATEUR",lIDUser,false});
			List<UpdateToChecked> pendingList = operationSession.pendingObjectList("UTILISATEUR",lID,false);
			ObjectNode jo = new ObjectMapper().createObjectNode();
			jo.put("code", "0000");
			jo.put("message","OK");  
			jo.putPOJO("data",pendingList);
			LOG.info("####### End >>>>>>>");
			return Response.ok().entity(jo.toString())
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
