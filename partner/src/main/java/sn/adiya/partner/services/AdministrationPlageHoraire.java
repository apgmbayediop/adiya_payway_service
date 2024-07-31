package sn.adiya.partner.services;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APGListPlageHoraireResponse;
import sn.fig.common.utils.APGPlageHoraire;
import sn.fig.common.utils.APGPlageHoraireRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.EntitePlageHoraire;
import sn.fig.entities.Partner;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;

@Stateless
@JBossLog
public class AdministrationPlageHoraire {
	static final String TAG = AdministrationPlageHoraire.class+"";
	public Response Service(String flashcode, APGPlageHoraireRequest apgPlageHoraireRequest) throws UnsupportedEncodingException, TransactionException{
		try {
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			APIUtilVue utilVue = APIUtilVue.getInstance();
			log.info("=============---- NEW PLAGE HORAIRE ----=============");
			log.info("=============---- NEW PLAGE HORAIRE ----=============");
			log.info("=============---- NEW PLAGE HORAIRE ----=============");
			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			String entityId = apgPlageHoraireRequest.getEntityId();
			utilVue.CommonLabel(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1802.getCode(), ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("entityId"), entityId);
			try {
				Long.parseLong(entityId);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("entityId")))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
						.build();	
			}
			Partner entite = (Partner) session.findObjectById(Partner.class, Long.parseLong(entityId), null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1802.getCode(), ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("entite"), entite);
			log.info("------- EntiteName : "+entite.getName()+" User : "+user.getNom()+" Entity : "+entityId);
			String action = apgPlageHoraireRequest.getAction();
			utilVue.CommonLabel(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1802.getCode(), ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("action"), action);
			String[] parameters = {"idPartner"};
			Object[] data = {Long.parseLong(entityId)}; 
			if(action != null && action.equals("VIEW")) {
				log.info("####### Start view PlageHoraire >>>>>>>");
				List<APGPlageHoraire> lAPGPlageHoraires = new ArrayList<APGPlageHoraire>();
				List<EntitePlageHoraire> lEntitePlageHoraires = session.executeNamedQueryList(EntitePlageHoraire.class, "findPlageHoraire", parameters, data);
				for(EntitePlageHoraire eph:lEntitePlageHoraires) {  
					lAPGPlageHoraires.add(new APGPlageHoraire(eph.getOuverture()+"", eph.getFermeture()+"", utilVue.getDayByNombre(eph.getDays()+""),eph.getDays()+"", eph.getEntite().getName(), eph.getAuthorizedConnexion() ? true:false));
				}
				log.info("####### End view PlageHoraire >>>>>>>");
				return Response.ok().entity(new APGListPlageHoraireResponse(ErrorResponse.REPONSE_SUCCESS.getCode(), ErrorResponse.REPONSE_SUCCESS.getMessage(""),lAPGPlageHoraires))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header("Access-Control-Allow-Credentials", "true")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.header(BEConstantes.AccessControlAllowHeaders, "Origin, Content-Type, Accept, Authorization, X-Requested-With")
						.build();
			}else if(action != null && action.equals("UPDATE")) {
				log.info("####### Start Edit PlageHoraire >>>>>>>");

				log.info("####### START PlageHoraire >>>>>>>");
				log.info("####### DEBT "+apgPlageHoraireRequest.toString()+" FIN");
				log.info("####### END PlageHoraire >>>>>>>");

				List<APGPlageHoraire> lAPGPlageHoraires = new ArrayList<APGPlageHoraire>();
				List<APGPlageHoraire> lPlageHoraires = apgPlageHoraireRequest.getlPlageHoraires();
				for(APGPlageHoraire plageHoraire:lPlageHoraires){
					if(plageHoraire == null){
						return Response.ok().entity(new AbstractResponse(ErrorResponse.AUTHENTICATION_ERRORS_1702.getCode(), ErrorResponse.AUTHENTICATION_ERRORS_1702.getMessage("channelType")))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
								.build();
					}
					String codeDays = plageHoraire.getCodeDays();
					Long idCodeDays = null;
					try {
						idCodeDays = Long.parseLong(codeDays);
					} catch (NumberFormatException e) {
						e.printStackTrace();
						return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1801.getCode(), ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("idCodeDays")))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
								.build();
					}
					String[] param = {"idPartner","days"};
					Object[] dat = {Long.parseLong(entityId),idCodeDays}; 
					EntitePlageHoraire entitePlageHoraire = session.executeNamedQuerySingle(EntitePlageHoraire.class, "findEPHById", param, dat);
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1802.getCode(), ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("entitePlageHoraire"), entitePlageHoraire);
					String[] tabDateStart = plageHoraire.getOuverture().replace(" ", ":").split(":");
					entitePlageHoraire.setOuverture(tabDateStart[1]+":"+tabDateStart[2]);     
					String[] tabDateEnd = plageHoraire.getFermeture().replace(" ", ":").split(":");
					entitePlageHoraire.setFermeture(tabDateEnd[1]+":"+tabDateEnd[2]);
					entitePlageHoraire.setAuthorizedConnexion((plageHoraire.getAuthorizedConnexion() == null) ? Boolean.FALSE : plageHoraire.getAuthorizedConnexion());
					
					session.updateObject(entitePlageHoraire);
				}			
				List<EntitePlageHoraire> lEntitePlageHoraires = session.executeNamedQueryList(EntitePlageHoraire.class, "findPlageHoraire", parameters, data);
				for(EntitePlageHoraire eph:lEntitePlageHoraires) {  
					lAPGPlageHoraires.add(new APGPlageHoraire(eph.getOuverture()+"", eph.getFermeture()+"", utilVue.getDayByNombre(eph.getDays()+""),eph.getDays()+"", eph.getEntite().getName(), eph.getAuthorizedConnexion() ? true:false));
				}
				log.info("####### End Edit PlageHoraire  >>>>>>>");
				return Response.ok().entity(new APGListPlageHoraireResponse(ErrorResponse.REPONSE_SUCCESS.getCode(), ErrorResponse.REPONSE_SUCCESS.getMessage(""),lAPGPlageHoraires))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header("Access-Control-Allow-Credentials", "true")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.header(BEConstantes.AccessControlAllowHeaders, "Origin, Content-Type, Accept, Authorization, X-Requested-With")
						.build();
			}else {
				log.info("####### End PlageHoraire  >>>>>>> Action "+action);
				return Response.ok().entity(new AbstractResponse(ErrorResponse.REPONSE_UNSUCCESS.getCode(), ErrorResponse.REPONSE_UNSUCCESS.getMessage("")))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header("Access-Control-Allow-Credentials", "true")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.header(BEConstantes.AccessControlAllowHeaders, "Origin, Content-Type, Accept, Authorization, X-Requested-With")
						.build();
			}
		}catch(TransactionException e) {
			return Response.ok().entity(new AbstractResponse(e.getCode(), e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header("Access-Control-Allow-Credentials", "true")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, "Origin, Content-Type, Accept, Authorization, X-Requested-With")
					.build();
		}
	}

}
