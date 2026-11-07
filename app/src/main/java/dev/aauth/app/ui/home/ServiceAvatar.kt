package dev.aauth.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Brand color for a well-known service. [slug] also names the optional logo drawable `svc_<slug>`. */
class ServiceBrand(val slug: String, val color: Color)

object ServiceBrands {
    private fun b(slug: String, rgb: Long) = ServiceBrand(slug, Color(0xFF000000 or rgb))

    // Longer names first, so a more specific name wins over a shorter one.
    private val brands: List<ServiceBrand> = listOf(
        b("github", 0x24292F), b("gitlab", 0xFC6D26), b("bitbucket", 0x0052CC),
        b("google", 0x4285F4), b("gmail", 0xEA4335), b("youtube", 0xFF0000),
        b("microsoft", 0x2F7FD3), b("outlook", 0x0078D4), b("azure", 0x0078D4),
        b("amazon", 0xFF9900), b("aws", 0xFF9900),
        b("apple", 0x1D1D1F), b("icloud", 0x3693F3),
        b("facebook", 0x1877F2), b("meta", 0x0866FF), b("instagram", 0xE4405F),
        b("twitter", 0x14171A), b("mastodon", 0x6364FF), b("bluesky", 0x1185FE),
        b("discord", 0x5865F2), b("telegram", 0x26A5E4), b("slack", 0x4A154B),
        b("reddit", 0xFF4500), b("linkedin", 0x0A66C2), b("twitch", 0x9146FF),
        b("steam", 0x171A21), b("epicgames", 0x2A2A2A),
        b("dropbox", 0x0061FF), b("notion", 0x1F1F1F), b("proton", 0x6D4AFF),
        b("cloudflare", 0xF38020), b("digitalocean", 0x0080FF), b("heroku", 0x430098),
        b("paypal", 0x003087), b("binance", 0xF0B90B), b("coinbase", 0x0052FF),
        b("kraken", 0x5741D9), b("bitwarden", 0x175DDC), b("lastpass", 0xD32D27),
        b("yandex", 0xFC3F1D), b("vk", 0x0077FF), b("mailru", 0x005FF9),
        b("ozon", 0x005BFF), b("sber", 0x21A038), b("tinkoff", 0xFFDD2D),
    ).sortedByDescending { it.slug.length }

    /** A one-letter name has to match exactly ("x" as a prefix would match half of the alphabet). */
    private val exact: Map<String, ServiceBrand> = mapOf("x" to b("x", 0x14171A))

    fun find(title: String): ServiceBrand? {
        val key = title.lowercase().filter { it.isLetterOrDigit() }
        if (key.isEmpty()) return null
        exact[key]?.let { return it }
        // Long names may appear anywhere ("GitHub Enterprise"); short ones must start the name,
        // otherwise "aws" would also match "Jaws".
        return brands.firstOrNull { if (it.slug.length >= 6) key.contains(it.slug) else key.startsWith(it.slug) }
    }
}

/**
 * Round avatar for an account. Known services get their brand color and, if a logo drawable named
 * `svc_<slug>` was added to the project (see docs/service-icons.md), the logo; everything else shows
 * the first letter on a tonal circle.
 */
@Composable
fun ServiceAvatar(title: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val brand = remember(title) { ServiceBrands.find(title) }
    val logoRes = remember(brand) {
        brand?.let { context.resources.getIdentifier("svc_${it.slug}", "drawable", context.packageName) } ?: 0
    }

    val background = brand?.color ?: MaterialTheme.colorScheme.primaryContainer
    val foreground = when {
        brand == null -> MaterialTheme.colorScheme.onPrimaryContainer
        background.luminance() > 0.5f -> Color.Black
        else -> Color.White
    }

    Surface(modifier = modifier.size(44.dp), shape = CircleShape, color = background) {
        Box(contentAlignment = Alignment.Center) {
            if (logoRes != 0) {
                Icon(
                    painter = painterResource(logoRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = foreground,
                )
            } else {
                Text(
                    text = title.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = foreground,
                )
            }
        }
    }
}
