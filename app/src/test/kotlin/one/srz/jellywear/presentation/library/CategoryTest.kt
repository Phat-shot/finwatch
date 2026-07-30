package one.srz.jellywear.presentation.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [Category.fromRoute] backs both navigation and the persisted
 * "visible categories" preference (AppPreferences stores routes as
 * strings), so unknown/changed routes must map to null -- that is what
 * silently drops stale entries from old app versions instead of crashing.
 */
class CategoryTest {

    @Test
    fun `every category resolves from its own route`() {
        assertEquals(Category.MUSIC, Category.fromRoute("music"))
        assertEquals(Category.AUDIO, Category.fromRoute("audio"))
        assertEquals(Category.SERIES, Category.fromRoute("series"))
        assertEquals(Category.MOVIES, Category.fromRoute("movies"))
        assertEquals(Category.FAVORITES, Category.fromRoute("favorites"))
        assertEquals(Category.PLAYLISTS, Category.fromRoute("playlists"))
    }

    @Test
    fun `round-trips through the route property`() {
        for (category in Category.entries) {
            assertEquals(category, Category.fromRoute(category.route))
        }
    }

    @Test
    fun `unknown route maps to null instead of throwing`() {
        assertNull(Category.fromRoute("podcasts"))
        assertNull(Category.fromRoute(""))
    }

    @Test
    fun `route matching is case-sensitive`() {
        // Routes are internal identifiers, always written lowercase; a
        // differently-cased value indicates corrupt/foreign data and must
        // not resolve.
        assertNull(Category.fromRoute("Music"))
        assertNull(Category.fromRoute("MUSIC"))
    }

    @Test
    fun `routes are unique -- lookup cannot be ambiguous`() {
        val routes = Category.entries.map { it.route }
        assertEquals(routes.size, routes.distinct().size)
    }
}
