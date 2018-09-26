import info.juanmendez.rxstories.model.Album
import info.juanmendez.rxstories.model.Band
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subscribers.TestSubscriber
import org.junit.Test

class RxSequences {

    fun getAlbumsByBand(bandName: String) : Single<List<Album>> {

        return Single.defer {
            Single.create<List<Band>> {
                it.onSuccess(API.getBands())
            }.map {
                it.filter { band -> band.name == bandName }
            } .map {
                if(it.isNotEmpty()) {
                    it[0]
                } else {
                    throw Exception("Band is not found")
                }
            }.map {
                API.getAlbums().filter {
                    it.bandId == it.bandId
                }
            }
        }
    }

    @Test
    fun `filter albums by band`() {

        var testSubscriber = TestObserver<List<Album>>()

        getAlbumsByBand("Nirvana")
               .subscribe(testSubscriber)

        testSubscriber.assertValueCount(0)

        testSubscriber = TestObserver()
        getAlbumsByBand("Metallica")
                .subscribe(testSubscriber)

        testSubscriber.assertValueCount(1)
    }

    @Test
    fun `get distinct albums by band name`() {
       val distinctSubsciber = TestObserver<Album>()
        Observable.create<Album> { observable ->
           API.getAlbums().forEach { album ->
               observable.onNext(album)
           }
           observable.onComplete()
       }.distinct {
           it.bandId
       }.subscribe(distinctSubsciber)

        //out of 7 albums, one band owns two of them..
        distinctSubsciber.assertValueCount(6)
    }


    @Test
    fun `get number of bands which own more than one album`() {

        val distinctSubsciber = TestObserver<MutableMap<Int, MutableCollection<Album>>>()

        Observable.create<Album> { observable ->
            API.getAlbums().forEach { album ->
                observable.onNext(album)
            }
            observable.onComplete()
        }.toMultimap ({
            it.bandId
        }, {
            it
        }).subscribe(distinctSubsciber)

        //so Guns n' Roses has 2 albums, and other bands 1
        //so there are six bands represented.
        distinctSubsciber.assertValueCount(1)

        //lets assure it's Guns n' f*cking Roses!!
        distinctSubsciber.assertValueAt(0) { map ->
            var bandWithMostSongs: Int? = null

            map.forEach {
                if( it.value.size > map[bandWithMostSongs]?.size ?: 0){
                    bandWithMostSongs = it.key
                }
            }
             bandWithMostSongs == 1 && map.size == 6
        }
    }
}
