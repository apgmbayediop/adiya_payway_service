package sn.payway.transaction.service;

import java.math.BigDecimal;

import javax.ejb.Stateless;

import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.aci.CommissionMonetique;
import sn.apiapg.lis.utils.SysVar;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

@Stateless
public class CommissionService {

	private static final BigDecimal HUNDRED =new BigDecimal("100");
	
	public  BigDecimal getCommissionMonetique(BigDecimal amount, CommissionMonetique commissionMonetique) {
		BigDecimal commission=BigDecimal.ZERO;
			
			if (SysVar.TypeCommission.COMMISSION_FIXE.equals
					(commissionMonetique.getTypeMontant())||
					BEConstantes.PARTNER_COMMISSION_VALEUR.equals(
							commissionMonetique.getTypeMontant())) {
			commission = commission.add(commissionMonetique.getCommission());
		} else {
			commission = amount.multiply(commissionMonetique.getCommission()).divide(HUNDRED);
		}
		BigDecimal fixedCommission =commissionMonetique.getFixedCommission() == null?BigDecimal.ZERO:commissionMonetique.getFixedCommission(); 
        commission =commission.add(fixedCommission);
		
		
		return commission;
	}
	
	public CommissionMonetique dispatchCommission(CommissionMonetique comm,BigDecimal commission) {
		BigDecimal commAccepteur = commission.multiply(comm.getCommissionAccepteur()).divide(HUNDRED);
		BigDecimal commDistributeur = commission.multiply(comm.getCommissionDistributeur()).divide(HUNDRED);
		BigDecimal commEmetteur = commission.multiply(comm.getCommissionEmetteur()).divide(HUNDRED);
		BigDecimal commGim = commission.multiply(comm.getCommissionGim()).divide(HUNDRED);
		BigDecimal  partnerCommission= commAccepteur.add(commDistributeur)
				.add(commEmetteur).add(commGim);
		BigDecimal commSupTechn = commission.subtract(partnerCommission);
		 CommissionMonetique dispaMonetique =new CommissionMonetique();
		 dispaMonetique.setCommission(commission);
		 dispaMonetique.setCommissionAccepteur(commAccepteur);
		 dispaMonetique.setCommissionDistributeur(commDistributeur);
		 dispaMonetique.setCommissionEmetteur(commEmetteur);
		 dispaMonetique.setCommissionGim(commGim);
		 dispaMonetique.setCommissionSupportTechnique(commSupTechn);
		 return dispaMonetique;
		}
	public CommissionMonetique findCommissionMonetique(CommissionMonetique req) {

		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

		String  params[] = 	{ "idPartner", "channelType", "meansType","subMeansType" };
		Object data[] = { req.getPartner().getIdPartner(), req.getChannelType(), req.getMeansType() ,req.getSubMeansType()};
		CommissionMonetique commission = sess.executeNamedQuerySingle(CommissionMonetique.class,
				"Comm.FindByPartenaireChannelAndSubMeans",params ,data );
		if(commission ==null) {
			data[3]="ALL";
			commission =  sess.executeNamedQuerySingle(CommissionMonetique.class,
					"Comm.FindByPartenaireChannelAndSubMeans",params ,data );;
		}
		return commission;
	}
}
