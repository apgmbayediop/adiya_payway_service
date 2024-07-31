package sn.payway.transaction.account.service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import javax.ejb.Stateless;

import org.jboss.logging.Logger;

import sn.apiapg.common.utils.AccountManagerNEW;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Account;
import sn.apiapg.entities.AccountTransaction;
import sn.apiapg.entities.Currency;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.Transaction;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.transaction.account.dto.BalanceDto;

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
