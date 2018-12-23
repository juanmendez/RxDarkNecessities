package chp1

import API
import info.juanmendez.rxstories.model.Song
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class WorkWithEmptyNever {
    private lateinit var testObserver: TestObserver<List<Song>>

    @Before
    fun `before`() {
        testObserver = TestObserver()
    }

    @Test
    fun `test empty observable`() {
        val emptyObservable = Observable.empty<List<Song>>()

        emptyObservable.subscribe(testObserver)
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `test never observable`() {
        val neverObservable  = Observable.never<List<Song>>()
        neverObservable.subscribe(testObserver)
        testObserver.assertNotComplete()
        testObserver.assertValueCount(0)
    }


    @Test
    fun `test error observable`() {
        val errorMessage = "not your day!"
        val errorObservable = Observable.error<List<Song>>( Throwable(errorMessage))
        errorObservable.subscribe(testObserver)
        testObserver.assertError {
            it.message == errorMessage
        }

        testObserver.assertNotComplete()
    }

    /**
     * An observable can be put to emit for every subscription
     * This uses a single, but the same happens with using a plain observable
     */
    @Test
    fun `test observable works multiple times per subscription`() {
        var timesParsingSongs = 0
        val songObservable = Single.create<List<Song>> {
            timesParsingSongs++
            it.onSuccess(API.getSongs())
        }

        songObservable.subscribe(testObserver)
        songObservable.subscribe({}, {})
        Assert.assertEquals(timesParsingSongs, 2)
    }

    /**
     * What if we want to only do this once? We can use cache instead!
     */

    @Test
    fun `test caching only parses songs once!`() {
        var timesParsingSongs = 0

        val songObserver = Single.create<List<Song>> {
            timesParsingSongs++
            it.onSuccess(API.getSongs())
        }.cache()

        songObserver.subscribe(testObserver)
        songObserver.subscribe({}, {})
        songObserver.subscribe({}, {})
        Assert.assertEquals(timesParsingSongs, 1)
    }
}