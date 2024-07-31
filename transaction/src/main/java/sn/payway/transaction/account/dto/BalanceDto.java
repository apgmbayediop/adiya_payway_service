package sn.payway.transaction.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sn.apiapg.entities.Partner;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class BalanceDto {

	
	private int settlementDay;
	private Partner partner;
	private String accountType;
}
