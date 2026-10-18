INSERT INTO authors (first_name, last_name, email)
VALUES ('Leo', 'Tolstoy', 'leo.tolstoy@example.com');
INSERT INTO authors (first_name, last_name, email)
VALUES ('Fyodor', 'Dostoevsky', 'fyodor.dostoevsky@example.com');
INSERT INTO books (title, slug, description, published_year, author_id, created_at, updated_at)
VALUES ('Crime and Punishment', 'crime-and-punishment', 'A novel about Raskolnikov.', 1866, 2,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
