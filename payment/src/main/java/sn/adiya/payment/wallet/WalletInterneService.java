package sn.adiya.payment.wallet;

import java.math.BigDecimal;

import javax.ejb.Stateless;
import javax.inject.Inject;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.entities.AccountWallet;
import sn.fig.entities.Wallet;
import sn.fig.entities.aci.CommissionMonetique;
import sn.fig.entities.aci.IsoAcquisition;
import sn.adiya.common.utils.Constantes;
import sn.adiya.payment.dto.PaymentDetails;
import sn.adiya.payment.dto.PaymentDto;
import sn.adiya.payment.services.finalize.FinalizePayment;
import sn.adiya.payment.utils.PaymentHelper;
import sn.adiya.transaction.service.CommissionService;
import sn.adiya.transaction.wallet.service.WalletAcctService;

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
