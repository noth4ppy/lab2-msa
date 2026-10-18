# Книжный каталог — ЛР №1 (CRUD на Spring Boot) и ЛР №2 (юнит-тестирование)

ЛР №1 — CRUD на Spring Boot, JUnit, Lombok, MapStruct. ЛР №2 — юнит-тестирование этого же
проекта с JUnit 5 и Mockito (раздел «Тесты ЛР №2» ниже).

Предметная область — «Книжный каталог»: книги (`Book`) и авторы (`Author`).
REST API для книг `/api/books`: CRUD через DTO и MapStruct-маппер, логика в сервисе
(интерфейс `BookService`, реализация `service/impl/BookServiceImpl`), частичное обновление (PUT/PATCH с `JsonNullable`), обработка 404,
валидация (400), поиск книг по автору (`GET /api/books?authorId=...`).

Стек: Java 17+, Spring Boot 3.5, Maven, Spring Data JPA, H2 (in-memory), Lombok,
MapStruct, jackson-databind-nullable; тесты — JUnit 5, MockMvc, Mockito, Instancio,
Datafaker, JsonUnit.

## Запуск

```bash
mvn spring-boot:run        # приложение на http://localhost:8081
mvn test                   # интеграционные + модульные тесты (ЛР1 и ЛР2)
```

Порт задан в `src/main/resources/application.properties` (`server.port=8081`).

## Консоль H2

- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:books`
- Пользователь: `sa`, пароль — пустой

Демо-данные (`src/main/resources/data.sql`): авторы `id=1` (Leo Tolstoy) и `id=2`
(Fyodor Dostoevsky), книга `crime-and-punishment` автора `id=2`.

## API

| Метод | Путь | Описание | Ответ |
|-------|------|----------|-------|
| GET | `/api/books` | список книг | 200 |
| GET | `/api/books?authorId={id}` | книги автора | 200 |
| POST | `/api/books` | создание (`BookCreateDTO`) | 201 / 400 / 404 (нет автора) / 409 (слаг занят) |
| GET | `/api/books/{id}` | книга по id | 200 / 404 |
| PUT | `/api/books/{id}` | частичное обновление (`BookUpdateDTO`) | 200 / 400 / 404 |
| PATCH | `/api/books/{id}` | частичное обновление (`BookUpdateDTO`) | 200 / 400 / 404 |
| DELETE | `/api/books/{id}` | удаление | 204 / 404 |

## Демонстрация (curl)

```bash
# 1. Список книг
curl -s http://localhost:8081/api/books

# 2. Создание книги (201 Created)
curl -s -i -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"authorId":1,"slug":"war-and-peace","title":"War and Peace","description":"Epic novel","publishedYear":1869}'

# 3. Список книг (две книги)
curl -s http://localhost:8081/api/books

# 4. Книга по id (id из ответа шага 2, обычно 2)
curl -s http://localhost:8081/api/books/2

# 5. Частичное обновление: меняется только title (PUT), затем только publishedYear (PATCH)
curl -s -X PUT http://localhost:8081/api/books/2 \
  -H "Content-Type: application/json" -d '{"title":"War and Peace (2nd ed.)"}'
curl -s -X PATCH http://localhost:8081/api/books/2 \
  -H "Content-Type: application/json" -d '{"publishedYear":1873}'

# 6. Поиск книг по автору
curl -s "http://localhost:8081/api/books?authorId=1"

# 7. Ошибка валидации (400)
curl -s -i -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" -d '{"slug":""}'

# 8. Удаление (204 No Content)
curl -s -i -X DELETE http://localhost:8081/api/books/2

# 9. Несуществующая книга (404 "Not Found: 2")
curl -s -i http://localhost:8081/api/books/2
```

Слаг уникален: повторное создание книги с тем же `slug` вернёт 409 Conflict.

## Тесты ЛР №2 (JUnit 5 + Mockito)

Тесты используют отдельную конфигурацию `src/test/resources/application.properties`:
in-memory H2 `jdbc:h2:mem:books_test`, `ddl-auto=create-drop` — рабочая БД `books` не
затрагивается. `data.sql` выполняется и в тестах (авторы `id=1, 2`, книга `id=1`), поэтому
тесты не опираются на фиксированные id. `@DataJpaTest` по умолчанию подменяет источник данных
встроенной H2 с уникальным именем.

| Класс | Слой | Средства | Тестов |
|-------|------|----------|--------|
| `repository/BookRepositoryUnitTests` | репозиторий | `@DataJpaTest`, AssertJ, `@TestMethodOrder`/`@Order`, `@Rollback(false)`, `@DisplayName`; `@Import(JpaAuditingConfig.class)` для аудита дат | 8 |
| `controller/BookControllerUnitTests` | контроллер | `@WebMvcTest(BooksController.class)`, MockMvc, `@MockitoBean BookService`, BDDMockito `given`/`willDoNothing`, `jsonPath` + Hamcrest `is`; `@Import(JacksonConfig.class)` | 9 |
| `service/BookServiceUnitTests` | сервис | `@ExtendWith(MockitoExtension.class)`, `@Mock` репозитории и маппер, `@InjectMocks BookServiceImpl`, BDDMockito, `verify(…, times(1))` | 10 |
| `controller/BooksControllerTest` (ЛР1) | интеграционные | `@SpringBootTest`, MockMvc, Instancio, Datafaker, JsonUnit | 16 |
| `service/BookServiceTest` (ЛР1) | сервис | Mockito, `@InjectMocks BookServiceImpl` | 11 |

В каждом классе ЛР2 первые 5 тестов — позитивные сценарии по методичке (создание, по id,
список, обновление, удаление), остальные — негативные (не найдено → `ResourceNotFoundException`
/ 404, неизвестный автор, ошибка валидации → 400). Репозиторий дополнительно проверяет
`findBySlug` и `findByAuthorId`. `DELETE` в контроллере возвращает 204 No Content.

Аудит дат (`@EnableJpaAuditing`) вынесен в `config/JpaAuditingConfig`: если оставить его на
главном классе, срез `@WebMvcTest` падает с «JPA metamodel must not be empty».

Запуск:

```bash
mvn test                                        # все тесты (54)
mvn test -Dtest=BookRepositoryUnitTests         # только репозиторий
mvn test -Dtest=BookControllerUnitTests         # только контроллер
mvn test -Dtest=BookServiceUnitTests            # только сервис
```

## Структура проекта

```
project/
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java/io/hexlet/spring
    │   │   ├── Lab1Application.java          # точка входа
    │   │   ├── config/JacksonConfig.java     # регистрация JsonNullableModule
    │   │   ├── config/JpaAuditingConfig.java # @EnableJpaAuditing (аудит createdAt/updatedAt)
    │   │   ├── controller/BooksController.java
    │   │   ├── dto/BookCreateDTO.java, BookUpdateDTO.java, BookDTO.java
    │   │   ├── exception/ResourceNotFoundException.java, GlobalExceptionHandler.java
    │   │   ├── mapper/BookMapper.java, JsonNullableMapper.java, ReferenceMapper.java
    │   │   ├── model/BaseEntity.java, Book.java, Author.java
    │   │   ├── repository/BookRepository.java, AuthorRepository.java
    │   │   ├── service/BookService.java      # интерфейс сервиса
    │   │   └── service/impl/BookServiceImpl.java
    │   └── resources/application.properties, data.sql
    └── test
        ├── java/io/hexlet/spring
        │   ├── config/TestConfig.java                    # бин Faker
        │   ├── controller/BooksControllerTest.java       # интеграционные (ЛР1)
        │   ├── controller/BookControllerUnitTests.java   # @WebMvcTest (ЛР2)
        │   ├── repository/BookRepositoryUnitTests.java   # @DataJpaTest (ЛР2)
        │   ├── service/BookServiceTest.java              # Mockito (ЛР1)
        │   └── service/BookServiceUnitTests.java         # Mockito + BDDMockito (ЛР2)
        └── resources/application.properties             # тестовая БД H2 books_test
```
