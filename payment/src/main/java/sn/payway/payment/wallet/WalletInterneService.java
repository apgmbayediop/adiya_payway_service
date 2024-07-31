package sn.payway.payment.wallet;

import java.math.BigDecimal;

import javax.ejb.Stateless;
import javax.inject.Inject;

import lombok.extern.jbosslog.JBossLog;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.entities.AccountWallet;
import sn.apiapg.entities.Wallet;
import sn.apiapg.entities.aci.CommissionMonetique;
import sn.apiapg.entities.aci.IsoAcquisition;
import sn.payway.common.utils.Constantes;
import sn.payway.payment.dto.PaymentDetails;
import sn.payway.payment.dto.PaymentDto;
import sn.payway.payment.services.finalize.FinalizePayment;
import sn.payway.payment.utils.PaymentHelper;
import sn.payway.transaction.service.CommissionService;
import sn.payway.transaction.wallet.service.WalletAcctService;

@Stateless
@JBossLog
public class WalletInterneService {

	
	
	@Inject
	private WalletAcctService wltAcctService;
	@Inject
	private FinalizePayment finalizePay;
	@Inject
	private CommissionService commissionService;
	@Inject
	private PaymentHelper payHelper;
	
	public PaymentDetails payment(PaymentDto dto, IsoAcquisition acqui, Wallet wallet) {
	
		
		log.info("wallet interne payment");
		AccountWallet acct = wltAcctService.findOrCreateAccount(wallet, BEConstantes.DEFAULT_COMPTE_PRINCIPAL);
		
		BigDecimal amount = new BigDecimal(acqui.getField4());
		PaymentDetails resp = new PaymentDetails();
		if(acct.getBalance().compareTo(amount)>=0) {
			CommissionMonetique reqCom =new CommissionMonetique();
			dto.setMeansType(Constantes.WALLET);
			reqCom.setMeansType(Constantes.WALLET);
			reqCom.setSubMeansType(payHelper.getSubMeansTypeFromBank(dto.getBank()));
			reqCom.setChannelType(acqui.getChannelType());
			reqCom.setPartner(acqui.getCaisse().getPointDeVente().getCommercant());
			CommissionMonetique commMonetique =commissionService.findCommissionMonetique(reqCom);
			if(commMonetique ==null) {
				resp.setCode(ErrorResponse.COMMISSION_NOT_FOUND.getCode());
				resp.setMessage(ErrorResponse.COMMISSION_NOT_FOUND.getMessage(""));
			}else {
				dto.setCardType("APG");
			finalizePay.updateTransaction(Constantes.ISO_SUCCESS_STATUS, dto, acqui, null);
			resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			resp.setAmount(amount);
			resp.setRequestId(dto.getRequestId());
			resp.setTransactionId(acqui.getId().toString());
			resp.setCurrencyName(acqui.getField49());
			}
		}else {
			resp.setCode(ErrorResponse.TRANSACTION_SOLDE_INSUFFISANT_OPTIMA.getCode());
		resp.setMessage(ErrorResponse.TRANSACTION_SOLDE_INSUFFISANT_OPTIMA.getMessage(""));
	}
	log.info(resp.getCode());
	log.info(resp.getMessage());
		
		return resp;
	}
}
