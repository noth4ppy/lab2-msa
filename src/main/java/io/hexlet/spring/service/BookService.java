package io.hexlet.spring.service;

import java.util.List;

import io.hexlet.spring.dto.BookCreateDTO;
import io.hexlet.spring.dto.BookDTO;
import io.hexlet.spring.dto.BookUpdateDTO;

/** CRUD operations on books. Implementation: {@link io.hexlet.spring.service.impl.BookServiceImpl}. */
public interface BookService {

    /** All books, or only the books of the given author when authorId is not null. */
    List<BookDTO> getAll(Long authorId);

    List<BookDTO> getAll();

    /** Search books by author (GET /api/books?authorId=...). */
    List<BookDTO> findByAuthor(Long authorId);

    /** @throws io.hexlet.spring.exception.ResourceNotFoundException if the book does not exist */
    BookDTO findById(Long id);

    /** @throws io.hexlet.spring.exception.ResourceNotFoundException if the author does not exist */
    BookDTO create(BookCreateDTO bookData);

    /** Partial update: only fields present in bookData are changed. */
    BookDTO update(BookUpdateDTO bookData, Long id);

    /** @throws io.hexlet.spring.exception.ResourceNotFoundException if the book does not exist */
    void delete(Long id);
}
