package sn.payway.card.link;

import java.time.Clock;
import java.time.LocalDateTime;

import javax.ejb.Stateless;

import org.jboss.logging.Logger;

import sn.payway.card.exception.CardException;
import sn.payway.common.utils.Constantes;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Card;
import sn.apiapg.entities.LinkedCard;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.Wallet;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

@Stateless
public class LinkCardService {

	private  static final Logger LOG = Logger.getLogger(LinkCardService.class);
	        private static final String CIN = "cin";
	
	  public void linkWithPartner(String cin, Partner partner) {
		  
		  LinkedCard linkedCard = new LinkedCard();
		  linkedCard.setCin(cin);
		  linkedCard.setDateWriting(LocalDateTime.now(Clock.systemUTC()));
		  linkedCard.setLinkedType(partner.getPType());
		  linkedCard.setReference(partner.getIdPartner().toString());
		  Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		  sess.saveObject(linkedCard);
		  Card card = sess.executeNamedQuerySingle(Card.class, "Card.findByCin", new String[] {CIN}, new String[] {cin});
          card.setLinked(true);
          sess.updateObject(card);
	  }
  public void linkWithWallet(String cin, Wallet wallet ) {
		  
		  LinkedCard linkedCard = new LinkedCard();
		  linkedCard.setCin(cin);
		  linkedCard.setDateWriting(LocalDateTime.now(Clock.systemUTC()));
		  linkedCard.setLinkedType(Constantes.WALLET);
		  linkedCard.setReference(wallet.getIdWallet().toString());
		  Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		  sess.saveObject(linkedCard);
		  wallet.setLinkedCin(cin);
	      sess.updateObject(wallet);
	      Card card = sess.executeNamedQuerySingle(Card.class, "Card.findByCin", new String[] {CIN}, new String[] {cin});
           card.setLinkedWallet(wallet.getIdWallet().toString());
           card.setLinked(true);
           sess.updateObject(card);
           
		}
  
  public void unlink(String cin) {
	  Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
	  LinkedCard lcard = sess.executeNamedQuerySingle(LinkedCard.class, "LC.findByCin", new String[] {CIN}, new String[] {cin});
      if(lcard == null) {
    	  LOG.info("card not linked");
      }else {
    	  if(Constantes.WALLET.equals(lcard.getLinkedType())) {
    		  Wallet wallet = sess.findObjectById(Wallet.class, Long.parseLong(lcard.getReference()),null);
    		  wallet.setLinkedCin(null);
    		  sess.updateObject(wallet);
    	  }else {
    		  Partner partner = sess.findObjectById(Partner.class, Long.parseLong(lcard.getReference()),null);
    	      partner.setNumeroCompte(null);
    	      partner.setModeReglement("VIREMENT");
    	      sess.updateObject(partner);
    	  }
    	  sess.deleteObject(LinkedCard.class.getSimpleName(),"cin" , lcard.getCin(), "");
    	  
      }
    	  
  }
  public String findCard(Partner partner) throws CardException{
	 
	  Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
	  LinkedCard lcard = sess.executeNamedQuerySingle(LinkedCard.class, "LC.findByTypeAndReference", new String[] {"type","reference"}, new String[] {partner.getPType(),partner.getIdPartner().toString()});
      if(lcard == null) {
    	  throw new CardException(ErrorResponse.CARD_NOT_FOUND.getCode(), ErrorResponse.CARD_NOT_FOUND.getMessage(""));
      }
      return lcard.getCin();
  }
}
