package com.jtspringproject.JtSpringProject.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.jtspringproject.JtSpringProject.models.Category;
import com.jtspringproject.JtSpringProject.models.Product;

@SpringBootTest
@ActiveProfiles("test")
class ProductDaoTest {

	@Autowired
	private productDao productDao;

	@Autowired
	private categoryDao categoryDao;

	private Product newProduct(String name) {
		Category category = categoryDao.addCategory("cat-" + name);
		Product product = new Product();
		product.setName(name);
		product.setImage("img.png");
		product.setCategory(category);
		product.setQuantity(5);
		product.setPrice(100);
		product.setWeight(2);
		product.setDescription("desc");
		return product;
	}

	@Test
	void addProductPersistsWithCategory() {
		Product saved = productDao.addProduct(newProduct("Laptop"));

		assertTrue(saved.getId() > 0);
		Product loaded = productDao.getProduct(saved.getId());
		assertNotNull(loaded);
		assertEquals("Laptop", loaded.getName());
		assertNotNull(loaded.getCategory());
		assertEquals("cat-Laptop", loaded.getCategory().getName());
	}

	@Test
	void getProductsIncludesSavedProduct() {
		Product saved = productDao.addProduct(newProduct("Phone"));

		assertTrue(productDao.getProducts().stream().anyMatch(p -> p.getId() == saved.getId()));
	}

	@Test
	void updateProductChangesPersistedFields() {
		Product saved = productDao.addProduct(newProduct("Tablet"));
		saved.setPrice(250);
		saved.setQuantity(1);

		productDao.updateProduct(saved);

		Product loaded = productDao.getProduct(saved.getId());
		assertEquals(250, loaded.getPrice());
		assertEquals(1, loaded.getQuantity());
	}

	@Test
	void deleteProductRemovesRowAndReportsResult() {
		Product saved = productDao.addProduct(newProduct("Camera"));

		assertTrue(productDao.deleteProduct(saved.getId()));
		assertNull(productDao.getProduct(saved.getId()));
		assertFalse(productDao.deleteProduct(saved.getId()));
	}
}
