package sn.payway.transaction.service;

import javax.ejb.Remote;

import sn.apiapg.entities.Transaction;
import sn.payway.transaction.context.TransactionContext;
import sn.payway.transaction.exception.TransactionException;

@Remote
public interface TransactionHandler {

	public TransactionContext before(TransactionContext context) throws TransactionException ;
	public Long execute(TransactionContext context)throws TransactionException ;
	public void after(TransactionContext context,Transaction trx) throws TransactionException ;
	public Transaction build(TransactionContext context);
	public Transaction setCommission(TransactionContext context, Transaction trx);
}
