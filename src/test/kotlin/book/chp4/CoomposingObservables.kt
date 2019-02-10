package book.chp4

import API
import info.juanmendez.rxstories.model.Song
import io.reactivex.Flowable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class CoomposingObservables {
    companion object {
        const val PAGE_SIZE = 10
    }

    private lateinit var songsObservable: Single<List<Song>>

    @Before
    fun setUp() {
        //getAllSongs() emits once, and caches thereafter for future subscriptions
        songsObservable = getAllSongs().cache()
    }

    @Test
    fun `get all songs incrementally`() {
        val size = songsObservable.blockingGet().size

        //flowable emits multiple times, so we collect all values using `collect`
        //which by the ways turns the flowable into a single observable
        val test = getSongsIncrementally(1)
                        .collect({ mutableListOf<Song>() },
                                { concatList, list ->
                                    concatList.addAll(list)
                                }).test()

        //did we collect all songs?
        test.assertValue {
            it.size == size
        }
    }


    fun getAllSongs(): Single<List<Song>> {
        return Single.just(API.getSongs())
    }


    fun getSongsByPage(pageAt: Int): Flowable<MutableList<Song>> {
        return Flowable.defer {
            songsObservable
                    .toFlowable()
                    .flatMapIterable { it }
                    .skip((pageAt - 1) * PAGE_SIZE.toLong())
                    .take(PAGE_SIZE.toLong())
                    .toList()
                    .toFlowable()
        }
    }

    /**
     * Deferred is applied here and in getSongsByPage because these are hot observables
     * Which means they execute even if not being subscribed, and to avoid that we
     * use the `defer` operator to execute once the observable is part of the subscription
     */
    fun getSongsIncrementally(pageStartAt: Int, finished: Boolean = false): Flowable<MutableList<Song>> {
        var listEmpty = finished

        return getSongsByPage(pageStartAt)
                .doOnNext {
                    //we need to stop recursion, or it can get out of hands
                    listEmpty = it.isEmpty()
                }
                .concatWith(
                        if (!listEmpty) {
                            Flowable.defer {
                                getSongsIncrementally(pageStartAt + 1, listEmpty)
                            }
                        } else {
                            Flowable.just(mutableListOf())
                        }
                )
    }
}