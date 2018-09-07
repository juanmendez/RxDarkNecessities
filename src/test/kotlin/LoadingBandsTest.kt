import info.juanmendez.rxstories.model.Album
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import java.io.File

class LoadingBandsTest {
    @Test
    fun `loadingBands`() {
        val testObserver = TestObserver<List<Band>>()

        //lets load up with a single
        val single = Single.create<List<Band>> {
            val file = File("csv/bands.csv")

            val bands: List<Band> = file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Band(it[0].toInt(), it[1])
                    }
            it.onSuccess(bands)
        }

        single.subscribe(testObserver)


        testObserver.assertSubscribed()
        testObserver.assertComplete()
        testObserver.assertValue {
            it.size == 7
        }
    }

    @Test
    fun `loadingAlbums`() {
        val testObserver = TestObserver<List<Album>>()

        //lets load up with a single
        val single = Single.create<List<Album>> {
            val file = File("csv/albums.csv")

            val albums: List<Album> = file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Album(it[0].toInt(), it[1].toInt(), it[2], it[3].toInt(), it[4])
                    }
            it.onSuccess(albums)
        }

        single.subscribe(testObserver)

        testObserver.assertSubscribed()
        testObserver.assertComplete()

        testObserver.assertValue {
            it.size == 7
        }
    }

    @Test
    fun `loadingSongs`() {
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
            it.size.equals(76)
        }
    }
}