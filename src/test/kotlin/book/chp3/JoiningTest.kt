package book.chp3

import API
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * we'll explore different ways of joining observables such as merging, combining latest, update when different
 */
class JoiningTest {

    @Test
    fun `lets zip different observables into one data structure`() {
        //lets join two single observables
        //it's a dumb demo, but it sure join them both!
        val zipObservable = SongUtil.getBands()
                .zipWith(
                        SongUtil.getSongsObservable(),
                        BiFunction<List<Band>, List<Song>, Pair<List<Band>, List<Song>>> { bands, songs ->
                            Pair(bands, songs)
                        })

        val test = zipObservable.test()
        test.assertComplete()
        test.assertValueCount(1)
    }

    @Test
    fun `lets test emitting by intervals`() {
        val totalSongs = API.getSongs().size
        val scheduler = TestScheduler()
        val test = getSongsByTimeIntervals(0, totalSongs, 10, 10L, scheduler).test()

        scheduler.advanceTimeTo(50000, TimeUnit.MILLISECONDS)
        test.assertComplete()
        test.assertValueAt(Math.ceil(totalSongs / 10.0).toInt() - 1) { it.size == totalSongs % 10 }
    }

    @Test
    fun `lets combine two streams using latestFrom`() {
        val totalSongs = API.getSongs().size
        val scheduler = TestScheduler()

        val o1 = getSongsByTimeIntervals(0, totalSongs / 2, 5, 10L, scheduler)
        val o2 = getSongsByTimeIntervals(totalSongs / 2, totalSongs, 10, 10L, scheduler)
        o2.delay(10, TimeUnit.MILLISECONDS, scheduler)

        val latestFrom = o1.withLatestFrom(o2, BiFunction<MutableList<Song>, MutableList<Song>, MutableList<Song>> { list1, list2 ->
            val mutableList = mutableListOf<Song>().apply {
                addAll(list1)
                addAll(list2)
            }

            mutableList
        }).collectInto(mutableListOf<Song>()) { collectedSongs, thisSongs ->
            collectedSongs.addAll(thisSongs)
        }

        val test = latestFrom.test()

        scheduler.advanceTimeTo(100, TimeUnit.MILLISECONDS)
        test.assertComplete()

        //see how songs are repeated due to one observable not emitting, and returning previous values emitted
    }


    @Test
    fun `lets combine two streams using concat`() {
        val totalSongs = API.getSongs().size
        val scheduler = TestScheduler()

        val o1 = getSongsByTimeIntervals(0, totalSongs / 2, 5, 10L, scheduler)
        val o2 = getSongsByTimeIntervals(totalSongs / 2, totalSongs, 10, 10L, scheduler)

        /**
         * in this case first observable has to emit them and complete, and then we jump to the next observable
         * therefore there are no items repeated
         */
        val latestFrom = o1.concatWith(o2)
                .collectInto(mutableListOf<Song>()) { collectedSongs, thisSongs ->
                    collectedSongs.addAll(thisSongs)
                }

        val test = latestFrom.test()

        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS)
        test.assertComplete()
    }


    @Test
    fun `lets combine two streams using switchIfEmpty`() {
        val totalSongs = API.getSongs().size
        val scheduler = TestScheduler()

        val o1 = getSongsByTimeIntervals(0, 0, 5, 10L, scheduler)
        val o2 = getSongsByTimeIntervals(0, 0, 5, 10L, scheduler)
        val o3 = getSongsByTimeIntervals(totalSongs / 2, totalSongs, 10, 10L, scheduler)


        /**
         * in this case first observable has to emit them and complete, and then we jump to the next observable
         * therefore there are no items repeated
         */
        val latestFrom = o1.switchIfEmpty(o2).switchIfEmpty(o3)
                .collectInto(mutableListOf<Song>()) { collectedSongs, thisSongs ->
                    collectedSongs.addAll(thisSongs)
                }

        val test = latestFrom.test()

        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS)
        test.assertComplete()
        test.assertValueCount(1)

        //only the second half is available
        test.assertValueAt(0) {it.size == totalSongs/2}
    }


    fun getSongsByTimeIntervals(start: Int, end: Int, howMany: Int, howOften: Long, scheduler: Scheduler): Observable<MutableList<Song>> {

        var startWith = start
        return Observable.interval(howOften, TimeUnit.MILLISECONDS, scheduler)
                .doAfterNext {
                    startWith += howMany
                }.flatMap {
                    SongUtil.getSongsSingle(startWith, Math.min(startWith + howMany, end)).toObservable()
                }.map {
                    it.toMutableList()
                }.takeWhile { startWith < end }
    }


}