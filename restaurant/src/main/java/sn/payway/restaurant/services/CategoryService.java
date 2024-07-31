package sn.payway.restaurant.services;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;

import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.restau.Category;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.restaurant.dto.CategoryDto;


@Stateless
public class CategoryService {

	public void create(CategoryDto dto) {
            
		String name  = (dto.getName().substring(0, 1).toUpperCase() + dto.getName().substring(1).toLowerCase()).trim();
		
		Category category = new Category();
		category.setDateWriting(Instant.now());
		category.setName(name);
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		
		session.saveObject(category);
		
	}
	
	public List<CategoryDto>  findAll() {
        
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		
		 return session.findAllObject(Category.class).stream().map(c->CategoryDto.builder().name(c.getName()).build()).collect(Collectors.toList());
		 }

}
