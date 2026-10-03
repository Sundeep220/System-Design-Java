# 🐍 Python Stack — Interview Prep (Django, LangChain, Celery, RAG)
> Separate prep file for Python-related tech from your resume.
> Use for discussion rounds — **all DSA will still be in Java**.

---

## Table of Contents
1. [Python Core Concepts](#1-python-core-concepts)
2. [Django & Django REST Framework](#2-django--django-rest-framework)
3. [LangChain & RAG Architecture](#3-langchain--rag-architecture)
4. [Celery & Async Task Queues](#4-celery--async-task-queues)
5. [Azure AI Search & Vector Search](#5-azure-ai-search--vector-search)
6. [Python Batch Job Optimization](#6-python-batch-job-optimization)
7. [Resume Story Questions](#7-resume-story-questions)

---

## 1. Python Core Concepts

---

### ❓ What is the GIL (Global Interpreter Lock)? Why does it matter?
> **Answer:**
> The GIL is a mutex in CPython that allows only one thread to execute Python bytecode at a time — even on multi-core machines.
>
> **Impact:**
> - CPU-bound tasks: multiple threads don't run in parallel → use `multiprocessing` instead
> - I/O-bound tasks (network, disk): GIL is released during I/O waits → `threading` or `asyncio` works fine
>
> **Why it matters for your work:**
> In your Python batch jobs at Mercedes-Benz, the workload was I/O-bound (database reads, streaming results) — so `asyncio` or threading was viable. For CPU-heavy transformations, `multiprocessing.Pool` would be the right choice.

---

### ❓ What is a generator? How is it different from a regular function/list?
> **Answer:**
> A generator is a function that uses `yield` instead of `return`. It produces values **lazily** — one at a time — instead of building the entire result in memory.
>
> ```python
> def stream_records(cursor, batch_size=1000):
>     while True:
>         rows = cursor.fetchmany(batch_size)
>         if not rows:
>             break
>         for row in rows:
>             yield row   # yields one row at a time, no full list in memory
> ```
>
> **Why it matters for your batch job work:**
> Your original Python batch jobs loaded entire datasets into memory. Replacing them with cursor-based generators (as above) achieved ~75% memory reduction — this is the exact pattern. The generator keeps only one batch in memory at a time.
>
> **Generator vs List:**
> | | List | Generator |
> |---|---|---|
> | Memory | All items at once | One item at a time |
> | Reusable | Yes | No (exhausted after one pass) |
> | Speed | Faster for small data | Essential for large/infinite streams |

---

### ❓ What is `asyncio`? When would you use it over threading?
> **Answer:**
> `asyncio` is Python's single-threaded concurrency model using an event loop and coroutines (`async/await`).
>
> - Use `asyncio` when you have many concurrent **I/O-bound** tasks (HTTP calls, DB queries, file reads)
> - Use `threading` for legacy blocking I/O code that can't be made async
> - Use `multiprocessing` for **CPU-bound** tasks (image processing, ML inference)
>
> ```python
> import asyncio
> import aiohttp
>
> async def fetch_event(session, url):
>     async with session.get(url) as resp:
>         return await resp.json()
>
> async def fetch_all(urls):
>     async with aiohttp.ClientSession() as session:
>         tasks = [fetch_event(session, url) for url in urls]
>         return await asyncio.gather(*tasks)   # all run concurrently
> ```

---

### ❓ What are Python decorators? Give a real-world example.
> **Answer:**
> A decorator is a function that wraps another function to add behavior without modifying its code.
>
> ```python
> import functools, time
>
> def timed(func):
>     @functools.wraps(func)
>     def wrapper(*args, **kwargs):
>         start = time.time()
>         result = func(*args, **kwargs)
>         print(f"{func.__name__} took {time.time() - start:.3f}s")
>         return result
>     return wrapper
>
> @timed
> def process_batch(records):
>     ...
> ```
>
> Django REST Framework uses decorators heavily: `@api_view`, `@permission_classes`, `@authentication_classes`.

---

### ❓ What is the difference between `*args` and `**kwargs`?
> **Answer:**
> - `*args` — captures any number of **positional** arguments as a tuple
> - `**kwargs` — captures any number of **keyword** arguments as a dict
>
> ```python
> def log_event(*args, **kwargs):
>     print(args)    # ('ERROR', 'Disk full')
>     print(kwargs)  # {'tenantId': 'abc', 'severity': 'HIGH'}
>
> log_event('ERROR', 'Disk full', tenantId='abc', severity='HIGH')
> ```

---

### ❓ Explain list comprehensions and when NOT to use them.
> **Answer:**
> ```python
> # List comprehension — concise, creates full list in memory
> filtered = [e for e in events if e['severity'] == 'HIGH']
>
> # Generator expression — lazy, no full list in memory
> filtered_gen = (e for e in events if e['severity'] == 'HIGH')
> ```
>
> **Don't use list comprehensions when:**
> - The dataset is large (use generator expression instead)
> - Logic is complex enough that readability suffers
> - You only need to iterate once (generator is cheaper)

---

### ❓ What are Python's mutable vs immutable types?
> **Answer:**
>
> | Immutable | Mutable |
> |---|---|
> | int, float, str, tuple, frozenset | list, dict, set, bytearray |
>
> **Gotcha — mutable default argument (common interview trap):**
> ```python
> # BAD — list is shared across all calls
> def append_event(event, events=[]):
>     events.append(event)
>     return events
>
> # GOOD
> def append_event(event, events=None):
>     if events is None:
>         events = []
>     events.append(event)
>     return events
> ```

---

## 2. Django & Django REST Framework

---

### ❓ What is Django? How does the request lifecycle work?
> **Answer:**
> Django is a Python web framework following the **MTV pattern** (Model-Template-View).
>
> **Request lifecycle:**
> ```
> HTTP Request
>     │
>     ▼
> [WSGI/ASGI server]  (Gunicorn / Uvicorn)
>     │
>     ▼
> [Django Middleware Stack]  (auth, CORS, logging — executes in order)
>     │
>     ▼
> [URL Router]  (urls.py — matches path to view)
>     │
>     ▼
> [View / ViewSet]  (business logic)
>     │
>     ├── Serializer (validate input / serialize output)
>     ├── ORM query (Model → SQL)
>     └── Response (JSON)
>     │
>     ▼
> [Middleware Stack]  (reverse order on response)
>     │
>     ▼
> HTTP Response
> ```

---

### ❓ What is Django REST Framework (DRF)? What does it add over plain Django?
> **Answer:**
> DRF is a toolkit built on top of Django for building RESTful APIs. Key additions:
>
> - **Serializers** — validate input, serialize model instances to JSON, handle nested objects
> - **ViewSets + Routers** — auto-generate CRUD endpoints with minimal code
> - **Authentication classes** — JWT, Token, Session, OAuth2
> - **Permission classes** — `IsAuthenticated`, `IsAdminUser`, custom
> - **Throttling** — built-in rate limiting
> - **Browsable API** — HTML interface for manual testing
>
> ```python
> # ViewSet — auto-generates GET /events/, GET /events/{id}/, POST, PUT, DELETE
> class EventViewSet(viewsets.ModelViewSet):
>     queryset = SecurityEvent.objects.all()
>     serializer_class = SecurityEventSerializer
>     permission_classes = [IsAuthenticated]
>     filter_backends = [DjangoFilterBackend, OrderingFilter]
>     filterset_fields = ['severity', 'tenantId']
>     ordering_fields = ['timestamp']
> ```

---

### ❓ What is a DRF Serializer? How does it work?
> **Answer:**
> A Serializer converts complex Python objects (Django models) to/from JSON, and validates incoming data.
>
> ```python
> class SecurityEventSerializer(serializers.ModelSerializer):
>     class Meta:
>         model = SecurityEvent
>         fields = ['id', 'tenantId', 'severity', 'message', 'timestamp']
>         read_only_fields = ['id', 'timestamp']
>
>     def validate_severity(self, value):
>         allowed = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
>         if value not in allowed:
>             raise serializers.ValidationError(f"Must be one of {allowed}")
>         return value
> ```
>
> **Serialization flow:**
> - `serializer.data` → Python dict → JSON response
> - `serializer.is_valid()` → validates input → `serializer.save()` → creates/updates model

---

### ❓ What is Django ORM? What is an N+1 problem and how do you fix it in Django?
> **Answer:**
> Django ORM translates Python model queries to SQL.
>
> **N+1 Problem:**
> ```python
> # BAD — 1 query for events, then 1 query per event for tenant = N+1 queries
> events = SecurityEvent.objects.all()
> for event in events:
>     print(event.tenant.name)   # lazy load → separate SQL per event
> ```
>
> **Fix with `select_related` (JOIN for ForeignKey) or `prefetch_related` (separate query + Python join for ManyToMany):**
> ```python
> # GOOD — 1 SQL with JOIN
> events = SecurityEvent.objects.select_related('tenant').all()
>
> # GOOD — for ManyToMany or reverse FK
> events = SecurityEvent.objects.prefetch_related('tags').all()
> ```

---

### ❓ What is Django middleware? How did you use it in NeoAssist?
> **Answer:**
> Middleware is a hook into Django's request/response processing pipeline. Each middleware can inspect or modify the request before it reaches the view, and the response before it's returned.
>
> ```python
> class DatadogUsageMiddleware:
>     def __init__(self, get_response):
>         self.get_response = get_response
>
>     def __call__(self, request):
>         response = self.get_response(request)
>         # After view executes — log usage to Datadog
>         if request.path.startswith('/api/generate'):
>             team_id = request.user.team_id
>             statsd.increment('neoassist.api.calls', tags=[f'team:{team_id}'])
>         return response
> ```
>
> In NeoAssist, you built logging middleware for Datadog usage metrics — tracking adoption per team (100+ teams onboarded). This is exactly the pattern above.

---

### ❓ What is the difference between `filter()` and `get()` in Django ORM?
> **Answer:**
> - `get()` — returns exactly **one** object; raises `DoesNotExist` if none, `MultipleObjectsReturned` if many
> - `filter()` — returns a **QuerySet** (can be empty, one, or many objects)
>
> ```python
> # get() — use when you expect exactly one result
> try:
>     event = SecurityEvent.objects.get(id=event_id, tenantId=tenant_id)
> except SecurityEvent.DoesNotExist:
>     raise Http404
>
> # filter() — use for lists
> events = SecurityEvent.objects.filter(severity='HIGH', tenantId=tenant_id)
> ```

---

### ❓ What is `Q` object in Django ORM? When would you use it?
> **Answer:**
> `Q` objects allow complex queries with OR conditions and negation — analogous to JPA Specifications.
>
> ```python
> from django.db.models import Q
>
> # Events that are HIGH severity OR from a flagged IP
> events = SecurityEvent.objects.filter(
>     Q(severity='HIGH') | Q(sourceIp__in=flagged_ips)
> )
>
> # NOT operator
> events = SecurityEvent.objects.filter(~Q(status='RESOLVED'))
>
> # Combined
> events = SecurityEvent.objects.filter(
>     Q(severity='CRITICAL') & (Q(sourceIp=ip) | Q(hostname=host))
> )
> ```

---

### ❓ How do Django migrations work?
> **Answer:**
> Migrations are Django's way of propagating model changes to the database schema.
>
> ```bash
> python manage.py makemigrations   # generates migration file from model diff
> python manage.py migrate          # applies pending migrations to DB
> ```
>
> Each migration file is a Python script with `operations` list (CreateModel, AddField, AlterField, etc.).
> Migration state is tracked in `django_migrations` table.
>
> **Zero-downtime migration tips:**
> - Adding nullable column: safe, no lock
> - Renaming column: use `db_column` alias, two-phase (add new → backfill → drop old)
> - Adding index: use `CONCURRENTLY` in Postgres — DRF `RunSQL` migration

---

### ❓ How does DRF authentication work? What did you use in NeoAssist (PingID MFA)?
> **Answer:**
> DRF authentication is pluggable via `authentication_classes`. For each request, DRF tries each class in order until one returns a user.
>
> **PingID MFA flow in NeoAssist:**
> ```
> 1. User logs in → redirected to PingID IdP (OAuth2 / SAML)
> 2. User completes MFA challenge on PingID
> 3. PingID issues an access token (JWT or opaque)
> 4. Django backend validates token:
>    - JWT: decode and verify signature against PingID JWKS endpoint
>    - Opaque: call PingID introspection endpoint
> 5. User identity + roles extracted → set on request.user
> ```
>
> ```python
> class PingIDJWTAuthentication(BaseAuthentication):
>     def authenticate(self, request):
>         token = request.headers.get('Authorization', '').replace('Bearer ', '')
>         if not token:
>             return None
>         payload = jwt.decode(token, options={"verify_signature": True},
>                              algorithms=["RS256"], jwks_client=jwks_client)
>         user = User.objects.get(email=payload['email'])
>         return (user, token)
> ```

---

## 3. LangChain & RAG Architecture

---

### ❓ What is LangChain? What problem does it solve?
> **Answer:**
> LangChain is a Python (and JS) framework for building applications powered by LLMs. It solves the problem of **orchestrating multiple LLM calls, tools, memory, and data sources** into a coherent pipeline.
>
> Core abstractions:
> - **Chain** — sequence of LLM calls or operations
> - **Agent** — LLM that decides which tools to call based on input
> - **Tool** — function the LLM can invoke (search, DB query, calculator)
> - **Memory** — conversation history management
> - **Retriever** — fetches relevant documents (vector search, keyword search)

---

### ❓ What is RAG (Retrieval-Augmented Generation)? Explain your implementation.
> **Answer:**
> RAG grounds LLM responses in external knowledge, reducing hallucinations and enabling domain-specific answers without fine-tuning.
>
> **Architecture:**
> ```
> User Query
>     │
>     ▼
> [Embedding Model]        ← converts query to dense vector (e.g. text-embedding-ada-002)
>     │
>     ▼
> [Vector Store]           ← Azure AI Search (your stack) / Pinecone / FAISS
> (ANN search)             ← retrieves top-K most similar document chunks
>     │
>     ▼
> [Context Assembly]       ← retrieved chunks + original query → prompt template
>     │
>     ▼
> [LLM]                    ← OpenAI GPT-4 generates answer grounded in context
>     │
>     ▼
> Answer to User
> ```
>
> **Your NeoAssist implementation:**
> ```python
> from langchain.chains import RetrievalQA
> from langchain_openai import AzureChatOpenAI, AzureOpenAIEmbeddings
> from langchain_community.vectorstores import AzureSearch
>
> embeddings = AzureOpenAIEmbeddings(deployment="text-embedding-ada-002")
> vector_store = AzureSearch(
>     azure_search_endpoint=settings.AZURE_SEARCH_ENDPOINT,
>     azure_search_key=settings.AZURE_SEARCH_KEY,
>     index_name="neoassist-docs",
>     embedding_function=embeddings.embed_query
> )
>
> llm = AzureChatOpenAI(deployment_name="gpt-4", temperature=0.0)
>
> rag_chain = RetrievalQA.from_chain_type(
>     llm=llm,
>     chain_type="stuff",      # "stuff" = put all chunks in one prompt
>     retriever=vector_store.as_retriever(search_kwargs={"k": 5}),
>     return_source_documents=True
> )
>
> result = rag_chain.invoke({"query": "How do I write a Cucumber test for login?"})
> ```

---

### ❓ What is a vector embedding? Why is it used in RAG?
> **Answer:**
> An embedding is a fixed-size dense numerical vector that captures **semantic meaning** of text. Similar meanings → close vectors in high-dimensional space.
>
> Example: `"dog"` and `"puppy"` will have embeddings that are geometrically close. `"dog"` and `"database"` will be far apart.
>
> **Why in RAG:**
> Traditional keyword search (BM25) fails on semantic queries — e.g., query "how to authenticate a user" won't match a document titled "OAuth2 token verification flow".
> Vector similarity search finds semantically relevant chunks even with different wording.
>
> **Similarity metric:** Cosine similarity (angle between vectors) — most common for text.

---

### ❓ What is chunking in RAG? Why does it matter?
> **Answer:**
> Documents are too long to embed as a whole (token limits) and too broad to be relevant.
> Chunking = splitting documents into smaller pieces before embedding.
>
> **Chunking strategies:**
> - **Fixed-size** — split every N tokens (simple, may cut mid-sentence)
> - **Recursive character** — split on `\n\n`, then `\n`, then ` ` (respects structure)
> - **Semantic** — split on meaning boundaries (better but slower)
>
> ```python
> from langchain.text_splitter import RecursiveCharacterTextSplitter
>
> splitter = RecursiveCharacterTextSplitter(
>     chunk_size=500,
>     chunk_overlap=50    # overlap prevents losing context at boundaries
> )
> chunks = splitter.split_documents(raw_docs)
> ```
>
> **Overlap (50 tokens):** each chunk shares 50 tokens with the previous → no context lost at boundaries.

---

### ❓ What are LangChain Agents? How do they differ from Chains?
> **Answer:**
> - **Chain** — fixed, deterministic sequence of steps defined by the developer
> - **Agent** — the LLM itself decides at runtime which tools to call and in what order, based on the input
>
> ```
> Chain:  Input → Step1 → Step2 → Step3 → Output  (hardcoded flow)
> Agent:  Input → LLM reasons → calls Tool A → sees result → calls Tool B → returns answer
> ```
>
> **Your multi-agent AI workflow (Mercedes-Benz):**
> Multiple agents, each with a defined role (Product Owner, Developer, QA), with custom skill configurations. The LLM selects which agent handles each SDLC task. Human-in-the-loop decision points between stages. MCP integrations (Jira, GitHub) as tools the agents can call.

---

### ❓ What is the difference between `chain_type` "stuff", "map_reduce", and "refine" in LangChain?
> **Answer:**
>
> | Chain Type | How it works | Use when |
> |---|---|---|
> | `stuff` | All retrieved chunks stuffed into one prompt | Few chunks, fits in context window |
> | `map_reduce` | Summarize each chunk individually, then combine summaries | Many chunks, large docs |
> | `refine` | Process first chunk, then iteratively refine with each subsequent chunk | Need high coherence |
>
> For NeoAssist (code generation, test generation), `stuff` is appropriate — you retrieve the top 5 relevant code snippets and send them all in one prompt.

---

### ❓ What is prompt engineering? What techniques did you use?
> **Answer:**
> Prompt engineering = crafting LLM input to get consistent, high-quality output.
>
> **Key techniques:**
> - **System prompt** — sets LLM role and behavior: *"You are a senior QA engineer. Generate Cucumber BDD tests following Given-When-Then format."*
> - **Few-shot examples** — include 2-3 input/output examples in the prompt
> - **Output format constraints** — *"Return only valid JSON with fields: testName, steps, assertions"*
> - **Chain of thought** — *"Think step by step before writing the test"*
> - **Temperature=0.0** — deterministic output for code generation tasks

---

## 4. Celery & Async Task Queues

---

### ❓ What is Celery? Why would you use it with Django?
> **Answer:**
> Celery is a distributed task queue for Python. It allows you to run time-consuming tasks **asynchronously** outside the HTTP request cycle.
>
> **Why needed with Django:**
> HTTP requests must respond in <30s. Tasks like LLM inference, document indexing, or batch processing take minutes. Celery offloads them to background workers.
>
> **Architecture:**
> ```
> Django View
>     │
>     ├── task.delay(args)  ← enqueue task (returns immediately)
>     │
>     ▼
> [Message Broker]          ← Redis or RabbitMQ (task queue)
>     │
>     ▼
> [Celery Worker]           ← separate process, picks up and executes task
>     │
>     ▼
> [Result Backend]          ← Redis (store task result / status)
> ```

---

### ❓ How do you define and call a Celery task?
> **Answer:**
> ```python
> # tasks.py
> from celery import shared_task
> from .rag import generate_test_cases
>
> @shared_task(bind=True, max_retries=3, default_retry_delay=10)
> def generate_tests_task(self, code_snippet: str, team_id: str):
>     try:
>         result = generate_test_cases(code_snippet)
>         index_to_vector_store(result, team_id)
>         return {"status": "success", "tests": result}
>     except Exception as exc:
>         raise self.retry(exc=exc, countdown=2 ** self.request.retries)
>
> # In Django view — fire and forget
> task = generate_tests_task.delay(code_snippet, team_id)
> return Response({"taskId": task.id}, status=202)
>
> # Poll result
> result = AsyncResult(task_id)
> return Response({"status": result.state, "result": result.result})
> ```

---

### ❓ What is the difference between `.delay()` and `.apply_async()`?
> **Answer:**
> - `.delay(*args, **kwargs)` — shorthand, simple enqueue
> - `.apply_async(args, kwargs, countdown=30, eta=datetime, queue='high_priority')` — full control
>
> ```python
> # Run after 60 seconds
> generate_tests_task.apply_async(args=[code, team], countdown=60)
>
> # Run on specific queue (for priority routing)
> generate_tests_task.apply_async(args=[code, team], queue='llm-heavy')
> ```

---

### ❓ What is Celery Beat? How does it relate to your batch jobs?
> **Answer:**
> Celery Beat is a scheduler that triggers Celery tasks on a cron or interval schedule — similar to a cron daemon but integrated with Celery.
>
> ```python
> # celery.py
> app.conf.beat_schedule = {
>     'refresh-vector-index-nightly': {
>         'task': 'tasks.refresh_index',
>         'schedule': crontab(hour=2, minute=0),  # 2am every day
>     },
>     'usage-report-weekly': {
>         'task': 'tasks.generate_usage_report',
>         'schedule': crontab(day_of_week='monday', hour=8),
>     }
> }
> ```
>
> In NeoAssist, nightly index refresh for new documentation = Celery Beat job. This is the Python equivalent of what you built with Kubernetes CronJobs at Mercedes-Benz.

---

### ❓ How do you handle task failures and retries in Celery?
> **Answer:**
> ```python
> @shared_task(bind=True, max_retries=5)
> def process_event(self, event_id):
>     try:
>         event = fetch_event(event_id)
>         index_to_es(event)
>     except TemporaryError as exc:
>         # Exponential backoff: 2, 4, 8, 16, 32 seconds
>         raise self.retry(exc=exc, countdown=2 ** self.request.retries)
>     except PermanentError:
>         # Don't retry — log and move on
>         logger.error(f"Permanent failure for event {event_id}")
>         return {"status": "failed"}
> ```
>
> **Dead Letter Queue in Celery:**
> After `max_retries` exceeded, task goes to `FAILURE` state. You can route failed tasks to a dedicated queue for inspection:
> ```python
> app.conf.task_routes = {
>     'tasks.*': {'queue': 'default'},
> }
> app.conf.task_reject_on_worker_lost = True
> ```

---

## 5. Azure AI Search & Vector Search

---

### ❓ What is Azure AI Search? How did you use it in NeoAssist?
> **Answer:**
> Azure AI Search (formerly Azure Cognitive Search) is a managed cloud search service that supports:
> - **Full-text search** (BM25, keyword)
> - **Vector search** (ANN — Approximate Nearest Neighbor)
> - **Hybrid search** (keyword + vector, reranked by semantic model)
>
> **In NeoAssist:**
> - Documents (code snippets, API docs, test templates) indexed with their embeddings
> - User query → embed → vector search in Azure AI Search → top-5 chunks returned
> - Chunks assembled into prompt → OpenAI generates test case / code answer
>
> Azure AI Search acted as the **retrieval layer** in the RAG pipeline.

---

### ❓ What is the difference between vector search and keyword search?
> **Answer:**
>
> | | Keyword Search (BM25) | Vector Search |
> |---|---|---|
> | How | Matches exact or stemmed terms | Matches semantic meaning via embedding similarity |
> | Strength | Precise, fast, explainable | Handles paraphrases, synonyms, semantic queries |
> | Weakness | Vocabulary mismatch problem | Slower, requires embedding model, less precise for exact terms |
> | Example | Query: "Kafka consumer" → finds docs with those exact words | Query: "how to read messages from a queue" → finds Kafka consumer docs |
>
> **Hybrid search:** Run both, combine scores (reciprocal rank fusion or semantic reranker). Best of both worlds — used in Azure AI Search's "semantic search" mode.

---

### ❓ What is an embedding index? How is it different from a regular DB index?
> **Answer:**
> A regular DB index (B-tree) supports exact match and range queries on discrete values.
>
> A vector/embedding index supports **approximate nearest neighbor (ANN)** search — finding the K vectors closest to a query vector in high-dimensional space.
>
> **ANN algorithms:**
> - **HNSW** (Hierarchical Navigable Small World) — graph-based, very fast, used by Azure AI Search, Pinecone, Weaviate
> - **IVF** (Inverted File Index) — clusters vectors, searches nearby clusters only
>
> Trade-off: ANN trades some accuracy (recall) for massive speed gain vs exact brute-force search.

---

## 6. Python Batch Job Optimization

---

### ❓ Walk me through how you optimized the Python batch jobs at Mercedes-Benz.
> **Answer:**
> The original jobs had three critical issues:
>
> **Problem 1 — Full dataset in memory:**
> ```python
> # BEFORE — loads all records at once
> records = db.execute("SELECT * FROM charging_events").fetchall()
> process(records)
> ```
>
> **Fix — Cursor-based pagination (streaming):**
> ```python
> # AFTER — processes N records at a time
> BATCH_SIZE = 5000
> last_id = 0
>
> while True:
>     records = db.execute(
>         "SELECT * FROM charging_events WHERE id > %s ORDER BY id LIMIT %s",
>         (last_id, BATCH_SIZE)
>     ).fetchall()
>
>     if not records:
>         break
>
>     process_batch(records)
>     last_id = records[-1]['id']
> ```
> Why cursor-based over `OFFSET`? `OFFSET N` gets slower as N grows (scans and discards N rows). Cursor-based uses a keyset — always an index seek.
>
> **Problem 2 — Fetching all columns:**
> ```python
> # BEFORE — SELECT * pulls unnecessary columns (BLOBs, large fields)
> records = db.execute("SELECT * FROM charging_events").fetchall()
>
> # AFTER — selective parsing, only needed fields
> records = db.execute(
>     "SELECT id, tenant_id, event_type, amount FROM charging_events WHERE id > %s LIMIT %s",
>     (last_id, BATCH_SIZE)
> ).fetchall()
> ```
>
> **Problem 3 — Materializing intermediate results:**
> Replaced intermediate lists with generators, used streaming output writers (CSV streaming, not building full string).
>
> **Result: ~75% memory reduction** — the combination of keyset pagination + column projection + generator-based processing.

---

### ❓ What is cursor-based pagination vs offset pagination? Why is cursor-based better at scale?
> **Answer:**
>
> **Offset pagination:**
> ```sql
> SELECT * FROM events ORDER BY id LIMIT 100 OFFSET 50000
> ```
> The database must scan and discard 50,000 rows to get to offset 50,000. Gets progressively slower. Also inconsistent if rows are inserted/deleted during pagination.
>
> **Cursor-based (keyset) pagination:**
> ```sql
> SELECT * FROM events WHERE id > 50000 ORDER BY id LIMIT 100
> ```
> Uses the index directly — O(log N) seek regardless of page depth. Stable — new inserts don't affect your position. Only limitation: can't jump to arbitrary page.
>
> **At scale:** For 10M row table, offset=9,900,000 scans 9.9M rows. Cursor-based always scans only 100 rows.

---

## 7. Resume Story Questions

---

### ❓ "Tell me about the NeoAssist platform you built."
> **Answer:**
> NeoAssist is an internal LLM + RAG-powered SDLC platform at Mercedes-Benz, designed to reduce the manual effort involved in writing test cases and code documentation.
>
> **Architecture:**
> - Django REST Framework as the API layer
> - LangChain for orchestrating the RAG pipeline
> - Azure AI Search as the vector store for code snippets and documentation
> - OpenAI GPT-4 as the LLM for generation (code gen, test case gen, code wiki)
> - Celery + Redis for async job processing (LLM calls are slow — can't block HTTP)
> - PingID MFA for enterprise SSO authentication
> - Datadog middleware for per-team usage tracking
>
> **Impact:** Reduced manual testing effort by ~70%, onboarded 100+ internal teams.
>
> **Capabilities built:**
> 1. Code generation from natural language specs
> 2. Test case generation (Cucumber BDD, Playwright E2E, TestNG unit tests)
> 3. Code wiki — explain existing code, generate documentation

---

### ❓ "How did you ensure the LLM outputs were reliable/production-grade in NeoAssist?"
> **Answer:**
> Four key measures:
>
> 1. **Temperature = 0.0** for code generation — deterministic, no creative drift
> 2. **Output format constraints in system prompt** — *"Return ONLY valid JSON, no markdown fences"* + JSON schema validation on the response
> 3. **RAG grounding** — LLM answers grounded in retrieved code context, not just training data → reduces hallucination
> 4. **Human review gate** — generated test cases weren't auto-committed; engineers reviewed before merge. This was also part of the multi-agent workflow's human-in-the-loop design.

---

### ❓ "What was the multi-agent AI workflow you built?"
> **Answer:**
> A multi-agent system where different AI agents handle different SDLC roles:
>
> ```
> User Input (feature request / task)
>         │
>         ▼
> [Orchestrator Agent]  ← decides which role-agent to invoke
>         │
>         ├──► [Product Owner Agent]   ← breaks task into user stories
>         │         └─ Tool: Jira API (create tickets)
>         │
>         ├──► [Developer Agent]       ← generates code skeleton / implementation
>         │         └─ Tool: GitLab API (create branch, commit)
>         │
>         ├──► [QA Agent]              ← generates test cases for the implementation
>         │         └─ Tool: Confluence (write test plan)
>         │
>         └──► [Human-in-the-loop]     ← approval gate between each stage
>                   └─ Engineer approves/rejects before next agent proceeds
> ```
>
> MCP (Model Context Protocol) provided standardized tool interfaces — agents called Jira, Confluence, GitHub, and GitLab through MCP rather than custom SDK integrations, making tool swap-out easy.
>
> **Impact:** ~40% increase in team delivery rate.

---

*Last updated: October 2026*
