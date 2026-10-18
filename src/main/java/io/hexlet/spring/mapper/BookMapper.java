package io.hexlet.spring.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import io.hexlet.spring.dto.BookCreateDTO;
import io.hexlet.spring.dto.BookDTO;
import io.hexlet.spring.dto.BookUpdateDTO;
import io.hexlet.spring.model.Book;

@Mapper(
        uses = { JsonNullableMapper.class, ReferenceMapper.class },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class BookMapper {

    @Mapping(target = "author", source = "authorId")
    public abstract Book map(BookCreateDTO dto);

    @Mapping(source = "author.id", target = "authorId")
    public abstract BookDTO map(Book model);

    @Mapping(target = "author", source = "authorId")
    public abstract void update(BookUpdateDTO dto, @MappingTarget Book model);
}
