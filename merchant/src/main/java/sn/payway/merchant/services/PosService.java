package sn.payway.merchant.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.aci.Caisse;
import sn.apiapg.entities.aci.PointDeVente;
import sn.apiapg.lis.utils.SysVar;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.merchant.dto.CaisseDto;
import sn.payway.merchant.dto.PosDto;
import sn.payway.merchant.dto.PosResponse;

@Stateless
public class PosService {

	public static final String NUM_SERIE="numeroSerie";
	public static final String NUM_POS="numeroPointDeVente";
	
	private static final Logger log = Logger.getLogger(PosService.class);
	
	
	@Inject
	private MerchantCaisseService caisseService;
	
	public PosResponse create(Long idUser, PosDto request) 
	  
	{
		PosResponse resp = new PosResponse();
		try 
		{
			log.info("........... CREATION DE POINT DE VENTE PAR L'UTILISATEUR "+idUser);
			log.info(request);
			
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			Partner commercant = session.findObjectById(Partner.class,request.getNumeroCommercant(),null);
			if (commercant == null) {
				resp.setCode(ErrorResponse.MERCHANT_NOT_FOUND.getCode());
				resp.setMessage(ErrorResponse.MERCHANT_NOT_FOUND.getMessage(""));
				log.info(resp.getMessage());
				return resp;
			}
			if(!BEConstantes.PARTNER_ACCEPTEUR.contains(commercant.getPType())) {
				resp.setCode(ErrorResponse.PARENT_NOT_ALLOWED.getCode());
				resp.setMessage(ErrorResponse.PARENT_NOT_ALLOWED.getMessage(""));
				log.info(resp.getMessage());
				return resp;
			}
				
			PointDeVente pointDeVente = new PointDeVente();
			pointDeVente.setNumeroPointDeVente(generationNumero(request.getNumeroCommercant().toString(), session, SysVar.TailleComplementNumero.POINTDEVENTE));
			pointDeVente.setNom(request.getNom());
			pointDeVente.setAdresse(request.getAdresse());
			pointDeVente.setTelephone(request.getTelephone());
			pointDeVente.setCommercant(commercant);
			Utilisateur user = session.findObjectById(Utilisateur.class, idUser, null);
			pointDeVente.setDateCreation(new Date());
			pointDeVente.setDateLastModification(pointDeVente.getDateCreation());
			pointDeVente.setTypeOperation(SysVar.TypeOperation.INSERT.name());
			pointDeVente.setUserCreation(user);pointDeVente.setUserLastModification(user);
			pointDeVente.setMarchandEcommercant(request.getMarchandEcommercant());
			pointDeVente =(PointDeVente)session.saveOrUpdateObject(pointDeVente);
			log.info("id "+pointDeVente.getNumeroPointDeVente());
			resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			resp.setPosNumber(pointDeVente.getNumeroPointDeVente());
			resp.setMerchantNumber(pointDeVente.getCommercant().getIdPartner().toString());
			resp.setMerchantName(pointDeVente.getCommercant().getName());
			if("YES".equalsIgnoreCase(request.getMarchandEcommercant()))
			{
				CaisseDto caisseRequest = new CaisseDto();
				caisseRequest.setNom("CAISSE "+request.getNom());
				caisseRequest.setPointDeVente(pointDeVente.getNumeroPointDeVente());
				caisseRequest.setTypeCaisse("WEB");
				caisseRequest.setTelephone(request.getTelephone());
			    PosResponse caisseVirtuel = caisseService.create(idUser, caisseRequest);
			    resp.setTerminalNumber(caisseVirtuel.getTerminalNumber());
			    resp.setSerialNumber(caisseVirtuel.getSerialNumber());
			    }
			log.info("............... [FIN] CREATION DE POINT DE VENTE");
		} 
		catch (Exception e) 
		{
			log.info("errorcreatePointDeVente", e);
			resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			resp.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return resp;
	}
	
	private String generationNumero(String prefix, Session session, int longComplement)
	{
		Integer nb = 0;
		switch (longComplement) 
		{
			case SysVar.TailleComplementNumero.COMMERCANT:{
				nb = session.executeNamedQuerySingle(Integer.class,"Commercant.CountCommercant", new String[] { "numeroCommercant" }, new String[] { prefix + "%" });
			break ;
			}
			
			case SysVar.TailleComplementNumero.POINTDEVENTE:
			{
				List<PointDeVente> list = session.executeNamedQueryList(PointDeVente.class,"PointDeVente.CountPointDeVente", new String[] { NUM_POS }, new String[] { prefix + "%" });
				nb = list ==null ||list.isEmpty()?0:list.size();
				break ;
			}
			case SysVar.TailleComplementNumero.CAISSE:{
				List<Caisse> list = session.executeNamedQueryList(Caisse.class,"Caisse.CountCaisse", new String[] { NUM_POS }, new String[] { prefix + "%" });
				nb = list ==null ||list.isEmpty()?0:list.size();
				break ;
			}
			default :nb =0;break;
		}
		
		return prefix + String.format("%0$" + longComplement + "s", nb + 1).replace(' ', '0');
	}

	
	public List<PosDto> listPosByMcc(String mcc) {
	
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		
		List<PointDeVente> lpos =  session.executeNamedQueryList(PointDeVente.class,"Pos.findByMcc",
				new String[] {"mcc"}, new String[] {mcc});
		List<PosDto> reponse = new ArrayList<>();
		lpos.forEach(p->reponse.add(new PosDto(p)));
		return reponse;
	}
	
	public List<PosDto> listMerchantByMcc(String mcc) {
		
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		
		List<Partner> lpos =  session.executeNamedQueryList(Partner.class,"findByPartnerByTypeAndMcc",
				new String[] {"type","mcc"}, new String[] {BEConstantes.ESPACE_ACCEPTEUR,mcc});
		List<PosDto> reponse = new ArrayList<>();
		lpos.forEach(p->{
			PosDto dto = new PosDto();
				dto.setNom(p.getName());
		dto.setTelephone(p.getTelephoneContact());
		dto.setNumeroCommercant(p.getIdPartner());
		dto.setRaisonSocialeCommercant(p.getName());
		reponse.add(dto);});
		
		
		return reponse;
	}

}
