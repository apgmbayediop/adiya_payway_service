package sn.payway.restaurant.dto;

import javax.validation.constraints.NotBlank;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class CategoryDto {

	
	
	@NotBlank(message = "Veuillez renseigner le nom de la categorie")
	private String name;
}
