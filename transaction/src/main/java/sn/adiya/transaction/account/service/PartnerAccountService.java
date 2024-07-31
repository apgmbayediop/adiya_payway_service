package sn.adiya.transaction.account.service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import javax.ejb.Stateless;

import org.jboss.logging.Logger;

import sn.fig.common.utils.AccountManagerNEW;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.Account;
import sn.fig.entities.AccountTransaction;
import sn.fig.entities.Currency;
import sn.fig.entities.Partner;
import sn.fig.entities.Transaction;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.transaction.account.dto.BalanceDto;

@Stateless
public class PartnerAccountService {

	private static final Logger LOG = Logger.getLogger(PartnerAccountService.class);
	
	
	public Account findOrCreate(Partner partner, String accountType,String currencyName) {
		Session session = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		Account account =session .executeNamedQuerySingle(Account.class,
				"getNewBalanceAccount", new String[] {"accountType","partner","currency"}, 
				 new Object[] {accountType,partner,currencyName});
		if(account ==null) {
			Currency currency =session.executeNamedQuerySingle(Currency.class, 
					"Currency.findByName", new String[] {"currencyName"}, 
					 new Object[] {currencyName});
			
			 account = AccountManagerNEW.createAccount(partner, currency, accountType);
		}
		return account;
	}
	public void updateBalance(BigDecimal amount, Account account,String description,Transaction trx) {
		
		if(!BigDecimal.ZERO.equals(amount)) {
			AccountTransaction accTrx = new AccountTransaction();
		accTrx.setAccount(account);
		accTrx.setAmount(amount);
		accTrx.setDate(new Date());
		accTrx.setDescription(description);
		accTrx.setTransaction(trx);
		Session session = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		session.saveObject(accTrx);
		}
	}
	public BigDecimal availableBalance(BalanceDto dto) {
		Calendar cal = Calendar.getInstance();
		
		cal.add(Calendar.DATE, -dto.getSettlementDay());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		Date end = cal.getTime();
		Account account = findOrCreate(dto.getPartner(), dto.getAccountType(), dto.getPartner().getCurrencyName());
		String params[]=new String[] {"account", "end"};
		Session session = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		
		BigDecimal balanceQ = session.executeNamedQuerySingle(BigDecimal.class,
				"acctTrx.balanceByDate", 
				params, 
				new Object[] {account,end});
		BigDecimal balance = balanceQ==null?BigDecimal.ZERO:balanceQ;			
		LOG.info("date Limite.........................................."+end);
		LOG.info("SOLDE.........................................."+balance);
		
		BigDecimal settlementsQ = session.executeNamedQuerySingle(BigDecimal.class,
				"acctTrx.settlementByDate", 
				params, 
				new Object[] {account,end});
		BigDecimal settlements = settlementsQ==null?BigDecimal.ZERO:settlementsQ;	
		LOG.info("DEBIT A DEDUIRE..........................."+settlements);
		
		
		return balance.add(settlements);
	}
}
