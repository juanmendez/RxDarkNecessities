import info.juanmendez.rxstories.model.Band
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import java.io.File

class LoadingBandsTest {
    @Test
    fun `loadFileWithRx`() {
        val testObserver = TestObserver<List<Band>>()

        //lets load up with a single
        val single = Single.create<List<Band>> {
            val file = File("csv/bands.csv")

            val bands: List<Band> = file.readLines().drop(1)
                    .map { it.split(",") }
                    .map {
                        Band(it[0].toInt(), it[1])
                    }
            it.onSuccess(bands)
        }

        single.subscribe(testObserver)

        //assert it has completed, and there are 7 bands..
        testObserver.assertSubscribed()
        testObserver.assertComplete()
        testObserver.assertOf {it.valueCount() == 7 }
    }
}