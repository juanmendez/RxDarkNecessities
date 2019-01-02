package book.chp3

import API
import info.juanmendez.rxstories.model.Song
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * lets explore how to work with delaying values being emitted.
 * hint we have delay, and timer
 */
class DelayTest {
    private val time: Long = 10L

    @Test
    fun `applying timer`() {
        val scheduler = TestScheduler()

        val timeO = Observable.timer(10, TimeUnit.MILLISECONDS, scheduler)
        val songO = getSongsByRange(0, 10)
        val timeSongO = timeO.flatMap { songO.toObservable() }

        val timeSongT = timeSongO.test()
        timeSongT.assertValueCount(0)

        /**
         * delay precedes in first observable and then flatMap modifies
         * into observables which emit, and are merged (which happens within)
         * for subscriber
         */
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        timeSongT.assertValueCount(1)
        timeSongT.assertValueAt(0) { it.size == 10 }
        timeSongT.assertComplete()
    }

    @Test
    fun `apply delay operator`() {
        /**
         * delay makes an observable running on Scheduler.computationIO
         *
         * I have been used to subscribe with the `TestScheduler`, but for delay, we need
         * to provide it as third parameter.. No wonder why the test kept failing.
         */
        val scheduler = TestScheduler()
        val songsO = getSongsByRange(0, 10).delay(10, TimeUnit.MILLISECONDS, scheduler)

        val songsT = songsO.test()
        songsT.assertValueCount(0)

        scheduler.advanceTimeBy(11, TimeUnit.MILLISECONDS)
        songsT.assertValueCount(1)

        val songT = songsO.toObservable().flatMapIterable { it }.test()
        scheduler.advanceTimeBy(9, TimeUnit.MILLISECONDS)
        songT.assertValueCount(0)

        //once first observable emits after 10ms, then the following ones
        //emit 10 without any delays.
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        songT.assertValueCount(10)
    }

    @Test
    fun `emit reverse order using flatMap and delay`() {
        /**
         * According to the book FlatMap uses merging of observables generated
         * as a single subscription. We are going to take a look at how
         * applying delay to each song changes the order from first to last
         * when subscribed
         */
        val scheduler = TestScheduler()
        var songIndex = 0
        val songO = getSongsByRange(0, 10)
                .toObservable()
                .flatMapIterable { it }
                .sorted { prev, next ->
                    //ensure songs are sorted by their id!
                    when {
                        prev.songId < next.songId -> -1
                        prev.songId > next.songId -> 1
                        else -> 0
                    }
                }
                .flatMap { song ->
                    val delay = calculateDelay(songIndex++, 10)
                    getSongObservable(song, delay, scheduler)
                }

        val songT = songO.test()

        //songs are delayed, and emitted in the reverse order from last to first
        for (i in 0..9) {
            scheduler.advanceTimeBy((i + 1) * time, TimeUnit.MILLISECONDS)

            //we can test
            songT.assertValueAt(i) { it.songId == 10 - i }
        }
    }

    @Test
    fun `emit reverse order using concat and delay`() {
        val scheduler = TestScheduler()
        var songIndex = 0
        var timeToLoadThemAll = 0L
        var delay = 0L

        val songO = getSongsByRange(0, 10)
                .toObservable()
                .flatMapIterable { it }
                .sorted { prev, next ->
                    //ensure songs are sorted by their id!
                    when {
                        prev.songId < next.songId -> -1
                        prev.songId > next.songId -> 1
                        else -> 0
                    }
                }
                .doOnNext {
                    //good spot to do calculations while emitting each element
                    delay = calculateDelay(songIndex++, 10)
                    timeToLoadThemAll += delay
                }
                .concatMap { song ->
                    //lines within doOnNext moved here would make the test to fail
                    getSongObservable(song, delay, scheduler)
                }

        val songT = songO.test()

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        //we can test
        songT.assertValueAt(0) { it.songId == 1 }

        scheduler.advanceTimeTo(timeToLoadThemAll, TimeUnit.MILLISECONDS)
        songT.assertValueCount(10)
    }

    private fun calculateDelay(position: Int, total: Int): Long {
        return (total - position) * time
    }

    private fun getSongsByRange(start: Int, end: Int): Single<List<Song>> {
        return Single.just(API.getSongsByRange(start, end))
    }

    private fun getSongObservable(song: Song, time: Long, scheduler: TestScheduler): Observable<Song> {

        return Observable
                .timer(
                        time,
                        TimeUnit.MILLISECONDS,
                        scheduler
                )
                .map {
                    song
                }
    }
}