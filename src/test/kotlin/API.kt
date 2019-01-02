import info.juanmendez.rxstories.model.Album
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import java.io.File

class API {
    companion object {
        fun getBands(): List<Band> {
            val file = File("csv/bands.csv")

            return file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Band(it[0].toInt(), it[1])
                    }
        }

        fun getAlbums(): List<Album> {
            val file = File("csv/albums.csv")

            return file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Album(it[0].toInt(), it[1].toInt(), it[2], it[3].toInt(), it[4])
                    }
        }

        fun getSongs(): List<Song> {
            val file = File("csv/songs.csv")

            return file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Song(it[0].toInt(), it[1], it[2], it[3].toInt(), it[4].toInt())
                    }
        }

        fun getSongsByRange(start: Int, end: Int): List<Song> {
            val songs = API.getSongs().toMutableList()
            val endsAt: Int = Math.min(end, songs.size)
            val startsAt: Int = Math.min(start, endsAt)

            return songs.subList(startsAt, endsAt)
        }
    }
}