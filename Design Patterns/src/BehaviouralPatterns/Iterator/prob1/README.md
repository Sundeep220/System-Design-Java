# 🧠 Problem: **Music Playlist with Skip Iterator**

### 📘 Scenario:

You're designing a **music player playlist**. The playlist supports adding songs and iterating over them. But now the player has a feature where the user wants to **skip every other song** while playing (like a shuffle-lite mode).

---

## 🎯 Requirements

1. Create a `Song` class with a `title` and `artist`.
2. Create a `Playlist` class that:

    * Stores a list of songs
    * Implements `Iterable<Song>`
    * Provides:

        * A **normal iterator** (default)
        * A **skip iterator** (skips every other song)
3. Use Java’s built-in `Iterator<T>` interface (no custom one).

---

## 🧑‍💻 Example Usage

```java
Playlist playlist = new Playlist();
playlist.addSong(new Song("Let It Be", "The Beatles"));
playlist.addSong(new Song("Bohemian Rhapsody", "Queen"));
playlist.addSong(new Song("Imagine", "John Lennon"));
playlist.addSong(new Song("Hotel California", "Eagles"));

System.out.println("Normal iteration:");
for (Song song : playlist) {
    System.out.println(song);
}

System.out.println("\nSkip iteration:");
Iterator<Song> skipIterator = playlist.skipIterator();
while (skipIterator.hasNext()) {
    System.out.println(skipIterator.next());
}
```

---

## 🟢 Output

```
Normal iteration:
Let It Be - The Beatles
Bohemian Rhapsody - Queen
Imagine - John Lennon
Hotel California - Eagles

Skip iteration:
Let It Be - The Beatles
Imagine - John Lennon
```

---
