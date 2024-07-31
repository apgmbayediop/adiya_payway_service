package sn.adiya.partner.uimcec;

import java.util.List;

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
public class UimcecUserDto extends UimcecPayDto {

	
	@JsonAlias("detailcompte")
	private List<UimcecUserDto> accounts;

	@JsonAlias("idcompte")
	private String idAccount;
	@JsonAlias("numerocompte")
	private String accountNumber;
	@JsonAlias("nomproduit")
	private String productName;
	@JsonAlias("nomcompte")
	private String accountName;
	@JsonAlias("typeproduit")
	private String productType;
	@JsonAlias("idproduit")
	private String productId;
	@JsonAlias("nomagence")
	private String agenceName;
	@JsonAlias("sexe")
	private String sex;
	@JsonAlias("codeclient")
	private String customerId;
	
	@JsonAlias("nom")
	private String customerFirstName;
	@JsonAlias("prenom")
	private String customerLastName;
	@JsonAlias("email")
	private String customerEmail;
	@JsonAlias("adressegeo")
	private String customerAddress;
	@JsonAlias("numtel")
	private String customerPhone;
	@JsonAlias("datenaissance")
	private String customerBirthDate;
	
}
