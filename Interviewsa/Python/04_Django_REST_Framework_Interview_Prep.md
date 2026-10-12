# 🐍 Django REST Framework (DRF) — Complete Interview Preparation Guide
**Coverage:** REST basics → DRF internals → Serializers → Views/ViewSets → Auth → Permissions → Throttling → Pagination → Filtering → Versioning → ORM & Performance → Transactions → Django core → Testing → Security → Deployment → Async  
**Use this file to:** Be prepared for every DRF question an interviewer might ask, from basic to advanced

---

## Table of Contents

### Part A — Foundations
1. [DRF Overview & REST Basics](#1-drf-overview--rest-basics)
2. [Request Lifecycle Inside DRF](#2-request-lifecycle-inside-drf)
3. [Views: FBV, APIView, Generics, Mixins, ViewSets](#3-views-fbv-apiview-generics-mixins-viewsets)
4. [Serializers (the heart of DRF)](#4-serializers-the-heart-of-drf)
5. [Request, Response, Parsers, Renderers, Content Negotiation](#5-request-response-parsers-renderers-content-negotiation)
6. [Routers & URL Configuration](#6-routers--url-configuration)

### Part B — API Features
7. [Authentication](#7-authentication)
8. [Permissions](#8-permissions)
9. [Throttling (Rate Limiting)](#9-throttling-rate-limiting)
10. [Pagination](#10-pagination)
11. [Filtering, Searching, Ordering](#11-filtering-searching-ordering)
12. [API Versioning](#12-api-versioning)
13. [Exception Handling & Error Responses](#13-exception-handling--error-responses)
14. [Caching](#14-caching)

### Part C — Data, Django Core & Production
15. [Django ORM Essentials for DRF](#15-django-orm-essentials-for-drf)
16. [ORM Performance (N+1, only, iterator, bulk)](#16-orm-performance-n1-only-iterator-bulk)
17. [Transactions & Data Consistency](#17-transactions--data-consistency)
18. [Django Core Concepts Asked in Interviews](#18-django-core-concepts-asked-in-interviews)
19. [Testing DRF APIs](#19-testing-drf-apis)
20. [Security Best Practices](#20-security-best-practices)
21. [API Documentation (OpenAPI / drf-spectacular)](#21-api-documentation-openapi--drf-spectacular)
22. [Async with Django & DRF](#22-async-with-django--drf)
23. [Production Deployment & Architecture](#23-production-deployment--architecture)
24. [Hands-on Coding Scenarios](#24-hands-on-coding-scenarios)
25. [Quick Revision Checklist](#25-quick-revision-checklist)

---

## Part A — Foundations

## 1. DRF Overview & REST Basics

### ❓ What is Django REST Framework and why would you use it?
**Answer:**
DRF is a toolkit on top of Django for building **Web APIs**. It gives you:
- **Serializers** — convert model instances to JSON and validate incoming JSON.
- **Class-based API views and ViewSets** — CRUD endpoints with little code.
- **Built-in authentication, permissions, throttling, pagination, filtering, versioning.**
- **Browsable API** — a human-friendly HTML UI for testing endpoints.
- **Content negotiation** — the same endpoint can return JSON, XML, or other formats.

Plain Django views return `HttpResponse`. DRF wraps them so that `request.data` is parsed for any content type and responses are rendered according to the `Accept` header.

---

### ❓ What are REST principles? Which HTTP methods and status codes do you use?
**Answer:**
REST principles: **resources** identified by URLs, **stateless** requests, uniform interface, representations (JSON), and HATEOAS (optional).

| HTTP Method | Typical Use | Idempotent? | Safe? |
|---|---|---|---|
| `GET` | Read resource/collection | Yes | Yes |
| `POST` | Create resource | No | No |
| `PUT` | Replace whole resource | Yes | No |
| `PATCH` | Partial update | Not guaranteed | No |
| `DELETE` | Remove resource | Yes | No |

| Status Code | Meaning | When DRF returns it |
|---|---|---|
| `200 OK` | Success | GET, PUT, PATCH success |
| `201 Created` | Resource created | POST success |
| `204 No Content` | Success, no body | DELETE success |
| `400 Bad Request` | Validation failed | Serializer `is_valid()` false |
| `401 Unauthorized` | Not authenticated | Missing/invalid credentials |
| `403 Forbidden` | Authenticated, not allowed | Permission denied |
| `404 Not Found` | Object missing | `get_object()` fails |
| `405 Method Not Allowed` | Verb not supported | Method missing on view |
| `429 Too Many Requests` | Rate limit hit | Throttle exceeded |

---

### ❓ What is the difference between a plain Django view and a DRF view?
**Answer:**
| Aspect | Django view | DRF `APIView` |
|---|---|---|
| Request object | `HttpRequest` | `Request` (wraps it; `request.data` parses JSON/form/multipart) |
| Response | `HttpResponse` / `JsonResponse` | `Response` (content-negotiated renderer) |
| Auth | Manual / session middleware | Pluggable `authentication_classes` |
| Permissions | Manual | `permission_classes` |
| CSRF | Enforced by middleware | Exempt for token-based auth (`APIView` is `csrf_exempt`) |
| Errors | Django error pages | JSON error bodies via exception handler |

---

## 2. Request Lifecycle Inside DRF

### ❓ Walk me through what happens when a request hits a DRF view.
**Answer:**
`APIView.as_view()` returns a view function. `dispatch()` does the following:
1. `initialize_request()` wraps `HttpRequest` in DRF `Request`.
2. `initial()` runs: content negotiation, versioning, **authentication**, **permission checks**, **throttle checks**.
3. The handler method (`get`, `post`, or the ViewSet action) runs.
4. The return value (a `Response`) is passed to `finalize_response()` which picks a renderer.

### 📊 Diagram — Request lifecycle

```mermaid
flowchart TD
    A["HTTP Request"] --> B["Django middleware chain (request path)"]
    B --> C["URL resolver urls.py"]
    C --> D["APIView.as_view() then dispatch()"]
    D --> E["initialize_request: wrap in DRF Request"]
    E --> F["initial(): content negotiation and versioning"]
    F --> G["perform_authentication: sets request.user"]
    G --> H{"check_permissions: has_permission"}
    H -- denied --> X["401 or 403 Response"]
    H -- allowed --> I{"check_throttles"}
    I -- limited --> Y["429 Too Many Requests"]
    I -- ok --> J["Handler: get / post / ViewSet action"]
    J --> K["Serializer: is_valid then save"]
    K --> L["Response then finalize_response: renderer"]
    L --> M["Django middleware chain (response path)"]
    M --> N["Client"]
```

---

## 3. Views: FBV, APIView, Generics, Mixins, ViewSets

### ❓ What is a function-based API view with `@api_view`?
**Answer:**
```python
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status

@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def book_list(request):
    if request.method == 'GET':
        books = Book.objects.all()
        return Response(BookSerializer(books, many=True).data)
    serializer = BookSerializer(data=request.data)
    if serializer.is_valid():
        serializer.save()
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
```
Use FBVs for simple endpoints; class-based views for reuse and inheritance.

---

### ❓ APIView vs generic views vs ViewSets — what is the difference?
**Answer:**
- **`APIView`** — you write `get`, `post`, etc. Full control, most boilerplate.
- **Generic views** (`ListCreateAPIView`, `RetrieveUpdateDestroyAPIView`) — pre-built behavior, one class per URL.
- **Mixins** — reusable pieces (`ListModelMixin`, `CreateModelMixin`, ...) composed with `GenericAPIView`.
- **ViewSets** — one class groups actions for a resource (`list`, `create`, `retrieve`, `update`, `partial_update`, `destroy`); the **router** generates the URLs.

```python
from rest_framework import generics, mixins, viewsets

class BookListCreate(generics.ListCreateAPIView):          # generic view
    queryset = Book.objects.all()
    serializer_class = BookSerializer

class BookComposed(mixins.ListModelMixin, mixins.CreateModelMixin,
                   generics.GenericAPIView):              # mixins
    queryset = Book.objects.all()
    serializer_class = BookSerializer
    def get(self, request, *args, **kwargs):
        return self.list(request, *args, **kwargs)
    def post(self, request, *args, **kwargs):
        return self.create(request, *args, **kwargs)

class BookViewSet(viewsets.ModelViewSet):                  # viewset
    queryset = Book.objects.all()
    serializer_class = BookSerializer
```

---

### ❓ Show the ViewSet class hierarchy.
**Answer:**
### 📊 Diagram — DRF view class hierarchy

```mermaid
classDiagram
    class APIView {
        +dispatch()
        +initial()
        +finalize_response()
    }
    class GenericAPIView {
        +queryset
        +serializer_class
        +get_queryset()
        +get_object()
        +get_serializer()
    }
    class ViewSetMixin {
        +as_view(actions)
    }
    class GenericViewSet
    class ModelViewSet
    class ReadOnlyModelViewSet
    class ListModelMixin {
        +list()
    }
    class CreateModelMixin {
        +create()
    }
    class RetrieveModelMixin {
        +retrieve()
    }
    class UpdateModelMixin {
        +update()
        +partial_update()
    }
    class DestroyModelMixin {
        +destroy()
    }
    APIView <|-- GenericAPIView
    GenericAPIView <|-- GenericViewSet
    ViewSetMixin <|-- GenericViewSet
    GenericViewSet <|-- ModelViewSet
    GenericViewSet <|-- ReadOnlyModelViewSet
    ListModelMixin <|-- ModelViewSet
    CreateModelMixin <|-- ModelViewSet
    RetrieveModelMixin <|-- ModelViewSet
    UpdateModelMixin <|-- ModelViewSet
    DestroyModelMixin <|-- ModelViewSet
    ListModelMixin <|-- ReadOnlyModelViewSet
    RetrieveModelMixin <|-- ReadOnlyModelViewSet
```

**Key points:**
- `ModelViewSet` = list + create + retrieve + update + partial_update + destroy.
- `ReadOnlyModelViewSet` = list + retrieve only.
- `GenericViewSet` = no actions; you add mixins yourself.

---

### ❓ How do you add custom actions to a ViewSet? What is `@action`?
**Answer:**
`@action` adds extra endpoints to a ViewSet and makes the router generate URLs for them.
```python
from rest_framework.decorators import action
from rest_framework.response import Response

class BookViewSet(viewsets.ModelViewSet):
    queryset = Book.objects.all()
    serializer_class = BookSerializer

    @action(detail=True, methods=['post'], url_path='mark-sold')
    def mark_sold(self, request, pk=None):
        book = self.get_object()           # applies permissions + object lookup
        book.sold = True
        book.save(update_fields=['sold'])
        return Response({'status': 'sold'})

    @action(detail=False, methods=['get'])
    def cheap(self, request):
        qs = self.get_queryset().filter(price__lt=10)
        return Response(self.get_serializer(qs, many=True).data)
```
- `detail=True` → URL includes pk: `/books/{pk}/mark-sold/`
- `detail=False` → collection URL: `/books/cheap/`

---

### ❓ Which ViewSet hooks are commonly overridden?
**Answer:**
```python
class BookViewSet(viewsets.ModelViewSet):
    serializer_class = BookSerializer

    def get_queryset(self):                  # scope data per user / per query params
        qs = Book.objects.select_related('author')
        if not self.request.user.is_staff:
            qs = qs.filter(owner=self.request.user)
        return qs

    def get_serializer_class(self):          # different serializer per action
        if self.action in ('create', 'update', 'partial_update'):
            return BookWriteSerializer
        return BookReadSerializer

    def perform_create(self, serializer):    # inject values that aren't from client
        serializer.save(owner=self.request.user)

    def perform_destroy(self, instance):     # soft delete instead of hard delete
        instance.is_deleted = True
        instance.save(update_fields=['is_deleted'])

    def get_serializer_context(self):        # extra data available to serializer
        ctx = super().get_serializer_context()
        ctx['currency'] = 'USD'
        return ctx
```

---

## 4. Serializers (the heart of DRF)

### ❓ What is a Serializer and what does it do?
**Answer:**
A serializer does two jobs:
1. **Serialization** — Python object (model instance, dict) → primitive types (`dict`, `list`, `str`, `int`) → JSON.
2. **Deserialization + validation** — incoming JSON → `validated_data` → model instance on `save()`.

Lifecycle:
```python
serializer = BookSerializer(instance)            # serialize
serializer.data                                  # {'id': 1, 'title': '...'}

serializer = BookSerializer(data=request.data)   # deserialize
serializer.is_valid(raise_exception=True)        # runs validation
serializer.validated_data                        # clean dict
serializer.save()                                # calls create() or update()
```

---

### ❓ Serializer vs ModelSerializer?
**Answer:**
| Aspect | `Serializer` | `ModelSerializer` |
|---|---|---|
| Fields | Declared manually | Auto-generated from model fields |
| `create()` / `update()` | You implement | Provided (with sensible defaults) |
| Unique validators | Manual | Auto (`unique=True` → `UniqueValidator`) |
| Best for | Non-model data, external APIs, complex logic | CRUD on models |

---

### ❓ Explain serializer field options.
**Answer:**
```python
class BookSerializer(serializers.ModelSerializer):
    author_name = serializers.CharField(source='author.name', read_only=True)  # dotted source
    internal_code = serializers.CharField(write_only=True, required=False)     # input only
    price = serializers.DecimalField(max_digits=6, decimal_places=2, min_value=0)
    published = serializers.BooleanField(default=False)                         # default on input
    notes = serializers.CharField(allow_blank=True, allow_null=True, required=False)

    class Meta:
        model = Book
        fields = ['id', 'title', 'author_name', 'price', 'published', 'notes', 'internal_code']
        read_only_fields = ['id']
        extra_kwargs = {'title': {'min_length': 3}}
```
| Option | Meaning |
|---|---|
| `read_only=True` | Output only; ignored on input |
| `write_only=True` | Input only; never returned (e.g., passwords) |
| `required` | Must be present in input (default True) |
| `default` | Used when missing from input |
| `allow_null` / `allow_blank` | Accept null / empty string |
| `source` | Attribute path on the object (`'author.name'`) |
| `validators` | List of extra validator callables |
| `help_text` / `label` | Used by browsable API and OpenAPI docs |

---

### ❓ How does validation work in a serializer? Show field-level and object-level validation.
**Answer:**
### 📊 Diagram — Serializer validation order

```mermaid
flowchart TD
    A["BookSerializer(data=request.data)"] --> B["is_valid()"]
    B --> C["to_internal_value: each field run_validation"]
    C --> D{"Field-level errors?"}
    D -- yes --> E["Collect errors per field"]
    D -- no --> F["validate_fieldname() for each field"]
    F --> G["validate(attrs): cross-field check"]
    G --> H{"Any errors?"}
    E --> H
    H -- yes --> I["serializer.errors then 400 Response"]
    H -- no --> J["validated_data ready"]
    J --> K["save(): create() or update()"]
```

```python
from rest_framework import serializers

class BookSerializer(serializers.ModelSerializer):
    class Meta:
        model = Book
        fields = ['id', 'title', 'price', 'start_date', 'end_date']

    # 1. Field-level: method name must be validate_<field_name>
    def validate_price(self, value):
        if value < 0:
            raise serializers.ValidationError("Price must be non-negative.")
        return value

    # 2. Object-level: runs after all field validators pass
    def validate(self, attrs):
        if attrs['start_date'] > attrs['end_date']:
            raise serializers.ValidationError({"end_date": "Must be after start_date."})
        return attrs
```
- Raise `ValidationError` with a string for non-field errors, or a dict keyed by field name.
- Validators can also be attached per field: `serializers.IntegerField(validators=[MinValueValidator(1)])`.

---

### ❓ What is the difference between `is_valid()`, `validated_data`, `data`, and `save()`?
**Answer:**
- `is_valid()` — runs all validation; returns bool; fills `errors` if False.
- `validated_data` — only available after `is_valid()`; contains cleaned input.
- `data` — the **output** representation (after `save()` or for an instance).
- `save()` — calls `create(validated_data)` if no instance was passed, else `update(instance, validated_data)`.

```python
s = BookSerializer(instance, data=payload, partial=True)  # PATCH semantics
if s.is_valid():
    obj = s.save(owner=request.user)  # extra kwargs merged into validated_data
```
`partial=True` skips "required" checks — used for PATCH.

---

### ❓ How do you serialize a list? What is `many=True`?
**Answer:**
`many=True` wraps the serializer in a `ListSerializer`, which iterates over the collection.
```python
books = Book.objects.all()
data = BookSerializer(books, many=True).data      # list of dicts

# Bulk validation on input
s = BookSerializer(data=request.data, many=True)  # request.data is a JSON list
s.is_valid(raise_exception=True)
s.save()                                          # calls create() per item
```
For custom bulk behavior, subclass `ListSerializer` and override `create()` to use `bulk_create`.

---

### ❓ How do you write nested serializers? Read-only vs writable.
**Answer:**
**Read-only nesting** (easy):
```python
class AuthorSerializer(serializers.ModelSerializer):
    class Meta:
        model = Author
        fields = ['id', 'name']

class BookSerializer(serializers.ModelSerializer):
    author = AuthorSerializer(read_only=True)
    class Meta:
        model = Book
        fields = ['id', 'title', 'author']
```

**Writable nesting** — DRF does **not** auto-save nested data. You must override `create`/`update`:
```python
class OrderItemSerializer(serializers.ModelSerializer):
    class Meta:
        model = OrderItem
        fields = ['product', 'quantity']

class OrderSerializer(serializers.ModelSerializer):
    items = OrderItemSerializer(many=True)

    class Meta:
        model = Order
        fields = ['id', 'customer', 'items']

    def create(self, validated_data):
        items_data = validated_data.pop('items')
        with transaction.atomic():
            order = Order.objects.create(**validated_data)
            OrderItem.objects.bulk_create(
                [OrderItem(order=order, **item) for item in items_data]
            )
        return order

    def update(self, instance, validated_data):
        items_data = validated_data.pop('items', None)
        with transaction.atomic():
            for attr, value in validated_data.items():
                setattr(instance, attr, value)
            instance.save()
            if items_data is not None:
                instance.items.all().delete()
                OrderItem.objects.bulk_create(
                    [OrderItem(order=instance, **item) for item in items_data]
                )
        return instance
```
Interview point: `DRF raises AssertionError` by default if you call `create()` on a writable nested serializer without overriding it.

---

### ❓ What is `SerializerMethodField` and when do you use it?
**Answer:**
A read-only field whose value comes from a method `get_<field_name>`. Use it for computed output.
```python
class BookSerializer(serializers.ModelSerializer):
    discounted_price = serializers.SerializerMethodField()
    is_owner = serializers.SerializerMethodField()

    class Meta:
        model = Book
        fields = ['id', 'title', 'price', 'discounted_price', 'is_owner']

    def get_discounted_price(self, obj):
        return round(obj.price * Decimal('0.9'), 2)

    def get_is_owner(self, obj):
        request = self.context.get('request')
        return bool(request and obj.owner_id == request.user.id)
```
Alternatives: a `@property` on the model with `source='prop_name'`, or overriding `to_representation()` for full control.

```python
def to_representation(self, instance):
    data = super().to_representation(instance)
    data['title'] = data['title'].title()   # transform output
    return data
```

---

### ❓ What is `HyperlinkedModelSerializer`?
**Answer:**
It represents relationships as **URLs** instead of primary keys (HATEOAS-style).
```python
class BookSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Book
        fields = ['url', 'title', 'author']   # url -> 'book-detail'; author -> 'author-detail'
```
Requires `request` in the serializer context and matching route names (`basename-detail`). Rarely used in modern APIs but may be asked.

---

### ❓ How do you pass the current user or request into a serializer? What is `HiddenField`?
**Answer:**
```python
from rest_framework.fields import CurrentUserDefault

class BookSerializer(serializers.ModelSerializer):
    owner = serializers.HiddenField(default=serializers.CurrentUserDefault())  # never in input/output

    class Meta:
        model = Book
        fields = ['id', 'title', 'owner']
```
- `context={'request': request}` makes `self.context['request']` available — needed for `HyperlinkedRelatedField` and permission-aware output.
- `CurrentUserDefault` is the standard way to inject the authenticated user into input data without trusting the client.

---

### ❓ How do you enforce unique-together constraints in DRF?
**Answer:**
Django's `Meta.unique_together` or `UniqueConstraint` is validated by DRF's `ModelSerializer` automatically (as a `UniqueTogetherValidator`). For a custom case:
```python
class Meta:
    model = Enrollment
    fields = ['student', 'course']
    validators = [
        serializers.UniqueTogetherValidator(
            queryset=Enrollment.objects.all(),
            fields=['student', 'course'],
            message="Student already enrolled in this course.",
        )
    ]
```

---

### ❓ `Meta.fields` vs `Meta.exclude` vs `Meta.depth` — which should you use?
**Answer:**
- **`fields = [...]`** — explicit whitelist. **Recommended** (prevents accidental exposure of new model fields).
- **`exclude = [...]`** — blacklist; new fields leak automatically.
- **`fields = '__all__'`** — dangerous in production.
- **`depth = 1`** — auto-nests related objects read-only (quick but inflexible).

---

## 5. Request, Response, Parsers, Renderers, Content Negotiation

### ❓ What are `request.data`, `request.query_params`, `request.FILES`?
**Answer:**
- `request.data` — parsed body for JSON, form, or multipart (depends on parsers).
- `request.query_params` — the URL query string (`?page=2`). Same as Django's `request.GET`.
- `request.FILES` — uploaded files (multipart only).
- `request.user` / `request.auth` — set by authentication.
- `request.method`, `request.version` (if versioning enabled), `request.accepted_renderer`.

```python
page = int(request.query_params.get('page', 1))
title = request.data.get('title')
```

---

### ❓ `Response` vs `JsonResponse` vs `HttpResponse`?
**Answer:**
- `rest_framework.response.Response` — takes Python data, renders via the negotiated renderer (JSON, Browsable API, etc.).
- `JsonResponse` — Django; always JSON; no content negotiation.
- `HttpResponse` — raw body; you handle encoding.

Use `Response` in DRF views. Use `status` constants instead of magic numbers: `status.HTTP_201_CREATED`.

---

### ❓ How does content negotiation work?
**Answer:**
DRF checks the `Accept` header (or `?format=json`) and picks a renderer from `renderer_classes`. If nothing matches → `406 Not Acceptable`.

```python
REST_FRAMEWORK = {
    'DEFAULT_RENDERER_CLASSES': [
        'rest_framework.renderers.JSONRenderer',
        'rest_framework.renderers.BrowsableAPIRenderer',  # disable in prod if not needed
    ],
    'DEFAULT_PARSER_CLASSES': [
        'rest_framework.parsers.JSONParser',
        'rest_framework.parsers.FormParser',
        'rest_framework.parsers.MultiPartParser',
    ],
}
```
- **Parsers** decode incoming bodies by `Content-Type`.
- **Renderers** encode outgoing data by `Accept`.

---

### ❓ How do you handle file uploads in DRF?
**Answer:**
```python
from rest_framework.parsers import MultiPartParser, FormParser
from rest_framework.views import APIView

class AvatarUploadView(APIView):
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request):
        file_obj = request.FILES['avatar']
        # validate size/type, then save to storage
        return Response({'filename': file_obj.name, 'size': file_obj.size},
                        status=status.HTTP_201_CREATED)
```
For a model field: `avatar = models.ImageField(upload_to='avatars/')` plus a serializer `ImageField` / `FileField`.

---

## 6. Routers & URL Configuration

### ❓ What is a router and how does it map ViewSets to URLs?
**Answer:**
A router inspects the ViewSet's actions and generates URL patterns automatically.
```python
from rest_framework.routers import DefaultRouter

router = DefaultRouter()
router.register(r'books', BookViewSet, basename='book')

urlpatterns = [
    path('api/', include(router.urls)),
]
```

| Method | URL | Action | Name |
|---|---|---|---|
| GET | `/api/books/` | list | `book-list` |
| POST | `/api/books/` | create | `book-list` |
| GET | `/api/books/{pk}/` | retrieve | `book-detail` |
| PUT/PATCH | `/api/books/{pk}/` | update / partial_update | `book-detail` |
| DELETE | `/api/books/{pk}/` | destroy | `book-detail` |
| POST | `/api/books/{pk}/mark-sold/` | custom `@action` | `book-mark-sold` |
| GET | `/api/` | API root (DefaultRouter only) | `api-root` |

### 📊 Diagram — Router URL generation

```mermaid
flowchart LR
    A["router.register(books, BookViewSet, basename=book)"] --> B["Inspect actions: list, create, retrieve, update, destroy"]
    B --> C["Generate URL patterns"]
    C --> D["/books/ and /books/pk/"]
    B --> E["@action detail=True or False"]
    E --> F["/books/pk/mark-sold/ and /books/cheap/"]
```

**Interview points:**
- `SimpleRouter` — no API root view. `DefaultRouter` — adds one plus format suffixes.
- `basename` is **required** if the ViewSet has no `queryset` attribute (or overrides `get_queryset`), because the router can't infer the URL name.
- Customize the lookup: `lookup_field = 'slug'`, `lookup_value_regex = '[0-9a-f-]{36}'`.

---

### ❓ How do you use a different URL lookup field or nested routes?
**Answer:**
```python
class BookViewSet(viewsets.ModelViewSet):
    lookup_field = 'isbn'
    lookup_value_regex = r'[0-9]{13}'
```
Nested resources (`/authors/{id}/books/`) are commonly done with the third-party package `drf-nested-routers`, or with a custom `@action`, or by filtering in `get_queryset` using `self.kwargs`:
```python
class AuthorBooksViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = BookSerializer

    def get_queryset(self):
        return Book.objects.filter(author_id=self.kwargs['author_pk'])
```

---

---

## Part B — API Features

## 7. Authentication

### ❓ What is the difference between authentication and authorization in DRF?
**Answer:**
- **Authentication** — *who are you?* Sets `request.user` and `request.auth`. Implemented by `authentication_classes`.
- **Authorization / Permissions** — *what can you do?* Implemented by `permission_classes`.
- If authentication fails, DRF returns `401` (when a `WWW-Authenticate` header is provided) or `403`.

---

### ❓ Which authentication schemes does DRF provide out of the box?
**Answer:**
| Class | How it works | Typical use |
|---|---|---|
| `SessionAuthentication` | Django session cookie; enforces CSRF | Browser clients on same domain |
| `BasicAuthentication` | `Authorization: Basic base64(user:pass)` | Testing / internal tools |
| `TokenAuthentication` | `Authorization: Token <key>` stored in DB | Simple mobile/CLI clients |
| `RemoteUserAuthentication` | Trusts `REMOTE_USER` header from a proxy | SSO behind reverse proxy |

Third-party: **`djangorestframework-simplejwt`** (JWT), **`django-allauth`/`dj-rest-auth`** (social login), **`django-oauth-toolkit`** (OAuth2 provider).

---

### ❓ How do you configure authentication globally and per view?
**Answer:**
```python
# settings.py
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'rest_framework_simplejwt.authentication.JWTAuthentication',
        'rest_framework.authentication.SessionAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticated',   # secure by default
    ],
}

# per view
class PublicHealthView(APIView):
    authentication_classes = []        # skip auth entirely
    permission_classes = [AllowAny]
```

---

### ❓ Explain token authentication with the built-in `TokenAuthentication`.
**Answer:**
```python
# settings.py
INSTALLED_APPS += ['rest_framework.authtoken']
REST_FRAMEWORK = {'DEFAULT_AUTHENTICATION_CLASSES': [
    'rest_framework.authentication.TokenAuthentication']}

# python manage.py migrate
# Token is created per user:
from rest_framework.authtoken.models import Token
token, _ = Token.objects.get_or_create(user=user)
```
Client sends `Authorization: Token 9944b09199c62bcf9418ad846dd0e4bbdfc6ee4b`.
- Tokens are stored in DB and never expire by default → revoke by deleting the row.
- Compare with JWT: JWT is **stateless** (validated by signature), token auth is **stateful** (DB lookup per request).

---

### ❓ How does JWT authentication work with `djangorestframework-simplejwt`?
**Answer:**
### 📊 Diagram — JWT login and refresh flow

```mermaid
sequenceDiagram
    participant C as Client
    participant A as Django API
    participant DB as Database
    C->>A: POST /api/token/ (username, password)
    A->>DB: Verify credentials
    DB-->>A: User OK
    A-->>C: access (5 min) + refresh (1 day)
    C->>A: GET /api/books/ with Authorization Bearer access
    A->>A: Verify signature and expiry (no DB hit)
    A-->>C: 200 OK data
    C->>A: POST /api/token/refresh/ (refresh)
    A-->>C: new access token
    C->>A: POST /api/token/blacklist/ (logout)
    A->>DB: Store refresh token in blacklist
```

```python
# settings.py
from datetime import timedelta
INSTALLED_APPS += ['rest_framework_simplejwt.token_blacklist']   # required for blacklist view
SIMPLE_JWT = {
    'ACCESS_TOKEN_LIFETIME': timedelta(minutes=5),
    'REFRESH_TOKEN_LIFETIME': timedelta(days=1),
    'ROTATE_REFRESH_TOKENS': True,
    'BLACKLIST_AFTER_ROTATION': True,
    'AUTH_HEADER_TYPES': ('Bearer',),
}

# urls.py
from rest_framework_simplejwt.views import (
    TokenObtainPairView, TokenRefreshView, TokenBlacklistView)
urlpatterns = [
    path('api/token/', TokenObtainPairView.as_view()),
    path('api/token/refresh/', TokenRefreshView.as_view()),
    path('api/token/blacklist/', TokenBlacklistView.as_view()),
]
```
**Interview points:**
- Access token is short-lived; refresh token is long-lived and rotated.
- To revoke a JWT before expiry you need a blacklist (DB/Redis) — this breaks pure statelessness.
- Store tokens in `httpOnly` cookies or memory on the frontend, not `localStorage`, to reduce XSS risk.
- Custom claims: override `TokenObtainPairSerializer.get_token(cls, user)` and add `token['role'] = user.role`.

---

### ❓ How do you write a custom authentication class?
**Answer:**
```python
from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed

class APIKeyAuthentication(BaseAuthentication):
    def authenticate(self, request):
        key = request.headers.get('X-API-Key')
        if not key:
            return None                          # let other auth classes try
        try:
            client = APIClient.objects.get(key=key, is_active=True)
        except APIClient.DoesNotExist:
            raise AuthenticationFailed('Invalid API key.')
        return (client.user, key)                # (request.user, request.auth)

    def authenticate_header(self, request):
        return 'X-API-Key'                        # makes unauthenticated responses 401
```
Return `None` → "not my credentials, try the next class". Raise `AuthenticationFailed` → reject.

---

### ❓ Session authentication and CSRF — why does it matter?
**Answer:**
`SessionAuthentication` relies on cookies, so browsers attach credentials automatically — this makes it vulnerable to **CSRF**. DRF enforces CSRF checks for authenticated session requests. Token/JWT clients that send `Authorization` headers are not subject to CSRF in the same way, which is why `APIView` is `csrf_exempt` at the view level and CSRF is enforced only inside `SessionAuthentication`.

---

## 8. Permissions

### ❓ How do permissions work in DRF?
**Answer:**
Two hooks on `BasePermission`:
- `has_permission(request, view)` — runs for every request (before the handler).
- `has_object_permission(request, view, obj)` — runs only when `get_object()` is called (retrieve/update/destroy on a single object).

```python
from rest_framework.permissions import BasePermission, SAFE_METHODS

class IsOwnerOrReadOnly(BasePermission):
    def has_permission(self, request, view):
        return request.user and request.user.is_authenticated

    def has_object_permission(self, request, view, obj):
        if request.method in SAFE_METHODS:       # GET, HEAD, OPTIONS
            return True
        return obj.owner == request.user
```

### 📊 Diagram — Permission check flow

```mermaid
flowchart TD
    A["Incoming request"] --> B["check_permissions: has_permission on every class"]
    B -- any False --> C["Denied: 401 if unauthenticated, else 403"]
    B -- all True --> D["Handler runs"]
    D --> E["get_object() called"]
    E --> F["check_object_permissions: has_object_permission"]
    F -- False --> G["403 Forbidden"]
    F -- True --> H["Return object"]
```

---

### ❓ What built-in permission classes exist?
**Answer:**
| Class | Rule |
|---|---|
| `AllowAny` | Everyone (default if nothing set) |
| `IsAuthenticated` | `request.user.is_authenticated` |
| `IsAdminUser` | `user.is_staff` |
| `IsAuthenticatedOrReadOnly` | Auth for writes, anonymous reads |
| `DjangoModelPermissions` | Uses Django's `add/change/delete/view` model permissions |
| `DjangoModelPermissionsOrAnonReadOnly` | Same, but anonymous read allowed |
| `DjangoObjectPermissions` | Object-level via `django-guardian` or custom backend |

---

### ❓ How do you combine permissions (AND, OR, NOT)?
**Answer:**
DRF supports operator composition (3.9+):
```python
from rest_framework.permissions import IsAuthenticated, IsAdminUser

class BookViewSet(viewsets.ModelViewSet):
    def get_permissions(self):
        if self.action in ('list', 'retrieve'):
            return [AllowAny()]
        if self.action == 'destroy':
            return [IsAuthenticated() & IsAdminUser()]      # AND
        return [IsAuthenticated() & (IsOwnerOrReadOnly() | IsAdminUser())]  # OR
```
- `&` — AND, `|` — OR, `~` — NOT.
- Use `get_permissions()` to vary permissions per action.

---

### ❓ Why must you not rely on `get_queryset` alone for security?
**Answer:**
`get_queryset` filters what a user can *see* in lists; `has_object_permission` protects direct access by PK. If you only restrict the queryset and not the object permission, a bad actor can still act on objects they can guess the ID of when the permission check is missing. Use **both**: scoped `get_queryset()` for data isolation plus object permissions for writes.

---

## 9. Throttling (Rate Limiting)

### ❓ How does throttling work in DRF?
**Answer:**
Throttles track request history in the **Django cache** (Redis/Memcached recommended in production) and return `429 Too Many Requests` with a `Retry-After` header when the rate is exceeded.

```python
REST_FRAMEWORK = {
    'DEFAULT_THROTTLE_CLASSES': [
        'rest_framework.throttling.AnonRateThrottle',
        'rest_framework.throttling.UserRateThrottle',
    ],
    'DEFAULT_THROTTLE_RATES': {
        'anon': '100/day',
        'user': '1000/day',
        'burst': '60/min',
    },
}
```

Custom scope per view:
```python
from rest_framework.throttling import UserRateThrottle

class BurstThrottle(UserRateThrottle):
    scope = 'burst'          # looks up DEFAULT_THROTTLE_RATES['burst']

class ReportView(APIView):
    throttle_classes = [BurstThrottle]
```

**Interview points:**
- `AnonRateThrottle` keys by IP; `UserRateThrottle` keys by user id.
- Use `ScopedRateThrottle` with `throttle_scope = 'reports'` on a view for per-endpoint limits.
- Behind a load balancer, make sure `REMOTE_ADDR` is the real client IP (configure `X-Forwarded-For` handling carefully to avoid spoofing).
- Throttling is per-process cache-based; a per-process `LocMemCache` will not share counts across workers.

---

## 10. Pagination

### ❓ What pagination styles does DRF support?
**Answer:**
| Class | Query params | Pros | Cons |
|---|---|---|---|
| `PageNumberPagination` | `?page=3` | Simple, random access | Slow on deep pages, unstable with inserts |
| `LimitOffsetPagination` | `?limit=20&offset=40` | Flexible | `OFFSET` gets slow on large tables |
| `CursorPagination` | `?cursor=...` | Fast, stable for feeds | No random access, needs an ordering field |

```python
REST_FRAMEWORK = {
    'DEFAULT_PAGINATION_CLASS': 'rest_framework.pagination.PageNumberPagination',
    'PAGE_SIZE': 20,
}

from rest_framework.pagination import CursorPagination

class BookCursorPagination(CursorPagination):
    page_size = 20
    ordering = '-published_at'      # must be indexed, unique-ish
    cursor_query_param = 'cursor'
    max_page_size = 100
```
Custom page size per request:
```python
class StandardPagination(PageNumberPagination):
    page_size = 20
    page_size_query_param = 'page_size'    # ?page_size=50
    max_page_size = 100
```

**Interview point:** `OFFSET 1000000` forces Postgres to scan and discard rows → use keyset/cursor pagination for large datasets.

---

## 11. Filtering, Searching, Ordering

### ❓ What filter backends are available and how do you configure them?
**Answer:**
| Backend | Source | Purpose |
|---|---|---|
| `SearchFilter` | DRF built-in | `?search=` across `search_fields` |
| `OrderingFilter` | DRF built-in | `?ordering=-price` |
| `DjangoFilterBackend` | `django-filter` | Field-level filters (`?price__gte=10`) |

```python
import django_filters
from rest_framework import filters

class BookFilter(django_filters.FilterSet):
    min_price = django_filters.NumberFilter(field_name='price', lookup_expr='gte')
    max_price = django_filters.NumberFilter(field_name='price', lookup_expr='lte')
    title = django_filters.CharFilter(lookup_expr='icontains')

    class Meta:
        model = Book
        fields = ['author', 'published']

class BookViewSet(viewsets.ModelViewSet):
    queryset = Book.objects.all()
    filter_backends = [
        django_filters.rest_framework.DjangoFilterBackend,
        filters.SearchFilter,
        filters.OrderingFilter,
    ]
    filterset_class = BookFilter
    search_fields = ['title', '=isbn', '^author__name']   # prefixes: ^ startswith, = exact, @ full-text
    ordering_fields = ['price', 'published_at']
    ordering = ['-published_at']
```

**Search prefixes:** `^` starts-with, `=` exact, `@` full-text (Postgres), none = icontains.

---

### ❓ How do you implement a custom filter?
**Answer:**
```python
from rest_framework.filters import BaseFilterBackend

class OwnerFilter(BaseFilterBackend):
    def filter_queryset(self, request, queryset, view):
        owner = request.query_params.get('owner')
        if owner:
            return queryset.filter(owner__username=owner)
        return queryset
```
Add to `filter_backends`. Interview point: a filter backend is a pipeline step that receives and returns a `QuerySet`.

---

## 12. API Versioning

### ❓ What versioning strategies does DRF support?
**Answer:**
| Class | Example | How |
|---|---|---|
| `URLPathVersioning` | `/api/v1/books/` | Version in URL path (most common) |
| `NamespaceVersioning` | Namespaced `include()` | Same as URL but via namespaces |
| `AcceptHeaderVersioning` | `Accept: application/json; version=1.0` | Header-based |
| `QueryParameterVersioning` | `/books/?version=1.0` | Query string |
| `HostNameVersioning` | `v1.api.example.com` | Subdomain |

```python
REST_FRAMEWORK = {
    'DEFAULT_VERSIONING_CLASS': 'rest_framework.versioning.URLPathVersioning',
    'DEFAULT_VERSION': 'v1',
    'ALLOWED_VERSIONS': ['v1', 'v2'],
}

# urls.py
urlpatterns = [path('api/<str:version>/', include(router.urls))]

# in a view
class BookViewSet(viewsets.ModelViewSet):
    def get_serializer_class(self):
        if self.request.version == 'v2':
            return BookSerializerV2
        return BookSerializer
```

---

## 13. Exception Handling & Error Responses

### ❓ How does DRF handle exceptions?
**Answer:**
`APIView.handle_exception()` catches exceptions. DRF's own exceptions (`ValidationError`, `NotFound`, `PermissionDenied`, `AuthenticationFailed`, `Throttled`, `MethodNotAllowed`) map to HTTP codes. Unknown exceptions become `500` (and are re-raised to Django).

| Exception | Status |
|---|---|
| `ValidationError` | 400 |
| `AuthenticationFailed` / `NotAuthenticated` | 401 |
| `PermissionDenied` | 403 |
| `NotFound` | 404 |
| `MethodNotAllowed` | 405 |
| `Throttled` | 429 |

### ❓ How do you customize the error response format?
**Answer:**
```python
# utils/exceptions.py
from rest_framework.views import exception_handler
from rest_framework.response import Response

def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)   # DRF default first
    if response is not None:
        response.data = {
            'error': {
                'code': response.status_code,
                'details': response.data,
            }
        }
    return response

# settings.py
REST_FRAMEWORK = {'EXCEPTION_HANDLER': 'utils.exceptions.custom_exception_handler'}
```

Raise DRF exceptions in your business logic:
```python
from rest_framework.exceptions import NotFound, PermissionDenied

def get_object(self):
    obj = super().get_object()
    if obj.is_archived:
        raise PermissionDenied("Archived books are read-only.")
    return obj
```

---

## 14. Caching

### ❓ How do you cache DRF responses?
**Answer:**
**1. Per-view cache (decorator on dispatch):**
```python
from django.utils.decorators import method_decorator
from django.views.decorators.cache import cache_page
from django.views.decorators.vary import vary_on_headers

@method_decorator(cache_page(60 * 15), name='dispatch')
@method_decorator(vary_on_headers('Authorization'), name='dispatch')
class BookListView(generics.ListAPIView):
    queryset = Book.objects.all()
    serializer_class = BookSerializer
```

**2. Manual low-level cache with the Django cache framework:**
```python
from django.core.cache import cache

def list(self, request, *args, **kwargs):
    key = f"books:list:{request.query_params.urlencode()}"
    data = cache.get(key)
    if data is None:
        data = self.get_serializer(self.get_queryset(), many=True).data
        cache.set(key, data, timeout=300)
    return Response(data)
```

**3. Invalidate on write** using signals:
```python
from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver

@receiver([post_save, post_delete], sender=Book)
def invalidate_book_cache(sender, **kwargs):
    cache.delete_pattern('books:list:*')   # django-redis; else version keys
```

**Interview points:**
- Never cache per-user data under a shared key — include the user id in the key or use `Vary`.
- Prefer **cache-aside** (read through cache, write invalidates) for simplicity.

---

## Part C — Data, Django Core & Production

## 15. Django ORM Essentials for DRF

### ❓ What is a QuerySet and when is the database actually hit?
**Answer:**
A `QuerySet` is **lazy** — building `Book.objects.filter(...)` does not run SQL. SQL runs when the queryset is **evaluated**: iteration, `list()`, `len()`, `bool()`, slicing with a step, `repr()`, or serializing it.

```python
qs = Book.objects.filter(price__lt=20)     # no query yet
for b in qs: ...                            # query runs here
len(qs)                                     # uses cache of qs (if evaluated)
qs.count()                                  # separate COUNT(*) query, no row fetch
qs.exists()                                 # cheaper than bool(qs) for existence checks
```

### 📊 Diagram — QuerySet lifecycle

```mermaid
flowchart LR
    A["Book.objects.filter(...)"] --> B["QuerySet built lazily, no SQL"]
    B --> C{"Evaluated?"}
    C -- "iterate, list(), len(), bool()" --> D["SQL executed, results cached in _result_cache"]
    C -- "count(), exists(), aggregate()" --> E["Specialized SQL, no full row fetch"]
    B -- "chained filter/order_by" --> B
```

---

### ❓ Explain `filter()` vs `exclude()` vs `get()` vs `first()`.
**Answer:**
- `filter()` → returns QuerySet (0..n rows).
- `exclude()` → QuerySet of rows NOT matching.
- `get()` → single object; raises `DoesNotExist` or `MultipleObjectsReturned`.
- `first()` / `last()` → single object or `None`; adds `ORDER BY pk` if unordered.

---

### ❓ Field lookups — what are they?
**Answer:**
Double-underscore syntax translates to SQL operators:
```python
Book.objects.filter(title__icontains="django")   # ILIKE '%django%'
Book.objects.filter(price__gte=10)               # price >= 10
Book.objects.filter(published_at__year=2024)     # EXTRACT(year ...)
Book.objects.filter(author__name="Ann")          # JOIN through FK
Book.objects.filter(tags__name__in=["py","dj"])  # M2M IN
Book.objects.filter(title__isnull=True)
```

---

### ❓ What are `Q` objects and `F` expressions?
**Answer:**
- `Q` — combine conditions with `|` (OR), `&` (AND), `~` (NOT).
- `F` — reference another column **in the database** (no Python round-trip, avoids race conditions).

```python
from django.db.models import Q, F

Book.objects.filter(Q(price__lt=10) | Q(author__name="Ann"))
Book.objects.filter(~Q(published=True))

# atomic increment without race condition
Book.objects.filter(pk=1).update(stock=F('stock') - 1)

# compare two columns
Book.objects.filter(sold_count__gt=F('stock'))
```

---

### ❓ `annotate()` vs `aggregate()` vs `values()`?
**Answer:**
| Method | Returns | Use |
|---|---|---|
| `aggregate()` | `dict` (one row) | Totals over the whole queryset: `aggregate(avg=Avg('price'))` |
| `annotate()` | QuerySet, each object gets extra attr | Per-row computed value: `annotate(n=Count('books'))` |
| `values()` | QuerySet of dicts | Only selected columns, skips model instantiation |
| `values_list(flat=True)` | QuerySet of tuples/scalars | Lightweight lists of ids |

```python
from django.db.models import Count, Avg, Sum, Max

Author.objects.annotate(book_count=Count('books')).filter(book_count__gte=3)
Book.objects.aggregate(avg_price=Avg('price'), total=Sum('price'))
Book.objects.values('author__name').annotate(n=Count('id'))   # GROUP BY author name
```

---

### ❓ What are `Subquery`, `Exists`, and `OuterRef`?
**Answer:**
```python
from django.db.models import OuterRef, Subquery, Exists

latest_review = Review.objects.filter(book=OuterRef('pk')).order_by('-created')
Book.objects.annotate(last_rating=Subquery(latest_review.values('rating')[:1]))

Book.objects.annotate(has_reviews=Exists(Review.objects.filter(book=OuterRef('pk'))))
```

---

### ❓ `select_related` vs `prefetch_related` — when to use which?
**Answer:**
| | `select_related` | `prefetch_related` |
|---|---|---|
| Relations | ForeignKey, OneToOne (forward) | ManyToMany, reverse FK |
| Mechanism | SQL `JOIN` | Separate query + Python join |
| Round trips | 1 | 1 per relation |

```python
Book.objects.select_related('author')                 # FK
Author.objects.prefetch_related('books')              # reverse FK
Book.objects.prefetch_related(
    Prefetch('reviews', queryset=Review.objects.filter(approved=True), to_attr='approved_reviews')
)
```

---

### ❓ Custom managers and QuerySets — when and why?
**Answer:**
Encapsulate reusable filters in one place:
```python
class BookQuerySet(models.QuerySet):
    def published(self):
        return self.filter(published=True)

    def by_author(self, author):
        return self.filter(author=author)

class Book(models.Model):
    objects = BookQuerySet.as_manager()

Book.objects.published().by_author(ann)
```

---

### ❓ `related_name`, `on_delete`, and reverse relations?
**Answer:**
```python
class Book(models.Model):
    author = models.ForeignKey(Author, on_delete=models.CASCADE, related_name='books')
author.books.all()           # reverse accessor via related_name
```
`on_delete` options: `CASCADE`, `PROTECT` (raise), `SET_NULL`, `SET_DEFAULT`, `DO_NOTHING` (dangerous), `RESTRICT`.

---

### ❓ Model `save()` vs `update()` vs `bulk_create()` vs `bulk_update()`?
**Answer:**
- `instance.save()` — one `INSERT`/`UPDATE`, triggers `pre_save`/`post_save` signals and custom `save()` logic.
- `QuerySet.update()` — single `UPDATE ... WHERE`, **skips** `save()` and signals, returns row count.
- `bulk_create(objs, batch_size=...)` — one multi-row `INSERT`, **skips** `save()` and signals; PKs are set on the returned objects on Postgres (and on backends that support `RETURNING`, depending on Django version).
- `bulk_update(objs, fields)` — efficient batched update of selected fields.

---

### ❓ Custom user model — why should you set it at the start of a project?
**Answer:**
Django's `User` is hard to swap later. Set `AUTH_USER_MODEL = 'accounts.User'` at the start (subclass `AbstractUser` or `AbstractBaseUser`). Changing it mid-project requires painful migrations.

---

## 16. ORM Performance (N+1, only, iterator, bulk)

### ❓ What is the N+1 problem? How do you detect and fix it?
**Answer:**
```python
# BAD: 1 query for books + N queries for authors
for book in Book.objects.all():
    print(book.author.name)

# GOOD: 1 query with JOIN
for book in Book.objects.select_related('author'):
    print(book.author.name)
```
**Detection:** `django-debug-toolbar` (SQL panel), `django-silk`, `CaptureQueriesContext` in tests:
```python
from django.db import connection
from django.test.utils import CaptureQueriesContext

with CaptureQueriesContext(connection) as ctx:
    list(Book.objects.select_related('author'))
assert len(ctx.captured_queries) == 1
```
In DRF, N+1 most often comes from `SerializerMethodField` or nested serializers touching relations — always check `get_queryset()`.

---

### ❓ What are `only()`, `defer()`, and `iterator()`?
**Answer:**
```python
Book.objects.only('id', 'title')          # SELECT id, title only
Book.objects.defer('large_description')   # all columns except this one
Book.objects.iterator(chunk_size=2000)    # streams rows; low memory for big exports
```
Caution: accessing a deferred field triggers an extra query per row.

---

### ❓ How do you handle large datasets efficiently?
**Answer:**
- Use `iterator()` for memory-bounded processing.
- Use **cursor/keyset pagination** instead of `OFFSET` for deep pages.
- Batch writes with `bulk_create` / `bulk_update` and `batch_size`.
- Add **database indexes** on filtered/ordered fields (`db_index=True`, `Meta.indexes`, `Index(fields=[...])`).
- Use `exists()` instead of `count() > 0`.
- Use `values()` / `values_list()` for lightweight reads.

---

### ❓ How do you add database indexes and constraints?
**Answer:**
```python
from django.db.models import Index, UniqueConstraint, CheckConstraint, Q

class Book(models.Model):
    title = models.CharField(max_length=200, db_index=True)
    price = models.DecimalField(max_digits=6, decimal_places=2)

    class Meta:
        indexes = [Index(fields=['author', '-published_at'], name='book_author_pub_idx')]
        constraints = [
            UniqueConstraint(fields=['isbn'], name='unique_isbn'),
            CheckConstraint(check=Q(price__gte=0), name='price_non_negative'),
        ]
```
Interview point: put the most selective / most-filtered column first in composite indexes. Partial indexes: `Index(fields=[...], condition=Q(...))`.

---

## 17. Transactions & Data Consistency

### ❓ How do transactions work in Django? What is `ATOMIC_REQUESTS`?
**Answer:**
- Django defaults to **autocommit**: each query commits immediately.
- `transaction.atomic()` groups operations: all succeed or all roll back.
- `ATOMIC_REQUESTS = True` wraps each **view** in a transaction (simple but holds DB locks for the whole request, including slow external calls).

```python
from django.db import transaction

@transaction.atomic
def place_order(data):
    order = Order.objects.create(**data)
    Stock.objects.filter(pk=data['item']).update(qty=F('qty') - 1)
    return order

with transaction.atomic():
    ...   # savepoints when nested
```

---

### ❓ How do you prevent race conditions (lost updates)?
**Answer:**
| Technique | Example |
|---|---|
| `F()` expressions | `Book.objects.filter(pk=1).update(stock=F('stock') - 1)` |
| Pessimistic lock | `Book.objects.select_for_update().get(pk=1)` inside `atomic()` |
| Optimistic lock | `version` field; `update(..., version=F('version')+1).filter(version=old)` |
| Unique constraints | Let the DB reject duplicates |

```python
with transaction.atomic():
    book = Book.objects.select_for_update().get(pk=pk)   # row locked until commit
    if book.stock <= 0:
        raise ValidationError("Out of stock")
    book.stock -= 1
    book.save(update_fields=['stock'])
```

---

### ❓ What is `transaction.on_commit` and why is it important?
**Answer:**
Schedules a callback to run **only after the transaction commits** — e.g., sending a Celery task or webhook. Without it, a task can run before the row is visible to the worker, or run even though the transaction rolled back.
```python
with transaction.atomic():
    order = Order.objects.create(...)
    transaction.on_commit(lambda: send_order_email.delay(order.id))
```

---

## 18. Django Core Concepts Asked in Interviews

### ❓ Explain Django's request/response cycle and middleware.
**Answer:**
Middleware is an ordered list of hooks wrapping every request/response. Order in `MIDDLEWARE` matters: request phase top → bottom, response phase bottom → top.

### 📊 Diagram — Django middleware onion

```mermaid
flowchart TD
    A["Request"] --> M1["SecurityMiddleware"]
    M1 --> M2["SessionMiddleware"]
    M2 --> M3["CommonMiddleware"]
    M3 --> M4["AuthenticationMiddleware: request.user"]
    M4 --> M5["Custom middleware"]
    M5 --> V["URL resolver then View / DRF APIView"]
    V --> R["Response"]
    R --> M5b["Custom middleware (response)"]
    M5b --> M4b["AuthenticationMiddleware"]
    M4b --> M3b["CommonMiddleware"]
    M3b --> M2b["SessionMiddleware"]
    M2b --> M1b["SecurityMiddleware"]
    M1b --> Out["Client"]
```

```python
class TimingMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response         # called once at server start

    def __call__(self, request):
        start = time.time()
        response = self.get_response(request)    # runs view + inner middleware
        response['X-Duration'] = str(time.time() - start)
        return response
```

---

### ❓ What is the difference between `WSGI` and `ASGI`?
**Answer:**
- **WSGI** — synchronous interface (gunicorn, uWSGI). One request per worker thread/process at a time.
- **ASGI** — async interface (uvicorn, daphne). Supports WebSockets, long-polling, async views.
- Django supports both; DRF is mostly sync (async views are partially supported).

---

### ❓ What are Django signals and when would you avoid them?
**Answer:**
Signals (`pre_save`, `post_save`, `pre_delete`, `post_delete`, `m2m_changed`, `request_finished`) decouple code. Pitfalls:
- Implicit control flow; hard to debug.
- Run inside the same transaction; an exception inside a receiver rolls back the save.
- Bulk operations (`update`, `bulk_create`) **don't fire** them.

Prefer explicit service-layer calls for critical business logic; use signals for cross-cutting concerns (audit logs, cache invalidation).

---

### ❓ What is the Django admin and how do you customize it?
**Answer:**
```python
from django.contrib import admin

@admin.register(Book)
class BookAdmin(admin.ModelAdmin):
    list_display = ('title', 'author', 'price')
    list_filter = ('published',)
    search_fields = ('title', 'author__name')
    readonly_fields = ('created_at',)
    list_select_related = ('author',)     # avoids N+1 in admin list
```

---

### ❓ What are Django's `settings` for production that interviewers expect you to know?
**Answer:**
| Setting | Production value / note |
|---|---|
| `DEBUG` | `False` |
| `SECRET_KEY` | From env var, never committed |
| `ALLOWED_HOSTS` | Explicit list of domains |
| `DATABASES` | PostgreSQL with `CONN_MAX_AGE` for connection reuse |
| `CACHES` | Redis backend |
| `STATIC_ROOT` + WhiteNoise / CDN | Serve static files |
| `SECURE_SSL_REDIRECT`, `SESSION_COOKIE_SECURE`, `CSRF_COOKIE_SECURE` | HTTPS only |
| `SECURE_HSTS_SECONDS` | HSTS header |
| `LOGGING` | Structured logs to stdout |

---

### ❓ Explain Django's `makemigrations`/`migrate` pitfalls.
**Answer:**
- Commit migration files; never edit applied migrations.
- Data migrations: use `RunPython` with reversible functions.
- Adding a non-null field to existing rows needs a default or a 3-step migration (add nullable → backfill → make non-null).
- Long migrations lock tables; use `CREATE INDEX CONCURRENTLY` (`AddIndexConcurrently` from `django.contrib.postgres`) on big tables.

---

### ❓ What is `django.contrib.contenttypes` / generic relations?
**Answer:**
Lets a model point to any other model via `ContentType` + `object_id` (`GenericForeignKey`). Used by comments, tags, audit logs. Downside: no DB-level FK integrity.

---

## 19. Testing DRF APIs

### ❓ How do you test DRF endpoints?
**Answer:**
```python
from rest_framework.test import APITestCase, APIClient
from rest_framework import status

class BookAPITests(APITestCase):
    @classmethod
    def setUpTestData(cls):
        cls.user = User.objects.create_user('ann', password='pw12345')
        cls.book = Book.objects.create(title='Django', price=20, owner=cls.user)

    def setUp(self):
        self.client.force_authenticate(user=self.user)   # skip real auth in tests

    def test_list_books(self):
        response = self.client.get('/api/books/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data['results']), 1)

    def test_create_requires_valid_payload(self):
        response = self.client.post('/api/books/', {'title': ''}, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('title', response.data)

    def test_cannot_delete_others_book(self):
        other = User.objects.create_user('bob', password='pw')
        self.client.force_authenticate(user=other)
        response = self.client.delete(f'/api/books/{self.book.pk}/')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
```

**Interview points:**
- `APIClient.force_authenticate()` bypasses auth for focused tests; test auth separately with real tokens.
- `setUpTestData` runs once per class (faster than `setUp`).
- Use `assertNumQueries(n)` to lock in query-count regressions.
- Alternative runner: `pytest` + `pytest-django` with `@pytest.mark.django_db` and `api_client` fixtures.

```python
def test_book_list_query_count(client, django_assert_num_queries, books):
    with django_assert_num_queries(2):
        client.get('/api/books/')
```

---

### ❓ How do you test serializers and permissions in isolation?
**Answer:**
```python
def test_price_validation():
    s = BookSerializer(data={'title': 'X', 'price': -1})
    assert not s.is_valid()
    assert 'price' in s.errors

def test_owner_permission(rf, user, book):
    request = rf.delete('/')
    request.user = user
    assert not IsOwnerOrReadOnly().has_object_permission(request, None, book)
```
`rf` is pytest-django's `RequestFactory`.

---

## 20. Security Best Practices

### ❓ What are the common API security concerns and how does DRF help?
**Answer:**
| Risk | Mitigation in DRF / Django |
|---|---|
| Broken object-level auth (IDOR) | Scoped `get_queryset()` + `has_object_permission` |
| Mass assignment | Explicit `fields` in serializer; never `fields='__all__'` on writes |
| Excessive data exposure | `write_only` for secrets; separate read/write serializers |
| Brute force / abuse | Throttling; account lockout (`django-axes`) |
| CSRF | Enforced in `SessionAuthentication`; use tokens for APIs |
| Injection | ORM parameterizes queries; avoid `raw()` / string-built SQL |
| XSS in JSON consumers | Correct `Content-Type`; `X-Content-Type-Options: nosniff` |
| Insecure transport | HTTPS, HSTS, `SECURE_*` settings |
| Secrets leak | Env vars, never commit `SECRET_KEY` |
| Unbounded lists | Pagination with `max_page_size` |

```python
class BookWriteSerializer(serializers.ModelSerializer):
    class Meta:
        model = Book
        fields = ['title', 'price']          # 'owner' is NOT client-settable (mass assignment)
```

---

### ❓ How do you disable the Browsable API in production?
**Answer:**
```python
REST_FRAMEWORK = {
    'DEFAULT_RENDERER_CLASSES': ['rest_framework.renderers.JSONRenderer'],
}
```

---

### ❓ How do you handle CORS with DRF?
**Answer:**
Use `django-cors-headers`:
```python
INSTALLED_APPS += ['corsheaders']
MIDDLEWARE = ['corsheaders.middleware.CorsMiddleware', ...]   # as high as possible
CORS_ALLOWED_ORIGINS = ['https://app.example.com']
```
Never set `CORS_ALLOW_ALL_ORIGINS = True` with credentials in production.

---

## 21. API Documentation (OpenAPI / drf-spectacular)

### ❓ How do you generate API docs for a DRF project?
**Answer:**
Use **`drf-spectacular`** (OpenAPI 3 generator):
```python
INSTALLED_APPS += ['drf_spectacular']
REST_FRAMEWORK['DEFAULT_SCHEMA_CLASS'] = 'drf_spectacular.openapi.AutoSchema'

# urls.py
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView
urlpatterns += [
    path('api/schema/', SpectacularAPIView.as_view(), name='schema'),
    path('api/docs/', SpectacularSwaggerView.as_view(url_name='schema')),
]
```
Customize per view with `@extend_schema` (request/response serializers, parameters, examples).

Legacy: DRF's built-in schema generation (`get_schema_view`) still works but is less featured.

---

## 22. Async with Django & DRF

### ❓ Does DRF support async views? What are the limits?
**Answer:**
- Django 3.1+ supports `async def` views, but **DRF's core (serializers, ORM-backed `get_object()`, authentication, permissions) is essentially synchronous**.
- Django ORM has async query methods (`aget`, `acreate`, `async for` over a QuerySet) from Django 4.1+, but DRF's serializers and viewset machinery don't use them — calling sync ORM code inside an async context raises `SynchronousOnlyOperation`.
- Practical guidance: keep DRF sync; use **FastAPI** or Django Channels for high-concurrency async workloads; offload slow I/O to **Celery**.

```python
from asgiref.sync import sync_to_async

async def external_price(request, pk):
    price = await sync_to_async(lambda: Book.objects.get(pk=pk).price)()
    async with httpx.AsyncClient() as client:
        r = await client.get(f"https://pricing.example.com/{pk}")
    return JsonResponse({'price': price, 'external': r.json()})
```

---

### ❓ How do you run long-running work in a DRF app? (Celery)
**Answer:**
```python
# tasks.py
from celery import shared_task

@shared_task(bind=True, autoretry_for=(ConnectionError,), retry_backoff=True, max_retries=5)
def send_report(self, report_id):
    report = Report.objects.get(pk=report_id)
    generate_pdf(report)

# views.py
class ReportViewSet(viewsets.ModelViewSet):
    @action(detail=True, methods=['post'])
    def generate(self, request, pk=None):
        send_report.delay(pk)
        return Response({'status': 'queued'}, status=status.HTTP_202_ACCEPTED)
```
Return `202 Accepted` and let the client poll a status endpoint or receive a webhook.

### 📊 Diagram — Async job pattern with Celery

```mermaid
sequenceDiagram
    participant C as Client
    participant API as DRF API
    participant Q as Broker (Redis/RabbitMQ)
    participant W as Celery Worker
    participant DB as Database
    C->>API: POST /reports/{id}/generate/
    API->>Q: send_report.delay(id)
    API-->>C: 202 Accepted (job queued)
    Q->>W: deliver task
    W->>DB: load + update report status
    W-->>Q: task result stored
    C->>API: GET /reports/{id}/ (poll)
    API-->>C: 200 status = done
```

---

## 23. Production Deployment & Architecture

### ❓ How do you deploy a Django + DRF app?
**Answer:**
### 📊 Diagram — Typical production architecture

```mermaid
flowchart LR
    U["Clients: web, mobile"] --> LB["Load balancer / Nginx TLS"]
    LB --> G1["Gunicorn + Django/DRF worker 1"]
    LB --> G2["Gunicorn + Django/DRF worker 2"]
    G1 --> PG[("PostgreSQL primary")]
    G2 --> PG
    PG --> RR[("Read replica")]
    G1 --> R[("Redis: cache + throttle counters")]
    G2 --> R
    G1 --> MQ["Celery broker"]
    G2 --> MQ
    MQ --> CW["Celery workers"]
    CW --> PG
    G1 --> S3["Object storage: media and static"]
```

- **App server:** Gunicorn (WSGI) or Uvicorn workers (ASGI); behind Nginx.
- **Workers:** `gunicorn -w 4 -k gthread myproject.wsgi`; tune with `2 * CPU + 1`.
- **Static files:** `collectstatic` + WhiteNoise or S3/CDN.
- **Containers:** Docker image; env-driven config (`DJANGO_SETTINGS_MODULE`, `DATABASE_URL`).
- **Migrations:** run as a one-off job before rolling out new code.
- **Health checks:** a `/health/` endpoint that checks DB and cache.

---

### ❓ How do you scale and optimize a DRF API?
**Answer:**
1. Measure first: query count (`django-debug-toolbar`), profiling (`cProfile`, `pyinstrument`), APM (Sentry, New Relic).
2. Fix queries: `select_related`/`prefetch_related`, indexes, `only()`.
3. Cache: per-view, low-level, or HTTP caching with `ETag` / `Last-Modified` (`ConditionalGetMiddleware`).
4. Paginate everything; cap `max_page_size`.
5. Move slow work to Celery.
6. Scale horizontally: stateless app servers (use JWT or token auth, store sessions in Redis).
7. Read replicas via database routers (`DATABASE_ROUTERS`).

---

### ❓ How do you structure a large DRF project?
**Answer:**
```
project/
  config/                # settings/, urls.py, wsgi.py, asgi.py
  apps/
    accounts/
      models.py
      serializers.py
      views.py
      services.py        # business logic, no HTTP
      selectors.py       # read-only query helpers
      permissions.py
      tests/
    books/
      ...
```
Principles: **thin views, fat services/selectors**, keep serializers for input/output shape only, keep business rules out of signals and serializers.

---

## 24. Hands-on Coding Scenarios

These are the types of live-coding questions interviewers commonly ask. Be ready to write them from scratch.

### ❓ Scenario 1: Write a `Book` model, serializer, and ModelViewSet with search, filtering and pagination.
**Answer:**
```python
# models.py
class Book(models.Model):
    title = models.CharField(max_length=200)
    price = models.DecimalField(max_digits=6, decimal_places=2)
    owner = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='books')
    created_at = models.DateTimeField(auto_now_add=True)

# serializers.py
class BookSerializer(serializers.ModelSerializer):
    owner = serializers.HiddenField(default=serializers.CurrentUserDefault())

    class Meta:
        model = Book
        fields = ['id', 'title', 'price', 'owner', 'created_at']
        read_only_fields = ['created_at']

    def validate_price(self, value):
        if value <= 0:
            raise serializers.ValidationError('Price must be positive.')
        return value

# views.py
class BookViewSet(viewsets.ModelViewSet):
    serializer_class = BookSerializer
    permission_classes = [IsAuthenticated, IsOwnerOrReadOnly]
    filter_backends = [filters.SearchFilter, filters.OrderingFilter]
    search_fields = ['title']
    ordering_fields = ['price', 'created_at']
    ordering = ['-created_at']

    def get_queryset(self):
        return Book.objects.select_related('owner').filter(owner=self.request.user)

# urls.py
router = DefaultRouter()
router.register('books', BookViewSet, basename='book')
urlpatterns = [path('api/', include(router.urls))]
```

---

### ❓ Scenario 2: Write a custom permission allowing read to everyone and write only to staff.
**Answer:**
```python
class IsStaffOrReadOnly(BasePermission):
    def has_permission(self, request, view):
        if request.method in SAFE_METHODS:
            return True
        return bool(request.user and request.user.is_staff)
```

---

### ❓ Scenario 3: Implement `POST /orders/` that creates an order with items atomically and validates stock.
**Answer:**
```python
class OrderItemInputSerializer(serializers.Serializer):
    product_id = serializers.IntegerField()
    quantity = serializers.IntegerField(min_value=1)

class OrderCreateSerializer(serializers.Serializer):
    items = OrderItemInputSerializer(many=True, allow_empty=False)

from rest_framework.exceptions import ValidationError

class OrderCreateView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        s = OrderCreateSerializer(data=request.data)
        s.is_valid(raise_exception=True)
        items = s.validated_data['items']

        try:
            with transaction.atomic():
                order = Order.objects.create(user=request.user)
                for item in items:
                    product = Product.objects.select_for_update().get(pk=item['product_id'])
                    if product.stock < item['quantity']:
                        raise ValidationError({'items': f'Insufficient stock for {product.pk}'})
                    product.stock -= item['quantity']
                    product.save(update_fields=['stock'])
                    OrderItem.objects.create(order=order, product=product, quantity=item['quantity'])
        except Product.DoesNotExist:
            return Response({'detail': 'Unknown product'}, status=status.HTTP_400_BAD_REQUEST)

        return Response(OrderSerializer(order).data, status=status.HTTP_201_CREATED)
```
Key points: validate input, lock rows, all-or-nothing transaction, correct status codes.

---

### ❓ Scenario 4: Fix this N+1 and slow endpoint.
```python
class BookViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = Book.objects.all()
    serializer_class = BookSerializer    # has author.name and reviews_count
```
**Answer:**
```python
class BookViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = BookSerializer

    def get_queryset(self):
        return (Book.objects
                .select_related('author')
                .annotate(reviews_count=Count('reviews'))   # computed in SQL, not per row
                .order_by('-id'))
```
Replace `SerializerMethodField` that does `obj.reviews.count()` with the annotated field — the method would otherwise issue one query per book.

---

### ❓ Scenario 5: Write a test that verifies only the owner can update a book.
**Answer:** (assumes a pytest fixture `api_client` that returns `APIClient()`)
```python
def test_non_owner_cannot_update(api_client, user, other_user, book):
    api_client.force_authenticate(user=other_user)
    resp = api_client.patch(f'/api/books/{book.pk}/', {'title': 'Hacked'}, format='json')
    assert resp.status_code == 403
    book.refresh_from_db()
    assert book.title != 'Hacked'
```

---

## 25. Quick Revision Checklist

| Area | Key points to remember |
|---|---|
| **Request flow** | `dispatch` → `initial` (auth, perms, throttle) → handler → `finalize_response` |
| **Views** | APIView (manual) → Generics (one class/URL) → ViewSet (router maps actions) |
| **`@action`** | `detail=True` includes pk; `url_path`, `url_name` customizable |
| **Serializer lifecycle** | `is_valid()` → `validated_data` → `save()` → `create()`/`update()` |
| **Validation hooks** | `validate_<field>()` for field, `validate()` for cross-field |
| **Nested writes** | Not automatic — override `create`/`update` in `transaction.atomic()` |
| **`SerializerMethodField`** | Read-only computed; beware N+1; prefer annotations |
| **`HiddenField(CurrentUserDefault())`** | Inject user without trusting client |
| **Routers** | `DefaultRouter` adds API root; `basename` required without `queryset` |
| **Auth** | Session (CSRF!), Token (DB), JWT (stateless, blacklist to revoke) |
| **Permissions** | `has_permission` (every request) + `has_object_permission` (`get_object`) |
| **Scoping** | Filter `get_queryset()` by user to prevent IDOR |
| **Throttling** | Cache-backed; `429` + `Retry-After`; scope per view |
| **Pagination** | PageNumber (simple), LimitOffset, Cursor (large / feeds) |
| **Filtering** | `django-filter` `FilterSet`, `SearchFilter` prefixes `^ = @`, `OrderingFilter` |
| **Versioning** | URLPath most common; set `request.version` |
| **Exceptions** | `EXCEPTION_HANDLER` for uniform error body |
| **QuerySet** | Lazy; evaluated on iteration/`list()`/`len()`; use `exists()`/`count()` |
| **`Q` / `F`** | `Q` for OR/NOT; `F` for DB-side arithmetic (race-safe) |
| **`annotate` vs `aggregate`** | per-row vs whole-set; `values()` for dicts |
| **N+1** | `select_related` (FK), `prefetch_related` (M2M/reverse FK), `Prefetch` for filtered |
| **Bulk ops** | `bulk_create` / `bulk_update` skip `save()` and signals |
| **Transactions** | `atomic()`, `select_for_update()`, `on_commit()` for side-effects |
| **Middleware** | Ordered; request top→bottom, response bottom→top |
| **WSGI vs ASGI** | Gunicorn sync vs Uvicorn async; DRF is mostly sync |
| **Signals** | Decoupled; bulk ops skip them; avoid for core business logic |
| **Testing** | `APITestCase`, `force_authenticate`, `assertNumQueries`, pytest-django |
| **Security** | Explicit serializer fields (mass assignment), HTTPS, CORS config, throttle |
| **Docs** | `drf-spectacular` → OpenAPI + Swagger UI |
| **Long tasks** | Celery + `202 Accepted` + status polling |
| **Production** | Gunicorn + Nginx, Postgres, Redis cache, `collectstatic`, env-based settings |
