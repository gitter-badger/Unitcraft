import org.junit.Before
import org.junit.Test
import unitcraft.server.Clock
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestBttler {

    lateinit var clock:Clock
    lateinit var now:Instant

    @Before fun before(){
        now = Instant.now()
        clock = Clock(Duration.ofMinutes(1), Duration.ofMinutes(2))
    }

    @Test fun start(){
        clock.start(now)
        assertEquals(clock.left,Duration.ofMinutes(1))
        assertTrue(!clock.stoped())

        clock.update(now+Duration.ofSeconds(30))
        assertEquals(clock.left,Duration.ofSeconds(30))
        assertTrue(!clock.stoped())
    }

    @Test fun elapse(){
        clock.start(now)
        clock.update(now+clock.left)
        assertTrue(clock.stoped())
    }

    @Test fun extend(){
        clock.extend()
        assertEquals(clock.left,Duration.ofMinutes(3))
    }

    @Test fun stop(){
        clock.start(now)
        clock.stop()
        assertTrue(clock.stoped())

        clock.update(now+Duration.ofMinutes(10))
        assertEquals(clock.left,Duration.ofMinutes(1))

        clock.start(now)
        assertFalse(clock.stoped())
    }

}