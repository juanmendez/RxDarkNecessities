package book.chp4

import API
import info.juanmendez.rxstories.model.Album
import info.juanmendez.rxstories.model.Band
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class FixingErrorResultsTest {
    private lateinit var albums: List<Album>
    private lateinit var bands: List<Band>

    @Before
    fun onBefore() {
        albums = API.getAlbums()
        bands = API.getBands()
    }

    @Test
    fun `fixing an error in an observable song`() {
        //we print each song from songObservable()
        //but one song will cause an error...
        //so we need to skip it!
        val albumsTest =
                getBand("Guns n’ Roses")
                        .flatMap { band ->
                            getAlbums(band, 1)
                        }.test()


        albumsTest.assertComplete()
        albumsTest.assertValueCount(1)

        //there are two GnR albums, one comes from the error
        albumsTest.assertValueAt(0) {
            it.size == 2 && it.filter { it.name.isEmpty() }.size == 1
        }
    }

    //an extra step, which can be ignored
    fun getBand(bandName: String): Single<Band> {
        return Observable
                .just(bands)
                .flatMapIterable { it }
                .filter {
                    it.name == bandName
                }.first(Band(1, bandName))
    }

    fun getAlbums(band: Band, makeErrorAt: Int): Single<MutableList<Album>> {
        var index = 0

        return Observable
                .just(albums)
                .flatMapIterable { it }
                .filter {
                    it.bandId == band.bandId
                }.map {
                    if (index == makeErrorAt) {
                        throw Throwable("Album issue at ${it.name}")
                    } else {
                        it
                    }

                }.doAfterNext {
                    index++
                }.onErrorResumeNext(
                        Observable.just(Album(0, 0, "", 2019, ""))
                ).toList()
    }
}