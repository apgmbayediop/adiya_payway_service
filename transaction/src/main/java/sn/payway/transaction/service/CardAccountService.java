package sn.payway.transaction.service;

import java.math.BigDecimal;
import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;

import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.AccountWallet;
import sn.apiapg.entities.Card;
import sn.apiapg.entities.CardAcctTransaction;
import sn.apiapg.entities.LinkedCard;
import sn.apiapg.entities.Transaction;
import sn.apiapg.entities.Wallet;
import sn.apiapg.entities.WalletAcctTransaction;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.transaction.exception.TransactionException;
import sn.payway.transaction.wallet.service.WalletAcctService;

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
