package sn.payway.card.generation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sn.apiapg.entities.Currency;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class CardGenerationDto {
	
	
	private int validity;
	private int gracePeriod;
	private int sequenceNumber;
	private String type; 
	private Currency currency;

}
