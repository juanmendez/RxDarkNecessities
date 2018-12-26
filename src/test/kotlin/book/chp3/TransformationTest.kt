package book.chp3

import API
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import java.io.File

class TransformationTest {
    @Test
    fun `lets replicate Api-getBands() with transformation`() {

        //* I didn't know for Single, Maybe, Observable, test() returns a testSubscriber
        //instead of assigning one when onSubscribe()
        val file = File("csv/bands.csv")

        val testSubscriber = Single.create<List<String>> {
            it.onSuccess(file.readLines())
        }.map {
            it.drop(1)
        }.map { linesOfStrings ->

            //these are Kotlin operators taken from the Api.getBands() method
            linesOfStrings.map {
                it.split(",")
            }.map {
                Band(it[0].toInt(), it[1])
            }.toList()
        }.test() //*

        testSubscriber.assertComplete()
    }

    @Test
    fun `lets use flat-map with observables`() {

        val bandsO = getBands()
        val bandsT = bandsO.test()
        bandsT.assertComplete()
        bandsT.assertValueAt(0) { it.isNotEmpty() }

        val favBandName = "Guns n’ Roses"
        val favBandT = bandsO
                .flatMap {
                    getBandsByName(it, favBandName)
                }.test()

        favBandT.assertComplete()
        favBandT.assertValueCount(1)
        favBandT.assertValueAt(0) { it.size == 1 }

        val yourBand = favBandT.values()[0][0]

        val songsO = getSongsObservable()
        val songsT = songsO.test()
        songsT.assertComplete()

        val favSongsO = songsO.flatMap {
            getSongsByBand(it, yourBand)
        }

        val favSongsT = favSongsO.test()
        favSongsT.assertComplete()
        favSongsT.assertValueAt(0) { it.isNotEmpty() }
    }

    @Test
    fun `lets combine all observables to get songs`() {
        val favBandName = "Guns n’ Roses"

        val songsO = getBands()
                .flatMap {
                    getBandsByName(it, favBandName)
                }.flatMap { filteredBands ->
                    if (filteredBands.isNotEmpty()) {
                        getSongsObservable().flatMap {
                            getSongsByBand(it, filteredBands[0])
                        }
                    } else {
                        Single.just(listOf<Song>())
                    }
                }

        val songsT = songsO.test()

        songsT.assertComplete()
        songsT.assertValueCount(1)
        songsT.assertValueAt(0) { it.isNotEmpty() }
    }

    @Test
    fun `lets turn songsO into a returned observable`() {
        var songsO = getBandSongs("Guns n’ Roses")
        var songsT = songsO.test()

        songsT.assertComplete()
        songsT.assertValueCount(1)
        songsT.assertValueAt(0) { it.isNotEmpty() }


        songsO = getBandSongs("")
        songsT = songsO.test()
        songsT.assertComplete()
        songsT.assertValueAt(0) { it.isEmpty() }
    }

    private fun getBandSongs(bandName: String): Single<List<Song>> {
        return getBands()
                .flatMap {
                    getBandsByName(it, bandName)
                }.flatMap { filteredBands ->
                    if (filteredBands.isNotEmpty()) {
                        getSongsObservable().flatMap {
                            getSongsByBand(it, filteredBands[0])
                        }
                    } else {
                        Single.just(listOf<Song>())
                    }
                }

    }

    private fun getBands(): Single<List<Band>> {

        return Single.create<List<Band>> {
            it.onSuccess(API.getBands())
        }
    }


    private fun getBandsByName(it: List<Band>, yourBandName: String): Single<MutableList<Band>> {
        return Observable
                .just(it)
                .flatMapIterable { it } //returns an Iterable sequence of values
                .filter { it.name == yourBandName }
                .toList() //returns Single<List<Observable> instance!
    }

    private fun getSongsObservable(): Single<List<Song>> {

        return Single.create<List<Song>> {
            it.onSuccess(API.getSongs())
        }
    }

    private fun getSongsByBand(it: List<Song>, yourBand: Band): Single<MutableList<Song>> {
        return Observable.just(it)
                .flatMapIterable { it }
                .filter { it.bandId == yourBand.bandId }
                .toList()
    }
}