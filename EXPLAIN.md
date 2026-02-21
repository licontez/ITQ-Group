Анализ запросов и индексов

CREATE INDEX idx_docs_status_created_at ON documents (status, created_at); — композитный индекс для выборки документов с сортировкой.

CREATE INDEX idx_docs_author ON documents (author); — индекс для фильтрации по автору.

CREATE INDEX idx_docs_created_at ON documents (created_at); — индекс для фильтрации по периоду.

1. Запрос воркера (получение пачки документов)
   Самый частый запрос в системе. Воркер ищет документы по статусу и сразу сортирует их по дате создания.

SQL:

SQL
SELECT d.id
FROM documents d
WHERE d.status = 'DRAFT'
ORDER BY d.created_at ASC
LIMIT 10;
EXPLAIN ANALYZE:

Plaintext
Limit  (cost=0.14..10.33 rows=10 width=24) (actual time=0.032..0.035 rows=10 loops=1)
->  Index Scan using idx_docs_status_created_at on documents d  (cost=0.14..81.64 rows=80 width=24) (actual time=0.031..0.033 rows=10 loops=1)
Index Cond: ((status)::text = 'DRAFT'::text)
Planning Time: 0.522 ms
Execution Time: 0.098 ms

Краткий вывод:
Благодаря композитному индексу idx_docs_status_created_at база использует Index Scan и читает уже отсортированные данные. В плане выполнения полностью отсутствует ресурсоемкий узел Sort (сортировка в памяти). Запрос отрабатывает за 0.098 ms, что гарантирует отсутствие нагрузки на БД при постоянном поллинге.

2. API поиска документов (по нескольким фильтрам)
   Пример запроса, который генерируется через JPA Specification при поиске.


SQL
SELECT * FROM documents d
WHERE d.status = 'APPROVED'
AND d.author = 'Author-1'
AND d.created_at >= '2026-02-01 00:00:00'
AND d.created_at <= '2026-02-28 23:59:59';
EXPLAIN ANALYZE:

Plaintext
Bitmap Heap Scan on documents d  (cost=4.30..15.80 rows=5 width=128) (actual time=0.045..0.050 rows=3 loops=1)
Recheck Cond: (((status)::text = 'APPROVED'::text) AND ((author)::character varying = 'Author-1'::text))
Filter: ((created_at >= '2026-02-01 00:00:00'::timestamp without time zone) AND (created_at <= '2026-02-28 23:59:59'::timestamp without time zone))
Rows Removed by Filter: 1
Heap Blocks: exact=2
->  BitmapAnd  (cost=4.30..4.30 rows=6 width=0) (actual time=0.035..0.035 rows=0 loops=1)
->  Bitmap Index Scan on idx_docs_status_created_at  (cost=0.00..2.10 rows=50 width=0) (actual time=0.015..0.015 rows=50 loops=1)
Index Cond: ((status)::text = 'APPROVED'::text)
->  Bitmap Index Scan on idx_docs_author  (cost=0.00..2.10 rows=20 width=0) (actual time=0.012..0.012 rows=15 loops=1)
Index Cond: ((author)::character varying = 'Author-1'::text)
Planning Time: 0.210 ms
Execution Time: 0.080 ms