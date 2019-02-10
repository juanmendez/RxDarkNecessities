package book.chp3

import API
import info.juanmendez.rxstories.model.Band
import info.juanmendez.rxstories.model.Song
import io.reactivex.Observable
import io.reactivex.Single

object SongUtil {

    fun getBands(): Single<List<Band>> {

        return Single.create<List<Band>> {
            it.onSuccess(API.getBands())
        }
    }


    fun getBandsByName(bands: List<Band>, queryName: String): Single<MutableList<Band>> {
        return Observable
                .just(bands)
                .flatMapIterable { it } //returns an Iterable sequence of values
                .filter { it.name == queryName }
                .toList() //returns Single<List<Observable> instance!
    }

    fun getSongsObservable(): Single<List<Song>> {

        return Single.create<List<Song>> {
            it.onSuccess(API.getSongs())
        }
    }

    fun getSongsSingle(start: Int, end: Int): Single<List<Song>> {
        return Single.create<List<Song>> {
            it.onSuccess(API.getSongsByRange(start, end))
        }
    }

    fun getSongsObservable(start: Int, end: Int): Observable<Song> {
        return getSongsSingle(start, end).toObservable().flatMapIterable { it }
    }

    fun getSongsByBand(songs: List<Song>, queryBand: Band): Single<MutableList<Song>> {
        return Observable.just(songs)
                .flatMapIterable { it }
                .filter { it.bandId == queryBand.bandId }
                .toList()
    }

    fun getBandSongs(bandName: String): Single<List<Song>> {
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

    //very rustic, but does the work..
    fun calculateTime(it: Song): Int {
        val songDuration = it.length
        val timeArray = songDuration.split(":")
        var totalTime = 0

        if (timeArray.size >= 4) {
            totalTime += timeArray[1].toInt() * 360
        }

        if (timeArray.size >= 3) {
            totalTime += timeArray[1].toInt() * 60
        }

        if (timeArray.size >= 2) {
            totalTime += timeArray[1].toInt()
        }

        return totalTime
    }
}