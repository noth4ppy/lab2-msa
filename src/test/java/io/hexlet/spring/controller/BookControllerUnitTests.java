package io.hexlet.spring.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.hexlet.spring.config.JacksonConfig;
import io.hexlet.spring.dto.BookCreateDTO;
import io.hexlet.spring.dto.BookDTO;
import io.hexlet.spring.dto.BookUpdateDTO;
import io.hexlet.spring.exception.ResourceNotFoundException;
import io.hexlet.spring.service.BookService;

/**
 * Controller layer unit tests: web slice (@WebMvcTest) with MockMvc, the service is a Mockito mock.
 * JacksonConfig is imported so that BookUpdateDTO (JsonNullable fields) is deserialized as in the app.
 */
@WebMvcTest(BooksController.class)
@Import(JacksonConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Autowired
    private ObjectMapper objectMapper;

    private BookCreateDTO createDTO;
    private BookDTO bookDTO;

    @BeforeEach
    public void setup() {
        createDTO = new BookCreateDTO();
        createDTO.setAuthorId(1L);
        createDTO.setSlug("war-and-peace");
        createDTO.setTitle("War and Peace");
        createDTO.setDescription("Epic novel.");
        createDTO.setPublishedYear(1869);

        bookDTO = new BookDTO();
        bookDTO.setId(1L);
        bookDTO.setAuthorId(1L);
        bookDTO.setSlug("war-and-peace");
        bookDTO.setTitle("War and Peace");
        bookDTO.setDescription("Epic novel.");
        bookDTO.setPublishedYear(1869);
    }

    @Test
    @DisplayName("Test 1: Create Book Test")
    @Order(1)
    public void saveBookTest() throws Exception {
        // precondition
        given(bookService.create(any(BookCreateDTO.class))).willReturn(bookDTO);

        // action
        var response = mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)));

        // verify
        response.andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(bookDTO.getId().intValue())))
                .andExpect(jsonPath("$.authorId", is(createDTO.getAuthorId().intValue())))
                .andExpect(jsonPath("$.slug", is(createDTO.getSlug())))
                .andExpect(jsonPath("$.title", is(createDTO.getTitle())))
                .andExpect(jsonPath("$.publishedYear", is(createDTO.getPublishedYear())));
        verify(bookService, times(1)).create(any(BookCreateDTO.class));
    }

    @Test
    @DisplayName("Test 2: Get List of Books Test")
    @Order(2)
    public void getBooksTest() throws Exception {
        // precondition
        var otherBook = new BookDTO();
        otherBook.setId(2L);
        otherBook.setAuthorId(2L);
        otherBook.setSlug("crime-and-punishment");
        otherBook.setTitle("Crime and Punishment");
        otherBook.setPublishedYear(1866);
        var books = List.of(bookDTO, otherBook);
        given(bookService.getAll(null)).willReturn(books);

        // action
        var response = mockMvc.perform(get("/api/books"));

        // verify
        response.andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.size()", is(books.size())))
                .andExpect(jsonPath("$[1].slug", is("crime-and-punishment")));
    }

    @Test
    @DisplayName("Test 3: Get Book By Id Test")
    @Order(3)
    public void getBookByIdTest() throws Exception {
        // precondition
        given(bookService.findById(bookDTO.getId())).willReturn(bookDTO);

        // action
        var response = mockMvc.perform(get("/api/books/{id}", bookDTO.getId()));

        // verify
        response.andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.id", is(bookDTO.getId().intValue())))
                .andExpect(jsonPath("$.slug", is(bookDTO.getSlug())))
                .andExpect(jsonPath("$.title", is(bookDTO.getTitle())))
                .andExpect(jsonPath("$.description", is(bookDTO.getDescription())));
    }

    @Test
    @DisplayName("Test 4: Update Book Test")
    @Order(4)
    public void updateBookTest() throws Exception {
        // precondition
        var updatedBook = new BookDTO();
        updatedBook.setId(bookDTO.getId());
        updatedBook.setAuthorId(bookDTO.getAuthorId());
        updatedBook.setSlug(bookDTO.getSlug());
        updatedBook.setTitle("War and Peace (2nd ed.)");
        updatedBook.setDescription(bookDTO.getDescription());
        updatedBook.setPublishedYear(1873);
        given(bookService.update(any(BookUpdateDTO.class), eq(bookDTO.getId()))).willReturn(updatedBook);

        var data = Map.of("title", "War and Peace (2nd ed.)", "publishedYear", 1873);

        // action
        var response = mockMvc.perform(put("/api/books/{id}", bookDTO.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)));

        // verify
        response.andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.title", is(updatedBook.getTitle())))
                .andExpect(jsonPath("$.publishedYear", is(updatedBook.getPublishedYear())))
                .andExpect(jsonPath("$.slug", is(bookDTO.getSlug())));
        verify(bookService, times(1)).update(any(BookUpdateDTO.class), eq(bookDTO.getId()));
    }

    @Test
    @DisplayName("Test 5: Delete Book Test")
    @Order(5)
    public void deleteBookTest() throws Exception {
        // precondition
        willDoNothing().given(bookService).delete(bookDTO.getId());

        // action
        var response = mockMvc.perform(delete("/api/books/{id}", bookDTO.getId()));

        // verify
        response.andExpect(status().isNoContent())
                .andDo(print());
        verify(bookService, times(1)).delete(bookDTO.getId());
    }

    @Test
    @DisplayName("Test 6: Get Unknown Book Returns 404")
    @Order(6)
    public void getBookByIdNotFoundTest() throws Exception {
        // precondition
        given(bookService.findById(99L)).willThrow(new ResourceNotFoundException("Not Found: 99"));

        // action
        var response = mockMvc.perform(get("/api/books/{id}", 99L));

        // verify
        response.andExpect(status().isNotFound())
                .andDo(print())
                .andExpect(content().string("Not Found: 99"));
    }

    @Test
    @DisplayName("Test 7: Create Invalid Book Returns 400")
    @Order(7)
    public void saveInvalidBookTest() throws Exception {
        // action
        var response = mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\": \"\", \"publishedYear\": 99999}"));

        // verify: validation fails before the service is called
        response.andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.slug").exists())
                .andExpect(jsonPath("$.authorId").exists())
                .andExpect(jsonPath("$.publishedYear").exists());
        verify(bookService, never()).create(any(BookCreateDTO.class));
    }

    @Test
    @DisplayName("Test 8: Create Book With Unknown Author Returns 404")
    @Order(8)
    public void saveBookUnknownAuthorTest() throws Exception {
        // precondition
        createDTO.setAuthorId(42L);
        given(bookService.create(any(BookCreateDTO.class)))
                .willThrow(new ResourceNotFoundException("Author Not Found: 42"));

        // action
        var response = mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)));

        // verify
        response.andExpect(status().isNotFound())
                .andDo(print())
                .andExpect(content().string("Author Not Found: 42"));
    }

    @Test
    @DisplayName("Test 9: Delete Unknown Book Returns 404")
    @Order(9)
    public void deleteBookNotFoundTest() throws Exception {
        // precondition
        willThrow(new ResourceNotFoundException("Not Found: 99")).given(bookService).delete(99L);

        // action
        var response = mockMvc.perform(delete("/api/books/{id}", 99L));

        // verify
        response.andExpect(status().isNotFound())
                .andDo(print());
    }
}
