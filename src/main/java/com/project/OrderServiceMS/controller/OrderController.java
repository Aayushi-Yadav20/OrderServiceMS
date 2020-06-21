package com.project.OrderServiceMS.controller;


import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.client.RestTemplate;

import com.project.OrderServiceMS.DTO.OrderDetailsDTO;
import com.project.OrderServiceMS.DTO.ProductId;
import com.project.OrderServiceMS.DTO.ProductsOrderedDTO;
import com.project.OrderServiceMS.service.OrderService;

@RestController
public class OrderController {
	
	@Autowired
	private OrderService orderservice;
	@Autowired
	public RestTemplate restTemplate;
	@Value("${userServiceUrl}")
	public String cartServiceUrl;
	@Value("${productServiceUrl}")
	public String productServiceUrl;
	
	@PostMapping("/orders/placeOrder")
	public String placeOrder(@RequestBody OrderDetailsDTO order) {
		
		String cartServiceUrl1=cartServiceUrl+"cart/checkout/"+order.getBuyerId();		
		try {
        ResponseEntity<ProductsOrderedDTO[]> responseEntity = restTemplate.getForEntity(cartServiceUrl1, ProductsOrderedDTO[].class);   
        ProductsOrderedDTO[] objects = responseEntity.getBody();

		// Product Service needs array of product Ids
		List<Integer> prodIds= new ArrayList<Integer>();
		for(int i=0;i<objects.length;i++) {prodIds.add(objects[i].getProdId());}
		
		// Populating that array in ProductId object to POST a request
		ProductId prodId1=new ProductId();
		prodId1.setProdId(prodIds);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<ProductId> request = new HttpEntity<ProductId>(prodId1, headers);
		
		// Contacting the Product Service, receives an array of Product Objects
		ResponseEntity<ProductsOrderedDTO[]> products1 = restTemplate.postForEntity(productServiceUrl,request, ProductsOrderedDTO[].class);
		ProductsOrderedDTO[] products12 = products1.getBody();
		ArrayList<ProductsOrderedDTO> products=new ArrayList<ProductsOrderedDTO>();
		for(int i=0;i<products12.length;i++) {
			products.add(products12[i]);
		}
		// To set the quantity, received from the Cart, into the corresponding product Objects
		products.forEach(element->{
			for(int j=0;j<objects.length;j++) {
				if(objects[j].getProdId().equals(element.getProdId())) {
					element.setQuantity(objects[j].getQuantity());
				}
			}
		});
		order.setOrderedProducts(products);
		
		orderservice.placeOrder(order);
		}
		catch(Exception e){
			e.printStackTrace();
			return "Error in placing the order! Contact your Admin";
		}
		return "Order placed Sucessfully";
	}
	
	@GetMapping(value="/orders/{buyerId}", produces= MediaType.APPLICATION_JSON_VALUE)
	public ArrayList <OrderDetailsDTO> getAllOrders(@PathVariable("buyerId") Integer buyerId ) {
		return orderservice.getAllOrders(buyerId);	
	}
	
	@DeleteMapping(value="/orders/delete/{orderId}", consumes= MediaType.APPLICATION_JSON_VALUE)
	public String deleteOrder(@PathVariable("orderId") Integer orderId ) {
		return orderservice.deleteOrder(orderId);
	}
	 @PutMapping(value="/orders/{prodId}/status")
	  public String updateStatus (@RequestBody ProductsOrderedDTO product) { 
	  Integer orderId=product.getOrderId(); 
	  Integer prodId=product.getProdId(); 
	  String status= product.getStatus(); 
	  return orderservice.updateStatus(orderId,prodId,status); 
	  }

	 
}