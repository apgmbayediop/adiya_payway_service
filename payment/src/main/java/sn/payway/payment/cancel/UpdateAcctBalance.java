package sn.payway.payment.cancel;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import sn.apiapg.entities.AccountTransaction;
import sn.apiapg.entities.CardAcctTransaction;
import sn.apiapg.entities.Transaction;
import sn.apiapg.entities.WalletAcctTransaction;
import sn.apiapg.session.Session;
import sn.payway.transaction.account.service.PartnerAccountService;
import sn.payway.transaction.exception.TransactionException;
import sn.payway.transaction.service.CardAccountService;
import sn.payway.transaction.wallet.service.WalletAcctService;

@Stateless
public class UpdateAcctBalance {

	@Inject
	private PartnerAccountService pAcctService;
	@Inject
	private WalletAcctService wltAcctService;
	@Inject
	private CardAccountService crdAcctService;

	public void updateAccountBalance(Session sess, Transaction trx) throws TransactionException {

		List<AccountTransaction> pAcct = sess.executeNamedQueryList(AccountTransaction.class,
				"findAccountTransactionByTransactions2", new String[] { "idTransaction" }, new Long[] { trx.getId() });
		List<WalletAcctTransaction> wAcct = sess.executeNamedQueryList(WalletAcctTransaction.class,
				"walletTrxByTransaction", new String[] { "idTransaction" }, new Long[] { trx.getId() });
		List<CardAcctTransaction> cardAcct = sess.executeNamedQueryList(CardAcctTransaction.class,
				"cardTrxByTransaction", new String[] { "idTransaction" }, new Long[] { trx.getId() });

		if (!pAcct.isEmpty()) {
			for (AccountTransaction acct : pAcct)
				pAcctService.updateBalance(acct.getAmount().negate(), acct.getAccount(),
						"Annulation|" + acct.getDescription(), trx);
		}
		if (!wAcct.isEmpty()) {
			for (WalletAcctTransaction acct : wAcct)
				wltAcctService.updateBalance(acct.getAccount(), trx, acct.getAmount().negate(),
						"Annulation|" + acct.getDescription());
		}
		if (!cardAcct.isEmpty()) {
			for (CardAcctTransaction acct : cardAcct) {
				crdAcctService.updateBalance(acct.getAmount().negate(), acct.getCard(),
						"Annulation|" + acct.getDescription(), trx);
			}
		}
	}
}
