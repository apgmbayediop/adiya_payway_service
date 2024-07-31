package sn.adiya.common.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.fig.common.utils.AbstractResponse;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(value = Include.NON_NULL)
public class ListResponse<E> extends AbstractResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 449767204156304553L;
	private List<E> data;
	
	private Long totalPages;
	private Long totalElements;
	private Long currentPage;
}
