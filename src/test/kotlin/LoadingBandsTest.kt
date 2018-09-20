import com.jakewharton.rxrelay2.ReplayRelay
import info.juanmendez.rxstories.model.Album
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import java.io.File

class LoadingBandsTest {
    private val totalSongs = 76

    @Test
    fun loadingBands() {
        val testObserver = TestObserver<List<Band>>()

        //lets load up with a single
        val single = Single.create<List<Band>> { emitter ->
            val file = File("csv/bands.csv")

            val bands: List<Band> = file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Band(it[0].toInt(), it[1])
                    }
            emitter.onSuccess(bands)
        }

        single.subscribe(testObserver)


        testObserver.assertSubscribed()
        testObserver.assertComplete()
        testObserver.assertValue {
            it.size == 7
        }
    }

    @Test
    fun loadingAlbums() {
        val testObserver = TestObserver<List<Album>>()

        //lets load up with a single
        val single = Single.create<List<Album>> { emitter ->
            val file = File("csv/albums.csv")

            val albums: List<Album> = file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Album(it[0].toInt(), it[1].toInt(), it[2], it[3].toInt(), it[4])
                    }
            emitter.onSuccess(albums)
        }

        single.subscribe(testObserver)

        testObserver.assertSubscribed()
        testObserver.assertComplete()

        testObserver.assertValue {
            it.size == 7
        }
    }

    @Test
    fun loadingSongs() {
        val testObserver = TestObserver<List<Song>>()

        //lets load up with a single
        val single = Single.create<List<Song>> {
            val file = File("csv/songs.csv")

            val songs: List<Song> = file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Song(it[0].toInt(), it[1], it[2], it[3].toInt(), it[4].toInt())
                    }
            it.onSuccess(songs)
        }

        single.subscribe(testObserver)

        //assert it has completed, and there are 7 bands..
        testObserver.assertSubscribed()
        testObserver.assertComplete()

        testObserver.assertValue {
            it.size == totalSongs
        }
    }

    fun getSongsByRange(start: Int, end: Int): List<Song> {
        val file = File("csv/songs.csv")
        var startsAt: Int
        var endsAt: Int

        var songs: MutableList<Song> = file.readLines().drop(1)
                .map { it.split(",") }
                .map {
                    Song(it[0].toInt(), it[1], it[2], it[3].toInt(), it[4].toInt())
                }.toMutableList()

        endsAt = Math.min(end, songs.size)
        startsAt = Math.min(start, endsAt)

        //this was intended, just to test handling an exception
        if (start >= songs.size || end >= songs.size) {
            throw IndexOutOfBoundsException()
        }

        return songs.subList(startsAt, endsAt)
    }

    @Test
    fun `using RxRelay for the first time`() {
        val testSubscriber = TestObserver<List<Song>>()

        val relay = ReplayRelay.create<List<Song>>()

        /**
         * How Single can be used as a proxy to track and error
         * and for our relay to define its response from the error
         */
        val requestSongsByRange = fun(start: Int, end: Int) {
            Single.create<List<Song>> {
                try {
                    it.onSuccess(getSongsByRange(start, end))
                } catch (e: Exception) {
                    it.onError(e)
                }
            }.subscribe({
                relay.accept(it)
            }, {
                relay.accept(listOf())
            })
        }

        requestSongsByRange(0, 10)
        requestSongsByRange(10, 20)
        requestSongsByRange(30, 40)
        relay.subscribe(testSubscriber)

        testSubscriber.assertOf {
            it.valueCount().equals(3)
        }

        requestSongsByRange(80, 90)
        testSubscriber.assertValueAt(3) {
            it.isEmpty()
        }
    }
}