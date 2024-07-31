package sn.payway.payment.sd;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Stateless;

import org.jboss.logging.Logger;

import sn.apiapg.common.utils.AccountManager;
import sn.apiapg.common.utils.AccountManagerBean;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.Transaction;
import sn.apiapg.entities.aci.IsoAcquisition;

@Stateless
public class SDManager {

	private static final Logger LOG = Logger.getLogger(SDManager.class);

	
	public Map<String, String> finalizeSDOperation(Partner commercant, boolean isCredit, BigDecimal amount,
			Transaction trx, IsoAcquisition acqui) {
		Map<String, String> dataPartner = new ConcurrentHashMap<>();
		try {
			LOG.info("****************finalizeSD****************************");
			 AccountManager accountManager = (AccountManager)BeanLocator.lookUp(AccountManagerBean.class.getSimpleName());
			trx.setIdEntitePayeur(acqui.getCaisse().getCaisseSD().getIdPartner());
			trx.setCommissionPayerCaisse(trx.getCommissionSender());
			BigDecimal commissionAPG= trx.getCommissionAPG();
			trx.setCommissionAPG(BigDecimal.ZERO);
			if(isCredit) {
				accountManager.creditAccountCaisse(trx);
			}else {
				accountManager.debitCaisse(trx);
			}
			trx.setCommissionAPG(commissionAPG);
			trx.setCommissionSender(BigDecimal.ZERO);
			dataPartner.put(BEConstantes.PRM_HTML_AMOUNT, trx.getPayoutAmount().toString());
			//dataPartner.put(BEConstantes.PRM_HTML_ACCOUNT, acqui.getCaisse().getCaisseSD().getPrincipalAccountNumber());
			dataPartner.put(BEConstantes.PRM_HTML_TRX_TYPE, BEConstantes.SENS_CREDIT);
			dataPartner.put(BEConstantes.PRM_HTML_TRX_ID, acqui.getId().toString());
			dataPartner.put(BEConstantes.PRM_HTML_BALANCE, trx.getNewBalance().toString());
			dataPartner.put(BEConstantes.PRM_HTML_MERCH_NAME, commercant.getName());
			dataPartner.put(BEConstantes.PRM_HTML_TRX_DESC, trx.getDescription());
			dataPartner.put(BEConstantes.PRM_HTML_CURRENCY, trx.getPayinCurrency());
			dataPartner.put(BEConstantes.PRM_HTML_DATE,
					new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME, Locale.getDefault()).format(trx.getDate()));
		}
		catch (Exception e) {
		LOG.error("finalSDError",e);
		}
		return dataPartner;
	}
}
