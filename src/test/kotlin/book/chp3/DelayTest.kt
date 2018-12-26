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
    fun `apply timer instead`() {
        /**
         * timer is similar except it is an observable which doesn't emit the
         * same value from a previous chained observable.
         * That's why we write it here first, instead of the way we used delay
         */
        val scheduler = TestScheduler()
        val songO = getSongsByRange(0, 10)
        val timeO = Observable.timer(10, TimeUnit.MILLISECONDS, scheduler)

        val timeSongO = timeO.flatMap { songO.toObservable() }

        val timeSongT = timeSongO.test()
        timeSongT.assertValueCount(0)

        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        timeSongT.assertValueCount(1)
        timeSongT.assertComplete()

        //TODO find how to emit each song every 10ms.
    }


    private fun getSongsByRange(start: Int, end: Int): Single<List<Song>> {
        return Single.just(API.getSongsByRange(start, end))
    }
}