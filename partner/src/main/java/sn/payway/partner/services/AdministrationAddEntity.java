package sn.payway.partner.services;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import sn.apiapg.account.entities.CaisseAccountEP;
import sn.apiapg.account.entities.CaisseAccountP;
import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.exception.TransactionException;
import sn.apiapg.common.utils.APGCommonRequest;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.AddPartners;
import sn.apiapg.common.utils.AdministrationJsonResponse1;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.FileOperation;
import sn.apiapg.common.utils.StringOperation;
import sn.apiapg.entities.EntitePlageHoraire;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.PartnerType;
import sn.apiapg.entities.Region;
import sn.apiapg.session.AdministrationSession;
import sn.apiapg.session.AdministrationSessionBean;
import sn.apiapg.session.OperationSession;
import sn.apiapg.session.OperationSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.utils.APGUtilVue;
import sn.payway.common.utils.Constantes;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/
@Stateless
public class AdministrationAddEntity {
	final static String TAG = AdministrationAddEntity.class+"";
	final static Logger LOG = Logger.getLogger(AdministrationAddEntity.class);

	public Response Service(String flashcode,APGCommonRequest apgPartnerRequest) throws Exception {
		try {
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());

			APIUtilVue utilVue = APIUtilVue.getInstance();
			LOG.info("####### Start Add Entity from APG >>>>>>>");
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			String loginContact = apgPartnerRequest.getLoginContact();
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			String partnerId = apgPartnerRequest.getPartnerId();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("partnerId : "), partnerId);
			try{
				Long.parseLong(partnerId);
			}catch(Exception e){
				return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("partnerId")))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}
			Partner PARTNER = session.findObjectById(Partner.class, Long.parseLong(partnerId), null);
			utilVue.CommonObject(null, TAG, ErrorResponse.AUTHENTICATION_ERRORS_1707.getCode(), ErrorResponse.AUTHENTICATION_ERRORS_1707.getMessage("Partner"), PARTNER);
			String parentId = apgPartnerRequest.getParent();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("parent : "), parentId);
			try{
				Long.parseLong(parentId);
			}catch(Exception e){
				return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("parentId")))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}
			if(Boolean.TRUE.equals(apgPartnerRequest.getHasUssd())) {
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("ussdLogin : "), apgPartnerRequest.getUssdLogin());
				if(apgPartnerRequest.getUssdLogin().length() != 12)
					utilVue.CommonLabel(null,TAG,ErrorResponse.USSD_LOGIN_INCORRECT_OPTIMA.getCode(),ErrorResponse.USSD_LOGIN_INCORRECT_OPTIMA.getMessage("ussdLogin : "), null);
				String pin = StringOperation.getTransactionCode(4);
				apgPartnerRequest.setUssdPassword(pin);
				String[] parameters = {"ussdLogin"};
				Object[] data = {apgPartnerRequest.getUssdLogin()};
				List<Utilisateur> lU = session.executeNamedQueryList(Utilisateur.class, "findUserByUssdLogin", parameters, data);
				if(!lU.isEmpty()) 
					utilVue.CommonLabel(null,TAG,ErrorResponse.USSD_DUPLICATA_OPTIMA.getCode(),ErrorResponse.USSD_DUPLICATA_OPTIMA.getMessage("ussdLogin"), null);

			}

			Partner parent = session.findObjectById(Partner.class, Long.parseLong(parentId), null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Parent"), parent);
			String countryIsoCode = apgPartnerRequest.getCountryIsoCode();
			String name = apgPartnerRequest.getName();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("name : "), name);
			String plafondTrx = apgPartnerRequest.getPlafondTrx();
			if(plafondTrx == null) plafondTrx = "0";
			String volumeTrx = apgPartnerRequest.getVolumeTrx();
			if(volumeTrx == null) volumeTrx = "0";
			String partnerTypeId = apgPartnerRequest.getPartnerType();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("partnerTypeId : "), partnerTypeId);
			PartnerType partnerType = administrationSession.findPartnerType(partnerTypeId);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("partnerType"), partnerType);
			if(partnerType.getType().equals(BEConstantes.PARTNER_CAISSE)){
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("plafondTrx : "), plafondTrx);
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("volumeTrx : "), volumeTrx);
			}
			Region reg = null;
			String idRegion = null, adress = null, otp = null, codeAgence = null, email = null;
	//		utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("telephone : "), telephone);
			Partner partner = new Partner();
			if(PARTNER.getName().equals(name) && parent == PARTNER ){
				return Response.ok().entity(new AbstractResponse("1", "Entity "+Constantes.F_DUPLICATE))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}else{
				/*BigDecimal bParent = operationSession.balancePartner(parent);
				LOG.info("### --- --- - Balance Parent "+parent.getName()+" : "+bParent+" "+parent.getCurrencyName());  
				if(bParent.compareTo(new BigDecimal(volumeTrx)) < 0) {
					return Response.ok().entity(new AbstractResponse(ErrorResponse.PARTNER_ERROR_2007.getCode(), ErrorResponse.PARTNER_ERROR_2007.getMessage("balance parent "+parent.getName()+" "+bParent+" "+parent.getCurrencyName())))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
							.build();
				}*/
				partner.setParent(parent);
				if(partnerType.getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR)) {
					String type = apgPartnerRequest.getType();
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("type fils distributeur : "), type);
					if(!type.equals("AG") && !type.equals("SD")) {
						return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("type fils distributeur incorrect : "+type)))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
								.build();
					}
					partner.setFilsDistributeur(type);
					partner.setTelephonePartner(apgPartnerRequest.getTelephonePartner());
					email = apgPartnerRequest.getEmailContact();
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("email : "),email);
					String prenomContact = apgPartnerRequest.getPrenomContact();
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("prenomContact : "),prenomContact);
					String nomContact = apgPartnerRequest.getNomContact();
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("nomContact : "),nomContact);

					partner.setPrincipalAccountNumber("DI"+utilVue.accountPrincipal(new Date()));
				}else if(partnerType.getType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) {
					partner.setTelephonePartner(apgPartnerRequest.getTelephonePartner());
					email = apgPartnerRequest.getEmailContact();
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("email : "),email);
					String prenomContact = apgPartnerRequest.getPrenomContact();
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("prenomContact : "),prenomContact);
					String nomContact = apgPartnerRequest.getNomContact();
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("nomContact : "),nomContact);

					partner.setPrincipalAccountNumber("SD"+utilVue.accountPrincipal(new Date()));
				}else if(partnerType.getType().equals(BEConstantes.PARTNER_AGENCE)){
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("countryIsoCode : "), countryIsoCode);
					adress = apgPartnerRequest.getAdresse();
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("adress : "), adress);
					idRegion = apgPartnerRequest.getRegion();
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("idRegion : "), idRegion);
					codeAgence = apgPartnerRequest.getCodeAgence();
					//utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("codeAgence : "), codeAgence);
					String[] parameters = {"codeAgence"};
					Object[] data = {codeAgence};
					List<Partner> lP = session.executeNamedQueryList(Partner.class, "findPartnerByCodeAgence", parameters, data);
					if(lP.size()>0) {
						return Response.ok().entity(new AbstractResponse("1", "Agence "+Constantes.F_DUPLICATE))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
								.build();
					}
					try {
						Long.parseLong(idRegion);
					} catch (NumberFormatException e) { 
						return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("idRegion : ")))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
								.build();
					}
					reg = session.findObjectById(Region.class, Long.parseLong(idRegion), "");
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("idRegion"), reg);

					partner.setIdRegion(reg.getIdRegion());
					partner.setRegionCode(reg.getRegionCode());
					partner.setRegionName(reg.getRegionName());

					partner.setCodeAgence(codeAgence);
					if(countryIsoCode != null && !countryIsoCode.equals(""))
						partner.setCountryIsoCode(countryIsoCode);
					else
						partner.setCountryIsoCode(PARTNER.getCountryIsoCode());

					partner.setPrincipalAccountNumber("AG"+utilVue.accountPrincipal(new Date()));
				}else if(partnerType.getType().equals(BEConstantes.PARTNER_CAISSE)) {
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("plafondTrx : "), plafondTrx);
					partner.setPlafondTrx(new BigDecimal(plafondTrx));
					partner.setCountryIsoCode(parent.getCountryIsoCode());
					partner.setTelephoneContact(parent.getTelephoneContact());
					partner.setIdRegion(parent.getIdRegion());
					partner.setRegionCode(parent.getRegionCode());
					partner.setRegionName(parent.getRegionName());

					if(PARTNER.getEspace().equals(BEConstantes.ESPACE_PROVIDER)) {
						if(partner.getParent().getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) { 
							partner.setPrincipalAccountNumber(BEConstantes.PREFIXE_CAISSE_SOUS_DISTRIBUTEUR+utilVue.accountPrincipal(new Date()));	
							apgPartnerRequest.setName(apgPartnerRequest.getName()+" | "+parent.getName());
						}else if(partner.getParent().getParent().getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR)) { 
							partner.setPrincipalAccountNumber(BEConstantes.PREFIXE_CAISSE_AGENCE_D+utilVue.accountPrincipal(new Date()));						
						}else if(partner.getParent().getParent().getPType().equals(BEConstantes.PARTNER_PROVIDER)) { 
							partner.setPrincipalAccountNumber(BEConstantes.PREFIXE_CAISSE_AGENCE_P+utilVue.accountPrincipal(new Date()));						
						}
					}
				}
				if(volumeTrx != null)
					partner.setVolumeTrx(new BigDecimal(volumeTrx));
				if(USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_P) || USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_SD) || USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPERVISEUR_SD)) {
					otp = apgPartnerRequest.getOtp();
					LOG.info("### --- --- OTP SUPER ADMIN: "+otp);
					if(USER.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) || USER.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER_DIASPORA) || USER.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER)) {
						if(!Boolean.TRUE.equals(utilVue.checkOtp(flashcode, otp))) {
							utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("otp : "), null);
						}
					}else {
						utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("otp : "), otp);
						String[] parameters = {"flashCode","otp"};
						Object[] data = {flashcode,otp};
						Utilisateur user = session.executeNamedQuerySingle(Utilisateur.class,"findByOTP", parameters, data);
						utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), user);
					}
					partner.setIsValidated(true);
				}else if(USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_DA)) {
					partner.setIsValidated(true);
				}else
					partner.setIsValidated(false);
				String secretKey = StringOperation.getSecret();
				partner = AddPartners.defaultEntity(parent, partner, apgPartnerRequest, secretKey, partnerType.getType());
				partner.setCanal(PARTNER.getCanal());
				partner.setCode(PARTNER.getCode());
				partner.setPartnerType(partnerType);
				partner.setPType(partnerType.getType());
				partner.setEspace(PARTNER.getEspace());
				LOG.info("PARTNER.getName() >>>>>> "+PARTNER.getName()+" >>>>>>>>> "+PARTNER.getIdPartner());
				LOG.info("PARTNER.getCountryIndicatif() >>>>>> "+PARTNER.getCountryIndicatif());
				partner.setCountryIndicatif(PARTNER.getCountryIndicatif());
				partner.setCountryIsoCode(PARTNER.getCountryIsoCode());
				partner.setCountryName(PARTNER.getCountryName());
				boolean isCreated = FileOperation.createDirectory("partners",partner.getName().trim().replace(" ", "_").toLowerCase());
				if(isCreated)
					LOG.info("FOLDER PARTNER >>>>>>>>>>>>> "+partner.getName()+" CREATED");

				if(BEConstantes.PARTNER_DISTRIBUTEUR.equals(partnerType.getType()) || BEConstantes.PARTNER_SOUS_DISTRIBUTEUR.equals(partnerType.getType()) || (BEConstantes.PARTNER_CAISSE.equals(partnerType.getType()) && operationSession.partnerUser(partner).getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER))) {
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("loginContact : "), loginContact);
					String param[] = {"login"};
					String [] Data = {loginContact};
					Utilisateur u  = session.executeNamedQuerySingle(Utilisateur.class,"findUserByLogin", param, Data);
					if(u != null) {
						return Response.ok().entity(new AbstractResponse("1", "Utilisateur (login "+loginContact+") "+Constantes.F_DUPLICATE))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
								.build();
					}
				}
				partner = (Partner) session.saveObject(partner);
		
				if(partner.getPType().equals(BEConstantes.PARTNER_CAISSE) || partner.getPType().equals(BEConstantes.PARTNER_AGENCE) || partner.getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) {
					if(partner.getParent() != null && partner.getParent().getParent() != null) {
						EntitePlageHoraire ePH = new EntitePlageHoraire();
						ePH.setDate(new Date());
						ePH.setAuthorizedConnexion(Boolean.FALSE);
						ePH.setEntite(partner);
						ePH.setFermeture(BEConstantes.DEFAULT_H_FERMETURE);
						ePH.setOuverture(BEConstantes.DEFAULT_H_OUVERTURE);
						for(Long i=1L; i<=7L; i++) {
							ePH.setDays(i);
							session.saveObject(ePH);
						}
					}
					defaultAccountCaisse(session, USER, partner, volumeTrx);
				}
				AddPartners.switchEntityType(partner);

				if(USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_MAKER_P) || USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_MAKER_EP))
					APGUtilVue.getInstance().addSuperAdminPartner(partner, loginContact, email, partner.getPrenomContact(), partner.getNomContact(),false,apgPartnerRequest);
				else
					APGUtilVue.getInstance().addSuperAdminPartner(partner, loginContact, email, partner.getPrenomContact(), partner.getNomContact(),true,apgPartnerRequest);

			}
			LOG.info("####### End Add Entity from APG >>>>>>>");
			return Response.ok().entity(AdministrationJsonResponse1.addEntityResponse(partner).toString())
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

	private static void defaultAccountCaisse(Session session, Utilisateur utilisateur, Partner partner, String volumeTrx) {
		if(partner.getEspace().equals(BEConstantes.ESPACE_ENVOYEUR_PAYEUR)) {
			CaisseAccountEP account = new CaisseAccountEP();
			account.setAccountNumber(partner.getPrincipalAccountNumber());
			account.setPrincipal(BigDecimal.ZERO);
			account.setAmount(BigDecimal.ZERO);  
			account.setDate(new Date());
			account.setDescription("initial volumeTrx");
			account.setOperationPartner(BEConstantes.OPERATION_CAISSE);
			account.setPartnerCode(partner.getCode());
			account.setIdUtilisateur(utilisateur.getIdUtilisateur()+"");
			account.setIsCredit(true);
			account.setCurrencyName(partner.getCurrencyName());

			session.saveObject(account);
		}else if(partner.getEspace().equals(BEConstantes.ESPACE_PROVIDER)) {
			CaisseAccountP account = new CaisseAccountP();
			account.setAccountNumber(partner.getPrincipalAccountNumber());
			account.setPrincipal(BigDecimal.ZERO);
			account.setAmount(BigDecimal.ZERO);  
			account.setDate(new Date());
			account.setDescription("initial volumeTrx");
			account.setOperationPartner(BEConstantes.OPERATION_CAISSE);
			account.setPartnerCode(partner.getCode());
			account.setIdUtilisateur(utilisateur.getIdUtilisateur()+"");
			account.setIsCredit(true);
			account.setCurrencyName(partner.getCurrencyName());

			session.saveObject(account);
		}
	}

}
