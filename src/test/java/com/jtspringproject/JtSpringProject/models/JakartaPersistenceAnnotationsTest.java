package com.jtspringproject.JtSpringProject.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JakartaPersistenceAnnotationsTest {

	static Stream<Class<?>> entities() {
		return Stream.of(User.class, Product.class, Category.class, Cart.class, CartProduct.class);
	}

	@ParameterizedTest
	@MethodSource("entities")
	void entityIsAnnotatedWithJakartaEntity(Class<?> entity) {
		jakarta.persistence.Entity annotation = entity.getAnnotation(jakarta.persistence.Entity.class);
		assertNotNull(annotation, entity.getSimpleName() + " must carry jakarta.persistence.Entity");
		assertEquals("jakarta.persistence", annotation.annotationType().getPackageName());
	}

	@ParameterizedTest
	@MethodSource("entities")
	void entityHasJakartaIdOrEmbeddedId(Class<?> entity) {
		boolean hasId = Arrays.stream(entity.getDeclaredFields())
				.anyMatch(f -> f.isAnnotationPresent(jakarta.persistence.Id.class)
						|| f.isAnnotationPresent(jakarta.persistence.EmbeddedId.class));
		assertTrue(hasId, entity.getSimpleName() + " must declare a jakarta @Id or @EmbeddedId");
	}

	@ParameterizedTest
	@MethodSource("entities")
	void noJavaxPersistenceAnnotationsRemain(Class<?> entity) {
		Stream<Annotation> classLevel = Arrays.stream(entity.getAnnotations());
		Stream<Annotation> fieldLevel = Arrays.stream(entity.getDeclaredFields())
				.flatMap((Field f) -> Arrays.stream(f.getAnnotations()));
		Stream.concat(classLevel, fieldLevel).forEach(a -> assertFalse(
				a.annotationType().getName().startsWith("javax.persistence"),
				entity.getSimpleName() + " still uses " + a.annotationType().getName()));
	}

	@Test
	void userTableAnnotationIsJakarta() {
		assertTrue(User.class.isAnnotationPresent(jakarta.persistence.Table.class));
	}

	@Test
	void cartProductTableAndEmbeddableAreJakarta() {
		jakarta.persistence.Table table = CartProduct.class.getAnnotation(jakarta.persistence.Table.class);
		assertNotNull(table);
		assertEquals("CART_PRODUCT", table.name());
		assertTrue(CartProductId.class.isAnnotationPresent(jakarta.persistence.Embeddable.class));
	}
}
