package sn.payway.restaurant.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sn.apiapg.entities.restau.Dish;
import sn.payway.merchant.dto.PosDto;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ItemDto {

	
	private Long id;
	private String name;
	private String description;
	private BigDecimal price;
	private long cookTime;
	private long quantity;
	private CategoryDto category;
	private PosDto pos;
	
	public ItemDto(Dish item) {
		super();
		this.name=item.getName();
		this.description =item.getDescription();
	    this.price = item.getPrice();
	    this.category = new CategoryDto(item.getName());
	    this.cookTime = item.getCookTime();
	    this.quantity = item.getQuantity();
	    this.id = item.getId();
	    this.pos =new PosDto(item.getPos());
	}
}
