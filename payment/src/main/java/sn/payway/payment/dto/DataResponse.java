package sn.payway.payment.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.apiapg.account.entities.WalletAccount;
import sn.apiapg.entities.Card;
import sn.apiapg.entities.Transaction;
import sn.apiapg.entities.Wallet;
import sn.apiapg.entities.aci.Caisse;

@Getter
@Setter
@NoArgsConstructor
public class DataResponse {

	private Map<String, String> dataPartner;
	private Map<String, String> dataCustomer;
	private Transaction trx;
	private Card customerCard;
	private Card merchantCard;
	private String customerEmail;
	private String merchantEmail;
	private Wallet wallet;
	private WalletAccount account;
	private Caisse caisse;
	private BigDecimal balance;
}
