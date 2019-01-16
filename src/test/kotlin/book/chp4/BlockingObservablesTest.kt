package book.chp4

import API
import info.juanmendez.rxstories.model.Song
import io.reactivex.Flowable
import junit.framework.Assert.assertTrue
import org.junit.Test

/**
 * BlockingObservables help us to work with observables in a non reactive world
 */
class BlockingObservablesTest {
    @Test
    fun `we require a list of songs, not an observable`() {

        //think you have to coexist with non reactive code
        //in this way we could pass the list itself to another class
        //or a third party library
        val flowableSong = coldSongFlowable()
        val singleSongList = flowableSong.toList()
        var songs = singleSongList.blockingGet()

        songs = coldSongFlowable().toList().blockingGet()
        assertTrue(songs is List<Song>)
    }

    @Test
    fun `embrace laziness`() {
        /**
         * So a hot observable, executes without subscription.
         * A cold observable executes only when there is a subsciption
         */

        //this is hot, and getSongs() is executed twice!
        hotSongs()
        hotSongs().subscribe()

        //this is cold, and getSongs() is executed once!
        coldSongFlowable()
        coldSongFlowable().subscribe()

        //wrapping hot observable in cold one and getSongs() is executed once!
        coldOverHotSongs()
        coldOverHotSongs().subscribe()
    }

    fun hotSongs() : Flowable<List<Song>> {
        return Flowable.just(getSongs("hotSongs()"))
    }

    fun coldSongFlowable(): Flowable<Song> {
        return Flowable.fromCallable {

            //fromCallable requires what's being return to be synchronous
            //for asynchronous execution, use Flowable.create{} instead
            getSongs("coldSongFlowable()")
        }.flatMapIterable { it }
    }

    fun coldOverHotSongs() : Flowable<List<Song>> {
        //Flowable.fromCallable returns a value
        //Flowable.defer returns an observable,
        // just like map vs flatmap operators

        return Flowable.defer {
            println("coldOverHotSongs()")
            hotSongs()
        }
    }

    fun getSongs(tag: String): List<Song> {
        println("$tag")
        return API.getSongs()
    }
}