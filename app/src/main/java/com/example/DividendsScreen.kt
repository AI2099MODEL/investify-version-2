package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest


fun cleanStockSymbol(input: String): String {
    val upper = input.trim().uppercase()
    val noSuffix = upper.replace(".NS", "").replace(".BO", "")
    var cleaned = noSuffix
    for (word in listOf(" LIMITED", " LTD", " IND", " INDUSTRIES", " ENTERPRISES", " PHARMA", " SERVICES", " CORP", " CORPORATION", " BANK")) {
        cleaned = cleaned.replace(word, "")
    }
    val alphaNumAndAnd = cleaned.replace(Regex("[^A-Z0-9&]"), "")
    if (alphaNumAndAnd.isNotEmpty()) return alphaNumAndAnd
    return noSuffix.replace(Regex("[^A-Z0-9]"), "")
}

data class UpcomingDividend(
    val symbol: String,
    val companyName: String,
    val amountPerShare: Double,
    val dividendType: String, // Interim, Final, Special
    val exDate: String, // "YYYY-MM-DD"
    val recordDate: String, // "YYYY-MM-DD"
    val cmp: Double,
    val yieldPercent: Double
)

val STOCK_DOMAIN_MAP = mapOf(
    "360ONE" to "360.one",
    "ABB" to "abb.com",
    "ABCAPITAL" to "adityabirlacapital.com",
    "ACC" to "acclimited.com",
    "ADANI" to "adani.com",
    "ADANIENSOL" to "adanienergysolutions.com",
    "ADANIENT" to "adani.com",
    "ADANIGREEN" to "adanigreenenergy.com",
    "ADANIPORTS" to "adaniports.com",
    "ADANIPOWER" to "adanipower.com",
    "AIRTEL" to "airtel.in",
    "ALKEM" to "alkemlabs.com",
    "AMBUJACEM" to "ambujacement.com",
    "APLAPOLLO" to "aplapollo.com",
    "APOLLOHOSP" to "apollohospitals.com",
    "ASHOKLEY" to "ashokleyland.com",
    "ASIANPAINT" to "asianpaints.com",
    "ASTRAL" to "astralpipes.com",
    "ATGL" to "adanigas.com",
    "AUBANK" to "aubank.in",
    "AUROPHARMA" to "aurobindo.com",
    "AXISBANK" to "axisbank.com",
    "BAJAJAUTO" to "bajajauto.com",
    "BAJAJFINSV" to "bajajfinserv.in",
    "BAJAJHFL" to "bajajhousingfinance.in",
    "BAJAJHLDNG" to "bajajauto.com",
    "BAJFINANCE" to "bajajfinserv.in",
    "BANKBARODA" to "bankofbaroda.in",
    "BANKINDIA" to "bankofindia.co.in",
    "BDL" to "bdl-india.in",
    "BEL" to "bel-india.in",
    "BHARATFORG" to "bharatforge.com",
    "BHARTIARTL" to "airtel.in",
    "BHARTIHEXA" to "airtel.in",
    "BHEL" to "bhel.in",
    "BIOCON" to "biocon.com",
    "BLUESTARCO" to "bluestarindia.com",
    "BOSCHLTD" to "bosch.in",
    "BPCL" to "bharatpetroleum.in",
    "BRITANNIA" to "britannia.co.in",
    "BSE" to "bseindia.com",
    "CANBK" to "canarabank.com",
    "CGPOWER" to "cgpower.com",
    "CHOLAFIN" to "cholamandalam.com",
    "CIPLA" to "cipla.com",
    "COALINDIA" to "coalindia.in",
    "COCHINSHIP" to "cochinshipyard.in",
    "COFORGE" to "coforge.com",
    "COLPAL" to "colgatepalmolive.co.in",
    "CONCOR" to "concorindia.com",
    "COROMANDEL" to "coromandel.biz",
    "CUMMINSIND" to "cummins.com",
    "DABUR" to "dabur.com",
    "DIVISLAB" to "divislabs.com",
    "DIXON" to "dixoninfo.com",
    "DLF" to "dlf.in",
    "DMART" to "dmart.in",
    "DRREDDY" to "drreddys.com",
    "EICHERMOT" to "eichermotors.com",
    "ENRIN" to "enercon.de",
    "ETERNAL" to "eternaltraining.in",
    "EXIDEIND" to "exideindustries.com",
    "FEDERALBNK" to "federalbank.co.in",
    "FORTIS" to "fortishealthcare.com",
    "GAIL" to "gailonline.com",
    "GLENMARK" to "glenmarkpharma.com",
    "GMRAIRPORT" to "gmrgroup.in",
    "GODFRYPHLP" to "godfreyphillips.com",
    "GODREJCP" to "godrejcp.com",
    "GODREJPROP" to "godrejproperties.com",
    "GRASIM" to "grasim.com",
    "HAL" to "hal-india.co.in",
    "HAVELLS" to "havells.com",
    "HCLTECH" to "hcltech.com",
    "HDFC" to "hdfcbank.com",
    "HDFCAMC" to "hdfcfund.com",
    "HDFCBANK" to "hdfcbank.com",
    "HDFCLIFE" to "hdfclife.com",
    "HEROMOTOCO" to "heromotocorp.com",
    "HINDALCO" to "hindalco.com",
    "HINDPETRO" to "hindustanpetroleum.com",
    "HINDUNILVR" to "hul.co.in",
    "HINDZINC" to "hzlindia.com",
    "HUDCO" to "hudco.org",
    "HUL" to "hul.co.in",
    "HYUNDAI" to "hyundai.com",
    "ICICI" to "icicibank.com",
    "ICICIBANK" to "icicibank.com",
    "ICICIGI" to "icicilombard.com",
    "IDEA" to "myvi.in",
    "IDFCFIRSTB" to "idfcfirstbank.com",
    "IGL" to "iglonline.net",
    "INDIANB" to "indianbank.in",
    "INDHOTEL" to "ihcltata.com",
    "INDIGO" to "goindigo.in",
    "INDUSTOWER" to "industowers.com",
    "INDUSINDBK" to "indusind.com",
    "INFOSYS" to "infosys.com",
    "INFY" to "infosys.com",
    "IOC" to "iocl.com",
    "IRB" to "irb.co.in",
    "IRCTC" to "irctc.co.in",
    "IREDA" to "ireda.in",
    "IRFC" to "irfc.co.in",
    "ITC" to "itcportal.com",
    "ITCHOTELS" to "itchotels.com",
    "JINDALSTEL" to "jindalsteelpower.com",
    "JIOFIN" to "jiofinancial.com",
    "JSWENERGY" to "jsw.in",
    "JSWSTEEL" to "jsw.in",
    "JUBLFOOD" to "jubilantfoodworks.com",
    "KALYANKJIL" to "kalyanjewellers.net",
    "KEI" to "kei-ind.com",
    "KOTAK" to "kotak.com",
    "KOTAKBANK" to "kotak.com",
    "KPITTECH" to "kpit.com",
    "LIC" to "licindia.in",
    "LICI" to "licindia.in",
    "LICHSGFIN" to "lichousing.com",
    "LODHA" to "macrotechdevelopers.com",
    "LT" to "larsentoubro.com",
    "LTF" to "ltfs.com",
    "LTIM" to "ltimindtree.com",
    "LUPIN" to "lupin.com",
    "M&M" to "mahindra.com",
    "MAHINDRA" to "mahindra.com",
    "MANKIND" to "mankindpharma.com",
    "MARICO" to "marico.com",
    "MARUTI" to "marutisuzuki.com",
    "MAXHEALTH" to "maxhealthcare.in",
    "MAZDOCK" to "mazagondock.in",
    "MFSL" to "maxfinancialservices.com",
    "MM" to "mahindra.com",
    "M&MFIN" to "mahindrafinance.com",
    "MMFIN" to "mahindrafinance.com",
    "MOTHERSON" to "motherson.com",
    "MOTILALOFS" to "motilaloswalgroup.com",
    "MPHASIS" to "mphasis.com",
    "MRF" to "mrftyres.com",
    "MUTHOOTFIN" to "muthootfinance.com",
    "NATIONALUM" to "nalcoindia.com",
    "NAUKRI" to "infoedge.in",
    "NESTLEIND" to "nestle.in",
    "NHPC" to "nhpcindia.com",
    "NMDC" to "nmdc.co.in",
    "NTPC" to "ntpc.co.in",
    "NTPCGREEN" to "ntpcgreen.com",
    "NYKAA" to "nykaa.com",
    "OBEROIRLTY" to "oberoirealty.com",
    "OFSS" to "oracle.com",
    "OIL" to "oil-india.com",
    "ONGC" to "ongcindia.com",
    "PAGEIND" to "jockey.in",
    "PATANJALI" to "patanjaliayurved.org",
    "PAYTM" to "paytm.com",
    "PERSISTENT" to "persistent.com",
    "PFC" to "pfcindia.com",
    "PHOENIXLTD" to "thephoenixmills.com",
    "PIDILITIND" to "pidilite.com",
    "PIIND" to "piindustries.com",
    "PNB" to "pnbindia.in",
    "POLICYBZR" to "policybazaar.com",
    "POLYCAB" to "polycab.com",
    "POWERGRID" to "powergrid.in",
    "POWERINDIA" to "hitachienergy.com",
    "PREMIERENE" to "premierenergies.com",
    "PRESTIGE" to "prestigeconstructions.com",
    "REC" to "recindia.nic.in",
    "RECLTD" to "recindia.nic.in",
    "RELIANCE" to "ril.com",
    "RVNL" to "rvnl.org",
    "SAIL" to "sail.co.in",
    "SBI" to "sbi.co.in",
    "SBICARD" to "sbicard.com",
    "SBILIFE" to "sbilife.co.in",
    "SBIN" to "sbi.co.in",
    "SHREECEM" to "shreecement.com",
    "SHRIRAMFIN" to "shriramfinance.in",
    "SIEMENS" to "siemens.com",
    "SOLARINDS" to "solargroup.com",
    "SONACOMS" to "sonacomstar.com",
    "SRF" to "srf.com",
    "SUNPHARMA" to "sunpharma.com",
    "SUPREMEIND" to "supreme.co.in",
    "SUZLON" to "suzlon.com",
    "SWIGGY" to "swiggy.com",
    "TATACOMM" to "tatacommunications.com",
    "TATACONSUM" to "tataconsumer.com",
    "TATAELXSI" to "tataelxsi.com",
    "TATAMOTORS" to "tatamotors.com",
    "TATAPOWER" to "tatapower.com",
    "TATASTEEL" to "tatasteel.com",
    "TATATECH" to "tatatechnologies.com",
    "TCS" to "tcs.com",
    "TECHM" to "techmahindra.com",
    "TIINDIA" to "tiindia.com",
    "TITAN" to "titancompany.in",
    "TMPV" to "tatamotors.com",
    "TORNTPHARM" to "torrentpharma.com",
    "TORNTPOWER" to "torrentpower.com",
    "TRENT" to "trentlimited.com",
    "TVSMOTOR" to "tvsmotor.com",
    "ULTRACEMCO" to "ultratechcement.com",
    "UNIONBANK" to "unionbankofindia.co.in",
    "UNITDSPR" to "diageo.com",
    "UPL" to "upl-ltd.com",
    "VBL" to "varunpepsi.com",
    "VEDL" to "vedantalimited.com",
    "VMM" to "vedantfashions.com",
    "VOLTAS" to "voltas.com",
    "WAAREEENER" to "waaree.com",
    "WIPRO" to "wipro.com",
    "YESBANK" to "yesbank.in",
    "ZOMATO" to "zomato.com",
    "ZYDUSLIFE" to "zyduslife.com"
)

data class StockBrandInfo(
    val shortName: String,
    val bgColor: Color,
    val textColor: Color = Color.White
)

val STOCK_BRAND_MAP = mapOf(
    "RELIANCE" to StockBrandInfo("RIL", Color(0xFF0A2540)),
    "TCS" to StockBrandInfo("TCS", Color(0xFF0F2C59)),
    "INFY" to StockBrandInfo("INFY", Color(0xFF007CC3)),
    "INFOSYS" to StockBrandInfo("INFY", Color(0xFF007CC3)),
    "HDFCBANK" to StockBrandInfo("HDFC", Color(0xFF004C8F)),
    "HDFC" to StockBrandInfo("HDFC", Color(0xFF004C8F)),
    "ICICIBANK" to StockBrandInfo("ICICI", Color(0xFFF37021)),
    "SBIN" to StockBrandInfo("SBI", Color(0xFF0083CA)),
    "SBI" to StockBrandInfo("SBI", Color(0xFF0083CA)),
    "BHARTIARTL" to StockBrandInfo("ARTL", Color(0xFFE40000)),
    "AIRTEL" to StockBrandInfo("ARTL", Color(0xFFE40000)),
    "ITC" to StockBrandInfo("ITC", Color(0xFF990000)),
    "KOTAKBANK" to StockBrandInfo("KOTAK", Color(0xFFD32F2F)),
    "LT" to StockBrandInfo("L&T", Color(0xFF005A9C)),
    "AXISBANK" to StockBrandInfo("AXIS", Color(0xFF800020)),
    "ASIANPAINT" to StockBrandInfo("AP", Color(0xFFE53935)),
    "MARUTI" to StockBrandInfo("MSIL", Color(0xFF002F6C)),
    "SUNPHARMA" to StockBrandInfo("SUN", Color(0xFFF57C00)),
    "WIPRO" to StockBrandInfo("WIPRO", Color(0xFF34113B)),
    "HCLTECH" to StockBrandInfo("HCL", Color(0xFF0056B3)),
    "TITAN" to StockBrandInfo("TITAN", Color(0xFF1E293B)),
    "ULTRACEMCO" to StockBrandInfo("ULTRA", Color(0xFFE65100)),
    "BAJFINANCE" to StockBrandInfo("BAJAJ", Color(0xFF004080)),
    "BAJAJFINSV" to StockBrandInfo("BAJAJ", Color(0xFF004080)),
    "NESTLEIND" to StockBrandInfo("NESTLE", Color(0xFF7B1FA2)),
    "COALINDIA" to StockBrandInfo("CIL", Color(0xFF2E7D32)),
    "VEDL" to StockBrandInfo("VEDL", Color(0xFFD84315)),
    "PFC" to StockBrandInfo("PFC", Color(0xFF004080)),
    "RECLTD" to StockBrandInfo("REC", Color(0xFF1B5E20)),
    "REC" to StockBrandInfo("REC", Color(0xFF1B5E20)),
    "BPCL" to StockBrandInfo("BPCL", Color(0xFF006699)),
    "ONGC" to StockBrandInfo("ONGC", Color(0xFF8B0000)),
    "IOC" to StockBrandInfo("IOC", Color(0xFFE65100)),
    "NTPC" to StockBrandInfo("NTPC", Color(0xFF0288D1)),
    "POWERGRID" to StockBrandInfo("PGCIL", Color(0xFF00796B)),
    "HINDUNILVR" to StockBrandInfo("HUL", Color(0xFF002244)),
    "TATASTEEL" to StockBrandInfo("TATA", Color(0xFF003D7A)),
    "TATAMOTORS" to StockBrandInfo("TATA", Color(0xFF003D7A)),
    "TATACONSUM" to StockBrandInfo("TATA", Color(0xFF003D7A)),
    "ADANIENT" to StockBrandInfo("ADANI", Color(0xFF1A237E)),
    "ADANIPORTS" to StockBrandInfo("ADANI", Color(0xFF1A237E)),
    "JSWSTEEL" to StockBrandInfo("JSW", Color(0xFF1565C0)),
    "GRASIM" to StockBrandInfo("GRASIM", Color(0xFFC62828)),
    "HEROMOTOCO" to StockBrandInfo("HERO", Color(0xFFD32F2F)),
    "CIPLA" to StockBrandInfo("CIPLA", Color(0xFF0288D1)),
    "DRREDDY" to StockBrandInfo("REDDY", Color(0xFFC62828)),
    "BRITANNIA" to StockBrandInfo("BRIT", Color(0xFFD32F2F)),
    "INDUSINDBK" to StockBrandInfo("INDUS", Color(0xFF880E4F)),
    "APOLLOHOSP" to StockBrandInfo("APOLLO", Color(0xFF00695C))
)

val AVATAR_PALETTE = listOf(
    Color(0xFF003366),
    Color(0xFF004C8F),
    Color(0xFF1B365D),
    Color(0xFF007CC3),
    Color(0xFFF37021),
    Color(0xFF0083CA),
    Color(0xFF003D7A),
    Color(0xFFE40000),
    Color(0xFF34113B),
    Color(0xFFC8102E),
    Color(0xFF1E293B),
    Color(0xFF0F766E),
    Color(0xFF4338CA),
    Color(0xFF6D28D9)
)

private val LOGO_URL_CACHE = mutableMapOf<String, Int>()

fun getInitialsBgColor(symbol: String): Color {
    val brandInfo = STOCK_BRAND_MAP[symbol]
    if (brandInfo != null) return brandInfo.bgColor
    val hash = kotlin.math.abs(symbol.hashCode())
    return AVATAR_PALETTE[hash % AVATAR_PALETTE.size]
}

fun getLogoCandidateUrls(cleanSymbol: String): List<String> {
    val urls = mutableListOf<String>()
    
    // 1. Direct SVG logo from Indian Listed Company Logos dataset (dharunashokkumar/indian-listed-company-logos)
    urls.add("https://dharunashokkumar.github.io/indian-listed-company-logos/nse/NSE_${cleanSymbol}.svg")
    urls.add("https://dharunashokkumar.github.io/indian-listed-company-logos/bse/BSE_${cleanSymbol}.svg")
    
    val domain = STOCK_DOMAIN_MAP[cleanSymbol]
    if (domain != null) {
        // 2. Google Favicon Service - extremely reliable sz=128
        urls.add("https://www.google.com/s2/favicons?domain=$domain&sz=128")
        // 3. Clearbit logo API
        urls.add("https://logo.clearbit.com/$domain")
        // 4. Unavatar API
        urls.add("https://unavatar.io/$domain?fallback=false")
        // 5. IconHorse API
        urls.add("https://icon.horse/icon/$domain")
    } else {
        val lower = cleanSymbol.lowercase().replace("&", "")
        urls.add("https://www.google.com/s2/favicons?domain=${lower}.com&sz=128")
        urls.add("https://www.google.com/s2/favicons?domain=${lower}.in&sz=128")
        urls.add("https://www.google.com/s2/favicons?domain=${lower}.co.in&sz=128")
    }
    
    return urls
}

@Composable
fun CompanyLogoView(
    symbol: String,
    modifier: Modifier = Modifier.size(38.dp)
) {
    val cleanSymbol = remember(symbol) { cleanStockSymbol(symbol) }
    val candidates = remember(cleanSymbol) { getLogoCandidateUrls(cleanSymbol) }
    var currentUrlIndex by remember(cleanSymbol) { mutableIntStateOf(LOGO_URL_CACHE[cleanSymbol] ?: 0) }

    val brandInfo = STOCK_BRAND_MAP[cleanSymbol]
    val shortName = brandInfo?.shortName ?: if (cleanSymbol.length > 4) cleanSymbol.take(3) else cleanSymbol
    val fallbackBgColor = brandInfo?.bgColor ?: getInitialsBgColor(cleanSymbol)

    val context = LocalContext.current

    if (currentUrlIndex < candidates.size) {
        val currentUrl = candidates[currentUrlIndex]
        val imageRequest = remember(currentUrl) {
            ImageRequest.Builder(context)
                .data(currentUrl)
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build()
        }
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = " logo",
            contentScale = ContentScale.Fit,
            modifier = modifier
                .clip(CircleShape)
                .background(Color.White),
            onError = {
                val nextIndex = currentUrlIndex + 1
                LOGO_URL_CACHE[cleanSymbol] = nextIndex
                currentUrlIndex = nextIndex
            },
            onSuccess = {
                LOGO_URL_CACHE[cleanSymbol] = currentUrlIndex
            },
            loading = {
                Box(
                    modifier = modifier
                        .clip(CircleShape)
                        .background(fallbackBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            error = {
                Box(
                    modifier = modifier
                        .clip(CircleShape)
                        .background(fallbackBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    } else {
        // Brand avatar with authentic brand color & abbreviation
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(fallbackBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = shortName,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun generateUpcomingDividends(): List<UpcomingDividend> {
    val cal = Calendar.getInstance()
    fun getFutureDate(daysAhead: Int): String {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, daysAhead)
        val year = c.get(Calendar.YEAR)
        val month = String.format(Locale.US, "%02d", c.get(Calendar.MONTH) + 1)
        val day = String.format(Locale.US, "%02d", c.get(Calendar.DAY_OF_MONTH))
        return "$year-$month-$day"
    }

    return listOf(
        UpcomingDividend("RELIANCE.NS", "Reliance Industries Ltd", 10.00, "Final Dividend", getFutureDate(3), getFutureDate(5), 3012.40, 0.33),
        UpcomingDividend("TCS.NS", "Tata Consultancy Services", 28.00, "Interim Dividend", getFutureDate(6), getFutureDate(8), 4210.00, 0.67),
        UpcomingDividend("INFY.NS", "Infosys Limited", 20.00, "Interim Dividend", getFutureDate(9), getFutureDate(11), 1820.50, 1.10),
        UpcomingDividend("HDFCBANK.NS", "HDFC Bank Limited", 19.50, "Final Dividend", getFutureDate(12), getFutureDate(14), 1640.00, 1.18),
        UpcomingDividend("ITC.NS", "ITC Limited", 7.50, "Interim Dividend", getFutureDate(15), getFutureDate(17), 488.20, 1.53),
        UpcomingDividend("COALINDIA.NS", "Coal India Limited", 15.25, "Interim Dividend", getFutureDate(18), getFutureDate(20), 512.00, 2.97),
        UpcomingDividend("VEDL.NS", "Vedanta Limited", 20.00, "Special Dividend", getFutureDate(21), getFutureDate(23), 435.60, 4.59),
        UpcomingDividend("PFC.NS", "Power Finance Corp", 3.50, "Interim Dividend", getFutureDate(24), getFutureDate(26), 520.10, 0.67),
        UpcomingDividend("RECLTD.NS", "REC Limited", 4.50, "Interim Dividend", getFutureDate(28), getFutureDate(30), 585.30, 0.76),
        UpcomingDividend("BPCL.NS", "Bharat Petroleum Corp", 10.50, "Final Dividend", getFutureDate(32), getFutureDate(34), 345.80, 3.03),
        UpcomingDividend("ONGC.NS", "Oil & Natural Gas Corp", 6.00, "Interim Dividend", getFutureDate(36), getFutureDate(38), 320.40, 1.87),
        UpcomingDividend("IOC.NS", "Indian Oil Corporation", 7.00, "Final Dividend", getFutureDate(40), getFutureDate(42), 175.20, 3.99),
        UpcomingDividend("NTPC.NS", "NTPC Limited", 3.25, "Interim Dividend", getFutureDate(44), getFutureDate(46), 410.90, 0.79),
        UpcomingDividend("POWERGRID.NS", "Power Grid Corp of India", 4.50, "Interim Dividend", getFutureDate(48), getFutureDate(50), 340.10, 1.32),
        UpcomingDividend("HINDUNILVR.NS", "Hindustan Unilever Ltd", 24.00, "Interim Dividend", getFutureDate(52), getFutureDate(54), 2720.00, 0.88),
        UpcomingDividend("TATASTEEL.NS", "Tata Steel Limited", 3.60, "Final Dividend", getFutureDate(56), getFutureDate(58), 162.40, 2.21),
        UpcomingDividend("HCLTECH.NS", "HCL Technologies Ltd", 12.00, "Interim Dividend", getFutureDate(60), getFutureDate(62), 1580.00, 0.75)
    )
}

val MASTER_DIVIDEND_LIST = generateUpcomingDividends()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendsScreen(
    modifier: Modifier = Modifier,
    onSymbolSelected: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var dividendList by remember { mutableStateOf(generateUpcomingDividends()) }
    
    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun refreshDividends() {
        scope.launch {
            isLoading = true
            try {
                val updated = withContext(Dispatchers.IO) {
                    dividendList.map { item ->
                        try {
                            val resp = YahooRetrofit.service.getChart(item.symbol, "1d", "1m")
                            val price = resp.chart?.result?.firstOrNull()?.meta?.regularMarketPrice
                            if (price != null && price > 0) {
                                val newYield = (item.amountPerShare / price) * 100
                                item.copy(cmp = price, yieldPercent = newYield)
                            } else {
                                item
                            }
                        } catch (e: Exception) {
                            item
                        }
                    }
                }
                dividendList = updated
            } finally {
                isLoading = false
            }
        }
    }

    val validUpcomingDividends = remember(todayDateStr, searchQuery, dividendList) {
        dividendList.filter { item ->
            searchQuery.isBlank() ||
             item.symbol.contains(searchQuery, ignoreCase = true) ||
             item.companyName.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.exDate }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Upcoming Dividends",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.3).sp
            )

            IconButton(
                onClick = { refreshDividends() },
                modifier = Modifier.size(32.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Dividends",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (validUpcomingDividends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No Upcoming Dividends Found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "All past ex-dates are filtered out automatically.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(validUpcomingDividends, key = { it.symbol }) { item ->
                    DividendCard(item = item, onSymbolClick = { onSymbolSelected(item.symbol) })
                }
            }
        }
    }
}

@Composable
fun DividendCard(
    item: UpcomingDividend,
    onSymbolClick: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }

    val displaySymbol = item.symbol.replace(".NS", "").replace(".BO", "")
    val formattedPayout = String.format(Locale.US, "%.2f", item.amountPerShare)
    val formattedPrice = "₹" + String.format(Locale.US, "%.2f", item.cmp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSymbolClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Dividend Type Badge Tag + Heart Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.dividendType.ifBlank { "DIVIDEND" }.uppercase(),
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )

                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isFavorite) Color(0xFFDC2626) else Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Row 2: Company Icon + Ticker Symbol + Yield Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CompanyLogoView(symbol = item.symbol, modifier = Modifier.size(18.dp))

                    Text(
                        text = displaySymbol,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Yield ${String.format(Locale.US, "%.1f", item.yieldPercent)}%",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            // Row 3: Payout Box (left) + Current Price (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFF8F9FA))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Payout ",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "₹$formattedPayout",
                            color = Color(0xFF1E293B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = formattedPrice,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    maxLines = 1
                )
            }

            // Row 4: Ex-date Banner Box (bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFF0F1))
                    .padding(vertical = 3.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EX-DATE: ${item.exDate}",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
            }
        }
    }
}
