package sn.payway.restaurant.controllers;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.AbstractResponse;
import sn.payway.restaurant.dto.ItemDto;
import sn.payway.restaurant.exception.RestaurantException;
import sn.payway.restaurant.services.DishService;


@Path("/dishes")
public class DishController {

	@Inject
	private DishService itemService;
	
	@POST
	public AbstractResponse createItem(@Valid  ItemDto item) throws RestaurantException {
		
		itemService.create(item);
		return new AbstractResponse(ErrorResponse.REPONSE_SUCCESS.getCode(),"");
	}
	
	@PUT
	public void updateItem(@Valid  ItemDto item) {
		
	}
	@GET
	public List<ItemDto> listIems(@QueryParam(value ="numPos") String numPos,
			@QueryParam(value= "category") String category) {
		
		return itemService.listItem(numPos, category);
	}
}
