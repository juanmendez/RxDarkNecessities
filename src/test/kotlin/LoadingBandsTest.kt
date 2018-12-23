import com.jakewharton.rxrelay2.ReplayRelay
import info.juanmendez.rxstories.model.Album
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test

class LoadingBandsTest {
    private val totalSongs = 76

    @Test
    fun loadingBands() {
        val testObserver = TestObserver<List<Band>>()

        //lets load up with a single
        val single = Single.create<List<Band>> { emitter ->
            emitter.onSuccess(API.getBands())
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
            emitter.onSuccess(API.getAlbums())
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
            it.onSuccess(API.getSongs())
        }

        single.subscribe(testObserver)

        //assert it has completed, and there are 7 bands..
        testObserver.assertSubscribed()
        testObserver.assertComplete()

        testObserver.assertValue {
            it.size == totalSongs
        }
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
                    it.onSuccess(API.getSongsByRange(start, end))
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