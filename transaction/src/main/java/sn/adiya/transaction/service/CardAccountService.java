package sn.adiya.transaction.service;

import java.math.BigDecimal;
import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;

import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.AccountWallet;
import sn.fig.entities.Card;
import sn.fig.entities.CardAcctTransaction;
import sn.fig.entities.LinkedCard;
import sn.fig.entities.Transaction;
import sn.fig.entities.Wallet;
import sn.fig.entities.WalletAcctTransaction;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.transaction.exception.TransactionException;
import sn.adiya.transaction.wallet.service.WalletAcctService;

@Stateless
public class CardAccountService {

	@Inject
	private WalletAcctService wltAcctService;

	public void updateBalance(BigDecimal amount, Card card, String description, Transaction trx)
			throws TransactionException {

		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		if (card.isLinked()) {
			LinkedCard lnkCard = sess.executeNamedQuerySingle(LinkedCard.class, "LC.findByCinAndType",
					new String[] { "cin","linkedType"}, new String[] { card.getCin(), "WALLET" });
			if (lnkCard == null) {
				throw new TransactionException(ErrorResponse.TRANSACTION_NOT_ALLOW_OPTIMA.getCode(),
						ErrorResponse.TRANSACTION_NOT_ALLOW_OPTIMA.getMessage(""));
			}
			Wallet wallet = sess.findObjectById(Wallet.class, Long.parseLong(lnkCard.getReference()), null);
			AccountWallet account = wltAcctService.findOrCreateAccount(wallet, BEConstantes.DEFAULT_COMPTE_PRINCIPAL);
			WalletAcctTransaction walletTrx = new WalletAcctTransaction();
			walletTrx.setAccount(account);
			walletTrx.setAmount(amount);
			walletTrx.setDate(new Date());
			walletTrx.setDescription(description);
			walletTrx.setTransaction(trx);
			sess.saveObject(walletTrx);
		} else {
			CardAcctTransaction crdTrx = new CardAcctTransaction();
			crdTrx.setAmount(trx.getPayinAmount());
			crdTrx.setCard(card);
			crdTrx.setDate(new Date());
			crdTrx.setDescription(description);
			crdTrx.setTransaction(trx);
			sess.saveObject(crdTrx);
		}
	}

}
