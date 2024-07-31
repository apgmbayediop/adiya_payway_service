package sn.payway.transaction.context;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;
import sn.apiapg.entities.Account;
import sn.apiapg.session.Session;

@Data
public class TransactionContext  implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3299253246272845883L;
	private Session session;
	private Account debitAccount;
	private BigDecimal amount;
	private String requestId;
	private String channelType;
	private BigDecimal payinCommission;
	private BigDecimal payoutCommission;
}
