package book.chp3

import info.juanmendez.rxstories.model.Song
import org.junit.Test

class CollectingTest {

    @Test
    fun `lets play with scan`() {
        val test = SongUtil
                .getSongsObservable(0, 10)
                .map {
                    SongUtil.calculateTime(it)
                }.scan(0) { scanned, thisSongTime -> scanned + thisSongTime }
                .doOnNext { println(it) }
                .test()

        /** 0
        610
        732
        1952
        3843
        5612
        8113
        9577
        10980
        13847
        15982*/
        test.assertValueCount(11)
    }

    @Test
    fun `lets use instead reducer, which brings the total sum`() {
        val test = SongUtil
                .getSongsObservable(0, 10)
                .map {
                    SongUtil.calculateTime(it)
                }.reduce(0) { scanned, thisSongTime -> scanned + thisSongTime }
                .test()

        /**15982*/
        test.assertValueCount(1)
        test.assertValueAt(0) { it == 15982 }
    }


    @Test
    fun `lets try out collecting all songs`() {
        var test = SongUtil
                .getSongsObservable(0, 10)
                .collect({ mutableListOf<Song>() }, { list, song ->
                    list.add(song)
                }).test()

        test.assertComplete()
        test.assertValueCount(1)
        test.assertValueAt(0) { it.size == 10 }
    }

    @Test
    fun `lets collect all songs in one string`() {
        /**
        1,The Getaway,04:10:00,2,1
        2,Dark Necessities,05:02:00,2,1
        3,We Turn Red,03:20:00,2,1
        4,The Longest Wave,03:31:00,2,1
        5,Goodbye Angels,04:29:00,2,1
        6,Sick Love,03:41:00,2,1
        7,Go Robot,04:24:00,2,1
        8,Feasting on the Flowers,03:23:00,2,1
        9,Detroit,03:47:00,2,1
        10,This Ticonderoga,03:35:00,2,1
         */

        var test = SongUtil
                .getSongsObservable(0, 10)
                .collect({ StringBuilder() }, { stringBuilder, song ->
                    stringBuilder.append(" ${song.name}")
                }).test()

        /**
         * result
         * The Getaway Dark Necessities We Turn Red The Longest Wave Goodbye Angels Sick Love Go Robot Feasting on the Flowers Detroit This Ticonderoga
         */
    }
}