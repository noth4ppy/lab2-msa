package io.hexlet.spring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;
    private BookDTO bookDTO;

    @BeforeEach
    public void setUp() {
        var author = new Author();
        author.setId(1L);
        author.setFirstName("Leo");
        author.setLastName("Tolstoy");
        author.setEmail("leo@example.com");

        book = new Book();
        book.setId(1L);
        book.setSlug("war-and-peace");
        book.setTitle("War and Peace");
        book.setDescription("Epic novel.");
        book.setPublishedYear(1869);
        book.setAuthor(author);

        bookDTO = new BookDTO();
        bookDTO.setId(1L);
        bookDTO.setAuthorId(1L);
        bookDTO.setSlug("war-and-peace");
        bookDTO.setTitle("War and Peace");
        bookDTO.setDescription("Epic novel.");
        bookDTO.setPublishedYear(1869);
    }

    @Test
    public void testGetAll() {
        when(bookRepository.findAll()).thenReturn(List.of(book));
        when(bookMapper.map(book)).thenReturn(bookDTO);

        var result = bookService.getAll(null);

        assertThat(result).containsExactly(bookDTO);
        verify(bookRepository, never()).findByAuthorId(any());
    }

    @Test
    public void testGetAllByAuthor() {
        when(bookRepository.findByAuthorId(1L)).thenReturn(List.of(book));
        when(bookMapper.map(book)).thenReturn(bookDTO);

        var result = bookService.getAll(1L);

        assertThat(result).containsExactly(bookDTO);
        verify(bookRepository).findByAuthorId(1L);
        verify(bookRepository, never()).findAll();
    }

    @Test
    public void testCreate() {
        var createDTO = new BookCreateDTO();
        createDTO.setAuthorId(1L);
        createDTO.setSlug("war-and-peace");
        createDTO.setTitle("War and Peace");
        createDTO.setPublishedYear(1869);
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(bookMapper.map(createDTO)).thenReturn(book);
        when(bookMapper.map(book)).thenReturn(bookDTO);

        var result = bookService.create(createDTO);

        verify(bookRepository).save(book);
        assertThat(result).isEqualTo(bookDTO);
    }

    @Test
    public void testCreateAuthorNotFound() {
        var createDTO = new BookCreateDTO();
        createDTO.setAuthorId(42L);
        when(authorRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.create(createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Author Not Found: 42");
        verify(bookRepository, never()).save(any());
    }

    @Test
    public void testFindById() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.map(book)).thenReturn(bookDTO);

        var result = bookService.findById(1L);

        assertThat(result.getSlug()).isEqualTo("war-and-peace");
    }

    @Test
    public void testFindByIdNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not Found: 99");
    }

    @Test
    public void testUpdate() {
        var updateDTO = new BookUpdateDTO();
        updateDTO.setTitle(JsonNullable.of("new title"));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.map(book)).thenReturn(bookDTO);

        var result = bookService.update(updateDTO, 1L);

        verify(bookMapper).update(updateDTO, book);
        verify(bookRepository).saveAndFlush(book);
        assertThat(result).isEqualTo(bookDTO);
    }

    @Test
    public void testUpdateNotFound() {
        var updateDTO = new BookUpdateDTO();
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(updateDTO, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not Found: 99");
        verify(bookRepository, never()).saveAndFlush(any());
    }

    @Test
    public void testUpdateAuthorNotFound() {
        var updateDTO = new BookUpdateDTO();
        updateDTO.setAuthorId(JsonNullable.of(42L));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(authorRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.update(updateDTO, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Author Not Found: 42");
        verify(bookMapper, never()).update(any(), any());
        verify(bookRepository, never()).saveAndFlush(any());
    }

    @Test
    public void testDelete() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.delete(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    public void testDeleteNotFound() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Not Found: 99");
        verify(bookRepository, never()).deleteById(any());
    }
}
