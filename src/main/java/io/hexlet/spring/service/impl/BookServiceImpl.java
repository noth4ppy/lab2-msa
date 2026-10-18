package io.hexlet.spring.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.hexlet.spring.dto.BookCreateDTO;
import io.hexlet.spring.dto.BookDTO;
import io.hexlet.spring.dto.BookUpdateDTO;
import io.hexlet.spring.exception.ResourceNotFoundException;
import io.hexlet.spring.mapper.BookMapper;
import io.hexlet.spring.model.Book;
import io.hexlet.spring.repository.AuthorRepository;
import io.hexlet.spring.repository.BookRepository;
import io.hexlet.spring.service.BookService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getAll(Long authorId) {
        return authorId == null ? getAll() : findByAuthor(authorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getAll() {
        return bookRepository.findAll().stream()
                .map(bookMapper::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> findByAuthor(Long authorId) {
        return bookRepository.findByAuthorId(authorId).stream()
                .map(bookMapper::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookDTO findById(Long id) {
        return bookMapper.map(getBook(id));
    }

    @Override
    @Transactional
    public BookDTO create(BookCreateDTO bookData) {
        ensureAuthorExists(bookData.getAuthorId());
        var book = bookMapper.map(bookData);
        bookRepository.save(book);
        return bookMapper.map(book);
    }

    @Override
    @Transactional
    public BookDTO update(BookUpdateDTO bookData, Long id) {
        var book = getBook(id);
        if (bookData.getAuthorId() != null && bookData.getAuthorId().isPresent()) {
            ensureAuthorExists(bookData.getAuthorId().get());
        }
        bookMapper.update(bookData, book);
        // flush so that @LastModifiedDate (set on @PreUpdate) is applied before the DTO is built
        bookRepository.saveAndFlush(book);
        return bookMapper.map(book);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Not Found: " + id);
        }
        bookRepository.deleteById(id);
    }

    private Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not Found: " + id));
    }

    private void ensureAuthorExists(Long authorId) {
        if (authorId == null || !authorRepository.existsById(authorId)) {
            throw new ResourceNotFoundException("Author Not Found: " + authorId);
        }
    }
}
