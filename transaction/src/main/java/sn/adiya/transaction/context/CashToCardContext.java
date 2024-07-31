package sn.adiya.transaction.context;

import lombok.Data;
import lombok.EqualsAndHashCode;
import sn.fig.entities.Card;
import sn.fig.entities.Partner;
import sn.fig.entities.aci.CommissionMonetique;

@Data
@EqualsAndHashCode(callSuper = false)
public class CashToCardContext extends TransactionContext {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9132474364351879779L;
	private Card toCard;
	private Partner partner;
	private CommissionMonetique dispatchCommission;
}
