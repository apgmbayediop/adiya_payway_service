package sn.payway.card.personalization.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class PersoDto {

	private String firstName;
	private String lastName;
	private String documentNumber;
	private String phoneNumber;
	private String cin;
	private BigDecimal loadAmount=BigDecimal.ZERO;
}
