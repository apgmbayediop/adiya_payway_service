package sn.adiya.payment.dto;

import java.text.Normalizer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.fig.entities.Wallet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_EMPTY)
public class Person {

	
	private String firstName;
	private String lastName;
	private String email;
	private String address;
	private String birthPlace;
	private String birthDate;
	private String country;
	private String city;
	private String phoneNumber;
	private String documentNumber;
	private String zipCode;
	private String state;
	
	
	public Person(Wallet wallet) {
		String address = wallet.getAddress()==null?null:Normalizer.normalize(wallet.getAddress(), Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
		this.address = address;
		this.birthDate = wallet.getBirthdate();
		this.birthPlace = wallet.getBirthplace();
		this.city = wallet.getVille();
		this.country = wallet.getCountryIsoCode();
		this.email = wallet.getEmail();
		this.firstName = Normalizer.normalize(wallet.getFirstname(), Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
		this.lastName = Normalizer.normalize(wallet.getLastname(), Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
		this.phoneNumber =wallet.getPhonenumber();
		this.state = wallet.getCountryIsoCode();
		this.zipCode = wallet.getCodePostal();
	}
}
