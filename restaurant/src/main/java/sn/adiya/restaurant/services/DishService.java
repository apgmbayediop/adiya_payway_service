package sn.adiya.restaurant.services;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.aci.PointDeVente;
import sn.fig.entities.restau.Category;
import sn.fig.entities.restau.Dish;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.restaurant.dto.ItemDto;
import sn.adiya.restaurant.exception.RestaurantException;



@Stateless
@JBossLog
public class DishService {

	
	public void create(ItemDto dto) throws RestaurantException {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		
		log.info(dto.getPos().getNumeroPointDeVente());
		PointDeVente pos = session.executeNamedQuerySingle(PointDeVente.class,"PointDeVente.findPointDeVenteByNumero",
				new String[] {"numeroPointDeVente"}, new String[] {dto.getPos().getNumeroPointDeVente()});
		if(pos ==null) {
			throw new RestaurantException("le point de vente n'existe pas");
		}
		Category category = session.findObjectById(Category.class, null,dto.getCategory().getName());
		
		if(category==null) {
			throw new RestaurantException("category non correcte");
		}
		Dish item = new Dish();
		item.setCategory(category);
		item.setCookTime(dto.getCookTime());
		item.setDateWriting(Instant.now(Clock.systemUTC()));
		item.setDescription(dto.getDescription());
		item.setName(dto.getName());
		item.setPos(pos);
		item.setPrice(dto.getPrice());
		item.setQuantity(dto.getQuantity());
		session.saveObject(item);
		}

	public List<ItemDto> listItem(String numPos , String category) {
		
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		List<Dish> items;
		if(category==null||category.isBlank()) {
			items = session.executeNamedQueryList(Dish.class,"findItemByPos" , new String[] {"pos"},new String[] {numPos} );
		}else {
			items = session.executeNamedQueryList(Dish.class,"findItemByPosAndCategorie" , new String[] {"pos","categorie"},new String[] {numPos,category} );
		}
		List<ItemDto> dishes = new ArrayList<>();
		items.forEach(item->dishes.add(new ItemDto(item)));
		
		return dishes;
	}
}
