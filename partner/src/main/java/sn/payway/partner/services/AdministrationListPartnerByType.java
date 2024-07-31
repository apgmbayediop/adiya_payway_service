package sn.payway.partner.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.json.JSONObject;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.exception.TransactionException;
import sn.apiapg.common.utils.APGCommonRequest;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.FileOperation;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.PartnerType;
import sn.apiapg.session.OperationSession;
import sn.apiapg.session.OperationSessionBean;
import sn.apiapg.session.ServiceSession;
import sn.apiapg.session.ServiceSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationListPartnerByType {

	static final String TAG = AdministrationListPartnerByType.class+"";
	static Logger LOG = Logger.getLogger(AdministrationListPartnerByType.class);

	public Response Service(String flashcode, String auth, APGCommonRequest apgCommonRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
			ServiceSession serviceSession = (ServiceSession) BeanLocator.lookUp(ServiceSessionBean.class.getSimpleName());
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			LOG.info("####### Start List Partner by Type from APG >>>>>>>");
			APGPartnerResponse response; 
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("auth : "), auth);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			String partnerType = apgCommonRequest.getPartnerType();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("partnerType : "), partnerType);
			Partner part = operationSession.findHmacKey(utilVue.apgCrypt(auth));
			utilVue.CommonObject(null, TAG, ErrorResponse.AUTHENTICATION_ERRORS_1707.getCode(), ErrorResponse.AUTHENTICATION_ERRORS_1707.getMessage("Partner"), part);
			Long idParent = apgCommonRequest.getIdParent();
			List<Partner> lPartner = new ArrayList<>();
			if(USER.getPartner().getPType().equals(BEConstantes.PARTNER_SUPER_AGREGATOR) ||
					USER.getPartner().getPType().equals(BEConstantes.PARTNER_AGREGATOR) ||
					USER.getPartner().getPType().equals(BEConstantes.PARTNER_SENDER_PAYER) || 
					USER.getPartner().getPType().equals(BEConstantes.PARTNER_PROVIDER) || 
					USER.getPartner().getPType().equals(BEConstantes.PARTNER_MONETIC) || 
					USER.getPartner().getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR) || 
					USER.getPartner().getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR) || 
					USER.getPartner().getPType().equals(BEConstantes.PARTNER_CAISSE) || 
					USER.getPartner().getPType().equals(BEConstantes.PARTNER_AGENCE)) {
				String[] parameters = {"type", "idPartner"};
				Object[] data = {partnerType, part.getIdPartner()};
				LOG.info("### --- --- - C'est un "+part.getPType()+" Nom : "+part.getName()+" id : "+part.getIdPartner());
				LOG.info("### --- --- - ID partner : "+part.getIdPartner());
				LOG.info("### --- --- - TYPE partner : "+partnerType);
				LOG.info("### --- --- - idParent : "+idParent);

				/*if(partnerType.equals(BEConstantes.PARTNER_CAISSE) || partnerType.equals(BEConstantes.PARTNER_DISTRIBUTEUR) 
						|| partnerType.equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR) || partnerType.equals(BEConstantes.PARTNER_AGENCE))
					lPartner = (List<Partner>) session.executeNamedQueryList(Partner.class,"findEPandPfilsByType", parameters, data);*/

				if(partnerType.equals(BEConstantes.PARTNER_CAISSE) || partnerType.equals(BEConstantes.PARTNER_DISTRIBUTEUR) 
						|| partnerType.equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR) || partnerType.equals(BEConstantes.PARTNER_AGENCE)) {
					LOG.info("### --- --- - BLOC 1");
					lPartner = session.executeNamedQueryList(Partner.class,"findEPandPfilsByType", parameters, data);
				}
				else if(partnerType.equals(BEConstantes.PARTNER_ACCEPTEUR) && idParent != null) {
					LOG.info("### --- --- - BLOC 2");
					String[] param = {"type","idPartner"};
					Object[] dt = {partnerType,idParent};
					lPartner = session.executeNamedQueryList(Partner.class,"findEPandPfilsByType", param, dt);
				}else {
					LOG.info("### --- --- - BLOC 3");
					if(Boolean.TRUE.equals(apgCommonRequest.getIsB2B())) {
						LOG.info("### --- --- - BLOC 3 >>>>>>>> 1");
						String[] param = {"type"};
						Object[] dt = {partnerType};
						lPartner = session.executeNamedQueryList(Partner.class,"findAllPartnerBusiness", param, dt);
					}else {
						LOG.info("### --- --- - BLOC 3 >>>>>>>> 2");
						Boolean isRelianceSender = Boolean.FALSE;
						Long idPartner = part.getIdPartner();
						LOG.info("### --- --- - BLOC 3 >>>>>>>> 2 : "+ idPartner);
						if((partnerType.equals(BEConstantes.PARTNER_SENDER) && part.getPType().equals(BEConstantes.PARTNER_CAISSE)) ||
								((partnerType.equals(BEConstantes.PARTNER_SENDER) || partnerType.equals(BEConstantes.PARTNER_PROVIDER)) && part.getPType().equals(BEConstantes.PARTNER_PROVIDER))) {
							Partner prov = operationSession.partnerUser(part);
							idPartner = prov.getIdPartner();
							if(prov.getParent().getCode().equals(BEConstantes.CODE_RELIANCE_AGREGATEUR)) {//60782
								LOG.info("### --- --- - BLOC 3 >>>>>>>> 4");
								String[] param = {"type", "idPartner"};
								Object[] dt = {partnerType, prov.getParent().getIdPartner()};
								lPartner = session.executeNamedQueryList(Partner.class,"findAllPartnerNotBusiness", param, dt);
								if(partnerType.equals(BEConstantes.PARTNER_SENDER)) {
									if(part.getParent().getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) {
										lPartner = lPartner.stream().filter(p -> 
										p.getAdditionalParameters() != null && p.getAdditionalParameters().contains("\"SD_PAY_TRANS\":true") && p.getAdditionalParameters().contains("\"VIEW\":true")
												).collect(Collectors.toList());
									}else if(part.getParent().getPType().equals(BEConstantes.PARTNER_AGENCE)){
										lPartner = lPartner.stream().filter(p -> 
										p.getAdditionalParameters() != null && p.getAdditionalParameters().contains("\"VIEW\":true")
												).collect(Collectors.toList());
									}
								}
								isRelianceSender = Boolean.TRUE;
							}
						}
						/*		if((partnerType.equals(BEConstantes.PARTNER_SENDER) || partnerType.equals(BEConstantes.PARTNER_PROVIDER)) && part.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
							part = operationSession.partnerUser(part);
							idPartner = part.getIdPartner();
							LOG.info("### --- --- - BLOC 3 >>>>>>>> 2 part ID : "+ part.getIdPartner());
							LOG.info("### --- --- - BLOC 3 >>>>>>>> 2 part NOM : "+ part.getName());
							LOG.info("### --- --- - BLOC 3 >>>>>>>> 2 idPartner : "+ idPartner);
							if(part.getParent().getCode().equals("64786")  || part.getParent().getCode().equals("60782")) {//60782
								LOG.info("### --- --- - BLOC 3 >>>>>>>> 4");
								String[] param = {"type", "idPartner"};
								Object[] dt = {partnerType, part.getParent().getIdPartner()};
								lPartner = session.executeNamedQueryList(Partner.class,"findAllPartnerNotBusiness", param, dt);
								isRelianceSender = Boolean.TRUE;
							}
						} */
						if(!isRelianceSender) {
							LOG.info("### --- --- - BLOC 4");
							String[] param = {"type", "idPartner"};
							Object[] dt = {partnerType, idPartner};
							lPartner = session.executeNamedQueryList(Partner.class,"findAllPartnerNotBusiness", param, dt);
						}
					}
				}
				LOG.info("### --- --- - NB partners fils : "+lPartner.size());
			}else {
				LOG.info("### --- --- - BLOC 5");
				LOG.info("### --- --- - EMPTY partners ");
			}
			List<APGPartner> lpts = new ArrayList<>();
			for(Partner partner : lPartner){
				if(Boolean.TRUE.equals(partner.getIsDefaultSender()))
					continue;
				boolean isCreated = FileOperation.createDirectory("partners",partner.getName().trim().replace(" ", "_").toLowerCase());
				if(isCreated)
					LOG.info("FOLDER PARTNER >>>>>>>>>>>>> "+partner.getName()+" CREATED");
				APGPartner pt = new APGPartner();
				pt.setType(partner.getFilsDistributeur());
				pt.setId(partner.getIdPartner());
				pt.setConsumerId(partner.getConsumerId());
				pt.setName(partner.getName());
				pt.setTelephonePartner(partner.getTelephonePartner());
				pt.setTelephoneContact(partner.getTelephoneContact());
				pt.setEmail(partner.getEmailContact());
				pt.setNomContact(partner.getNomContact());
				pt.setPrenomContact(partner.getPrenomContact());
				pt.setPartnerCode(partner.getCode());
				pt.setCurrencyName(partner.getCurrencyName());
				pt.setCodeAgence(partner.getCodeAgence());
				pt.setMcc(partner.getMcc());
				pt.setModeReglement(partner.getModeReglement());
				pt.setNumeroCompte(partner.getNumeroCompte());
				pt.setPrefixeWallet(partner.getPrefixeWallet());
				pt.setAuth(utilVue.apgDeCrypt(partner.getToken()));
				BigDecimal mainBalance = BigDecimal.ZERO;
				if(partner.getPType().equals(BEConstantes.PARTNER_AGREGATOR)) {
					if(partner.getCode().equals(BEConstantes.CODE_RELIANCE_AGREGATEUR))
						mainBalance = operationSession.getNewBalance(partner, BEConstantes.DEFAULT_COMPTE_PRINCIPAL, partner.getCurrencyName());
					else
						mainBalance = serviceSession.balancePivotA(partner.getCode(), partner.getPrincipalAccountNumber(), partner.getCurrencyName());
				}
				else
					mainBalance = operationSession.balancePartner(partner);
				pt.setBalance(mainBalance+"");

				PartnerType pType = partner.getPartnerType();
				pt.setPartnerType(pType.getType());
				pt.setPartnerTypeId(pType.getIdPartnerType()+"");
				pt.setActive(partner.getIsActive());
				pt.setCountryIsoCode(partner.getCountryIsoCode());
				pt.setCountryIndicatif(partner.getCountryIndicatif());
				pt.setIsB2B(partner.getIsB2B());
				pt.setIsDecouvert(partner.getIsDecouvert());
				pt.setBalanceDecouvert(partner.getBalanceDecouvert());
				try {
					pt.setSdPayTrans(new JSONObject(partner.getAdditionalParameters()).optBoolean("SD_PAY_TRANS"));
				} catch (Exception e) {
					// TODO: handle exception
				}
				lpts.add(pt);
			}
			LOG.info("####### End List Partner payer from APG >>>>>>> "+lpts.size());
			response = new APGPartnerResponse("0", "OK",lpts);
			return Response.ok().entity(response)
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.build();
		}catch (TransactionException e) {
			return Response.ok().entity(new AbstractResponse(e.getCode(), e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		}	
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok().entity(new AbstractResponse("1", e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		}	
	}

}
