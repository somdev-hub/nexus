package com.nexus.core.service.implementations;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.model.entities.Product;
import com.nexus.core.payload.ProductDto;
import com.nexus.core.repository.ProductRepo;
import com.nexus.core.service.interfaces.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepo productRepo;
	private final ModelMapper modelMapper;
	@Override
	public ResponseEntity<?> addProduct(ProductDto product) {
		Product productMapped = modelMapper.map(product, Product.class);
		Product savedProduct = productRepo.save(productMapped);
		return new ResponseEntity<>(modelMapper.map(savedProduct, ProductDto.class), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getProductById(Long id) {
		Product product = productRepo.findByProductId(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", id));
		return new ResponseEntity<>(modelMapper.map(product, ProductDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getProductByIdAndOrg(Long id, Long orgId) {
		Product product = productRepo.findByProductIdAndOrg(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", id));
		return new ResponseEntity<>(modelMapper.map(product, ProductDto.class), HttpStatus.OK);
	}
	@Override
	public ResponseEntity<?> getAllProductsByOrgId(Long orgId, Pageable pageable) {
		Pageable safePageable = sanitizePageable(pageable);
		Page<Product> products = productRepo.findByOrg(orgId, safePageable);
		Page<ProductDto> productDtos = products.map(p -> modelMapper.map(p, ProductDto.class));
		return new ResponseEntity<>(productDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> updateProductByIdAndOrg(Long id, Long orgId, ProductDto dto) {
		Product product = productRepo.findByProductIdAndOrg(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", id));
		if (dto.getName() != null)
			product.setName(dto.getName());
		if (dto.getCode() != null)
			product.setCode(dto.getCode());
		if (dto.getDescription() != null)
			product.setDescription(dto.getDescription());
		if (dto.getPrice() != null)
			product.setPrice(dto.getPrice());
		if (dto.getSellingPrice() != null)
			product.setSellingPrice(dto.getSellingPrice());
		if (dto.getCost() != null)
			product.setCost(dto.getCost());
		if (dto.getTaxCharged() != null)
			product.setTaxCharged(dto.getTaxCharged());
		if (dto.getTaxPercentage() != null)
			product.setTaxPercentage(dto.getTaxPercentage());
		if (dto.getProductStatus() != null)
			product.setProductStatus(dto.getProductStatus());
		if (dto.getProductCategory() != null)
			product.setProductCategory(dto.getProductCategory());
		if (dto.getProductManager() != null)
			product.setProductManager(dto.getProductManager());
		if (dto.getSubCategory() != null)
			product.setSubCategory(dto.getSubCategory());
		if (dto.getBrand() != null)
			product.setBrand(dto.getBrand());
		if (dto.getUnitOfMeasure() != null)
			product.setUnitOfMeasure(dto.getUnitOfMeasure());
		if (dto.getCurrency() != null)
			product.setCurrency(dto.getCurrency());
		if (dto.getMinOrderQuantity() != null)
			product.setMinOrderQuantity(dto.getMinOrderQuantity());
		if (dto.getMaxOrderQuantity() != null)
			product.setMaxOrderQuantity(dto.getMaxOrderQuantity());
		if (dto.getLeadTimeDays() != null)
			product.setLeadTimeDays(dto.getLeadTimeDays());
		if (dto.getWeight() != null)
			product.setWeight(dto.getWeight());
		if (dto.getDimensions() != null)
			product.setDimensions(dto.getDimensions());
		if (dto.getBarcode() != null)
			product.setBarcode(dto.getBarcode());
		if (dto.getSku() != null)
			product.setSku(dto.getSku());
		if (dto.getTags() != null)
			product.setTags(dto.getTags());
		Product saved = productRepo.save(product);
		return new ResponseEntity<>(modelMapper.map(saved, ProductDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> deleteProductByIdAndOrg(Long id, Long orgId) {
		Product product = productRepo.findByProductIdAndOrg(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", id));
		productRepo.delete(product);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	private Pageable sanitizePageable(Pageable pageable) {
		if (pageable == null || pageable.isUnpaged()) {
			return pageable;
		}
		if (!pageable.getSort().isSorted()) {
			return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
		}
		for (Sort.Order order : pageable.getSort()) {
			if ("UNSORTED".equalsIgnoreCase(order.getProperty())) {
				return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
			}
		}
		return pageable;
	}

}
