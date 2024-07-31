package sn.adiya.partner.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.FileOperation;
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerType;
import sn.fig.session.AdministrationSession;
import sn.fig.session.AdministrationSessionBean;
import sn.fig.session.OperationSession;
import sn.fig.session.OperationSessionBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationListPartnerOut {
	public Response Service(String flashcode) throws TransactionException{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());

			System.out.println("####### Start List Partner payer from APG >>>>>>>");
			APGPartnerResponse response; 
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, "AdministrationListPartnerOut", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			Partner partnerAPG = administrationSession.findPartnerCode(BEConstantes.CODE_APG);
			utilVue.CommonObject(null, "CreditPayoutAccount", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Partner APG : "), partnerAPG);
			
			String[] parameters = {"type"};
			Object[] data = {BEConstantes.PARTNER_PAYER};
			List<Partner> lPartnerOUT = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", parameters, data);
			Object[] datas = {BEConstantes.PARTNER_SENDER_PAYER};
			List<Partner> lPartnerIN_OUT = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", parameters, datas);
			List<Partner> lPartner = new ArrayList<Partner>();
			if(!lPartnerOUT.isEmpty() && lPartnerOUT.size() >0) lPartner.addAll(lPartnerOUT);
			if(!lPartnerIN_OUT.isEmpty() && lPartnerIN_OUT.size() >0) lPartner.addAll(lPartnerIN_OUT);			
			List<APGPartner> lpts = new ArrayList<APGPartner>();
			for(Partner partner : lPartner){
				boolean isCreated = FileOperation.createDirectory("partners",partner.getName().trim().replace(" ", "_").toLowerCase());
				if(isCreated)
					System.out.println("FOLDER PARTNER >>>>>>>>>>>>> "+partner.getName()+" CREATED");
				APGPartner pt = new APGPartner();
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

				BigDecimal lastBalancePartner = operationSession.balancePartner(partner);
				pt.setBalance(lastBalancePartner+"");

				PartnerType pType = partner.getPartnerType();
				pt.setPartnerType(pType.getType());
				pt.setPartnerTypeId(pType.getIdPartnerType()+"");
			//	pt.setPrincipalAccountNumber(partner.getPrincipalAccountNumber());

				lpts.add(pt);
			}
			System.out.println("####### End List Partner payer from APG >>>>>>>");
			response = new APGPartnerResponse("0", "OK",lpts);

			return Response.ok().entity(response)
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.build();
		} catch (Exception e) {
			return Response.ok().entity(new AbstractResponse("1", e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		}	
	}

}
