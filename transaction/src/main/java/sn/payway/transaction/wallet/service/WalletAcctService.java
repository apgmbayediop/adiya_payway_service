package sn.payway.transaction.wallet.service;

import java.math.BigDecimal;
import java.util.Date;

import javax.ejb.Stateless;

import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.AccountType;
import sn.apiapg.entities.AccountWallet;
import sn.apiapg.entities.Transaction;
import sn.apiapg.entities.Wallet;
import sn.apiapg.entities.WalletAcctTransaction;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

@Stateless
public class WalletAcctService {

	public AccountWallet findOrCreateAccount(Wallet wallet, String accountType) {
		Session session = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		AccountWallet account =session .executeNamedQuerySingle(AccountWallet.class,
				"findAccountByWalletAndType", new String[] {"wallet","accountType"}, 
				 new Object[] {wallet,accountType});
		if(account ==null) {
			AccountType type =session.findObjectById(AccountType.class, null, accountType);
			BigDecimal maxBalance ="VERIFIED".equals(wallet.getIsFullKyc())? new BigDecimal("200000"): new BigDecimal("2000000"); 
			String numerCompte = String.join("_", accountType,wallet.getWallet());
			account = new AccountWallet();
			account.setAccountType(type);
			account.setBalance(BigDecimal.ZERO);
			account.setDate(new Date());
			account.setDescription(type.getLibelle()+"|"+wallet.getPrincipalAccountNumber());
			account.setMaxBalance(maxBalance);

			account.setMinBalance(BigDecimal.ZERO);
			account.setNumeroCompte(numerCompte);
			account.setWallet(wallet);
			account =(AccountWallet)session.saveObject(account);
		}
		return account;
	}
	public WalletAcctTransaction updateBalance(AccountWallet account,Transaction trx,BigDecimal amount,String description)
	 {
		WalletAcctTransaction wltTrx = new WalletAcctTransaction();
		wltTrx.setAccount(account);
		wltTrx.setAmount(amount);
		wltTrx.setDate(new Date());
		wltTrx.setDescription(description);
		wltTrx.setTransaction(trx);
		Session sess =(Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
       wltTrx = (WalletAcctTransaction)sess.saveObject(wltTrx);
       
       return wltTrx;
	 }
}
