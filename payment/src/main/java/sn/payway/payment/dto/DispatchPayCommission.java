package sn.payway.payment.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DispatchPayCommission {

	
	private BigDecimal payinCommission;
	private BigDecimal payoutCommission;
	private BigDecimal commissionAccepteur;
	private BigDecimal commissionDistributeur ;
	private BigDecimal commissionEmetteur;
	private BigDecimal commissionGim ;
	private BigDecimal commissionSupportTechnique;
	private BigDecimal taxe;
	private BigDecimal commissionBanque;
	
}
