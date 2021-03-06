package com.ethlo.dachs.test;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity
public class OrderLine
{
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;
    
    @OneToOne(cascade=CascadeType.ALL)
    @JoinColumn(name="product_id")
	private Product product;
	
	@ManyToOne
	@JoinColumn(name="order_id")
	private ProductOrder order;

	private int count;
	
	private int amount;
	
	public OrderLine withProduct(Product product)
	{
		this.product = product;
		return this;
	}
	
	public Integer getId()
	{
		return this.id;
	}
}
