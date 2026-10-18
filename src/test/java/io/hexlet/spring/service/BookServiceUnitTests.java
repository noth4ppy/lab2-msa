package io.hexlet.spring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

import io.hexlet.spring.dto.BookCreateDTO;
import io.hexlet.spring.dto.BookDTO;
import io.hexlet.spring.dto.BookUpdateDTO;
import io.hexlet.spring.exception.ResourceNotFoundException;
import io.hexlet.spring.mapper.BookMapper;
import io.hexlet.spring.model.Author;
import io.hexlet.spring.model.Book;
import io.hexlet.spring.repository.AuthorRepository;
import io.hexlet.spring.repository.BookRepository;
import io.hexlet.spring.service.impl.BookServiceImpl;

/**
 * Service layer unit tests: plain JUnit 5 + Mockito (no Spring context).
 * Repositories and the mapper are mocks, BookServiceImpl gets them through @InjectMocks.
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookServiceUnitTests {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Author author;
    private Book book;

    @BeforeEach
    public void setup() {
        author = new Author();
        author.setId(1L);
        author.setFirstName("Leo");
        author.setLastName("Tolstoy");
        author.setEmail("leo.tolstoy@example.com");

        book = new Book();
        book.setId(1L);
        book.setSlug("war-and-peace");
        book.setTitle("War and Peace");
        book.setDescription("Epic novel.");
        book.setPublishedYear(1869);
        book.setAuthor(author);
    }

    /** Stands in for the MapStruct mapper: builds the DTO from the current state of the entity. */
    private static BookDTO toDTO(Book model) {
        var dto = new BookDTO();
        dto.setId(model.getId());
        dto.setAuthorId(model.getAuthor().getId());
        dto.setSlug(model.getSlug());
        dto.setTitle(model.getTitle());
        dto.setDescription(model.getDescription());
        dto.setPublishedYear(model.getPublishedYear());
        return dto;
    }

    @Test
    @DisplayName("Test 1: Save Book Test")
    @Order(1)
    public void saveBookTest() {
        // precondition
        var createDTO = new BookCreateDTO();
        createDTO.setAuthorId(1L);
        createDTO.setSlug("war-and-peace");
        createDTO.setTitle("War and Peace");
        createDTO.setDescription("Epic novel.");
        createDTO.setPublishedYear(1869);
        given(authorRepository.existsById(1L)).willReturn(true);
        given(bookMapper.map(createDTO)).willReturn(book);
        given(bookRepository.save(book)).willReturn(book);
        given(bookMapper.map(book)).willAnswer(invocation -> toDTO(book));

        // action
        var savedBook = bookService.create(createDTO);

        // verify
        System.out.println(savedBook);
        assertThat(savedBook).isNotNull();
        assertThat(savedBook.getSlug()).isEqualTo("war-and-peace");
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    @DisplayName("Test 2: Get Book By Id Test")
    @Order(2)
    public void getBookById() {
        // precondition
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookMapper.map(book)).willAnswer(invocation -> toDTO(book));

        // action
        var existingBook = bookService.findById(book.getId());

        // verify
        System.out.println(existingBook);
        assertThat(existingBook).isNotNull();
        assertThat(existingBook.getId()).isEqualTo(1L);
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Test 3: Get All Books Test")
    @Order(3)
    public void getAllBooks() {
        // precondition
        var otherBook = new Book();
        otherBook.setId(2L);
        otherBook.setSlug("anna-karenina");
        otherBook.setTitle("Anna Karenina");
        otherBook.setPublishedYear(1878);
        otherBook.setAuthor(author);
        given(bookRepository.findAll()).willReturn(List.of(book, otherBook));
        given(bookMapper.map(any(Book.class))).willAnswer(invocation -> toDTO(invocation.getArgument(0)));

        // action
        var books = bookService.getAll();

        // verify
        System.out.println(books);
        assertThat(books).isNotNull();
        assertThat(books.size()).isGreaterThan(1);
        assertThat(books).extracting(BookDTO::getSlug).containsExactly("war-and-peace", "anna-karenina");
        verify(bookRepository, never()).findByAuthorId(any());
    }

    @Test
    @DisplayName("Test 4: Update Book Test")
    @Order(4)
    public void updateBook() {
        // precondition
        var updateDTO = new BookUpdateDTO();
        updateDTO.setTitle(JsonNullable.of("War and Peace (2nd ed.)"));
        updateDTO.setPublishedYear(JsonNullable.of(1873));
        given(bookRepository.findById(book.getId())).willReturn(Optional.of(book));
        // the mocked mapper applies the present fields as the real MapStruct mapper does
        willAnswer(invocation -> {
            book.setTitle(updateDTO.getTitle().get());
            book.setPublishedYear(updateDTO.getPublishedYear().get());
            return null;
        }).given(bookMapper).update(updateDTO, book);
        given(bookRepository.saveAndFlush(book)).willReturn(book);
        given(bookMapper.map(book)).willAnswer(invocation -> toDTO(book));

        // action
        var updatedBook = bookService.update(updateDTO, book.getId());

        // verify
        System.out.println(updatedBook);
        assertThat(updatedBook.getTitle()).isEqualTo("War and Peace (2nd ed.)");
        assertThat(updatedBook.getPublishedYear()).isEqualTo(1873);
        assertThat(updatedBook.getSlug()).isEqualTo("war-and-peace");
        verify(bookMapper, times(1)).update(updateDTO, book);
        verify(bookRepository, times(1)).saveAndFlush(book);
    }

    @Test
    @DisplayName("Test 5: Delete Book Test")
    @Order(5)
    public void deleteBook() {
        // precondition
        given(bookRepository.existsById(book.getId())).willReturn(true);
        willDoNothing().given(bookRepository).deleteById(book.getId());

        // action
        bookService.delete(book.getId());

        // verify
        verify(bookRepository, times(1)).deleteById(book.getId());
    }

    @Test
    @DisplayName("Test 6: Get Books By Author Test")
    @Order(6)
    public void getBooksByAuthor() {
        // precondition
        given(bookRepository.findByAuthorId(1L)).willReturn(List.of(book));
        given(bookMapper.map(book)).willAnswer(invocation -> toDTO(book));

        // action
        var books = bookService.getAll(1L);

        // verify
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getAuthorId()).isEqualTo(1L);
        verify(bookRepository, times(1)).findByAuthorId(1L);
        verify(bookRepository, never()).findAll();
    }

    @Test
    @DisplayName("Test 7: Get Unknown Book Throws ResourceNotFoundException")
    @Order(7)
    public void getBookByIdNotFound() {
        // precondition
        given(bookRepository.findById(99L)).willReturn(Optional.empty());

        // action + verify
        assertThatThrownBy(() -> bookService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not Found: 99");
        verify(bookMapper, never()).map(any(Book.class));
    }

    @Test
    @DisplayName("Test 8: Save Book With Unknown Author Throws ResourceNotFoundException")
    @Order(8)
    public void saveBookUnknownAuthor() {
        // precondition
        var createDTO = new BookCreateDTO();
        createDTO.setAuthorId(42L);
        given(authorRepository.existsById(42L)).willReturn(false);

        // action + verify
        assertThatThrownBy(() -> bookService.create(createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Author Not Found: 42");
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Test 9: Update Book With Unknown Author Throws ResourceNotFoundException")
    @Order(9)
    public void updateBookUnknownAuthor() {
        // precondition
        var updateDTO = new BookUpdateDTO();
        updateDTO.setAuthorId(JsonNullable.of(42L));
        given(bookRepository.findById(book.getId())).willReturn(Optional.of(book));
        given(authorRepository.existsById(42L)).willReturn(false);

        // action + verify
        assertThatThrownBy(() -> bookService.update(updateDTO, book.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Author Not Found: 42");
        verify(bookMapper, never()).update(any(), any());
        verify(bookRepository, never()).saveAndFlush(any(Book.class));
    }

    @Test
    @DisplayName("Test 10: Delete Unknown Book Throws ResourceNotFoundException")
    @Order(10)
    public void deleteBookNotFound() {
        // precondition
        given(bookRepository.existsById(99L)).willReturn(false);

        // action + verify
        assertThatThrownBy(() -> bookService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not Found: 99");
        verify(bookRepository, never()).deleteById(any());
    }
}
