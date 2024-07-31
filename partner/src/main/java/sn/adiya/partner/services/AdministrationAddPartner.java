package sn.adiya.partner.services;

import java.math.BigDecimal;
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
import sn.fig.common.utils.AccountManagerBeanNEW;
import sn.fig.common.utils.AccountManagerNEW;
import sn.fig.common.utils.AddPartners;
import sn.fig.common.utils.AdministrationJsonResponse1;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.FileOperation;
import sn.fig.common.utils.StringOperation;
import sn.fig.entities.Currency;
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerType;
import sn.fig.session.AdministrationSession;
import sn.fig.session.AdministrationSessionBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.APGUtilVue;
import sn.adiya.common.utils.Constantes;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * Moussa SENE
 * Cherif DIOUF
 * @version 1.0 
 **/

@Stateless
public class AdministrationAddPartner {
	final static String TAG = AdministrationAddPartner.class+"";
	final static Logger LOG = Logger.getLogger(AdministrationAddPartner.class);

	public Response Service(String flashcode,APGCommonRequest apgPartnerRequest) throws Exception{
		try {
			AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());
			
			List<Utilisateur> lLoginAgents = administrationSession.findByLoginAgent(apgPartnerRequest.getLoginContact());
			if(!lLoginAgents.isEmpty()){
				return Response.ok().entity(new AbstractResponse("1", "Utilisateur (login "+apgPartnerRequest.getLoginContact()+") "+Constantes.F_DUPLICATE))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			AccountManagerNEW accountManagerNEW = (AccountManagerNEW) BeanLocator.lookUp(AccountManagerBeanNEW.class.getSimpleName());

			LOG.info("#### --- --- - Debut Add Partner from APG - --- --- ###");
			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), user);
			if(user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_EP) || user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_P)){
				String otp = apgPartnerRequest.getOtp();
				LOG.info("### --- --- OTP SUPER ADMIN : "+otp);
				LOG.info("### --- --- DEBUG flashcode  : "+flashcode);
				LOG.info("### --- --- DEBUG partnerId  : "+user.getPartner().getIdPartner());
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
			String withOperationCode = apgPartnerRequest.getWithOperationCode();
			String pName = apgPartnerRequest.getName();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("name : "),pName);
			String emailContact = apgPartnerRequest.getEmailContact();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("emailContact : "),emailContact);
			String currencyName = apgPartnerRequest.getCurrencyName();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("currencyName : "),currencyName);
			String countryIsoCode = apgPartnerRequest.getCountryIsoCode();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("countryIsoCode : "),countryIsoCode);
			String prenomContact = apgPartnerRequest.getPrenomContact();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("prenomContact : "),prenomContact);
			String nomContact = apgPartnerRequest.getNomContact();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("nomContact : "),nomContact);
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("tel1 : "),apgPartnerRequest.getTelephoneContact());
			String canal = apgPartnerRequest.getCanal();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("canal : "),canal);
			String partnerCode = apgPartnerRequest.getPartnerCode();
			/*
			 * CONTROL DOUBLON CODE
			 */
			if(partnerCode != null) {
				String[] params = {"code"};
				Object[] dats = {partnerCode};
				Partner partner = session.executeNamedQuerySingle(Partner.class,"findPartnerByCode", params, dats);
				if(partner != null)
					utilVue.CommonObject(null, TAG, "1", "Partner "+Constantes.F_DUPLICATE+" partnerCode", null);
			}
			/*
			 * CONTROL DOUBLON PREFIXE
			 */
			if(apgPartnerRequest.getPrefixeWallet() != null) {
				String[] params = {"prefixeWallet"};
				Object[] dats = {apgPartnerRequest.getPrefixeWallet()};
				Partner partner = session.executeNamedQuerySingle(Partner.class,"findPartnerByPrefix", params, dats);
				if(partner != null)
					utilVue.CommonObject(null, TAG, "1", "Partner "+Constantes.F_DUPLICATE+" prefixeWallet", null);
			}

			/*
			 * CONTROL DOUBLON PARTNER NAME
			 */
			String[] params = {"name"};
			Object[] dats = {pName};
			Partner partner = session.executeNamedQuerySingle(Partner.class,"findPartnerByName", params, dats);
			if(partner != null)
				utilVue.CommonObject(null, TAG, "1", "Partner "+Constantes.F_DUPLICATE, null);

			partner = new Partner();

			String secretKey = StringOperation.getSecret();
			String partnerTypeId = apgPartnerRequest.getPartnerType();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("partnerType"),partnerTypeId);
			try {
				Long.parseLong(partnerTypeId);
			} catch (NumberFormatException e) {
				utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1801.getCode(), ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("Partner Type"), null);
			}
			PartnerType partnerType = session.findObjectById(PartnerType.class, Long.parseLong(partnerTypeId), null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Partner Type"), partnerType);
			partner.setPartnerType(partnerType);
			partner.setSecretKeyWallet(utilVue.generateTerminal());
			partner = AddPartners.defaultPartner(partner, apgPartnerRequest, canal, secretKey, partnerType.getType());
			partner.setIsActive(true);

			if(withOperationCode != null && withOperationCode.equals("")) {
				if(withOperationCode.equals("1")  || withOperationCode.equals("true"))
					partner.setWithOperationCode(true);
			}else
				partner.setWithOperationCode(false);

			partner.setIsB2B(false);
			if(BEConstantes.PARTNER_SENDER.equals(partnerType.getType()) && Boolean.TRUE.equals(apgPartnerRequest.getIsB2B())){
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("ninea"),apgPartnerRequest.getNinea());
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("adresse"),apgPartnerRequest.getAdresse());
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("description"),apgPartnerRequest.getDescription());
				partner.setNinea(apgPartnerRequest.getNinea());
				partner.setAdresse(apgPartnerRequest.getAdresse());
				partner.setDescription(apgPartnerRequest.getDescription());
				partner.setIsB2B(true);
			}
			if(partner.getAdresse() != null || partner.getAdresse() != ""){
				partner.setAdresse(apgPartnerRequest.getAdresse());
			}
			Long parentId = null;
			Partner parent = null;
			if(apgPartnerRequest.getParent() != null) {
				try {
					parentId = Long.parseLong(apgPartnerRequest.getParent());
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("parentId"), parentId);
				} catch (NumberFormatException e) {
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1801.getCode(), ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("parentId"), null);
				}
			}
			if(parentId != null) {
				parent = session.findObjectById(Partner.class, parentId, null);
				utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Partner Parent"), parent);
				AddPartners.commonValidateParentFils(parent, partner, TAG);
			}else {
				String[] parameters = {"code"};
				Object[] data = {BEConstantes.CODE_APG};
				parent = session.executeNamedQuerySingle(Partner.class, "findPartnerByCode", parameters, data);
				utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Partner Parent"), parent);
			} 
			if(!parent.getPType().equals(BEConstantes.PARTNER_SUPER_AGREGATOR) && !parent.getPType().equals(BEConstantes.PARTNER_AGREGATOR) && !parent.getPType().equals(BEConstantes.PARTNER_PROVIDER) && !parent.getPType().equals(BEConstantes.PARTNER_MONETIC)) {
				utilVue.CommonLabel(null,TAG,ErrorResponse.PARTNER_ERROR_2001.getCode(),ErrorResponse.PARTNER_ERROR_2001.getMessage("Parent : "+parent.getName()+" type : "+parent.getPType()),null);
			}
			partner.setCode(apgPartnerRequest.getPartnerCode());
			boolean isCreated = FileOperation.createDirectory("partners",partner.getName().trim().replace(" ", "_").toLowerCase());
			if(isCreated)
				LOG.info("FOLDER PARTNER >>>>>>>>>>>>> "+partner.getName()+" CREATED");

			if(BEConstantes.PARTNER_ACCEPTEUR.equals(partnerType.getType())){
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("Mcc"),apgPartnerRequest.getMcc());
				partner.setMcc(apgPartnerRequest.getMcc());
				partner.setModeReglement(apgPartnerRequest.getModeReglement());
				partner.setNumeroCompte(apgPartnerRequest.getNumeroCompte());
				partner.setEspace(BEConstantes.ESPACE_ACCEPTEUR);
				partner.setSenderSms(parent.getSenderSms());
				partner.setMaxBalance(BigDecimal.valueOf(200000));
				if(apgPartnerRequest.getMcc().equals(BEConstantes.MCC_SCHOOL))
					partner.setSenderSms("SCHOOLM");
			}
			if(BEConstantes.PARTNER_EMETTEUR.equals(partnerType.getType())) {
				partner.setEspace(BEConstantes.ESPACE_EMETTEUR);
				if(Boolean.TRUE.equals(apgPartnerRequest.getManageBalance())) {
					partner.setAdditionalParameters("{\"manageBalance\":true}");
				}
			}
			if(BEConstantes.PARTNER_ACCEPTEUR.equals(partnerType.getType()) || BEConstantes.PARTNER_EMETTEUR.equals(partnerType.getType())) {
				LOG.info("#### PARENT "+parent.getName()+" #### partnerType "+parent.getPType());
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("parent"),apgPartnerRequest.getParent());
				try {
					Long.parseLong(apgPartnerRequest.getParent());
				}catch(Exception e) {
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1801.getCode(),ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("Parent"),null);
				}
				if(!BEConstantes.PARTNER_MONETIC.equals(parent.getPType()) && !BEConstantes.PARTNER_PROVIDER.equals(parent.getPType()) && !BEConstantes.PARTNER_AGREGATOR.equals(parent.getPType()))
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("Parent must be "+BEConstantes.PARTNER_MONETIC),null);
			}

			partner.setParent(parent);
			
			String[] parameters = {"currencyName"};
			Object[] data = {currencyName};
			Currency currency =  session.executeNamedQuerySingle(Currency.class,"Currency.findByName", parameters, data);
			utilVue.CommonObject(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("currencyName"), currency);
			// INITIALISATION COMPTE PROVIDER AVEC SOLDE D'OUVERTURE
			if(BEConstantes.PARTNER_PROVIDER.equals(partner.getPartnerType().getType())) {
				try {
					LOG.info("plafond >>>>>>>> "+apgPartnerRequest.getBalance());
					partner.setOpeningBalance(new BigDecimal(apgPartnerRequest.getBalance()+""));
				} catch (Exception e) {
					partner.setOpeningBalance(BigDecimal.ZERO);
				}
			}
			partner = (Partner) session.saveObject(partner);
			if(partner.getCode() == null){
				partner.setCode(partner.getIdPartner()+"");
			}
			session.updateObject(partner);


			if(BEConstantes.PARTNER_AGREGATOR.equals(partnerType.getType()) || BEConstantes.PARTNER_SENDER_PAYER.equals(partnerType.getType()) 
					|| BEConstantes.PARTNER_ACCEPTEUR.equals(partnerType.getType()) || BEConstantes.PARTNER_PROVIDER.equals(partnerType.getType()) 
					|| BEConstantes.PARTNER_SENDER.equals(partnerType.getType()) || BEConstantes.PARTNER_MONETIC.equals(partnerType.getType())
					|| BEConstantes.PARTNER_PAYER.equals(partnerType.getType()) || BEConstantes.PARTNER_EMETTEUR.equals(partnerType.getType()) ) {
				String loginContact = apgPartnerRequest.getLoginContact();
				if(!BEConstantes.PARTNER_PAYER.equals(partnerType.getType()))
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("loginContact"), loginContact);
				APGUtilVue.getInstance().addSuperAdminPartner(partner, loginContact, emailContact, prenomContact, nomContact,true,apgPartnerRequest);
			}

			
			accountManagerNEW.initAccount(partner, currency);


			// CREATION COMPTE
			AddPartners.switchPartnerType(partner);

			//NOTIFICATION
			if(!BEConstantes.PARTNER_ACCEPTEUR.equals(partnerType.getType()) && !BEConstantes.PARTNER_MONETIC.equals(partnerType.getType()) && !BEConstantes.PARTNER_EMETTEUR.equals(partnerType.getType()))
				AddPartners.defaultNotify(partner, canal, secretKey);

			LOG.info("#### --- --- - Fin Add Partner from APG - --- --- ###");
			return Response.ok().entity(AdministrationJsonResponse1.addPartnerResponse(partner).toString()) 
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
