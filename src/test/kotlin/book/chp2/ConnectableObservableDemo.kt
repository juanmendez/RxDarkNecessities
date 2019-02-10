package book.chp2

import API
import info.juanmendez.rxstories.model.Song
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.TestObserver
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test

class ConnectableObservableDemo {
    private lateinit var testSubscriber: TestObserver<List<Song>>
    private lateinit var composite: CompositeDisposable

    @Before
    fun `before`() {
        testSubscriber = TestObserver()
        composite = CompositeDisposable()
    }

    /**
     * With connected observable we can work just like with a regular observable.
     * Except, that instead of being a cold emitter happening in the first subscription,
     * it starts emitting until we call the shots with `connectedObservable.connect()`
     * This function returns a disposable which we can use to unsubscribe
     */
    @Test
    fun `collect first 20 songs`() {

        var timesParsingSongs = 0
        val songsTable = mutableListOf<Song>()
        val lateSubscriber = TestObserver<List<Song>>()

        val connectedObservable = Observable.create<List<Song>> {
            timesParsingSongs++
            println("start")
            it.onNext(API.getSongsByRange(0, 5))
            it.onNext(API.getSongsByRange(5, 10))
            it.onNext(API.getSongsByRange(10, 15))
            it.onNext(API.getSongsByRange(15, 20))
            println("completed")
        }.doOnNext {
            songsTable?.addAll(it)
        }.publish()

        composite.add(
                connectedObservable.subscribeWith(testSubscriber)
        )

        assertEquals(songsTable.size, 0)

        /**
         * observable emits until connect is called!
         */
        val ourDisposable = connectedObservable.connect()
        assertEquals(songsTable.size, 20)

        composite.add(
                connectedObservable.subscribeWith(lateSubscriber)
        )

        assertEquals(timesParsingSongs, 1)

        //so our late subscriber missed the data
        //according to documentation, it can only catch what's emitted after
        lateSubscriber.assertValueCount(0)
        testSubscriber.assertValueCount(4)

        //answering: https://stackoverflow.com/posts/53900418
        composite.clear()
        assertTrue(testSubscriber.isDisposed)
        assertTrue(lateSubscriber.isDisposed)
        assertFalse(composite.size() > 0)
    }


    @Test
    fun `test Defer over Just, cold wrapping hot`() {
        val lateSubscriber = TestObserver<List<Song>>()
        var songs : List<Song> = listOf()

        Observable.just(API.getSongs())

        Observable.defer {
            Observable.just(API.getSongs())
        }.subscribe(testSubscriber)
    }
}