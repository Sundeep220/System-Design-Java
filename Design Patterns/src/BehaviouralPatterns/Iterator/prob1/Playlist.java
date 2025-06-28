package BehaviouralPatterns.Iterator.prob1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Playlist implements Iterable<Song> {
    private final List<Song> songs = new ArrayList<>();

    public void addSong(Song song) {
        songs.add(song);
    }

    @Override
    public Iterator<Song> iterator() {
        return songs.iterator();
    }

    // Custom skip iterator: skips every other song
    public Iterator<Song> skipIterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < songs.size();
            }

            @Override
            public Song next() {
                Song song = songs.get(index);
                index += 2; // skip every other
                return song;
            }
        };
    }
}
