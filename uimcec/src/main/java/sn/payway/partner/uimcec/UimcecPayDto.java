package sn.payway.partner.uimcec;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UimcecPayDto {

	@JsonAlias("ulrphoto")
	private String urlPhoto;
	@JsonAlias("ulrpiece")
	private String ulrPiece;
	private String code;
	private String message;
	@JsonAlias({"code_message","codeReponse","codereponse"})
	private String codeMessage;
	@JsonAlias("referencereponse")
	private String referencePayer;
	@JsonAlias({ "solde", "soldetheorique" })
	private BigDecimal balance;
	@JsonAlias({"descriptionreponse","detailreponse"})
	private String description;
	@JsonAlias("ulrsignature")
	private String urlsignature;
	private String responsePayer;
	private String pan;
	private String cardCin;
	@JsonAlias("messagetype")
	private String messageType;
	@JsonAlias("referenceope")
	private String reference;
	private String token;
	private String methode;
	@JsonAlias("idapicall")
	private String idApiCall;
	@JsonAlias({ "currency", "codedevise" })
	private String currencyName;
	@JsonAlias("nomdevise")
	private String currencyLabel;
}
