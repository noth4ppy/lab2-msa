package io.hexlet.spring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.hexlet.spring.model.Author;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
}
