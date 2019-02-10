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
        val test =
                getSongsIncrementally(1)
                        .collect({ mutableListOf<Song>() },
                                { concatList, list ->
                                    concatList.addAll(list)
                                }).test()

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
     *
     * Deferred is required, if not in place then every flow can execute at once.
     * getSongsByPage implements its own deferred operation.
     *
     * finished helps to keep track when the list gets empty
     */
    fun getSongsIncrementally(pageStartAt: Int, finished: Boolean = false): Flowable<MutableList<Song>> {
        var listEmpty = finished

        return getSongsByPage(pageStartAt)
                .doOnNext {
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