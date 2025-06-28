package BehaviouralPatterns.Iterator.prob1;

import java.util.Iterator;

public class Main {
        public static void main(String[] args) {
            Playlist playlist = new Playlist();
            playlist.addSong(new Song("Let It Be", "The Beatles"));
            playlist.addSong(new Song("Bohemian Rhapsody", "Queen"));
            playlist.addSong(new Song("Imagine", "John Lennon"));
            playlist.addSong(new Song("Hotel California", "Eagles"));

            System.out.println("🎵 Normal Iteration:");
            for (Song song : playlist) {
                System.out.println(song);
            }

            System.out.println("\n🎵 Skip Iteration:");
            Iterator<Song> skip = playlist.skipIterator();
            while (skip.hasNext()) {
                System.out.println(skip.next());
            }
        }
}
