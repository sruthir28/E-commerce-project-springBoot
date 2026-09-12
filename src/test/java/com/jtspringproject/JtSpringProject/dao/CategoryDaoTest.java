package com.jtspringproject.JtSpringProject.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.jtspringproject.JtSpringProject.models.Category;

@SpringBootTest
@ActiveProfiles("test")
class CategoryDaoTest {

	@Autowired
	private categoryDao categoryDao;

	@Test
	void addCategoryPersistsAndAssignsId() {
		Category category = categoryDao.addCategory("Books");

		assertTrue(category.getId() > 0);
		assertEquals("Books", categoryDao.getCategory(category.getId()).getName());
	}

	@Test
	void getCategoriesIncludesSavedCategory() {
		Category category = categoryDao.addCategory("Toys");

		assertTrue(categoryDao.getCategories().stream().anyMatch(c -> c.getId() == category.getId()));
	}

	@Test
	void updateCategoryRenamesExistingCategory() {
		Category category = categoryDao.addCategory("Old");

		Category updated = categoryDao.updateCategory(category.getId(), "New");

		assertEquals("New", updated.getName());
		assertEquals("New", categoryDao.getCategory(category.getId()).getName());
	}

	@Test
	void updateCategoryReturnsNullForUnknownId() {
		assertNull(categoryDao.updateCategory(Integer.MAX_VALUE, "Nothing"));
	}

	@Test
	void deleteCategoryRemovesRowAndReportsResult() {
		Category category = categoryDao.addCategory("Temp");

		assertTrue(categoryDao.deleteCategory(category.getId()));
		assertNull(categoryDao.getCategory(category.getId()));
		assertFalse(categoryDao.deleteCategory(category.getId()));
	}
}
