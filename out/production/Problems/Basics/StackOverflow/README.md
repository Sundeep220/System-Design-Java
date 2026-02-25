# 📘 StackOverflow — Low Level Design Problem

## 🎯 Problem Statement

Design a simplified version of a Q&A platform similar to StackOverflow where:

* Users can post questions.
* Users can answer questions.
* Users can comment on questions and answers.
* Users can vote on content.
* Questions can have tags.
* Users earn reputation based on votes.
* System should handle concurrent updates safely.

---

# ✅ Functional Requirements

## 1️⃣ User Management

* Each user has:

    * Unique ID
    * Name
    * Reputation score

* Authentication is **out of scope**.

* Users are identified via a unique atomic integer ID.

---

## 2️⃣ Question Management

Users can:

* Create questions
* Edit their own questions
* Delete their own questions

Each question contains:

* Title
* Content
* Author
* Creation time
* Version (for optimistic locking)
* List of tags (max 10)
* List of answers
* List of comments
* Votes

Deleting a question deletes:

* Its answers
* Its comments
* Its votes

(Composition relationship)

---

## 3️⃣ Answer Management

Users can:

* Post answers to questions
* Edit their own answers
* Delete their own answers

Each answer contains:

* Content
* Author
* Creation time
* Version
* Reference to parent question
* Comments
* Votes

---

## 4️⃣ Comment Management

Users can:

* Comment on:

    * Questions
    * Answers

Comments:

* Are not nested (no reply-to-comment)
* Have:

    * Content
    * Author
    * Creation time
    * Reference to parent Post

---

## 5️⃣ Voting System

Users can:

* Upvote
* Downvote
* Undo vote

Voting rules:

* A user can vote only once per post.
* A user cannot both upvote and downvote simultaneously.
* Vote can be changed (upvote → downvote).
* Vote can be removed.

Votes apply to:

* Questions
* Answers

Each vote contains:

* Voter (User)
* Vote type (UPVOTE / DOWNVOTE)
* Target Post

---

## 6️⃣ Reputation System

Reputation is updated incrementally.

Example scoring (configurable):

* +10 for answer upvote
* +5 for question upvote
* −2 for downvote

Rules:

* Reputation increases on upvotes.
* Reputation decreases on downvotes.
* Undoing a vote reverses the reputation impact.
* Reputation is stored and updated incrementally (not computed on demand).

Reputation update is synchronous.

---

## 7️⃣ Tag System

* Tags are reusable.
* Questions can have at most 10 tags.
* Tags are searchable.
* Relationship: Many-to-Many (Question ↔ Tag)

---

## 8️⃣ Search Functionality

Search allows:

* Search by keyword (simple matching)
* Search by tag
* Search by user
* Pagination supported

Design:

* Search logic lives in `SearchService` abstraction.
* Initial implementation: In-memory filtering.
* Future extensibility: ElasticSearch implementation.

---

# ⚙️ Non-Functional Requirements

## 🔹 Concurrency

System must handle:

* Concurrent voting
* Concurrent edits

Approach:

* Use atomic increment for vote counters.
* Use optimistic locking with `version` field in Post.
* Reject stale updates if version mismatch.

---

## 🔹 Consistency Model

* Strong consistency for voting counters.
* Optimistic concurrency for edits.
* Reputation updated synchronously.

---

## 🔹 Scalability (Future Consideration)

* Search abstraction allows migration to ElasticSearch.
* Reputation could be moved to async event processing.
* Vote counters could use atomic DB operations.

---

# 🧱 Final Domain Model

Core Entities:

1. User
2. Post (abstract)
3. Question extends Post
4. Answer extends Post
5. Comment
6. Vote
7. Tag

---

# 🧠 Key Design Decisions

## Abstraction

`Post` is an abstract class containing:

* id
* content
* author
* createdAt
* version
* List<Comment>
* List<Vote>

Encapsulated methods:

* addVote()
* addComment()

---

## Composition

* Question owns Answer (composition)
* Post owns Comment (composition)
* Post owns Vote (composition)

---

## Service Layer

* VoteService
* ReputationService
* QuestionService
* SearchService

Domain objects hold state.
Services orchestrate business rules.

---

# 🔥 Edge Cases Considered

* Duplicate voting prevention
* Vote change handling
* Vote undo handling
* Version conflict on edit
* Max 10 tags validation
* Deleting question cascades answers

---

# 🎯 Final Scope (For LLD Interview)

We will implement:

* Core domain entities
* VoteService
* ReputationService
* Basic SearchService (in-memory)
* No database (in-memory collections)
* No authentication

---

This is now a **clean, interview-ready problem summary**.

---

