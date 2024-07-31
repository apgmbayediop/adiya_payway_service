package sn.adiya.transaction.service;

import java.math.BigDecimal;
import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.entities.Account;
import sn.fig.entities.LinkedCard;
import sn.fig.entities.Partner;
import sn.fig.entities.Transaction;
import sn.fig.entities.Wallet;
import sn.fig.entities.aci.CommissionMonetique;
import sn.adiya.transaction.account.service.PartnerAccountService;
import sn.adiya.transaction.context.CashToCardContext;
import sn.adiya.transaction.context.TransactionContext;
import sn.adiya.transaction.exception.TransactionException;

@Stateless
@JBossLog
public class CashToCardTrxService implements TransactionHandler{

	@Inject
	private CommissionService commService;
	@Inject
	private CardAccountService crdAcctService;
	@Inject
	private PartnerAccountService partnAcctService;
	
	@Override
	public TransactionContext before(TransactionContext context) throws TransactionException {
		CashToCardContext ctx = (CashToCardContext)context;
		CommissionMonetique commMonetique = ctx.getSession().executeNamedQuerySingle(CommissionMonetique.class,
				"Comm.FindByPartenaireChannelAndSubMeans", 
				new String[] {"idPartner","channelType","meansType","subMeansType"}, 
				new Object[] {ctx.getPartner().getIdPartner(),ctx.getChannelType(),"CARTE","ALL"});
		if(commMonetique ==null) {
			throw new TransactionException(ErrorResponse.COMMISSION_INTROUVABLE_OPTIMA.getCode(), ErrorResponse.COMMISSION_INTROUVABLE_OPTIMA.getMessage(""));
		}
		BigDecimal payinCommission=commService.getCommissionMonetique(ctx.getAmount(), commMonetique);
		Account account =ctx.getSession().executeNamedQuerySingle(Account.class, "getNewBalanceAccount", 
				new String[]{"accountType","partner","currency"}, new Object[] {BEConstantes.DEFAULT_COMPTE_PRINCIPAL,
						ctx.getPartner(),ctx.getPartner().getCurrencyName()});
		BigDecimal total = ctx.getAmount().add(payinCommission);
		if(total.compareTo(account.getBalance())>0||account.getBalance().subtract(total).compareTo(account.getMinBalance())<0) {
			throw new TransactionException(ErrorResponse.TRANSACTION_SOLDE_INSUFFISANT_OPTIMA.getCode(),
					ErrorResponse.TRANSACTION_SOLDE_INSUFFISANT_OPTIMA.getMessage(""));
				
		}
		ctx.setDispatchCommission(commService.dispatchCommission(commMonetique, payinCommission));
		ctx.setPayinCommission(payinCommission);
		ctx.setPayoutCommission(payinCommission);
		ctx.setDebitAccount(account);
		return ctx;
	}

	@Override
	@Transactional(value = TxType.REQUIRES_NEW)
	public Long execute(TransactionContext context) throws TransactionException {
		log.info("execute cash to card");
		CashToCardContext ctx= (CashToCardContext)before(context);
		Transaction trx = build(ctx);
		trx.setStatus(BEConstantes.VALIDATED);
		BigDecimal amount = ctx.getAmount().add(ctx.getPayinCommission());
		
		trx=(Transaction)ctx.getSession().saveObject(trx);
		String description="CASH TO CARD |"+trx.getId();
		partnAcctService.updateBalance(amount.negate(), ctx.getDebitAccount(), description, trx);
		crdAcctService.updateBalance(trx.getPayinAmount(),ctx.getToCard(),description,trx);
		after(context, trx);
		return trx.getId();
	}

	@Override
	public void after(TransactionContext context,Transaction trx) {
		Transaction transaction =addLinkedElement(context, trx);
		CashToCardContext ctx = (CashToCardContext)context;
		CommissionMonetique commResp = ctx.getDispatchCommission();
		trx.setCommissionSenderSA(commResp.getCommissionEmetteur());
		trx.setCommissionSender(commResp.getCommissionAccepteur());
		trx.setCommissionAPG(commResp.getCommissionSupportTechnique());
		trx.setCommissionSponsor(commResp.getCommissionDistributeur());
		trx.setCommissionPayerP(commResp.getCommissionGim());
		
		Partner apg = ctx.getSession().executeNamedQuerySingle(Partner.class, "findPartnerByCode",
				new String[] {"code"}, new String[] {BEConstantes.CODE_APG});
		Account accountEmetteur= partnAcctService.findOrCreate(trx.getToCard().getActivatePartner(),
				BEConstantes.DEFAULT_COMPTE_COMMISSION, trx.getPartner().getCurrencyName());
		Account accountDistributeur= partnAcctService.findOrCreate(trx.getPartner().getParent(),
				BEConstantes.DEFAULT_COMPTE_COMMISSION, trx.getPartner().getCurrencyName());
		Account accountAPG= partnAcctService.findOrCreate(apg,
				BEConstantes.DEFAULT_COMPTE_COMMISSION, trx.getPartner().getCurrencyName());
		String descriptionCom = String.join("|","COMMISSION", trx.getDescription());
		partnAcctService.updateBalance(commResp.getCommissionEmetteur(), accountEmetteur, descriptionCom, transaction);
		partnAcctService.updateBalance(commResp.getCommissionDistributeur(), accountDistributeur, descriptionCom, transaction);
		partnAcctService.updateBalance(commResp.getCommissionSupportTechnique(), accountAPG, descriptionCom, transaction);
		
		context.getSession().updateObject(transaction);
	}

	@Override
	public Transaction build(TransactionContext context) {
		CashToCardContext ctx =(CashToCardContext)context;
		Transaction trx = new Transaction();
		trx.setPartner(ctx.getPartner());
		trx.setPartnerPayer(ctx.getPartner());
		trx.setPayinAmount(ctx.getAmount());
		trx.setPayinCommission(ctx.getPayinCommission());
		trx.setPayoutAmount(ctx.getAmount());
		trx.setPayoutCommission(ctx.getPayoutCommission());
		trx.setChannelType(ctx.getChannelType());
		trx.setInTrRefNumber(ctx.getRequestId());
		trx.setDate(new Date());
		trx.setBeneficiaryFirstName(ctx.getToCard().getRegister().getFirstname());
		trx.setBeneficiaryLastName(ctx.getToCard().getRegister().getLastname());
		trx.setBeneficiaryMobileNumber(ctx.getToCard().getRegister().getPhonenumber());
		trx.setPayinCurrency(ctx.getPartner().getCurrencyName());
		trx.setPayoutCurrency(ctx.getPartner().getCurrencyName());
		trx.setToCard(ctx.getToCard());
		trx.setStatus(BEConstantes.STATUS_TRANSACTION_VALIDATED);
		return trx;
	}
	
	
	private Transaction addLinkedElement(TransactionContext context,Transaction trx) {
		CashToCardContext ctx =(CashToCardContext)context;
		if(trx.getToCard().isLinked()) {
			LinkedCard lnkCard = ctx.getSession().executeNamedQuerySingle(LinkedCard.class, "LC.findByCin",
					new String[] { "cin" }, new String[] { ctx.getToCard().getCin() });
			if ("WALLET".equals(lnkCard.getLinkedType())) {
				Wallet wallet = ctx.getSession().findObjectById(Wallet.class, Long.parseLong(lnkCard.getReference()), null);
				trx.setIdtWallet(wallet.getWallet());
				
			}
		}
		return trx;
	}

	@Override
	public Transaction setCommission(TransactionContext context, Transaction trx) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
