package io.hexlet.spring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;

import io.hexlet.spring.config.JpaAuditingConfig;
import io.hexlet.spring.model.Author;
import io.hexlet.spring.model.Book;

/**
 * Repository layer unit tests: JPA slice (@DataJpaTest) on an embedded H2 database, JUnit 5 + AssertJ.
 * Tests run in a fixed order and share the saved book: its id is remembered after the first test,
 * because data.sql already contains a book and the generated id is not known in advance.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookRepositoryUnitTests {

    private static final String SLUG = "war-and-peace";

    private static Long bookId;
    private static Long authorId;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    @DisplayName("Test 1: Save Book Test")
    @Order(1)
    @Rollback(value = false)
    public void saveBookTest() {
        var author = new Author();
        author.setFirstName("Leo");
        author.setLastName("Tolstoy");
        author.setEmail("leo.tolstoy.repo-test@example.com");
        authorRepository.save(author);

        var book = new Book();
        book.setTitle("War and Peace");
        book.setSlug(SLUG);
        book.setDescription("Epic novel.");
        book.setPublishedYear(1869);
        book.setAuthor(author);

        bookRepository.save(book);

        bookId = book.getId();
        authorId = author.getId();

        assertThat(book.getId()).isGreaterThan(0);
        // auditing is enabled through JpaAuditingConfig
        assertThat(book.getCreatedAt()).isNotNull();
        assertThat(book.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Test 2: Get Book By Id Test")
    @Order(2)
    public void getBookTest() {
        var book = bookRepository.findById(bookId).orElseThrow();

        assertThat(book.getId()).isEqualTo(bookId);
        assertThat(book.getSlug()).isEqualTo(SLUG);
        assertThat(book.getAuthor().getId()).isEqualTo(authorId);
    }

    @Test
    @DisplayName("Test 3: Get List of Books Test")
    @Order(3)
    public void getListOfBooksTest() {
        var books = bookRepository.findAll();

        assertThat(books.size()).isGreaterThan(0);
        assertThat(books).extracting(Book::getSlug).contains(SLUG);
    }

    @Test
    @DisplayName("Test 4: Find Book By Slug Test")
    @Order(4)
    public void findBySlugTest() {
        var book = bookRepository.findBySlug(SLUG);

        assertThat(book).isPresent();
        assertThat(book.get().getId()).isEqualTo(bookId);
        assertThat(book.get().getTitle()).isEqualTo("War and Peace");
    }

    @Test
    @DisplayName("Test 5: Find Books By Author Id Test")
    @Order(5)
    public void findByAuthorIdTest() {
        var books = bookRepository.findByAuthorId(authorId);

        assertThat(books).hasSize(1);
        assertThat(books.get(0).getSlug()).isEqualTo(SLUG);
    }

    @Test
    @DisplayName("Test 6: Update Book Test")
    @Order(6)
    @Rollback(value = false)
    public void updateBookTest() {
        var book = bookRepository.findById(bookId).orElseThrow();
        book.setTitle("War and Peace (2nd ed.)");
        book.setSlug("war-and-peace-2nd-ed");

        var bookUpdated = bookRepository.saveAndFlush(book);

        assertThat(bookUpdated.getTitle()).isEqualTo("War and Peace (2nd ed.)");
        assertThat(bookUpdated.getSlug()).isEqualTo("war-and-peace-2nd-ed");
        assertThat(bookRepository.findBySlug(SLUG)).isEmpty();
    }

    @Test
    @DisplayName("Test 7: Delete Book Test")
    @Order(7)
    @Rollback(value = false)
    public void deleteBookTest() {
        bookRepository.deleteById(bookId);

        var bookOptional = bookRepository.findById(bookId);

        assertThat(bookOptional).isEmpty();
    }

    @Test
    @DisplayName("Test 8: Find By Unknown Slug Returns Empty")
    @Order(8)
    public void findByUnknownSlugTest() {
        assertThat(bookRepository.findBySlug("no-such-book")).isEmpty();
        assertThat(bookRepository.findByAuthorId(999_999L)).isEmpty();
    }
}
