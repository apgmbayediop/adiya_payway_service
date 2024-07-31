package sn.adiya.payment.cancel;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import sn.fig.entities.AccountTransaction;
import sn.fig.entities.CardAcctTransaction;
import sn.fig.entities.Transaction;
import sn.fig.entities.WalletAcctTransaction;
import sn.fig.session.Session;
import sn.adiya.transaction.account.service.PartnerAccountService;
import sn.adiya.transaction.exception.TransactionException;
import sn.adiya.transaction.service.CardAccountService;
import sn.adiya.transaction.wallet.service.WalletAcctService;

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
