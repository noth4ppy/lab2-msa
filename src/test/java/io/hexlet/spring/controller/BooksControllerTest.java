package io.hexlet.spring.controller;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.hexlet.spring.config.TestConfig;
import io.hexlet.spring.dto.BookCreateDTO;
import io.hexlet.spring.dto.BookDTO;
import io.hexlet.spring.model.Author;
import io.hexlet.spring.model.Book;
import io.hexlet.spring.repository.AuthorRepository;
import io.hexlet.spring.repository.BookRepository;
import net.datafaker.Faker;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class BooksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private Faker faker;

    private Author testAuthor;
    private Book testBook;

    @BeforeEach
    public void setUp() {
        testAuthor = createAuthor();

        testBook = Instancio.of(Book.class)
                .ignore(Select.field(Book::getId))
                .ignore(Select.field(Book::getCreatedAt))
                .ignore(Select.field(Book::getUpdatedAt))
                .supply(Select.field(Book::getTitle), () -> faker.book().title())
                .supply(Select.field(Book::getSlug), () -> "book-" + faker.internet().uuid())
                .supply(Select.field(Book::getDescription), () -> faker.lorem().paragraph())
                .supply(Select.field(Book::getPublishedYear), () -> faker.number().numberBetween(1800, 2025))
                .supply(Select.field(Book::getAuthor), () -> testAuthor)
                .create();
    }

    private Author createAuthor() {
        var author = Instancio.of(Author.class)
                .ignore(Select.field(Author::getId))
                .supply(Select.field(Author::getFirstName), () -> faker.name().firstName())
                .supply(Select.field(Author::getLastName), () -> faker.name().lastName())
                .supply(Select.field(Author::getEmail), () -> faker.internet().uuid() + "@example.com")
                .create();
        return authorRepository.save(author);
    }

    @Test
    public void testIndex() throws Exception {
        bookRepository.save(testBook);

        var body = mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isArray();
        assertThatJson(body).inPath("$[*].slug").isArray().contains(testBook.getSlug());
    }

    @Test
    public void testIndexByAuthor() throws Exception {
        bookRepository.save(testBook);
        var otherAuthor = createAuthor();
        var otherBook = Instancio.of(Book.class)
                .ignore(Select.field(Book::getId))
                .ignore(Select.field(Book::getCreatedAt))
                .ignore(Select.field(Book::getUpdatedAt))
                .supply(Select.field(Book::getSlug), () -> "other-" + faker.internet().uuid())
                .supply(Select.field(Book::getPublishedYear), () -> 1999)
                .supply(Select.field(Book::getAuthor), () -> otherAuthor)
                .create();
        bookRepository.save(otherBook);

        var body = mockMvc.perform(get("/api/books").param("authorId", testAuthor.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).isArray().hasSize(1);
        assertThatJson(body).node("[0].slug").isEqualTo(testBook.getSlug());
        assertThatJson(body).node("[0].authorId").isEqualTo(testAuthor.getId());
    }

    @Test
    public void testCreate() throws Exception {
        var dto = new BookCreateDTO();
        dto.setAuthorId(testAuthor.getId());
        dto.setSlug(testBook.getSlug());
        dto.setTitle(testBook.getTitle());
        dto.setDescription(testBook.getDescription());
        dto.setPublishedYear(testBook.getPublishedYear());

        var request = post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto));

        var body = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("id").isPresent(),
                v -> v.node("slug").isEqualTo(dto.getSlug()),
                v -> v.node("authorId").isEqualTo(testAuthor.getId()),
                v -> v.node("createdAt").isPresent(),
                v -> v.node("updatedAt").isPresent()
        );

        var book = bookRepository.findBySlug(dto.getSlug()).orElseThrow();
        assertThat(book.getTitle()).isEqualTo(dto.getTitle());
        assertThat(book.getPublishedYear()).isEqualTo(dto.getPublishedYear());
        assertThat(book.getAuthor().getId()).isEqualTo(testAuthor.getId());
        assertThat(book.getCreatedAt()).isNotNull();
        assertThat(book.getUpdatedAt()).isNotNull();
    }

    @Test
    public void testCreateInvalid() throws Exception {
        var data = Map.of("slug", "", "publishedYear", 99999);

        var request = post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        var body = mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("title").isPresent(),
                v -> v.node("slug").isPresent(),
                v -> v.node("authorId").isPresent(),
                v -> v.node("publishedYear").isPresent()
        );
    }

    @Test
    public void testCreateWithUnknownAuthor() throws Exception {
        var dto = new BookCreateDTO();
        dto.setAuthorId(999_999L);
        dto.setSlug(testBook.getSlug());
        dto.setTitle(testBook.getTitle());
        dto.setPublishedYear(2000);

        var request = post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto));

        mockMvc.perform(request)
                .andExpect(status().isNotFound());

        assertThat(bookRepository.findBySlug(dto.getSlug())).isEmpty();
    }

    @Test
    public void testCreateDuplicateSlug() throws Exception {
        bookRepository.save(testBook);
        var dto = new BookCreateDTO();
        dto.setAuthorId(testAuthor.getId());
        dto.setSlug(testBook.getSlug());
        dto.setTitle("Duplicate");
        dto.setPublishedYear(2000);

        var request = post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto));

        mockMvc.perform(request)
                .andExpect(status().isConflict());
    }

    @Test
    public void testShow() throws Exception {
        bookRepository.save(testBook);

        var body = mockMvc.perform(get("/api/books/" + testBook.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("id").isEqualTo(testBook.getId()),
                v -> v.node("slug").isEqualTo(testBook.getSlug()),
                v -> v.node("title").isEqualTo(testBook.getTitle()),
                v -> v.node("description").isEqualTo(testBook.getDescription()),
                v -> v.node("publishedYear").isEqualTo(testBook.getPublishedYear()),
                v -> v.node("authorId").isEqualTo(testAuthor.getId())
        );
    }

    @Test
    public void testShowNotFound() throws Exception {
        var body = mockMvc.perform(get("/api/books/999999"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo("Not Found: 999999");
    }

    @Test
    public void testUpdate() throws Exception {
        bookRepository.save(testBook);

        var data = Map.of("title", "new title");

        var request = put("/api/books/" + testBook.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isOk());

        var book = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(book.getTitle()).isEqualTo("new title");
        // fields absent from the request are not changed
        assertThat(book.getSlug()).isEqualTo(testBook.getSlug());
        assertThat(book.getDescription()).isEqualTo(testBook.getDescription());
        assertThat(book.getPublishedYear()).isEqualTo(testBook.getPublishedYear());
        assertThat(book.getAuthor().getId()).isEqualTo(testAuthor.getId());
    }

    @Test
    public void testPatch() throws Exception {
        bookRepository.save(testBook);
        var newAuthor = createAuthor();

        var data = new HashMap<String, Object>();
        data.put("publishedYear", 2001);
        data.put("authorId", newAuthor.getId());
        data.put("description", null);

        var request = patch("/api/books/" + testBook.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        var body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("publishedYear").isEqualTo(2001),
                v -> v.node("authorId").isEqualTo(newAuthor.getId()),
                v -> v.node("title").isEqualTo(testBook.getTitle())
        );

        var book = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(book.getPublishedYear()).isEqualTo(2001);
        assertThat(book.getAuthor().getId()).isEqualTo(newAuthor.getId());
        assertThat(book.getDescription()).isNull();
        assertThat(book.getTitle()).isEqualTo(testBook.getTitle());
    }

    @Test
    public void testUpdateRefreshesUpdatedAtInResponse() throws Exception {
        bookRepository.save(testBook);
        var saved = bookRepository.findById(testBook.getId()).orElseThrow();
        var previousUpdatedAt = saved.getUpdatedAt();

        var putBody = mockMvc.perform(put("/api/books/" + testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("title", "put title"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var afterPut = om.readValue(putBody, BookDTO.class);

        assertThat(afterPut.getCreatedAt()).isNotNull();
        assertThat(afterPut.getUpdatedAt()).isAfter(afterPut.getCreatedAt());
        assertThat(afterPut.getUpdatedAt()).isAfter(previousUpdatedAt);

        var patchBody = mockMvc.perform(patch("/api/books/" + testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("title", "patch title"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var afterPatch = om.readValue(patchBody, BookDTO.class);

        assertThat(afterPatch.getCreatedAt()).isEqualTo(afterPut.getCreatedAt());
        assertThat(afterPatch.getUpdatedAt()).isAfter(afterPut.getUpdatedAt());
    }

    @Test
    public void testUpdateInvalid() throws Exception {
        bookRepository.save(testBook);

        var request = patch("/api/books/" + testBook.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"\"}");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        var book = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(book.getTitle()).isEqualTo(testBook.getTitle());
    }

    @Test
    public void testUpdateWithUnknownAuthor() throws Exception {
        bookRepository.save(testBook);

        var request = put("/api/books/" + testBook.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"authorId\": 999999}");

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateNotFound() throws Exception {
        var request = put("/api/books/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("title", "x")));

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDestroy() throws Exception {
        bookRepository.save(testBook);

        mockMvc.perform(delete("/api/books/" + testBook.getId()))
                .andExpect(status().isNoContent());

        assertThat(bookRepository.existsById(testBook.getId())).isFalse();
    }

    @Test
    public void testDestroyNotFound() throws Exception {
        mockMvc.perform(delete("/api/books/999999"))
                .andExpect(status().isNotFound());
    }
}
