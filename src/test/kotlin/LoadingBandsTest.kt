import com.jakewharton.rxrelay2.PublishRelay
import info.juanmendez.rxstories.model.Album
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

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

    /**
     * Curiosity made me wonder about SchedulerTest
     * @see stackoverflow.com/questions/26699147/how-to-use-testscheduler-in-rxjava
     */
    @Test
    fun `test with TestScheduler`() {

        val testScheduler = TestScheduler()
        val testObserver = TestObserver<List<Song>>()
        val single = Single.create<List<Song>> {
            val file = File("csv/songs.csv")

            val songs: List<Song> = file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Song(it[0].toInt(), it[1], it[2], it[3].toInt(), it[4].toInt())
                    }
            it.onSuccess(songs)
        }

        //we delay 5 seconds to complete
        single.timeout(5, TimeUnit.SECONDS).observeOn(testScheduler).subscribe(testObserver)

        //at 0 seconds
        testObserver.assertNotComplete()
        testObserver.assertValue {
            it.isEmpty()
        }


        //at 6 seconds
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)
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

        //TODO make use of transformations to move the following logic to the chain of methods above
        endsAt = Math.min(end, songs.size)
        startsAt = Math.min(start, endsAt)

        if (endsAt == startsAt) {
            songs.clear()
        } else {
            songs = songs.subList(startsAt, endsAt)
        }

        return songs
    }


    @Test
    fun `emit 10 songs per second using getSongsByRange`() {
        //test songs by range, don't go too fast my friend
        var songs = getSongsByRange(80, 90)
        Assert.assertTrue(songs.isEmpty())

        songs = getSongsByRange(70, 77)

        //there are only 76 songs
        Assert.assertTrue(songs.size == 6)
    }

    @Test
    fun `use a Flowable emitter to spit 10 songs`() {
        val testSubscriber = TestSubscriber<List<Song>>()

        val songFlowable = Flowable.create<List<Song>>({ emitter ->
            emitter.onNext(getSongsByRange(0, 10))
        }, BackpressureStrategy.BUFFER)

        songFlowable.subscribe(testSubscriber)

        testSubscriber.assertValue {
            it.size == 10
        }
    }

    @Test
    fun `use a Flowable to spit 10 songs per second using TestScheduler`() {

        val testSubscriber = TestSubscriber<List<Song>>()
        val testScheduler = TestScheduler()
        val setUnit = 10

        Flowable.intervalRange(0, 8, 0, 1, TimeUnit.SECONDS, testScheduler)
                .map {
                    val set = it.toInt()
                    val listSongs = getSongsByRange(set * setUnit, (set + 1) * setUnit)
                    listSongs
                }.subscribe(testSubscriber)


        /**
         * every emit received by testSubscriber is added to its list of emits List<List<Song>>
         */
        for (i in 0..7) {
            testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

            testSubscriber.assertOf { subscriber ->
                subscriber.assertValueAt(i) {
                    if (i == 7) {
                        it.size == 6
                    } else {
                        it.size == 10
                    }
                }
            }
        }
    }


    @Test
    fun `get songs every five seconds`() {

        val songSubject = PublishSubject.create<List<Song>>()
        val testSubscriber = TestObserver<List<Song>>()
        val testScheduler = TestScheduler()

        val observable = songSubject.throttleFirst(5, TimeUnit.SECONDS)

        observable.subscribe(testSubscriber)

        //click every second
        songSubject.onNext(getSongsByRange(0, 10))
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        songSubject.onNext(getSongsByRange(0, 10))
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        songSubject.onNext(getSongsByRange(0, 10))
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        songSubject.onNext(getSongsByRange(0, 10))
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        testSubscriber.assertValueCount(1)
    }


    @Test
    fun `enforce to request songs every five seconds`() {
        /**
         * user is eager to get songs and clicks several times
         *  to pull the data, so this throttle enforces to execute only once
         */

        val songSubject = PublishSubject.create<List<Song>>()
        val requestSubject = PublishSubject.create<List<Int>>()

        val testSubscriber = TestObserver<List<Song>>()
        val testScheduler = TestScheduler()

        val requestSongs = { start: Int, end: Int ->
            requestSubject.onNext(listOf(start, end))
        }

        requestSubject.throttleFirst(5, TimeUnit.SECONDS).subscribe {
            if (it.size == 2) {
                songSubject.onNext(getSongsByRange(it[0], it[1]))
            }
        }

        songSubject.subscribe(testSubscriber)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        requestSongs(0, 10)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        requestSongs(10, 20)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        requestSongs(30, 40)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        requestSongs(50, 60)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        testSubscriber.assertValueCount(1)
    }


    @Test
    fun `using debounce`() {
        val songSubject = PublishSubject.timer(5, TimeUnit.SECONDS).timeInterval()

        val testSubscriber = TestObserver<List<Song>>()
        val testScheduler = TestScheduler()

        val requestSongs = { start: Int, end: Int ->
           songSubject.map {
               getSongsByRange(start, end)
           }
        }


        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        requestSongs(0, 10)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        requestSongs(10, 20)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        requestSongs(30, 40)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        requestSongs(50, 60)
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        testSubscriber.assertValueCount(1)
    }
}