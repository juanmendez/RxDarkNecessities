import info.juanmendez.rxstories.model.Album
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class LoadingBandsTest {
    val totalSongs = 76

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
            it.size == totalSongs
        }
    }

    /**
     * Curiosity made me wonder about SchedulerTest
     * @see https://stackoverflow.com/questions/26699147/how-to-use-testscheduler-in-rxjava
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
        var startsAt = start
        var endsAt = end

        var songs: MutableList<Song> = file.readLines().drop(1)
                .map { it.split(",") }
                .map {
                    Song(it[0].toInt(), it[1], it[2], it[3].toInt(), it[4].toInt())
                }.toMutableList()

        //TODO make use of transformations to move the following logic to the chain of methods above
        endsAt = Math.min(end, songs.size - 1)
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
        Assert.assertTrue(songs.size == 5)
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

        var set = 0
        val songFlowable = Flowable.create<List<Song>>({ emitter ->
            emitter.onNext(getSongsByRange(set * setUnit, (set + 1 ) * setUnit))
            set++
        }, BackpressureStrategy.BUFFER)
                .delay(1, TimeUnit.SECONDS)





        songFlowable.subscribe(testSubscriber)

        val songsCollected = mutableListOf<Song>()
        for (i in 1..8) {

            testSubscriber.assertValue {
                songsCollected.addAll(it)
                songsCollected.size == Math.min(i * setUnit, totalSongs)
            }
            testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        }
    }
}