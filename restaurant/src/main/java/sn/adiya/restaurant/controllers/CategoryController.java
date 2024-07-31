package sn.adiya.restaurant.controllers;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.AbstractResponse;
import sn.adiya.restaurant.dto.CategoryDto;
import sn.adiya.restaurant.services.CategoryService;


@Path("/dishes/categories")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class CategoryController {

	@Inject
	private CategoryService categoryService;
	
	@POST
	public AbstractResponse createCategory( CategoryDto dto) {
		
		categoryService.create(dto);
		return new AbstractResponse(ErrorResponse.REPONSE_SUCCESS.getCode(),"");
	}
	
	@GET
	public List<CategoryDto> findCategories(){
		return categoryService.findAll();
	}
}
