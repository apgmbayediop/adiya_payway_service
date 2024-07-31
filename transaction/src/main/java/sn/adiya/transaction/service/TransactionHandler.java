package sn.adiya.transaction.service;

import javax.ejb.Remote;

import sn.fig.entities.Transaction;
import sn.adiya.transaction.context.TransactionContext;
import sn.adiya.transaction.exception.TransactionException;

@Remote
public interface TransactionHandler {

	public TransactionContext before(TransactionContext context) throws TransactionException ;
	public Long execute(TransactionContext context)throws TransactionException ;
	public void after(TransactionContext context,Transaction trx) throws TransactionException ;
	public Transaction build(TransactionContext context);
	public Transaction setCommission(TransactionContext context, Transaction trx);
}
