import com.jakewharton.rxrelay2.ReplayRelay
import info.juanmendez.rxstories.model.Album
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit

class RxSequences {
    @Test
    fun `fromIterable(list)`() {
        val bands = API.getBands()
        val testSubscriber = TestObserver<Band>()

        Observable.fromIterable(bands).subscribe(testSubscriber)
        testSubscriber.assertComplete()
        testSubscriber.assertOf {
            it.valueCount().equals(bands.size)
        }
    }

    @Test
    fun defer() {
        /**
         * do not create the Observable until the observer subscribes, and create a fresh Observable for each observer
         * http://reactivex.io/documentation/operators/defer.html
         */
        val bands = API.getBands()
        val testSubscriber1 = TestObserver<List<Band>>()
        val testSubscriber2 = TestObserver<List<Band>>()

        val defer = Observable.defer {
            Observable.just(bands.subList(0, (0 until bands.size).shuffled().last()))
        }

        defer.subscribe(testSubscriber1)
        defer.subscribe(testSubscriber2)

        /**
         * even though both are subscribed to defer, they both are handling different observables.
         * testSubscriber1 has only one list, instead of 2
         */
        testSubscriber1.assertOf {
            it.valueCount() == 1
        }

        testSubscriber2.assertOf {
            it.valueCount() == 1
        }

        val result1 = testSubscriber1.values()[0]
        val result2 = testSubscriber2.values()[0]
        Assert.assertFalse("both subscribers are getting different results", result1.size == result2.size)
    }

    fun getSongsByRange(start: Int, end: Int): List<Song> {
        val songs = API.getSongs().toMutableList()
        val endsAt: Int = Math.min(end, songs.size)
        val startsAt: Int = Math.min(start, endsAt)

        //this was intended, just to test handling an exception
        if (start >= songs.size || end >= songs.size) {
            throw IndexOutOfBoundsException()
        }

        return songs.subList(startsAt, endsAt)
    }

    @Test
    fun `defer with success | error`() {
        val requestSongsByRange = fun(start: Int, end: Int) : Single<List<Song>> {
            return Single.create<List<Song>> {
                try {
                    it.onSuccess(getSongsByRange(start, end))
                } catch (e: Exception) {
                    it.onSuccess(listOf())
                }
            }
        }

        val testSubscriber = TestObserver<List<Song>>()
        requestSongsByRange(80, 90).subscribe(testSubscriber)
        testSubscriber.assertValueAt(0) {
            it.isEmpty()
        }
    }

    @Test
    fun `transition to observable`() {
        val testSubscriber = TestObserver<List<Album>>()
        val testScheduler = TestScheduler()

        //you can make use of another execution within.
        Single.timer(5, TimeUnit.SECONDS, testScheduler).map {
            API.getAlbums()
        }.subscribe(testSubscriber)

        testSubscriber.assertValueCount(0)
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueCount(1)
        testSubscriber.isTerminated

    }
}