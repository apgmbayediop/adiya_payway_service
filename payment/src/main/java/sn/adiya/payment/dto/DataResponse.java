package sn.adiya.payment.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.fig.account.entities.WalletAccount;
import sn.fig.entities.Card;
import sn.fig.entities.Transaction;
import sn.fig.entities.Wallet;
import sn.fig.entities.aci.Caisse;

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
