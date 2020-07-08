package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteComparatorTest {

    private val routeComparator = RouteComparator()

    @Test
    fun `route with different geometry is new`() {
        val currentRoute = """indacAcvqniGkFBmJB{FJ_JrEqBbAoRvEwJdBmC\sJb@eEDkEI_Mc@oIUsEF_FTgFt@qHjBmFjB}ItEqItAkE~BeW`NcEbCsNjIcCaFq@wA"""
        val alternative = """{{dacAiaqniGdAlIjA^vC\nD?GwX\sFgH?oJ?yF\_JtEqBz@qRtEuJxBoC?uJ|@_E?mE?aM}@oI?sE?}E^cF\yHxBkFzA_JrEmIzAmExBcWfNeExCoNnHgCuEu@yA"""
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns currentRoute
            }
        }
        val directionsRoute: DirectionsRoute = mockk {
            every { geometry() } returns alternative
        }

        val isNewRoute = routeComparator.isNewRoute(routeProgress, directionsRoute)

        assertTrue(isNewRoute)
    }

    @Test
    fun `route with same geometry is not new`() {
        val currentRoute = """indacAcvqniGkFBmJB{FJ_JrEqBbAoRvEwJdBmC\sJb@eEDkEI_Mc@oIUsEF_FTgFt@qHjBmFjB}ItEqItAkE~BeW`NcEbCsNjIcCaFq@wA"""
        val alternative = """indacAcvqniGkFBmJB{FJ_JrEqBbAoRvEwJdBmC\sJb@eEDkEI_Mc@oIUsEF_FTgFt@qHjBmFjB}ItEqItAkE~BeW`NcEbCsNjIcCaFq@wA"""
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns currentRoute
            }
        }
        val directionsRoute: DirectionsRoute = mockk {
            every { geometry() } returns alternative
        }

        val isNewRoute = routeComparator.isNewRoute(routeProgress, directionsRoute)

        assertFalse(isNewRoute)
    }

    @Test
    fun `alternative is not new when alternative is empty`() {
        val currentGeometry = """indacAcvqniGkFBmJB{FJ_JrEqBbAoRvEwJdBmC\sJb@eEDkEI_Mc@oIUsEF_FTgFt@qHjBmFjB}ItEqItAkE~BeW`NcEbCsNjIcCaFq@wA"""
        val alternativeGeometry = ""
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns currentGeometry
            }
        }
        val directionsRoute: DirectionsRoute = mockk {
            every { geometry() } returns alternativeGeometry
        }

        val isNewRoute = routeComparator.isNewRoute(routeProgress, directionsRoute)

        assertFalse(isNewRoute)
    }

    @Test
    fun `routes with same beginning and end, can still be different`() {
        val currentGeometry = """e|h~bAslcjiGvBab@tHe}AnEs~@AqdAB{d@EsQCii@Ie\EeUFoh@Reg@nHwEbUsNjUuM`ZiQ|McI`e@eZbf@yX|[wRdCyAfYcQpYwPlVcOvn@y^nUqKfPwFnLeDvW{Hd^sKhr@kTzk@yd@jMuK|IqC`PrgAhM|u@nInm@pB`KnCjMbFzS`EtOrIl_@`T|{@rChMzL`i@jGbX|CzMtd@bjBbEfPdU~}@pFdTjAxDzH~XjGrSdOlc@xTvs@tUzt@jKl_@hP`d@bM``@tQ`h@vVlu@dYlw@pLl]nNrd@lApEtCdIpXpv@|AtExUpt@bXps@`Stk@lDfKlSbl@rv@~{BdD~HdIdMnOpQ|e@la@tt@|m@nEpDlItBxYhVlJ~H|P|L`LhI~SdL~FnBbXjElYbFlW`G|f@|Hpt@|J|Fm^vJs`@|Swk@jPee@j@_BpA{BrEaIfHcM~]vS`EbB"""
        val alternativeGeometry = """e|h~bAslcjiGvBab@tHe}AnEs~@AqdAB{d@EsQCii@Ie\EeUh`@}@ta@_@x\mAlE~YjA`IpAxGvCzSxG`c@lJjn@~AxKZvIIvDfYzSpAdAbHvFlLnEnJHvJEXtKnBdh@mCxV\~H|Jv`@zc@uKtZoIz]aKva@kKj]mIzOyDf_@mJvNcEbOqD`SmF~XiH\bOj@jXZnIX`Lt@jMt@fLnAlPn@bJ|BfS`Pg@nFOpDUde@iFbOk@fq@}BrRL|SCfBWfa@wU|[eR\SrOaJpJkFxTvs@tUzt@jKl_@hP`d@bM``@tQ`h@vVlu@dYlw@pLl]nNrd@lApEtCdIpXpv@|AtExUpt@bXps@`Stk@lDfKlSbl@rv@~{BdD~HdIdMnOpQ|e@la@tt@|m@nEpDlItBxYhVlJ~H|P|L`LhI~SdL~FnBbXjElYbFlW`G|f@|Hpt@|J|Fm^vJs`@|Swk@jPee@j@_BpA{BrEaIfHcM~]vS`EbB"""
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns currentGeometry
            }
        }
        val directionsRoute: DirectionsRoute = mockk {
            every { geometry() } returns alternativeGeometry
        }

        val isNewRoute = routeComparator.isNewRoute(routeProgress, directionsRoute)

        assertTrue(isNewRoute)
    }

    @Test
    fun `route progress should be on route`() {
        val currentGeometry = """}u`}bAwgejiGd@tDVdCzA~ItKxa@rIbXhH~W~@jD`CzClGbIbDlE|DpEhHvIlJ`KpKvLlQ|U`BjBza@|d@fA`BdClDda@zk@zA~Bp_@|k@lBxC`BdCzHrLpOjW~FxJbG`KjPfYlBzCdDjDt@rD`Ltk@zJ~e@vCnNlBtKxB|JdC~IvDdGlEdGvC`CrB`B`DpB|KjEtEl@fF?lBGdYkA~@KnAWdL{ErKyCxBe@hGY~BN`DvA|BnA`CvAbBxB`BbCvDxHjApC}TrPqDlEx@jPh@rc@DzCmAp^qFra@yIzd@gZpc@kH|K`EbB"""
        val alternativeGeometry = """mq||bAwm~iiGdS~TfA`BdClDda@zk@zA~Bp_@|k@lBxC`BdCzHrLpOjW~FxJbG`KjPfYlBzCdDjDt@rD`Ltk@zJ~e@vCnNlBtKxB|JdC~IvDdGlEdGvC`CrB`B`DpB|KjEtEl@fF?lBGdYkA~@KnAWdL{ErKyCxBe@hGY~BN`DvA|BnA`CvAbBxB`BbCvDxHjApC}TrPqDlEx@jPh@rc@DzCmAp^qFra@yIzd@gZpc@kH|K`EbB"""
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns currentGeometry
            }
        }
        val directionsRoute: DirectionsRoute = mockk {
            every { geometry() } returns alternativeGeometry
        }

        val isNewRoute = routeComparator.isNewRoute(routeProgress, directionsRoute)

        assertFalse(isNewRoute)
    }
}
